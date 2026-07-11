package fowo.core.resolver

import io.github.z4kn4fein.semver.Version
import java.io.File
import java.time.Instant

data class GitVersion(
    val tag: String,
    val semver: Version?,
    val commitHash: String,
    val commitTime: Instant
)

object VersionScanner {
    private val cacheDir = File(System.getProperty("user.home"), ".cache/fowo/sources")

    /**
     * Ensures the repository is cloned locally and fetches all tags with their timestamps.
     */
    fun scanVersions(name: String, repoUrl: String): List<GitVersion> {
        cacheDir.mkdirs()
        val sourceDir = File(cacheDir, name)

        if (!sourceDir.exists()) {
            println("==> Cloning $repoUrl to scan versions...")
            // Clone with full working tree (not --bare) so checkout works during build
            val proc = ProcessBuilder("git", "clone", repoUrl, sourceDir.absolutePath)
                .inheritIO().start()
            if (proc.waitFor() != 0) {
                System.err.println("Failed to clone $repoUrl")
                return emptyList()
            }
        } else {
            // Fetch updates
            ProcessBuilder("git", "fetch", "--tags", "--force")
                .directory(sourceDir).start().waitFor()
        }

        // Use for-each-ref to list tags.
        // %(objectname) gives the tag object hash for annotated tags, or the commit hash for lightweight tags.
        // %(*objectname) gives the dereferenced commit hash for annotated tags (empty for lightweight tags).
        // We use both and prefer the dereferenced hash when available.
        val proc = ProcessBuilder(
            "git", "for-each-ref", "--sort=-creatordate",
            "--format", "%(refname:short) %(objectname) %(*objectname) %(creatordate:unix)",
            "refs/tags"
        ).directory(sourceDir).start()

        // Read stdout BEFORE waitFor() to prevent deadlocks when output exceeds pipe buffer
        val output = proc.inputStream.bufferedReader().readText()
        proc.waitFor()

        val versions = mutableListOf<GitVersion>()
        for (line in output.lines()) {
            if (line.isBlank()) continue
            val parts = line.split(" ")
            if (parts.size >= 3) {
                val tag = parts[0]
                val objectHash = parts[1]
                val derefHash = parts[2]  // empty string for lightweight tags
                // Remaining parts are the timestamp
                val timeStr = parts.drop(3).joinToString(" ").trim()
                val timestamp = timeStr.toLongOrNull() ?: 0L

                // For annotated tags, use the dereferenced commit hash; for lightweight tags, use objectname
                val commitHash = if (derefHash.isNotBlank()) derefHash else objectHash

                val parsedSemver = try {
                    Version.parse(tag.removePrefix("v"))
                } catch (e: Exception) {
                    null
                }

                versions.add(GitVersion(tag, parsedSemver, commitHash, Instant.ofEpochSecond(timestamp)))
            }
        }

        // Also add the HEAD/main branch as a fallback 'latest' version if no tags exist
        if (versions.isEmpty()) {
            val headProc = ProcessBuilder("git", "log", "-1", "--format=%H %ct").directory(sourceDir).start()
            // Read stdout before waitFor()
            val headOutput = headProc.inputStream.bufferedReader().readText().trim()
            headProc.waitFor()
            if (headOutput.isNotBlank()) {
                val p = headOutput.split(" ")
                if (p.size >= 2) {
                    val hash = p[0]
                    val ts = p[1].toLongOrNull() ?: 0L
                    versions.add(GitVersion("HEAD", null, hash, Instant.ofEpochSecond(ts)))
                }
            }
        }

        return versions
    }
}
