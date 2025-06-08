package io.github.dtrounine.lineage.output.simple_json

import io.github.dtrounine.lineage.output.OutputFormat
import io.github.dtrounine.lineage.output.model.LineageReport
import io.github.dtrounine.lineage.output.prettyJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream
import java.io.OutputStream

class SimpleJsonOutputFormat: OutputFormat("json") {
    /**
     * Writes the given lineage data to the specified output in a simple JSON format.
     *
     * @param lineageData The lineage data to write.
     * @param output The output destination.
     */
    @OptIn(ExperimentalSerializationApi::class)
    override fun write(lineageReport: LineageReport, output: OutputStream) {
        prettyJson.encodeToStream(lineageReport, output)
        output.flush()
    }
}