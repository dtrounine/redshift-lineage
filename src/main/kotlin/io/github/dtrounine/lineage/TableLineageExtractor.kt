package io.github.dtrounine.lineage

import io.github.dtrounine.lineage.model.LineageData
import io.github.dtrounine.lineage.sql.ast.*
import io.github.dtrounine.lineage.util.merge

/**
 * Extracts lineage information from parsed SQL statements,
 * focusing on table lineage (no column level lineage).
 */
class TableLineageExtractor {

    fun getLineage(statements: List<Ast_Statement>): LineageData {
        return statements.map { getLineage(it) }
            .reduce { acc, lineageInfo -> acc.merge(lineageInfo) }
    }

    fun getLineage(statement: Ast_Statement): LineageData {
        return when (statement) {
            is Ast_SelectStatement -> getSelectLineage(statement)
            else -> LineageData.newEmpty()
        }
    }

    private fun getSelectLineage(select: Ast_SelectStatement): LineageData {
        // TODO: process CTEs
        return getSelectLineage(select.selectClause)
    }

    private fun getSelectLineage(select: Ast_SelectClause): LineageData {
        return when (select) {
            is Ast_CombineSelectClause -> {
                val left = getSelectLineage(select.left)
                val right = getSelectLineage(select.right)
                // TODO: verify schema compatibility for duplicate keys
                left.merge(right)
            }
            is Ast_SimpleSelectClause ->  getSimpleSelectLineage(select)
        }
    }

    private fun getSimpleSelectLineage(select: Ast_SimpleSelectClause): LineageData {
        val fromLineage = select.from?.let { getLineage(it) } ?: LineageData.newEmpty()

        return select.into?.let {
            val targetTable = it.tableFqn
            val intoLineage = LineageData(
                lineage = mapOf(targetTable to fromLineage.sources),
                sources = emptySet()
            )
            fromLineage.merge(intoLineage)
        } ?: fromLineage

    }

    private fun getLineage(from: Ast_From): LineageData {
        var out = LineageData.newEmpty()
        for (fromElement in from.elements) {
            val lineage = getLineage(fromElement)
            out = out.merge(lineage)
        }
        return out
    }

    fun getLineage(fromElement: Ast_FromElement): LineageData {
        var lineage = getLineage(fromElement.src)
        fromElement.joins.forEach { join ->
            val joinLineage = getLineage(join)
            lineage = lineage.merge(joinLineage)
        }
        return lineage
    }

    private fun getLineage(join: Ast_Join): LineageData {
        return getLineage(join.joinTo)
    }

    private fun getLineage(simpleFrom: Ast_SimpleFromElement): LineageData {
        when (simpleFrom) {
            is Ast_SimpleFromTableRef -> {
                val table = simpleFrom.tableFqn
                return LineageData(emptyMap(), setOf(table))
            }
            is Ast_SimpleFromSubQuery -> {
                return getLineage(simpleFrom.subQuery)
            }
            is Ast_SimpleNamedFromElement -> {
                return getLineage(simpleFrom.from)
            }
        }
    }

}
