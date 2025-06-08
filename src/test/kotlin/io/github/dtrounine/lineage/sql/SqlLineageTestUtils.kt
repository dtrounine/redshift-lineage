package io.github.dtrounine.lineage.sql

import com.charleskorn.kaml.Yaml
import io.github.dtrounine.lineage.TableLineageExtractor
import io.github.dtrounine.lineage.model.LineageInfo
import kotlin.test.assertEquals

fun assertLineage(sql: String, expectedLineageYaml: String) {
    val expectedLineage = Yaml.default.decodeFromString(LineageInfo.serializer(), expectedLineageYaml.trimIndent())
    val statements = parseRedshiftSqlToAst(sql)
    val lineageInfo: LineageInfo = TableLineageExtractor().getAggregatedLineage(statements)
    assertEquals(expectedLineage.lineage, lineageInfo.lineage)
    assertEquals(expectedLineage.sources, lineageInfo.sources)
}