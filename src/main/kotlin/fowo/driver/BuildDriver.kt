package fowo.driver

import fowo.model.BuildSystem
import fowo.model.Dependency
import java.nio.file.Path

data class BuildResult(val success: Boolean, val errorOutput: String? = null)

interface BuildDriver {
    val buildSystem: BuildSystem

    /** Detect if this build system is used in the given source directory */
    fun detect(sourceDir: Path): Boolean

    /** Parse dependencies from build files */
    fun parseDependencies(sourceDir: Path): List<Dependency>

    /** Configure, build, and install the project */
    fun build(sourceDir: Path, installPrefix: Path, extraEnv: Map<String, String>): BuildResult
}
