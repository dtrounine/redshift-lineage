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
package io.github.dtrounine.lineage.sql

import io.github.dtrounine.lineage.sql.ast.AstParser
import io.github.dtrounine.lineage.sql.ast.Ast_Statement
import io.github.dtrounine.lineage.sql.parser.generated.RedshiftSqlLexer
import io.github.dtrounine.lineage.sql.parser.generated.RedshiftSqlParser
import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.ast.Point
import java.io.File
import java.io.InputStream

fun logTokens(srcFile: File, stopPosition: Point?) {
    val charStream = CharStreams.fromPath(srcFile.toPath())
    val lexer = RedshiftSqlLexer(charStream)
    val tokens = CommonTokenStream(lexer)
    tokens.fill()
    for (token in tokens.tokens) {
        if (stopPosition != null
            && (token.line > stopPosition.line || token.line == stopPosition.line && token.charPositionInLine > stopPosition.column)) {
            break
        }
        token.line
        println("${lexer.vocabulary.getSymbolicName(token.type)}: ${token.text}")
    }
}

fun parseRedshiftSqlToAst(sql: InputStream): List<Ast_Statement> {
    val charStream = CharStreams.fromStream(sql)
    return parseRedshiftSqlToAst(charStream)
}
fun parseRedshiftSqlToAst(sql: String): List<Ast_Statement> {
    val charStream = CharStreams.fromString(sql)
    return parseRedshiftSqlToAst(charStream)
}

private fun parseRedshiftSqlToAst(charStream: CharStream): List<Ast_Statement> {
    val lexer = RedshiftSqlLexer(charStream)
    val tokens = CommonTokenStream(lexer)
    val parser = RedshiftSqlParser(tokens)
    parser.buildParseTree = true

    val root: RedshiftSqlParser.RootContext = parser.root()
    return AstParser().parseRoot(root)
}