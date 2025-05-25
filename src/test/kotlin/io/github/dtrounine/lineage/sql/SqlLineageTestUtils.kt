package io.github.dtrounine.lineage.sql

import com.charleskorn.kaml.Yaml
import io.github.dtrounine.lineage.TableLineageExtractor
import io.github.dtrounine.lineage.model.LineageData
import kotlin.test.assertEquals

fun assertLineage(sql: String, expectedLineageYaml: String) {
    val expectedLineage = Yaml.default.decodeFromString(LineageData.serializer(), expectedLineageYaml.trimIndent())
    val statements = parseRedshiftSqlToAst(sql)
    val lineageData: LineageData = TableLineageExtractor().getLineage(statements)
    assertEquals(expectedLineage, lineageData)
}