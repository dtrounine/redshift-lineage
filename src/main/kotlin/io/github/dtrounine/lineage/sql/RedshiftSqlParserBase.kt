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

import io.github.dtrounine.lineage.sql.parser.generated.RedshiftSqlLexer
import io.github.dtrounine.lineage.sql.parser.generated.RedshiftSqlParser
import org.antlr.v4.kotlinruntime.*

abstract class RedshiftSqlParserBase(input: TokenStream) : Parser(input) {

    fun GetParsedSqlTree(script: String, line: Int): ParserRuleContext {
        val ph: RedshiftSqlParser = GetRedshiftSqlParser(script)
        val result: ParserRuleContext = ph.root()
        return result
    }


    private fun TrimQuotes(s: String?): String? {
        return if (s.isNullOrEmpty()) s else s.substring(1, s.length - 1)
    }

    fun unquote(s: String?): String {
        val slength = s!!.length
        val r = StringBuilder(slength)
        var i = 0
        while (i < slength) {
            val c = s[i]
            r.append(c)
            if (c == '\'' && i < slength - 1 && (s[i + 1] == '\'')) i++
            i++
        }
        return r.toString()
    }

    fun GetRedshiftSqlParser(script: String): RedshiftSqlParser {
        val charStream: CharStream = CharStreams.fromString(script)
        val lexer: Lexer = RedshiftSqlLexer(charStream)
        val tokens: CommonTokenStream = CommonTokenStream(lexer)
        val parser: RedshiftSqlParser = RedshiftSqlParser(tokens)
        lexer.removeErrorListeners()
        parser.removeErrorListeners()
        return parser
    }

    fun OnlyAcceptableOps(): Boolean {
        val c: Token = (this._input as CommonTokenStream).LT(1) ?: return false
        val text: String = c.text ?: return false
        return (text == "!" || text == "!!" || text == "!=-")
    }
}
