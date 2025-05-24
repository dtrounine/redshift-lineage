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