package io.github.dtrounine.lineage.model

import kotlinx.serialization.Serializable

/**
 * Represents the lineage information extracted from some input by the lineage extractor tool.
 *
 * Note: this lineage information may relate to a single SQL statement or a whole SQL script, this is
 * determined by the context in which this information is used.
 */
@Serializable
data class LineageInfo(
    /**
     * Map of sinks to their sources (sources may be empty).
     */
    val lineage: Map<String, Set<String>>,

    /**
     * Set of all discovered sources including those not used for any named sink.
     */
    val sources: Set<String>,

    /**
     * Context information about the source of this lineage information, such as the file name and position
     * in the source text.
     */
    val context: SourceContextInfo? = null
) {
    fun withContext(context: SourceContextInfo): LineageInfo {
        return LineageInfo(
            lineage = this.lineage,
            sources = this.sources,
            context = context
        )
    }

    companion object {
        fun newEmpty(): LineageInfo {
            return LineageInfo(
                lineage = emptyMap(),
                sources = emptySet(),
                context = null
            )
        }
    }
}
