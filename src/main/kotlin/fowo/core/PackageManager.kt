package fowo.core

import fowo.core.resolver.SatResolver
import kotlinx.coroutines.runBlocking

object PackageManager {
    fun install(url: String, name: String? = null, buildSystemHint: fowo.model.BuildSystem? = null) {
        val actualName = name ?: url.substringAfterLast("/").substringBefore(".git")
        
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
        val result = resolver.resolve(actualName, url, rootBuildSystemHint = buildSystemHint)
        
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
