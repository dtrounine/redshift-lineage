package io.github.dtrounine.lineage.model

import kotlinx.serialization.Serializable

/**
 * Represents a position in the source text, defined by a starting and stopping point.
 * This is typically used to indicate where a specific piece of code or text is located within a larger source file.
 *
 * @property start The starting position of the text segment (inclusive).
 * @property stop The stopping position of the text segment (exclusive).
 */
@Serializable
data class SourcePosition(
    /**
     * The starting position of the text segment (inclusive).
     */
    val start: TextPosition,

    /**
     * The stopping position of the text segment (exclusive).
     */
    val stop: TextPosition
)
