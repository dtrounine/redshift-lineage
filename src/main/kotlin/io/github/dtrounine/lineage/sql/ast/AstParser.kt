package io.github.dtrounine.lineage.sql.ast

import io.github.dtrounine.lineage.sql.parser.generated.RedshiftSqlParser

class AstParser {

    fun parseRoot(rootContext: RedshiftSqlParser.RootContext): List<Ast_Statement> {
        val statements = mutableListOf<Ast_Statement>()
        val stmnts = rootContext.stmtblock().stmtmulti().stmt()
        for (statementContext in stmnts) {
            val statementAst: Ast_Statement? = parseStatement(statementContext)
            statementAst?.let {
                statements.add(it)
            }
        }
        return statements
    }

    fun parseStatement(stmtContext: RedshiftSqlParser.StmtContext): Ast_Statement? {
        when (stmtContext) {
            is RedshiftSqlParser.SelectStatementContext -> return parseSelectStatement(stmtContext.selectstmt())
            // Add other statement types here
            else -> {
                println("Unsupported statement type: ${stmtContext.javaClass.simpleName}")
                return null
            }
        }
    }

    fun parseSelectStatement(selectstmtContext: RedshiftSqlParser.SelectstmtContext): Ast_SelectStatement {
        selectstmtContext.select_no_parens()?.let {
            return parseSelectStatementNoParentheses(it)
        }
        val selectParens = selectstmtContext.select_with_parens()!!
        return parseSelectStatemantWithParantheses(selectParens)
    }

    fun parseSelectStatemantWithParantheses(selectContext: RedshiftSqlParser.Select_with_parensContext): Ast_SelectStatement {
        var selectParens = selectContext
        while (true) {
            when (selectParens) {
                is RedshiftSqlParser.SelectWithParenthesesContext -> {
                    selectParens = selectParens.select_with_parens()
                }
                is RedshiftSqlParser.SelectNoParenthesesContext -> {
                    return parseSelectStatementNoParentheses(selectParens.select_no_parens())
                }
            }
        }
    }

    fun parseSelectStatementNoParentheses(selectContext: RedshiftSqlParser.Select_no_parensContext): Ast_SelectStatement {
        val selectClause: Ast_SelectClause = parseSelectClause(selectContext.select_clause())
        val with: List<Ast_Cte> = selectContext.with_clause()?.let { parseWithClause(it) } ?: emptyList()
        val sortClause: Ast_SortClause? = selectContext.sort_clause()?.let { parseSortClause(it) }
        return Ast_SelectStatement(
            selectContext,
            with,
            selectClause,
            sortClause
        )
    }

    fun parseWithClause(withClauseContext: RedshiftSqlParser.With_clauseContext): List<Ast_Cte> =
        withClauseContext.cte_list().common_table_expr().map { cteContext ->
            val cteName = cteContext.name().text
            val selectStatement = parseSelectStatement(cteContext.selectstmt())
            Ast_Cte(cteContext, cteName, selectStatement)
        }

    fun parseSortClause(sortClauseContext: RedshiftSqlParser.Sort_clauseContext): Ast_SortClause {
        val orders = sortClauseContext.sortby_list().sortby().map { sortByContext ->
            val expression = parseExpression(sortByContext.a_expr())
            val orderType = sortByContext.asc_desc_()?.let {
                when (it.text) {
                    "ASC" -> SortOrderType.ASC
                    "DESC" -> SortOrderType.DESC
                    else -> throw IllegalArgumentException("Unknown sort order: ${it.text}")
                }
            } ?: SortOrderType.ASC
            Ast_SortOrder(sortByContext, orderType, expression)
        }
        return Ast_SortClause(sortClauseContext, orders)
    }

    fun parseSelectClause(selectClause: RedshiftSqlParser.Select_clauseContext): Ast_SelectClause {
        /**
         * Parse the select clause, which can be a simple select or a combination of selects.
         * The combination can be UNION or EXCEPT.
         * Builds a tree structure of the select clauses, where each node corresponds to a combine operator
         * UNION or EXCEPT. Each leaf node is a simple select clause.
         */
        val baseSelect = parseSimpleSelect(selectClause.simple_select())
        val combineSelects = selectClause.select_combination_list()
        var result: Ast_SelectClause = baseSelect
        for (i in combineSelects.simple_select().indices) {
            val rightSelect = parseSimpleSelect(combineSelects.simple_select()[i])
            val operator = parseSelectCombineOperator(combineSelects.select_comb_op()[i])
            val all_or_distinct = combineSelects.all_or_distinct(i)
            val modifier = all_or_distinct?.let { parseSelectCombineOperatorModifier(it) }
            result = Ast_CombineSelectClause(
                operator.context,
                operator,
                modifier,
                result,
                rightSelect
            )
        }
        return result
    }

    fun parseSelectCombineOperator(selectCombOp: RedshiftSqlParser.Select_comb_opContext): Ast_SelectCombineOperator {
        val operatorType = when (selectCombOp.text) {
            "UNION" -> SelectCombineOperatorType.UNION
            "EXCEPT" -> SelectCombineOperatorType.EXCEPT
            else -> throw IllegalArgumentException("Unknown select combine operator: ${selectCombOp.text}")
        }
        return Ast_SelectCombineOperator(selectCombOp, operatorType)
    }

    fun parseSelectCombineOperatorModifier(
        all_or_distinct_Context: RedshiftSqlParser.All_or_distinctContext
    ): SelectCombineOperatorModifier {
        val modifierType = when (all_or_distinct_Context.text) {
            "ALL" -> SelectCombineOperatorModifier.ALL
            "DISTINCT" -> SelectCombineOperatorModifier.DISTINCT
            else -> throw IllegalArgumentException("Unknown select combine operator modifier: ${all_or_distinct_Context.text}")
        }
        return modifierType
    }

    fun parseSimpleSelect(simpleSelectContext: RedshiftSqlParser.Simple_selectContext): Ast_SimpleSelectClause {
        val isDistinct = simpleSelectContext.distinct_clause() != null
        val targets = simpleSelectContext.target_list()!!.target_el().map { targetElContext ->
            when (targetElContext) {
                is RedshiftSqlParser.Target_starContext -> Ast_StarSelectTarget(targetElContext)
                is RedshiftSqlParser.Target_labelContext -> parseTargetLabelContext(targetElContext)
                else -> throw IllegalArgumentException("Unknown select target type: ${targetElContext.javaClass.simpleName}")
            }
        }
        val from: Ast_From? = simpleSelectContext.from_clause()?.let { parseFromClause(it) }
        val into: Ast_OptTempTableName? = simpleSelectContext.into_clause()?.let { parseIntoClause(it) }
        return Ast_SimpleSelectClause(simpleSelectContext, isDistinct, targets, from, into)
    }

    fun parseTargetLabelContext(targetLabelcontext: RedshiftSqlParser.Target_labelContext): Ast_ColumnSelectTarget {
        val expression = parseExpression(targetLabelcontext.a_expr())
        val alias = targetLabelcontext.colLabel()?.text ?: targetLabelcontext.bareColLabel()?.text
        return Ast_ColumnSelectTarget(targetLabelcontext, expression, alias)
    }

    fun parseExpression(exprContext: RedshiftSqlParser.A_exprContext): Ast_Expression {
        // Implement parsing logic for expressions
        return Ast_Expression(exprContext)
    }

    fun parseFromClause(fromContext: RedshiftSqlParser.From_clauseContext): Ast_From {
        val fromElements: List<Ast_FromElement> = fromContext.from_list().table_ref().map { tableRefContext ->
            parseTableRef(tableRefContext)
        }
        return Ast_From(fromContext, fromElements)
    }

    fun parseTableRef(tableRefContext: RedshiftSqlParser.Table_refContext): Ast_FromElement {
        val simpleFrom: Ast_SimpleFromElement = parseSimpleTableRef(tableRefContext.simple_table_ref())
        val joins: List<Ast_Join> = tableRefContext.join_clause_list()?.join_clause()?.map { joinClauseContext ->
            parseJoinClause(joinClauseContext)
        } ?: emptyList()
        return Ast_FromElement(tableRefContext, simpleFrom, joins)
    }

    fun parseSimpleTableRef(tableRefContext: RedshiftSqlParser.Simple_table_refContext): Ast_SimpleFromElement {
        val alias = tableRefContext.alias_clause()?.text

        tableRefContext.relation_expr()?.let {
            val tableFqn = it.qualified_name().text
            return Ast_SimpleFromTableRef(
                tableRefContext,
                tableFqn,
                alias
            )
        }
        tableRefContext.select_with_parens()?.let {
            val selectStatement = parseSelectStatemantWithParantheses(it)
            return Ast_SimpleFromSubQuery(
                tableRefContext,
                selectStatement,
                alias
            )
        }
        tableRefContext.table_ref()?.let {
            val fromElement = parseTableRef(it)
            return Ast_SimpleNamedFromElement(
                tableRefContext,
                fromElement,
                alias
            )
        }
        throw IllegalArgumentException("Unknown table reference type: ${tableRefContext.javaClass.simpleName}")
    }

    fun parseJoinClause(joinClauseContext: RedshiftSqlParser.Join_clauseContext): Ast_Join {
        return when (joinClauseContext) {
            is RedshiftSqlParser.Cross_join_clauseContext -> {
                val fromElement: Ast_FromElement = parseTableRef(joinClauseContext.table_ref())
                Ast_CrossJoin(joinClauseContext, fromElement)
            }
            is RedshiftSqlParser.Qualified_join_clauseContext -> {
                val fromElement: Ast_FromElement = parseTableRef(joinClauseContext.table_ref())
                val joinCondition: Ast_JoinCondition = parseJoinCondition(joinClauseContext.join_qual())
                val joinOperator: Ast_QualifiedJoinOperator? = joinClauseContext.join_type()?.let { parseJoinType(it) }
                Ast_QualifiedJoin(joinClauseContext, joinOperator, fromElement, joinCondition)
            }
            else -> throw IllegalArgumentException("Unknown join clause type: ${joinClauseContext.javaClass.simpleName}")
        }
    }

    fun parseJoinType(joinTypeContext: RedshiftSqlParser.Join_typeContext): Ast_QualifiedJoinOperator {
        val joinType = when (joinTypeContext.text) {
            "INNER" -> JoinType.INNER
            "LEFT" -> JoinType.LEFT
            "RIGHT" -> JoinType.RIGHT
            "FULL" -> JoinType.FULL
            else -> throw IllegalArgumentException("Unknown join type: ${joinTypeContext.text}")
        }
        val isOuter = joinTypeContext.OUTER_P() != null
        return Ast_QualifiedJoinOperator(joinTypeContext, joinType, isOuter)
    }

    fun parseJoinCondition(joinQualContext: RedshiftSqlParser.Join_qualContext): Ast_JoinCondition {
        joinQualContext.name_list()?.let { nameListcontext ->
            val columns = nameListcontext.name().map { it.text }
            return Ast_JoinUsingCondition(joinQualContext, columns)
        }
        joinQualContext.a_expr()?.let { exprContext ->
            val expression = parseExpression(exprContext)
            return Ast_JoinOnCondition(joinQualContext, expression)
        }
        throw IllegalArgumentException("Unknown join condition type: ${joinQualContext.javaClass.simpleName}")
    }

    fun parseIntoClause(intoClauseContext: RedshiftSqlParser.Into_clauseContext): Ast_OptTempTableName {
        val optTempTableName = intoClauseContext.opttempTableName()
        val tableFqn = optTempTableName.qualified_name().text
        val isTemporary = (optTempTableName.TEMPORARY() != null) or (optTempTableName.TEMP() != null)
        return Ast_OptTempTableName(
            intoClauseContext,
            tableFqn,
            isTemporary
        )
    }
}
