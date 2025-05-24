package io.github.dtrounine.lineage.model

/**
 * Data class representing the lineage of data sources and sinks
 * discovered during the analysis.
 */
data class LineageData(
    /**
     * Map of sinks to their sources (sources may be empty).
     */
    val lineage: Map<String, Set<String>>,

    /**
     * Set of all discovered sources including those not used for any named sink.
     */
    val sources: Set<String>
) {
    companion object {
        fun newEmpty(): LineageData {
            return LineageData(
                lineage = emptyMap(),
                sources = emptySet()
            )
        }
    }
}
