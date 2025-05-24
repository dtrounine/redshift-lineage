package io.github.dtrounine.lineage.output

import io.github.dtrounine.lineage.model.LineageData
import java.io.OutputStream

class LineageOutputWriter(private val outputFormatName: String) {
    fun write(lineageData: LineageData, outputStream: OutputStream) {
        val outputFormat = OutputFormatFactory.createOutputFormat(outputFormatName)
        outputFormat.write(lineageData, outputStream)
    }
}