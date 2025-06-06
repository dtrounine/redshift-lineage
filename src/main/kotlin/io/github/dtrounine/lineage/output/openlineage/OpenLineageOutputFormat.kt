package io.github.dtrounine.lineage.output.openlineage

import io.github.dtrounine.lineage.model.LineageData
import io.github.dtrounine.lineage.model.LineageReport
import io.github.dtrounine.lineage.model.StatementLineageReport
import io.github.dtrounine.lineage.output.OutputFormat
import io.openlineage.client.OpenLineage
import io.openlineage.client.OpenLineage.RunEvent
import io.openlineage.client.OpenLineageClient
import java.io.OutputStream
import java.net.URI
import java.time.ZonedDateTime

class OpenLineageOutputFormat: OutputFormat("openlineage") {
    override fun write(lineageReport: LineageReport, output: OutputStream) {
        val ol = OpenLineage(URI("https://openlineage.io/spec/1-0-2/OpenLineage.json"))
        val client = OpenLineageClient.builder()
            .transport(OutputStreamTransport(output))
            .build()

        lineageReport.statements.forEach {
            val events = getEventsForStatement(ol, it)
            events.forEach { event ->
                client.emit(event)
            }
        }
    }

    private fun getEventsForStatement(ol: OpenLineage, statement: StatementLineageReport): List<RunEvent> =
        statement.lineage.entries.map { (sink, sources) ->
            val olSources = sources.map { input ->
                ol.newInputDatasetBuilder()
                    .namespace("redshift://cluster.region:5439")
                    .name(input)
                    .build()
            }.toList()
            val olOutput = ol.newOutputDatasetBuilder()
                .namespace("redshift://cluster.region:5439")
                .name(sink)
                .build()
            val event = ol.newRunEventBuilder()
                .eventTime(ZonedDateTime.now())
                .eventType(OpenLineage.RunEvent.EventType.COMPLETE)
                .inputs(olSources)
                .outputs(listOf(olOutput))
                .build()
            event
        }
}
