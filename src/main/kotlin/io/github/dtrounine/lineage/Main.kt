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
