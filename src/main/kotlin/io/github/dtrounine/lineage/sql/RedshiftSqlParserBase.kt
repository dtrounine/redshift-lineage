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
