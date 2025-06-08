package io.github.dtrounine.lineage.output.model

import kotlinx.serialization.Serializable

@Serializable
class LineageSink(
    val target: LineageReference,
    val sources: Set<LineageReference>,
)
