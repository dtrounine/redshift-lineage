package io.github.dtrounine.lineage.output

import io.github.dtrounine.lineage.model.LineageData
import io.github.dtrounine.lineage.model.LineageReport
import java.io.OutputStream
import java.io.PrintWriter

abstract class OutputFormat(open val name: String) {
    /**
     * Writes the given lineage data to the specified output.
     *
     * @param lineageData The lineage data to write.
     * @param output The output destination.
     */
    abstract fun write(lineageReport: LineageReport, output: OutputStream)
}