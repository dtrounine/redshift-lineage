package io.github.dtrounine.lineage.output

import io.github.dtrounine.lineage.output.model.LineageReport
import java.io.OutputStream

abstract class OutputFormat(open val name: String) {
    /**
     * Writes the given lineage data to the specified output.
     *
     * @param lineageReport The lineage data to write.
     * @param output The output destination.
     */
    abstract fun write(lineageReport: LineageReport, output: OutputStream)
}
