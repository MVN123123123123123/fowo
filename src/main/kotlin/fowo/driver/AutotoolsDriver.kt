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

    override fun build(sourceDir: Path, installPrefix: Path, extraEnv: Map<String, String>): BuildResult {
        val nproc = Runtime.getRuntime().availableProcessors()
        
        val cmds = mutableListOf<List<String>>()
        
        val hasConfigureAc = File(sourceDir.toFile(), "configure.ac").exists() || 
                             File(sourceDir.toFile(), "configure.in").exists()
        
        if (hasConfigureAc) {
            // Always regenerate build system from configure.ac/configure.in.
            // Pre-generated Makefiles often hardcode specific autotools versions
            // (e.g. aclocal-1.15) that may not be installed on the build host.
            cmds.add(listOf("autoreconf", "-fi"))
        } else if (!File(sourceDir.toFile(), "configure").exists()) {
            // No configure.ac/in and no configure script — nothing we can do
            return BuildResult(false, "No configure.ac, configure.in, or configure script found")
        }
        
        cmds.add(listOf("./configure", "--prefix=${installPrefix.toFile().absolutePath}"))
        cmds.add(listOf("make", "-j", "$nproc"))
        cmds.add(listOf("asroot", "make", "install"))
        
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
