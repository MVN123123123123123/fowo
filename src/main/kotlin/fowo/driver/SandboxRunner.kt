package fowo.driver

import java.io.File
import java.nio.file.Path

object SandboxRunner {
    /**
     * Replaces the command of the given ProcessBuilder with a bubblewrap (bwrap) invocation.
     * This creates a sandboxed environment where the host system is read-only, and only
     * the source directory is writable. Network is enabled.
     */
    fun wrap(command: List<String>, sourceDir: Path): List<String> {
        val bwrapCmd = mutableListOf(
            "bwrap",
            "--ro-bind", "/", "/",
            "--dev", "/dev",
            "--proc", "/proc",
            "--tmpfs", "/tmp",
            "--bind", sourceDir.toFile().absolutePath, sourceDir.toFile().absolutePath,
            "--unshare-all",
            "--share-net",
            "--new-session"
        )
        
        // Add the original command
        bwrapCmd.addAll(command)
        return bwrapCmd
    }

    /**
     * Creates and starts a sandboxed process.
     * Note: "asroot" commands shouldn't be sandboxed directly with unprivileged bwrap unless sudo is configured perfectly.
     * For installation steps using `asroot`, we might bypass the unprivileged sandbox, as they need real root to install to /usr/local.
     */
    fun startSandboxed(command: List<String>, sourceDir: Path, extraEnv: Map<String, String> = emptyMap(), asRoot: Boolean = false): ProcessBuilder {
        val finalCmd = if (asRoot) {
            // For installation, we use asroot (which runs sudo). We could wrap the bwrap inside asroot,
            // but typical install steps need write access to /usr/local. We will just run them asroot directly,
            // or pass appropriate binds to a root bwrap.
            // For simplicity and to ensure install works, we will execute asroot without bwrap for now,
            // as the build phase is where arbitrary makefiles run and need sandboxing.
            command
        } else {
            wrap(command, sourceDir)
        }

        val pb = ProcessBuilder(finalCmd)
        pb.directory(sourceDir.toFile())
        pb.environment().putAll(extraEnv)
        pb.inheritIO()
        return pb
    }
}
