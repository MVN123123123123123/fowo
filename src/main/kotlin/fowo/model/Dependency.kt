package fowo.model

import kotlinx.serialization.Serializable

@Serializable
data class Dependency(
    val name: String,
    val version: String? = null,
    val type: DepType = DepType.UNKNOWN
)

@Serializable
enum class DepType {
    SYSTEM, SOURCE, UNKNOWN
}
