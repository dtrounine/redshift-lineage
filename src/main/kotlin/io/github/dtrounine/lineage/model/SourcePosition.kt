package io.github.dtrounine.lineage.model

import kotlinx.serialization.Serializable

@Serializable
data class SourcePosition(val start: TextPosition, val stop: TextPosition)
