package fowo.model

import kotlinx.serialization.Serializable

@Serializable
enum class BuildSystem {
    CMAKE, MESON, AUTOTOOLS, CARGO, MAKE, UNKNOWN
}
