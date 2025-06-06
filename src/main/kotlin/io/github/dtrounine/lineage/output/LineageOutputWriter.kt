package io.github.dtrounine.lineage.output

import io.github.dtrounine.lineage.model.LineageData
import io.github.dtrounine.lineage.model.LineageReport
import java.io.OutputStream

class LineageOutputWriter(private val outputFormatName: String) {
    fun write(lineageReport: LineageReport, outputStream: OutputStream) {
        val outputFormat = OutputFormatFactory.createOutputFormat(outputFormatName)
        outputFormat.write(lineageReport, outputStream)
    }
}