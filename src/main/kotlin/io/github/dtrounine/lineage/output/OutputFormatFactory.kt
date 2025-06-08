package io.github.dtrounine.lineage.output

import io.github.dtrounine.lineage.output.openlineage.OpenLineageOutputFormat
import io.github.dtrounine.lineage.output.simple_json.SimpleJsonOutputFormat
import io.github.dtrounine.lineage.output.yaml.YamlOutputFormat

const val OUTPUT_FORMAT_JSON = "json"
const val OUTPUT_FORMAT_OPENLINEAGE = "openlineage"
const val OUTPUT_FORMAT_YAML = "yaml"

object OutputFormatFactory {

    fun getAvailableFormats(): List<String> {
        return listOf(OUTPUT_FORMAT_JSON, OUTPUT_FORMAT_YAML, OUTPUT_FORMAT_OPENLINEAGE)
    }
    fun createOutputFormat(name: String): OutputFormat {
        return when (name.lowercase()) {
            OUTPUT_FORMAT_JSON -> SimpleJsonOutputFormat()
            OUTPUT_FORMAT_OPENLINEAGE -> OpenLineageOutputFormat()
            OUTPUT_FORMAT_YAML -> YamlOutputFormat()
            else -> throw IllegalArgumentException("Unsupported output format: $name")
        }
    }
}