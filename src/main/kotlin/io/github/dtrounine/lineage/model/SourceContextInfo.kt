package io.github.dtrounine.lineage.model

import kotlinx.serialization.Serializable

/**
 * Represents the context information of a source (typically, where the SQL script originates from),
 * including its name and position within the source.
 */
@Serializable
data class SourceContextInfo(
    /**
     * The name of the source, which could be a file name, URL, or any identifier for the source.
     *
     * NULLABLE: This field can be null if the source name is not available or not applicable.
     *           This is typically the case when the input was provided as an unnamed string or stream,
     *           e.g. from stdin.
     */
    val sourceName: String?,
    /**
     * The position in the source text where the SQL code corresponding to this context is located.
     */
    val positionInSource: SourcePosition?
)
