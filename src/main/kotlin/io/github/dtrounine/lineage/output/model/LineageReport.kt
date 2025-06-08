package io.github.dtrounine.lineage.output.model

import io.github.dtrounine.lineage.model.LineageInfo
import kotlinx.serialization.Serializable

/**
 * Represents the output lineage report produced by the lineage output writer.
 */
@Serializable
data class LineageReport(
    val statements: List<LineageReportEntry>
) {
    companion object {
        fun fromLineageInfo(lineageInfo: List<LineageInfo>): LineageReport = LineageReport(
            lineageInfo.map { LineageReportEntry(
                lineage = it.lineage.entries.map { entry ->
                    LineageSink(
                        target = LineageReference(entry.key),
                        sources = entry.value.map { LineageReference(it) }.toSet()
                    )
                },
                sources = it.sources.map { LineageReference(it) }.toSet(),
                context = it.context
            )}
        )
    }
}
