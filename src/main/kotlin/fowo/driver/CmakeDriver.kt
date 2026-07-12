package fowo.driver

import fowo.model.BuildSystem
import fowo.model.Dependency
import fowo.parser.CmakeParser
import java.io.File
import java.nio.file.Path

class CmakeDriver : BuildDriver {
    override val buildSystem = BuildSystem.CMAKE

    override fun detect(sourceDir: Path): Boolean {
        return File(sourceDir.toFile(), "CMakeLists.txt").exists()
    }

    override fun parseDependencies(sourceDir: Path): List<Dependency> {
        val cmakeFile = File(sourceDir.toFile(), "CMakeLists.txt")
        if (!cmakeFile.exists()) return emptyList()
        return CmakeParser.parse(cmakeFile.readText())
    }

    override fun build(sourceDir: Path, installPrefix: Path, extraEnv: Map<String, String>, configFlags: List<String>): BuildResult {
        val buildDir = File(sourceDir.toFile(), "build")
        buildDir.mkdirs()

        val nproc = Runtime.getRuntime().availableProcessors()
        
        val cmds = listOf(
            listOf("cmake", "-B", buildDir.absolutePath, "-S", sourceDir.toFile().absolutePath, "-DCMAKE_INSTALL_PREFIX=${installPrefix.toFile().absolutePath}") + configFlags,
            listOf("cmake", "--build", buildDir.absolutePath, "-j", "$nproc"),
            listOf("asroot", "cmake", "--install", buildDir.absolutePath)
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
