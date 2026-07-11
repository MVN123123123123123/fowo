package fowo.core.resolver

import fowo.model.Dependency
import fowo.parser.*
import java.io.File

object DependencyExtractor {
    private val cacheDir = File(System.getProperty("user.home"), ".cache/fowo/sources")

    /**
     * Extracts dependencies of a package at a specific commit hash by reading its build files from the bare repo.
     */
    fun extractDependenciesAt(pkgName: String, commitHash: String, buildSystemHint: fowo.model.BuildSystem? = null): List<Dependency> {
        val repoDir = File(cacheDir, pkgName)
        if (!repoDir.exists()) return emptyList()

        val allParsers = listOf(
            fowo.model.BuildSystem.CMAKE to ("CMakeLists.txt" to CmakeParser::parse),
            fowo.model.BuildSystem.MESON to ("meson.build" to MesonParser::parse),
            fowo.model.BuildSystem.AUTOTOOLS to ("configure.ac" to AutotoolsParser::parse),
            fowo.model.BuildSystem.CARGO to ("Cargo.toml" to CargoParser::parse)
        )

        // If a hint is provided, prioritize it and ONLY try that one.
        val parsersToCheck = if (buildSystemHint != null) {
            allParsers.filter { it.first == buildSystemHint }
        } else {
            allParsers
        }

        for ((_, parserInfo) in parsersToCheck) {
            val (fileName, parser) = parserInfo
            val content = getFileContent(repoDir, commitHash, fileName)
            if (content != null) {
                return parser(content)
            }
        }
        
        return emptyList()
    }

    private fun getFileContent(repoDir: File, commitHash: String, fileName: String): String? {
        try {
            val proc = ProcessBuilder("git", "show", "$commitHash:$fileName")
                .directory(repoDir)
                .start()

            // Read stdout BEFORE waitFor() to prevent deadlocks when output exceeds pipe buffer
            val content = proc.inputStream.bufferedReader().readText()
            val ret = proc.waitFor()
            if (ret == 0) {
                return content
            }
        } catch (e: Exception) {
            // File not found or git error
        }
        return null
    }
}
