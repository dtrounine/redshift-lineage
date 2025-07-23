/**
 * MIT License with Commons Clause v1.0
 *
 * Copyright © 2025 Dmitrii Trunin (dtrounine@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software **without** restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished
 * to do so, **subject to** the following conditions:
 *
 * 1. The above copyright notice and this permission notice shall be included
 *    in all copies or substantial portions of the Software.
 *
 * 2. **Commons Clause License Condition v1.0**
 *    Without limiting other conditions in the MIT License, the grant of rights
 *    under the License will **not** include, and the License does not grant to you,
 *    the right to **Sell** the Software.
 *
 *    For purposes of this condition, **“Sell”** means practicing any or all of
 *    the rights granted to you under the MIT License to provide to third parties,
 *    for a fee or other consideration, a product or service whose value derives,
 *    entirely or substantially, from the functionality of the Software.
 *
 *    **This includes any service or software which, at any extent, provides**
 *    - data-lineage functionality, or
 *    - SQL-code-analysis functionality.
 *
 *    Any license notice or attribution required by the MIT License must also
 *    include this Commons Clause License Condition notice.
 *
 * 3. THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *    THE SOFTWARE.
 */
package io.github.dtrounine.lineage

import io.github.dtrounine.lineage.model.LineageInfo
import io.github.dtrounine.lineage.model.SourceContextInfo
import io.github.dtrounine.lineage.model.SourcePosition
import io.github.dtrounine.lineage.model.TextPosition
import io.github.dtrounine.lineage.sql.ast.*
import io.github.dtrounine.lineage.util.mergeAll
import io.github.dtrounine.lineage.util.mergeOnlyLineage
import io.github.dtrounine.lineage.util.mergeSourcePositions
import io.github.dtrounine.lineage.util.sourcePositionFromAst

/**
 * Extracts lineage information from parsed SQL statements,
 * focusing on table lineage (no column level lineage).
 */
class TableLineageExtractor {

    /**
     * Get aggregated lineage information from a list of SQL statements. All inputs and outputs
     * from all statements are merged into a single lineage report, so that all details about
     * individual inputs and outputs lineage from separate statements are lost.
     *
     * So, for example, if you have two statements:
     *
     * ```sql
     * INSERT INTO departures SELECT ... FROM schedule;
     * SELECT ... INTO delayed_trains FROM departures JOIN delay_status ON ... ;
     * ```
     *
     * you will get a lineage report that contains:
     *
     * ```
     * inputs:
     *  - schedule
     *  - delay_status
     * outputs:
     *  - delayed_trains
     *  - departures
     * ```
     *
     * Note: If you need lineage information for each statement separately,
     * consider using [getLineage(statement: Ast_Statement)] method.
     */
    fun getAggregatedLineage(statements: List<Ast_Statement>): LineageInfo {
        if (statements.isEmpty()) {
            return LineageInfo.newEmpty()
        }
        val mergedLineage = statements.map { getLineage(it) }
            .reduce { acc, lineageInfo -> acc.mergeAll(lineageInfo) }
        val sourcePosition: SourcePosition? = statements.map { sourcePositionFromAst(it) }
            .reduce() { acc, position ->  mergeSourcePositions(acc, position) }
        return mergedLineage.withContext(
            SourceContextInfo(
                sourceName = null,
                positionInSource = sourcePosition
            )
        )
    }

    fun getLineage(statement: Ast_Statement): LineageInfo {
        val lineage = when (statement) {
            is Ast_SelectStatement -> getSelectLineage(statement)
            is Ast_InsertStatement -> getInsertLineage(statement)
            is Ast_DeleteStatement -> getDeleteLineage(statement)
            is Ast_CreateTableAsSelect -> getCreateAsSelectLineage(statement)
            is Ast_AlterRenameTableStatement -> getAlterRenameTableLineage(statement)
            else -> LineageInfo.newEmpty()
        }
        val sourcePosition: SourcePosition? = sourcePositionFromAst(statement)
        return sourcePosition?.let {
            lineage.withContext(
                SourceContextInfo(
                    sourceName = null,
                    positionInSource = it
                )
            )
        } ?: lineage
    }

    private fun getSelectLineage(select: Ast_SelectStatement): LineageInfo {
        val cteLineage = select.with?.let { getWithClauseLineage(it) } ?: LineageInfo.newEmpty()
        val selectLineage = getSelectLineage(select.selectClause)
        val resolvedSelectLineage = resolvedTransitiveLineage(
            selectLineage,
            cteLineage.lineage
        )
        return resolvedSelectLineage
    }

    private fun getSelectLineage(select: Ast_SelectClause): LineageInfo {
        return when (select) {
            is Ast_CombineSelectClause -> {
                val left = getSelectLineage(select.left)
                val right = getSelectLineage(select.right)
                // TODO: verify schema compatibility for duplicate keys
                left.mergeAll(right)
            }
            is Ast_CoreSelectClause ->  getSimpleSelectLineage(select)
            is Ast_ValuesSelectClause -> LineageInfo.newEmpty() // Values clause does not have lineage
            is Ast_NestedSelectStatementClause -> getSelectLineage(select.selectStatement)
        }
    }

    private fun getSimpleSelectLineage(select: Ast_CoreSelectClause): LineageInfo {
        val fromLineage = select.from?.let { getLineage(it) } ?: LineageInfo.newEmpty()
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
            result = result.mergeAll(LineageInfo(
                lineage = emptyMap(),
                sources = allTargetSources
            ))
        }
        if (additionalClauseSource.isNotEmpty()) {
            result = result.mergeAll(LineageInfo(
                lineage = emptyMap(),
                sources = additionalClauseSource
            ))
        }

        select.into?.let {
            val targetTable = it.tableFqn
            val intoLineage = LineageInfo(
                lineage = mapOf(targetTable to result.sources),
                sources = emptySet()
            )
            result = result.mergeAll(intoLineage)
        }

        return result
    }

    private fun getLineage(from: Ast_From): LineageInfo {
        var out = LineageInfo.newEmpty()
        for (fromElement in from.elements) {
            val lineage = getLineage(fromElement)
            out = out.mergeAll(lineage)
        }
        return out
    }

    fun getLineage(fromElement: Ast_FromElement): LineageInfo {
        var lineage = getLineage(fromElement.src)
        fromElement.joins.forEach { join ->
            val joinLineage = getLineage(join)
            lineage = lineage.mergeAll(joinLineage)
        }
        return lineage
    }

    private fun getLineage(join: Ast_Join): LineageInfo {
        return getLineage(join.joinTo)
    }

    private fun getLineage(simpleFrom: Ast_SimpleFromElement): LineageInfo {
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

    private fun getInsertLineage(insert: Ast_InsertStatement): LineageInfo {
        val targetTable = insert.into.targetFqn
        val sourcesLineage = getSelectLineage(insert.selectStatement)
        val rawInsertLineage = LineageInfo(
            lineage = mapOf(targetTable to sourcesLineage.sources),
            sources = sourcesLineage.sources
        )
        val cteLineage = insert.with?.let { getWithClauseLineage(it) } ?: LineageInfo.newEmpty()
        val resolvedInsertLineage = resolvedTransitiveLineage(
            rawInsertLineage,
            cteLineage.lineage
        )
        return resolvedInsertLineage.mergeOnlyLineage(sourcesLineage)
    }

    private fun getWithClauseLineage(with: Ast_WithClause): LineageInfo {
        var resolvedLineage = LineageInfo.newEmpty()
        with.ctes.forEach {
            val subLineage = resolvedTransitiveLineage(getLineage(it.selectStatement), resolvedLineage.lineage)
            val cteLineage = LineageInfo(
                lineage = mapOf(it.name to subLineage.sources),
                sources = subLineage.sources
            )
            resolvedLineage = resolvedLineage.mergeAll(cteLineage).mergeAll(subLineage)
        }
        return resolvedLineage
    }

    private fun getDeleteLineage(delete: Ast_DeleteStatement): LineageInfo {
        val targetTable = delete.from.tableFqn
        val whereSources = delete.where?.let { where ->
            getExpressionSources(where)
        } ?: emptySet()
        val deleteLineage = LineageInfo(
            lineage = mapOf(targetTable to whereSources),
            sources = whereSources
        )
        val cteLineage = delete.with?.let { getWithClauseLineage(it) } ?: LineageInfo.newEmpty()
        val deletedLineage = resolvedTransitiveLineage(deleteLineage, cteLineage.lineage)
        return deletedLineage
    }

    private fun getCreateAsSelectLineage(create: Ast_CreateTableAsSelect): LineageInfo {
        val targetTable = create.tableName
        val sourcesLineage = getSelectLineage(create.selectStatement)
        val createLineage = LineageInfo(
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
        srcLineage: LineageInfo,
        previouslyResolvedLineage: Map<String, Set<String>>
    ): LineageInfo {
        val resolvedLineage = srcLineage.lineage.mapValues { (sink, sources) ->
            resolvedTransitiveSources(sources, previouslyResolvedLineage)
        }
        val resolvedSources = resolvedTransitiveSources(srcLineage.sources, previouslyResolvedLineage)
        return LineageInfo(
            lineage = resolvedLineage,
            sources = resolvedSources
        )
    }

    private fun getAlterRenameTableLineage(
        alter: Ast_AlterRenameTableStatement
    ): LineageInfo {
        val oldTable = alter.fromTableFqn
        val newTableName = alter.toTableName
        // Remove the last part (after the last dot) of the oldTableFqn and append the new table name to get the
        // new table fqn
        val newTable = if (oldTable.contains('.')) {
            oldTable.substringBeforeLast('.') + '.' + newTableName
        } else {
            newTableName
        }
        return LineageInfo(
            lineage = mapOf(newTable to setOf(oldTable)),
            sources = setOf(oldTable)
        )
    }
}
