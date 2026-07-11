package fowo.driver

import fowo.model.BuildSystem
import fowo.model.Dependency
import java.io.File
import java.nio.file.Path

class MakeDriver : BuildDriver {
    override val buildSystem = BuildSystem.MAKE

    override fun detect(sourceDir: Path): Boolean {
        return File(sourceDir.toFile(), "Makefile").exists()
    }

    override fun parseDependencies(sourceDir: Path): List<Dependency> {
        // Simple heuristic parsing or none for standard Makefiles without a specific standard
        return emptyList()
    }

    override fun build(sourceDir: Path, installPrefix: Path, extraEnv: Map<String, String>): BuildResult {
        val nproc = Runtime.getRuntime().availableProcessors()
        
        val cmds = listOf(
            listOf("make", "-j", "$nproc", "PREFIX=${installPrefix.toFile().absolutePath}"),
            listOf("asroot", "make", "install", "PREFIX=${installPrefix.toFile().absolutePath}")
        )
        
        for (cmd in cmds) {
            val asRoot = cmd.first() == "asroot"
            val pb = SandboxRunner.startSandboxed(cmd, sourceDir, extraEnv, asRoot)
            val proc = pb.start()
            val ret = proc.waitFor()
            if (ret != 0) {
                return BuildResult(false, proc.errorStream.bufferedReader().readText())
            }
        }
        return BuildResult(true)
    }
}
