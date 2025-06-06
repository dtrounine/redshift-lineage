package io.github.dtrounine.lineage.model

import kotlinx.serialization.Serializable

@Serializable
data class TextPosition(
    val line: Int,
    val positionInLine: Int
)
