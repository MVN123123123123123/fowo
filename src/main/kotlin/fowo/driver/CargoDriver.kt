package fowo.driver

import fowo.model.BuildSystem
import fowo.model.Dependency
import fowo.parser.CargoParser
import java.io.File
import java.nio.file.Path

class CargoDriver : BuildDriver {
    override val buildSystem = BuildSystem.CARGO

    override fun detect(sourceDir: Path): Boolean {
        return File(sourceDir.toFile(), "Cargo.toml").exists()
    }

    override fun parseDependencies(sourceDir: Path): List<Dependency> {
        val file = File(sourceDir.toFile(), "Cargo.toml")
        if (!file.exists()) return emptyList()
        return CargoParser.parse(file.readText())
    }

    override fun build(sourceDir: Path, installPrefix: Path, extraEnv: Map<String, String>): BuildResult {
        val pb = SandboxRunner.startSandboxed(listOf("cargo", "install", "--path", ".", "--root", installPrefix.toFile().absolutePath), sourceDir, extraEnv, asRoot = false)
        val proc = pb.start()
        val ret = proc.waitFor()
        if (ret != 0) {
            return BuildResult(false, proc.errorStream.bufferedReader().readText())
        }
        return BuildResult(true)
    }
}
