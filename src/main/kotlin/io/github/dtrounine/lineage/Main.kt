package io.github.dtrounine.lineage

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import io.github.dtrounine.lineage.output.LineageOutputWriter
import io.github.dtrounine.lineage.sql.parser.generated.RedshiftSqlLexer
import io.github.dtrounine.lineage.sql.parser.generated.RedshiftSqlParser
import io.github.dtrounine.lineage.sql.ast.AstParser
import io.github.dtrounine.lineage.sql.ast.Ast_Statement
import io.github.dtrounine.lineage.sql.parseRedshiftSqlToAst
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
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
            "json", "openlineage"
        ).default("json")

    override fun run() {
        getInputStream(inFile).use { inputStream ->
            getOutputStream(outFile).use { outputStream ->
                val statements = parseRedshiftSqlToAst(inputStream)
                val lineageData = TableLineageExtractor().getLineage(statements)
                LineageOutputWriter(outFormat).write(lineageData, outputStream)
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
