package io.github.dtrounine.lineage.output

import io.github.dtrounine.lineage.output.openlineage.OpenLineageOutputFormat
import io.github.dtrounine.lineage.output.simple_json.SimpleJsonOutputFormat

const val OUTPUT_FORMAT_JSON = "json"
const val OUTPUT_FORMAT_OPENLINEAGE = "openlineage"

object OutputFormatFactory {

    fun getAvailableFormats(): List<String> {
        return listOf(OUTPUT_FORMAT_JSON, OUTPUT_FORMAT_OPENLINEAGE)
    }
    fun createOutputFormat(name: String): OutputFormat {
        return when (name.lowercase()) {
            OUTPUT_FORMAT_JSON -> SimpleJsonOutputFormat()
            OUTPUT_FORMAT_OPENLINEAGE -> OpenLineageOutputFormat()
            else -> throw IllegalArgumentException("Unsupported output format: $name")
        }
    }
}