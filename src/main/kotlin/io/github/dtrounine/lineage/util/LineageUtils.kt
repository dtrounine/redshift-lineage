package io.github.dtrounine.lineage.util

import io.github.dtrounine.lineage.model.LineageData

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
 * Merges two [LineageData] instances by combining their lineage maps and their lists of sources.
 */
fun LineageData.mergeAll(other: LineageData): LineageData = LineageData(
    lineage = mergeLineage(this.lineage, other.lineage),
    sources = this.sources union other.sources
)

/**
 * Merges two [LineageData] instances by combining their lineage maps,
 * but does not merge the sources.
 */
fun LineageData.mergeOnlyLineage(other: LineageData): LineageData = LineageData(
    lineage = mergeLineage(this.lineage, other.lineage),
    sources = this.sources // Sources are not merged
)

fun LineageData.println() {
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
