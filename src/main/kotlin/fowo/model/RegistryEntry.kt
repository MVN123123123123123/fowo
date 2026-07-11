package fowo.model

import kotlinx.serialization.Serializable

@Serializable
data class RegistryEntry(
    val repoUrl: String,
    val branch: String? = null,
    val buildSystemHint: BuildSystem? = null,
    val configFlags: List<String> = emptyList()
)
