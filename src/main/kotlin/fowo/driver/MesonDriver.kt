package fowo.driver

import fowo.model.BuildSystem
import fowo.model.Dependency
import fowo.parser.MesonParser
import java.io.File
import java.nio.file.Path

class MesonDriver : BuildDriver {
    override val buildSystem = BuildSystem.MESON

    override fun detect(sourceDir: Path): Boolean {
        return File(sourceDir.toFile(), "meson.build").exists()
    }

    override fun parseDependencies(sourceDir: Path): List<Dependency> {
        val mesonFile = File(sourceDir.toFile(), "meson.build")
        if (!mesonFile.exists()) return emptyList()
        return MesonParser.parse(mesonFile.readText())
    }

    override fun build(sourceDir: Path, installPrefix: Path, extraEnv: Map<String, String>, configFlags: List<String>): BuildResult {
        val buildDir = File(sourceDir.toFile(), "build")
        
        val cmds = listOf(
            listOf("meson", "setup", "build", "--prefix=${installPrefix.toFile().absolutePath}") + configFlags,
            listOf("ninja", "-C", buildDir.absolutePath),
            listOf("asroot", "ninja", "-C", buildDir.absolutePath, "install")
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
