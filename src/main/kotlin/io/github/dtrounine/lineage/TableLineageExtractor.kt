package io.github.dtrounine.lineage

import io.github.dtrounine.lineage.model.LineageData
import io.github.dtrounine.lineage.sql.ast.*
import io.github.dtrounine.lineage.util.mergeAll
import io.github.dtrounine.lineage.util.mergeOnlyLineage

/**
 * Extracts lineage information from parsed SQL statements,
 * focusing on table lineage (no column level lineage).
 */
class TableLineageExtractor {

    fun getLineage(statements: List<Ast_Statement>): LineageData {
        if (statements.isEmpty()) {
            return LineageData.newEmpty()
        }
        return statements.map { getLineage(it) }
            .reduce { acc, lineageInfo -> acc.mergeAll(lineageInfo) }
    }

    fun getLineage(statement: Ast_Statement): LineageData {
        return when (statement) {
            is Ast_SelectStatement -> getSelectLineage(statement)
            is Ast_InsertStatement -> getInsertLineage(statement)
            is Ast_DeleteStatement -> getDeleteLineage(statement)
            is Ast_CreateTableAsSelect -> getCreateAsSelectLineage(statement)
            else -> LineageData.newEmpty()
        }
    }

    private fun getSelectLineage(select: Ast_SelectStatement): LineageData {
        val cteLineage = select.with?.let { getWithClauseLineage(it) } ?: LineageData.newEmpty()
        val selectLineage = getSelectLineage(select.selectClause)
        val resolvedSelectLineage = resolvedTransitiveLineage(
            selectLineage,
            cteLineage.lineage
        )
        return resolvedSelectLineage
    }

    private fun getSelectLineage(select: Ast_SelectClause): LineageData {
        return when (select) {
            is Ast_CombineSelectClause -> {
                val left = getSelectLineage(select.left)
                val right = getSelectLineage(select.right)
                // TODO: verify schema compatibility for duplicate keys
                left.mergeAll(right)
            }
            is Ast_CoreSelectClause ->  getSimpleSelectLineage(select)
            is Ast_ValuesSelectClause -> LineageData.newEmpty() // Values clause does not have lineage
            is Ast_NestedSelectStatementClause -> getSelectLineage(select.selectStatement)
        }
    }

    private fun getSimpleSelectLineage(select: Ast_CoreSelectClause): LineageData {
        val fromLineage = select.from?.let { getLineage(it) } ?: LineageData.newEmpty()
        val allTargetSources: MutableSet<String> = mutableSetOf()
        select.targets.forEach { target ->
            when (target) {
                is Ast_ColumnSelectTarget -> {
                    val exprSources = getExpressionSources(target.expression)
                    allTargetSources.addAll(exprSources)
                }
                else -> {
                    // Star target or other targets do not add lineage, they just select all columns
                }
            }
        }
        val additionalClauseSource: MutableSet<String> = mutableSetOf()
        select.where?.let { additionalClauseSource.addAll(getExpressionSources(it)) }
        select.having?.let { additionalClauseSource.addAll(getExpressionSources(it)) }
        select.qualify?.let { additionalClauseSource.addAll(getExpressionSources(it)) }

        var result = fromLineage

        if (allTargetSources.isNotEmpty()) {
            result = result.mergeAll(LineageData(
                lineage = emptyMap(),
                sources = allTargetSources
            ))
        }
        if (additionalClauseSource.isNotEmpty()) {
            result = result.mergeAll(LineageData(
                lineage = emptyMap(),
                sources = additionalClauseSource
            ))
        }

        select.into?.let {
            val targetTable = it.tableFqn
            val intoLineage = LineageData(
                lineage = mapOf(targetTable to result.sources),
                sources = emptySet()
            )
            result = result.mergeAll(intoLineage)
        }

        return result
    }

    private fun getLineage(from: Ast_From): LineageData {
        var out = LineageData.newEmpty()
        for (fromElement in from.elements) {
            val lineage = getLineage(fromElement)
            out = out.mergeAll(lineage)
        }
        return out
    }

    fun getLineage(fromElement: Ast_FromElement): LineageData {
        var lineage = getLineage(fromElement.src)
        fromElement.joins.forEach { join ->
            val joinLineage = getLineage(join)
            lineage = lineage.mergeAll(joinLineage)
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

    private fun getInsertLineage(insert: Ast_InsertStatement): LineageData {
        val targetTable = insert.into.targetFqn
        val sourcesLineage = getSelectLineage(insert.selectStatement)
        val rawInsertLineage = LineageData(
            lineage = mapOf(targetTable to sourcesLineage.sources),
            sources = sourcesLineage.sources
        )
        val cteLineage = insert.with?.let { getWithClauseLineage(it) } ?: LineageData.newEmpty()
        val resolvedInsertLineage = resolvedTransitiveLineage(
            rawInsertLineage,
            cteLineage.lineage
        )
        return resolvedInsertLineage.mergeOnlyLineage(sourcesLineage)
    }

    private fun getWithClauseLineage(with: Ast_WithClause): LineageData {
        var resolvedLineage = LineageData.newEmpty()
        with.ctes.forEach {
            val subLineage = resolvedTransitiveLineage(getLineage(it.selectStatement), resolvedLineage.lineage)
            val cteLineage = LineageData(
                lineage = mapOf(it.name to subLineage.sources),
                sources = subLineage.sources
            )
            resolvedLineage = resolvedLineage.mergeAll(cteLineage).mergeAll(subLineage)
        }
        return resolvedLineage
    }

    private fun getDeleteLineage(delete: Ast_DeleteStatement): LineageData {
        val targetTable = delete.from.tableFqn
        val whereSources = delete.where?.let { where ->
            getExpressionSources(where)
        } ?: emptySet()
        val deleteLineage = LineageData(
            lineage = mapOf(targetTable to whereSources),
            sources = whereSources
        )
        val cteLineage = delete.with?.let { getWithClauseLineage(it) } ?: LineageData.newEmpty()
        val deletedLineage = resolvedTransitiveLineage(deleteLineage, cteLineage.lineage)
        return deletedLineage
    }

    private fun getCreateAsSelectLineage(create: Ast_CreateTableAsSelect): LineageData {
        val targetTable = create.tableName
        val sourcesLineage = getSelectLineage(create.selectStatement)
        val createLineage = LineageData(
            lineage = mapOf(targetTable to sourcesLineage.sources),
            sources = sourcesLineage.sources
        )
        return createLineage.mergeOnlyLineage(sourcesLineage)
    }

    private fun getExpressionSources(expr: Ast_Expression): Set<String> {
        val sources = mutableSetOf<String>()
        val sourceTablesAccumulator: AstVisitor = object : AbstractAstVisitor() {

            override fun visitAst_SimpleFromTableRef(fromTable: Ast_SimpleFromTableRef) {
                sources.add(fromTable.tableFqn)
            }

            override fun visitAst_SimpleFromSubQueryRef(subQuery: Ast_SimpleFromSubQuery) {
                sources.addAll(getLineage(subQuery.subQuery).sources)
            }
        }
        expr.accept(sourceTablesAccumulator)
        return sources
    }


    private fun resolvedTransitiveSources(
        sources: Set<String>,
        lineage: Map<String, Set<String>>
    ): Set<String> {
        return sources.flatMap { source ->
            lineage[source] ?: setOf(source)
        }.toSet()
    }

    private fun resolvedTransitiveLineage(
        srcLineage: LineageData,
        previouslyResolvedLineage: Map<String, Set<String>>
    ): LineageData {
        val resolvedLineage = srcLineage.lineage.mapValues { (sink, sources) ->
            resolvedTransitiveSources(sources, previouslyResolvedLineage)
        }
        val resolvedSources = resolvedTransitiveSources(srcLineage.sources, previouslyResolvedLineage)
        return LineageData(
            lineage = resolvedLineage,
            sources = resolvedSources
        )
    }
}
