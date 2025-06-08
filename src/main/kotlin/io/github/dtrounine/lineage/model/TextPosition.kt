package io.github.dtrounine.lineage.model

import kotlinx.serialization.Serializable

/**
 * Represents a position in the text, defined by a line number and a position within that line.
 *
 * @property line The line number in the text (1-based index).
 * @property positionInLine The position within the line (0-based index).
 */
@Serializable
data class TextPosition(
    val line: Int,
    val positionInLine: Int
) : Comparable<TextPosition> {
    /**
     * Compares this TextPosition with another TextPosition.
     * The comparison is first done by line number, then by position within the line.
     */
    override fun compareTo(other: TextPosition): Int {
        return when {
            this.line != other.line -> this.line - other.line
            else -> this.positionInLine - other.positionInLine
        }
    }
}
