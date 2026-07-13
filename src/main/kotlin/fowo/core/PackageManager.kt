package fowo.core

import fowo.core.resolver.SatResolver
import kotlinx.coroutines.runBlocking

object PackageManager {
    fun install(url: String, name: String? = null, buildSystemHint: fowo.model.BuildSystem? = null, branch: String? = null) {
        val actualName = name ?: url.substringAfterLast("/").substringBefore(".git")
        
        if (InstalledDatabase.isInstalled(actualName)) {
            System.err.println("Package $actualName is already installed via fowo.")
            return
        }
        if (SystemDepChecker.isInstalledViaDnf5(actualName)) {
            System.err.println("Package $actualName is already installed via dnf5.")
            return
        }

        println("==> Initializing root session for installation...")
        try {
            val proc = ProcessBuilder("asroot", "--init").inheritIO().start()
            if (proc.waitFor() != 0) {
                System.err.println("Authentication failed or aborted. Cannot proceed with installation.")
                return
            }
        } catch (e: Exception) {
            System.err.println("Failed to start asroot. Make sure it is installed and in your PATH.")
            return
        }
        
        println("==> Starting dependency resolution for $actualName...")
        val resolver = SatResolver()
        val result = resolver.resolve(actualName, url, rootBranchOrTag = branch, rootBuildSystemHint = buildSystemHint)
        
        if (result == null) {
            System.err.println("Failed to resolve dependencies for $actualName.")
            return
        }
        
        println("==> Resolution complete. Packages to build:")
        for ((pkg, gitVer) in result.versions) {
            println("    $pkg @ ${gitVer.tag} (${gitVer.commitHash})")
        }
        
        // Execute DAG in parallel, passing edges and root hint
        runBlocking {
            PackageBuilder.buildGraph(result.versions, result.edges, rootPkg = actualName, rootHint = buildSystemHint)
        }
    }
}
