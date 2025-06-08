package io.github.dtrounine.lineage.output.model

import kotlinx.serialization.Serializable

/**
 * Represents a reference in the output lineage report.
 * A reference is typically a table name or a similar identifier.
 */
@Serializable
data class LineageReference(val name: String)
