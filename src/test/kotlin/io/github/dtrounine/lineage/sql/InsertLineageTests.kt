package io.github.dtrounine.lineage.sql

import io.github.dtrounine.lineage.TableLineageExtractor
import io.github.dtrounine.lineage.model.LineageData
import org.junit.jupiter.api.Test

class InsertLineageTests {

    @Test
    fun testInsertLineage() {
        val sql = """
            INSERT INTO users (id, name, age)
            VALUES (1, 'Alice', 30), (2, 'Bob', 25);
        """.trimIndent()
        val statements = parseRedshiftSqlToAst(sql)
        val lineageData: LineageData = TableLineageExtractor().getLineage(statements)
        assert(lineageData.sources.isEmpty())
        assert(lineageData.lineage.size == 1)
        assert(lineageData.lineage.containsKey("users"))
        assert(lineageData.lineage["users"]!!.isEmpty())
    }
}