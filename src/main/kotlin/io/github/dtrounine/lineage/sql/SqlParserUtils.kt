package io.github.dtrounine.lineage.sql

import io.github.dtrounine.lineage.sql.parser.generated.RedshiftSqlLexer
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.ast.Point
import java.io.File

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