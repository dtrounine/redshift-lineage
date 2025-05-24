package io.github.dtrounine.lineage.output.simple_json

import io.github.dtrounine.lineage.model.LineageData
import kotlinx.serialization.Serializable

/**
 * Represents a piece of Lineage information.
 *
 * For a given SQL query, it contains the lineage information, which includes the
 * output tables, that means the tables that are created or modified as a result of the query,
 * and their corresponding lineage information -- list of source tables that are used to
 * create or modify the output tables.
 */
@Serializable
data class LineageInfo(
    val lineage: List<LineageInfoEntry>
) {
    companion object {
        fun fromLineageData(lineageData: LineageData): LineageInfo {
            val entries: MutableList<LineageInfoEntry> = mutableListOf()
            lineageData.lineage.entries.forEach { (output, inputs) ->
                val entry = LineageInfoEntry(
                    outputs = setOf(LineageTable(output)),
                    inputs = inputs.map { LineageTable(it) }.toSet()
                )
                entries.add(entry)
            }
            return LineageInfo(entries)
        }
    }
}

@Serializable
data class LineageInfoEntry(
    val outputs: Set<LineageTable>,
    val inputs: Set<LineageTable>
)

@Serializable
data class LineageTable(
    val name: String
)