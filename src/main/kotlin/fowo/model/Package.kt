package fowo.model

import kotlinx.serialization.Serializable

@Serializable
data class Package(
    val name: String,
    val repoUrl: String,
    val clonePath: String,
    val buildSystem: BuildSystem,
    val dependencies: List<Dependency>,
    val version: String,
    val installedAt: Long
)
