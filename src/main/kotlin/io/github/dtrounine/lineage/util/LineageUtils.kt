package io.github.dtrounine.lineage.util

import io.github.dtrounine.lineage.model.LineageInfo
import io.github.dtrounine.lineage.model.SourcePosition
import io.github.dtrounine.lineage.model.TextPosition
import io.github.dtrounine.lineage.sql.ast.Ast_Node

fun mergeLineage(left: Map<String, Set<String>>, right: Map<String, Set<String>>): Map<String, Set<String>> {
    val merged: MutableMap<String, Set<String>> = mutableMapOf()

    for ((key, value) in left) {
        merged[key] = value.toMutableSet()
    }

    for ((key, value) in right) {
        merged.merge(key, value.toMutableSet()) { oldValue, newValue -> oldValue union newValue }
    }

    return merged
}

/**
 * Merges two [LineageInfo] instances by combining their lineage maps and their lists of sources.
 */
fun LineageInfo.mergeAll(other: LineageInfo): LineageInfo = LineageInfo(
    lineage = mergeLineage(this.lineage, other.lineage),
    sources = this.sources union other.sources,
    context = this.context
)

/**
 * Merges two [LineageInfo] instances by combining their lineage maps,
 * but does not merge the sources.
 */
fun LineageInfo.mergeOnlyLineage(other: LineageInfo): LineageInfo = LineageInfo(
    lineage = mergeLineage(this.lineage, other.lineage),
    sources = this.sources, // Sources are not merged,
    context = this.context
)

fun LineageInfo.println() {
    println("LineageInfo:")
    println("  lineage:")
    for ((key, value) in lineage) {
        println("    - $key -> ${ value.joinToString(", ")}")
    }
    println("  sources:")
    for (source in sources) {
        println("    - $source")
    }
}

fun sourcePositionFromAst(ast: Ast_Node): SourcePosition? {
    val sourcePosition: SourcePosition? = ast.context.start?.let { start ->
        ast.context.stop?.let { stop ->
            SourcePosition(
                start = TextPosition(start.line, start.charPositionInLine),
                stop = TextPosition(stop.line, stop.charPositionInLine + (stop.text?.length ?: 0))
            )
        }
    }
    return sourcePosition
}

fun mergeSourcePositions(
    left: SourcePosition?,
    right: SourcePosition?
): SourcePosition? {
    if (left == null) return right
    if (right == null) return left

    val start = minOf(left.start, right.start)
    val stop = maxOf(left.stop, right.stop)

    return SourcePosition(
        start = start,
        stop = stop
    )
}
