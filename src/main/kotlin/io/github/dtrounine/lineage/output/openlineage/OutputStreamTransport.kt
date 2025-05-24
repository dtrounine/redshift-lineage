package io.github.dtrounine.lineage.output.openlineage

import io.openlineage.client.OpenLineage
import io.openlineage.client.OpenLineageClientUtils
import io.openlineage.client.transports.Transport
import java.io.OutputStream

class OutputStreamTransport(out: OutputStream): Transport() {

    private val out = out.bufferedWriter()
    override fun emit(runEvent: OpenLineage.RunEvent) {
        emit(OpenLineageClientUtils.toJson(runEvent))
    }

    override fun emit(datasetEvent: OpenLineage.DatasetEvent) {
        emit(OpenLineageClientUtils.toJson(datasetEvent))
    }

    override fun emit(jobEvent: OpenLineage.JobEvent) {
        emit(OpenLineageClientUtils.toJson(jobEvent))
    }

    private fun emit(eventJson: String) {
        out.write(eventJson)
        out.write("\n")
        out.flush()
    }
}
