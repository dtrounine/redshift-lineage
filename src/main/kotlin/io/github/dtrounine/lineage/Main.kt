package io.github.dtrounine.lineage

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import io.github.dtrounine.lineage.model.LineageInfo
import io.github.dtrounine.lineage.model.SourceContextInfo
import io.github.dtrounine.lineage.model.SourcePosition
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
                val lineageInfos: List<LineageInfo> = if (splitStatements) {
                    statements.map { TableLineageExtractor().getLineage(it) }
                } else {
                    listOf(TableLineageExtractor().getAggregatedLineage(statements))
                }
                val lineageInfoWithContext = lineageInfos.map { lineageInfo ->
                    lineageInfo.withContext(
                        SourceContextInfo(
                            sourceName = inFile?.path,
                            positionInSource = lineageInfo.context?.positionInSource
                        )
                    )
                }
                LineageOutputWriter(outFormat).write(lineageInfoWithContext, outputStream)
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
