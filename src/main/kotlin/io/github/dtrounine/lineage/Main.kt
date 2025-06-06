package io.github.dtrounine.lineage

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import io.github.dtrounine.lineage.model.LineageReport
import io.github.dtrounine.lineage.model.SourcePosition
import io.github.dtrounine.lineage.model.StatementLineageReport
import io.github.dtrounine.lineage.model.TextPosition
import io.github.dtrounine.lineage.output.LineageOutputWriter
import io.github.dtrounine.lineage.output.OUTPUT_FORMAT_JSON
import io.github.dtrounine.lineage.output.OUTPUT_FORMAT_OPENLINEAGE
import io.github.dtrounine.lineage.output.OUTPUT_FORMAT_YAML
import io.github.dtrounine.lineage.sql.parseRedshiftSqlToAst
import java.io.File
import java.io.InputStream
import java.io.OutputStream


class RedLinCli: CliktCommand(
    name = "redshift-lineage",
    help = "Redshift SQL lineage extractor",
    printHelpOnEmptyArgs = false) {

    private val outFile: File? by
        option(
            names = arrayOf("--out-file"),
            help = "Pathname of the output file. Stdout by default."
        ).convert {
            File(it)
        }

    private val inFile: File? by
        option(
            names = arrayOf("--in-file"),
            help = "Pathname of the input file. Stdin by default."
        ).convert {
            File(it)
        }

    private val outFormat: String by
        option(
            names = arrayOf("--out-format"),
            help = "Output format. Default is json."
        ).choice(
            OUTPUT_FORMAT_JSON, OUTPUT_FORMAT_OPENLINEAGE, OUTPUT_FORMAT_YAML
        ).default("json")

    private val splitStatements: Boolean by
        option(
            names = arrayOf("--split-statements"),
            help = "Split the input into separate statements for processing. Output the lineage for each statement separately." +
                    " This option is disabled by default."
        )
        .flag(default = false)

    override fun run() {
        getInputStream(inFile).use { inputStream ->
            getOutputStream(outFile).use { outputStream ->
                val statements = parseRedshiftSqlToAst(inputStream)
                val lineageReport: LineageReport = if (splitStatements) {
                    // Process each statement separately
                    val statementReports = statements.map { statement ->
                        val lineageData = TableLineageExtractor().getLineage(statement)
                        val sourcePosition: SourcePosition? = statement.context.start?.let { start ->
                            statement.context.stop?.let { stop ->
                                SourcePosition(
                                    start = TextPosition(start.line, start.charPositionInLine),
                                    stop = TextPosition(stop.line, stop.charPositionInLine + (stop.text?.length ?: 0))
                                )
                            }
                        }
                        StatementLineageReport(lineageData.lineage, lineageData.sources, sourcePosition)
                    }
                    LineageReport(statementReports)
                } else {
                    // Process all statements at once
                    val lineageData = TableLineageExtractor().getLineage(statements)
                    LineageReport.fromLineageData(lineageData)
                }
                LineageOutputWriter(outFormat).write(lineageReport, outputStream)
            }
        }
    }
}

private fun getInputStream(inputFile: File?): InputStream {
    return inputFile?.inputStream() ?: System.`in`
}

private fun getOutputStream(outputFile: File?): OutputStream {
    return outputFile?.outputStream() ?: System.out
}

fun main(arg: Array<String>) = RedLinCli().main(arg)
