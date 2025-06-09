/**
 * MIT License with Commons Clause v1.0
 *
 * Copyright © 2025 Dmitrii Trunin (dtrounine@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software **without** restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished
 * to do so, **subject to** the following conditions:
 *
 * 1. The above copyright notice and this permission notice shall be included
 *    in all copies or substantial portions of the Software.
 *
 * 2. **Commons Clause License Condition v1.0**
 *    Without limiting other conditions in the MIT License, the grant of rights
 *    under the License will **not** include, and the License does not grant to you,
 *    the right to **Sell** the Software.
 *
 *    For purposes of this condition, **“Sell”** means practicing any or all of
 *    the rights granted to you under the MIT License to provide to third parties,
 *    for a fee or other consideration, a product or service whose value derives,
 *    entirely or substantially, from the functionality of the Software.
 *
 *    **This includes any service or software which, at any extent, provides**
 *    - data-lineage functionality, or
 *    - SQL-code-analysis functionality.
 *
 *    Any license notice or attribution required by the MIT License must also
 *    include this Commons Clause License Condition notice.
 *
 * 3. THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *    THE SOFTWARE.
 */
package io.github.dtrounine.lineage.output.openlineage

import io.github.dtrounine.lineage.output.OutputFormat
import io.github.dtrounine.lineage.output.model.LineageReport
import io.github.dtrounine.lineage.output.model.LineageReportEntry
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

    private fun getEventsForStatement(ol: OpenLineage, statement: LineageReportEntry): List<RunEvent> =
        statement.lineage.map { sink ->
            val olSources = sink.sources.map { input ->
                ol.newInputDatasetBuilder()
                    .namespace("redshift://cluster.region:5439")
                    .name(input.name)
                    .build()
            }.toList()
            val olOutput = ol.newOutputDatasetBuilder()
                .namespace("redshift://cluster.region:5439")
                .name(sink.target.name)
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
