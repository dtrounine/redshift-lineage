package io.github.dtrounine.lineage.model

import kotlinx.serialization.Serializable

@Serializable
data class StatementLineageReport(
    /**
     * Map of sinks to their sources (sources may be empty).
     */
    val lineage: Map<String, Set<String>>,

    /**
     * Set of all discovered sources including those not used for any named sink.
     */
    val sources: Set<String>,

    /**
     * The position in the source text where the SQL code corresponding to this lineage is located.
     */
    val sourcePosition: SourcePosition? = null,
)

/**
 * Lineage report containing the lineage of data sources and sinks.
 */
@Serializable
data class LineageReport(
    val statements: List<StatementLineageReport>
) {

    companion object {
        /**
         * Creates a new empty lineage report.
         */
        fun fromLineageData(lineageData: LineageData,
                            sourcePosition: SourcePosition? = null): LineageReport {
            val statement = StatementLineageReport(
                lineage = lineageData.lineage,
                sources = lineageData.sources,
                sourcePosition = sourcePosition
            )
            return LineageReport(
                statements = listOf(statement)
            )
        }
    }
}
