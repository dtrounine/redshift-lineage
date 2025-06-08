package io.github.dtrounine.lineage.output

import io.github.dtrounine.lineage.model.LineageInfo
import io.github.dtrounine.lineage.output.model.LineageReport
import java.io.OutputStream

class LineageOutputWriter(private val outputFormatName: String) {
    fun write(lineageInfos: List<LineageInfo>, outputStream: OutputStream) {
        val outputFormat = OutputFormatFactory.createOutputFormat(outputFormatName)
        outputFormat.write(LineageReport.fromLineageInfo(lineageInfos), outputStream)
    }
}