package io.github.dtrounine.lineage.output.model

import io.github.dtrounine.lineage.model.SourceContextInfo
import kotlinx.serialization.Serializable

@Serializable
data class LineageReportEntry(
    val lineage: List<LineageSink>,
    val sources: Set<LineageReference>,
    val context: SourceContextInfo?
)
