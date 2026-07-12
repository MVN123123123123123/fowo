package fowo.core

import fowo.core.resolver.GitVersion
import fowo.driver.*
import fowo.parser.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Path

object PackageBuilder {
    private val cacheDir = File(System.getProperty("user.home"), ".cache/fowo/sources")
    private val installPrefix = File("/usr/local")
    
    private val drivers = listOf(
        CmakeDriver(),
        MesonDriver(),
        AutotoolsDriver(),
        CargoDriver(),
        MakeDriver()
    )

    /**
     * Builds all resolved packages respecting the dependency DAG.
     * @param resolution Map of package name to its resolved GitVersion
     * @param edges Map of package name to list of source dependency names (from SAT resolver)
     */
    suspend fun buildGraph(resolution: Map<String, GitVersion>, edges: Map<String, List<String>>, rootPkg: String? = null, rootHint: fowo.model.BuildSystem? = null) = coroutineScope {
        val deferredMap = mutableMapOf<String, Deferred<Boolean>>()
        
        // Collect all system deps from edges info — any dependency that is NOT
        // a source dep (not in resolution) is treated as a system dep
        val systemDeps = mutableSetOf<String>()
        
        // The edges from the SAT resolver only contain source deps that are in resolution.
        // To find system deps, we re-check the parsed deps. But actually the SAT resolver
        // already filtered them out. We need the system deps that were skipped.
        // For now, let's extract system deps from the build files of each resolved package.
        for ((pkg, gitVer) in resolution) {
            val hint = if (pkg == rootPkg) rootHint else Registry.lookup(pkg)?.buildSystemHint
            val deps = fowo.core.resolver.DependencyExtractor.extractDependenciesAt(pkg, gitVer.commitHash, hint)
            for (dep in deps) {
                if (SystemDepChecker.isSystemDep(dep.name)) {
                    systemDeps.add(dep.name)
                }
            }
        }

        // 1. Install all system deps upfront to avoid dnf5 lock collisions
        for (sysDep in systemDeps) {
            if (!SystemDepChecker.isInstalledViaPkgConfig(sysDep)) {
                if (!SystemDepChecker.installSystemDep(sysDep)) {
                    System.err.println("Failed to install system dep $sysDep")
                    return@coroutineScope
                }
            }
        }

        // 2. Build the DAG of source dependencies using edges from the SAT resolver
        for ((pkg, _) in resolution) {
            scheduleBuild(pkg, resolution, edges, deferredMap, rootPkg, rootHint)
        }
        
        // Wait for all to finish
        var success = true
        for ((pkg, deferred) in deferredMap) {
            if (!deferred.await()) {
                System.err.println("Build failed for package $pkg")
                success = false
            }
        }
        
        if (success) {
            println("==> All packages built and installed successfully!")
        }
    }

    private fun kotlinx.coroutines.CoroutineScope.scheduleBuild(
        pkg: String,
        resolution: Map<String, GitVersion>,
        edges: Map<String, List<String>>,
        deferredMap: MutableMap<String, Deferred<Boolean>>,
        rootPkg: String?,
        rootHint: fowo.model.BuildSystem?
    ): Deferred<Boolean> {
        return deferredMap.getOrPut(pkg) {
            async {
                val deps = edges[pkg] ?: emptyList()
                // Wait for all dependencies to build first
                for (dep in deps) {
                    if (!resolution.containsKey(dep)) continue // skip if not in resolution
                    val depDeferred = scheduleBuild(dep, resolution, edges, deferredMap, rootPkg, rootHint)
                    if (!depDeferred.await()) {
                        return@async false
                    }
                }
                
                // Now build this package
                val gitVer = resolution[pkg]!!
                val hint = if (pkg == rootPkg) rootHint else Registry.lookup(pkg)?.buildSystemHint
                buildPackage(pkg, gitVer, hint)
            }
        }
    }

    private fun buildPackage(name: String, gitVer: GitVersion, hint: fowo.model.BuildSystem? = null): Boolean {
        println("==> Preparing $name at commit ${gitVer.commitHash}")
        val sourceDir = File(cacheDir, name)

        // Checkout the specific commit
        val proc = ProcessBuilder("git", "checkout", gitVer.commitHash).directory(sourceDir).inheritIO().start()
        if (proc.waitFor() != 0) {
            System.err.println("Failed to checkout ${gitVer.commitHash} for $name")
            return false
        }
        ProcessBuilder("git", "submodule", "update", "--init", "--recursive").directory(sourceDir).inheritIO().start().waitFor()

        // Detect driver
        val driver = if (hint != null) {
            drivers.find { it.buildSystem == hint }
        } else {
            drivers.find { it.detect(sourceDir.toPath()) }
        }
        if (driver == null) {
            System.err.println("Could not detect build system for $name")
            return false
        }

        println("==> Building $name using ${driver.buildSystem}...")
        val extraEnv = mapOf(
            "PKG_CONFIG_PATH" to "${installPrefix.absolutePath}/lib/pkgconfig:${installPrefix.absolutePath}/share/pkgconfig",
            "CMAKE_PREFIX_PATH" to installPrefix.absolutePath
        )

        val configFlags = Registry.lookup(name)?.configFlags ?: emptyList()
        val result = driver.build(sourceDir.toPath(), installPrefix.toPath(), extraEnv, configFlags)
        if (!result.success) {
            System.err.println("Build failed for $name:")
            System.err.println(result.errorOutput)
            return false
        }
        
        println("==> Installed $name successfully!")
        return true
    }
}
