package io.github.dtrounine.lineage.output.simple_json

import io.github.dtrounine.lineage.model.LineageData
import io.github.dtrounine.lineage.model.LineageReport
import io.github.dtrounine.lineage.model.SourcePosition
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
        fun fromLineageData(lineageReport: LineageReport): LineageInfo {
            val entries: MutableList<LineageInfoEntry> = mutableListOf()
            lineageReport.statements.forEach {
                val outputs: MutableSet<String> = mutableSetOf()
                val inputs: MutableSet<String> = mutableSetOf()
                it.lineage.entries.forEach { (entryOutput, entryInputs) ->
                    outputs.add(entryOutput)
                    inputs.addAll(entryInputs)
                }
                entries.add(LineageInfoEntry(
                    outputs.map { LineageTable(it) }.toSet(),
                    inputs.map { LineageTable(it) }.toSet(),
                    sourcePosition = it.sourcePosition
                ))
            }
            return LineageInfo(entries)
        }
    }
}

@Serializable
data class LineageInfoEntry(
    val outputs: Set<LineageTable>,
    val inputs: Set<LineageTable>,
    val sourcePosition: SourcePosition?
)

@Serializable
data class LineageTable(
    val name: String
)