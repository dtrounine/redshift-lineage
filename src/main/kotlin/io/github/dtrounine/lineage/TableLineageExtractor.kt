package io.github.dtrounine.lineage

import io.github.dtrounine.lineage.sql.ast.*
import io.github.dtrounine.lineage.util.merge

class TableLineageExtractor {

    fun getLineage(statements: List<Ast_Statement>): LineageInfo {
        return statements.map { getLineage(it) }
            .reduce { acc, lineageInfo -> acc.merge(lineageInfo) }
    }

    fun getLineage(statement: Ast_Statement): LineageInfo {
        return when (statement) {
            is Ast_SelectStatement -> getSelectLineage(statement)
            else -> LineageInfo.newEmpty()
        }
    }

    fun getSelectLineage(select: Ast_SelectStatement): LineageInfo {
        // TODO: process CTEs
        return getSelectLineage(select.selectClause)
    }

    fun getSelectLineage(select: Ast_SelectClause): LineageInfo {
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

    fun getSimpleSelectLineage(select: Ast_SimpleSelectClause): LineageInfo {
        val fromLineage = select.from?.let { getLineage(it) } ?: LineageInfo.newEmpty()

        return select.into?.let {
            val targetTable = it.tableFqn
            val intoLineage = LineageInfo(
                lineage = mapOf(targetTable to fromLineage.sources),
                sources = emptySet()
            )
            fromLineage.merge(intoLineage)
        } ?: fromLineage

    }

    fun getLineage(from: Ast_From): LineageInfo {
        var out = LineageInfo.newEmpty()
        for (fromElement in from.elements) {
            val lineage = getLineage(fromElement)
            out = out.merge(lineage)
        }
        return out
    }

    fun getLineage(fromElement: Ast_FromElement): LineageInfo {
        var lineage = getLineage(fromElement.src)
        fromElement.joins.forEach { join ->
            val joinLineage = getLineage(join)
            lineage = lineage.merge(joinLineage)
        }
        return lineage
    }

    fun getLineage(join: Ast_Join): LineageInfo {
        return getLineage(join.joinTo)
    }

    fun getLineage(simpleFrom: Ast_SimpleFromElement): LineageInfo {
        when (simpleFrom) {
            is Ast_SimpleFromTableRef -> {
                val table = simpleFrom.tableFqn
                return LineageInfo(emptyMap(), setOf(table))
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