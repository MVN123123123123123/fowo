package fowo.driver

import fowo.model.BuildSystem
import fowo.model.Dependency
import fowo.parser.AutotoolsParser
import java.io.File
import java.nio.file.Path

class AutotoolsDriver : BuildDriver {
    override val buildSystem = BuildSystem.AUTOTOOLS

    override fun detect(sourceDir: Path): Boolean {
        val dir = sourceDir.toFile()
        return File(dir, "configure.ac").exists() || 
               File(dir, "configure.in").exists() || 
               File(dir, "configure").exists()
    }

    override fun parseDependencies(sourceDir: Path): List<Dependency> {
        val file = File(sourceDir.toFile(), "configure.ac")
        if (!file.exists()) return emptyList()
        return AutotoolsParser.parse(file.readText())
    }

    override fun build(sourceDir: Path, installPrefix: Path, extraEnv: Map<String, String>, configFlags: List<String>): BuildResult {
        val nproc = Runtime.getRuntime().availableProcessors()
        
        val cmds = mutableListOf<List<String>>()
        
        val hasConfigureAc = File(sourceDir.toFile(), "configure.ac").exists() || 
                             File(sourceDir.toFile(), "configure.in").exists()
        
        if (hasConfigureAc) {
            // Always regenerate build system from configure.ac/configure.in if possible.
            val pb = SandboxRunner.startSandboxed(listOf("autoreconf", "-fi"), sourceDir, extraEnv, false)
            val proc = pb.start()
            val ret = proc.waitFor()
            if (ret != 0 && !File(sourceDir.toFile(), "configure").exists()) {
                return BuildResult(false, "autoreconf failed and no configure script exists. Check the console output above for details.")
            } else if (ret != 0) {
                println("Warning: autoreconf failed, but falling back to existing configure script.")
            }
        } else if (!File(sourceDir.toFile(), "configure").exists()) {
            // No configure.ac/in and no configure script — nothing we can do
            return BuildResult(false, "No configure.ac, configure.in, or configure script found")
        }
        
        cmds.add(listOf("./configure", "--prefix=${installPrefix.toFile().absolutePath}") + configFlags)
        cmds.add(listOf("make", "-j", "$nproc"))
        cmds.add(listOf("asroot", "make", "install"))
        
        for (cmd in cmds) {
            val asRoot = cmd.first() == "asroot"
            val pb = SandboxRunner.startSandboxed(cmd, sourceDir, extraEnv, asRoot)
            val proc = pb.start()
            val ret = proc.waitFor()
            if (ret != 0) {
                return BuildResult(false, "Command '${cmd.joinToString(" ")}' failed with exit code $ret. Check the console output above for details.")
            }
        }
        return BuildResult(true)
    }
}
