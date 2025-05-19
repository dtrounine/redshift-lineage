package io.github.dtrounine.lineage

import io.github.dtrounine.lineage.sql.parser.generated.RedshiftSqlLexer
import io.github.dtrounine.lineage.sql.parser.generated.RedshiftSqlParser
import io.github.dtrounine.lineage.sql.ast.AstParser
import io.github.dtrounine.lineage.util.println
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.ast.Point
import java.io.File

fun main() {
    val srcFile = File("src/test/resources/test-1.sql")

    logTokens(srcFile, stopPosition = Point(7, 30))

    val charStream = CharStreams.fromPath(srcFile.toPath())
    val lexer = RedshiftSqlLexer(charStream)
    val tokens = CommonTokenStream(lexer)
    val parser = RedshiftSqlParser(tokens)
    parser.buildParseTree = true

    val root: RedshiftSqlParser.RootContext = parser.root()

    val statements = AstParser().parseRoot(root)
    statements.forEach { statement ->
        println("Parsed statement: $statement")
    }

    val lineageInfo = TableLineageExtractor().getLineage(statements)
    lineageInfo.println()
}

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