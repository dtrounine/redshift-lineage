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

    private fun parseStatement(stmtContext: RedshiftSqlParser.StmtContext): Ast_Statement? {
        when (stmtContext) {
            is RedshiftSqlParser.SelectStatementContext -> return parseSelectStatement(stmtContext.selectstmt())
            is RedshiftSqlParser.DropStatementContext -> return parseDropStatement(stmtContext.dropstmt())
            is RedshiftSqlParser.InsertStatementContext -> return parseInsertStatement(stmtContext.insertstmt())
            is RedshiftSqlParser.DeleteStatementContext -> return parseDeleteStatement(stmtContext.deletestmt())
            is RedshiftSqlParser.CreateStatementContext -> return parseCreateStatement(stmtContext.createstmt())
            // Add other statement types here
            else -> {
//                println("Unsupported statement type: ${stmtContext.javaClass.simpleName}")
                return null
            }
        }
    }

    private fun parseSelectStatement(selectstmtContext: RedshiftSqlParser.SelectstmtContext): Ast_SelectStatement {
        selectstmtContext.select_no_parens()?.let {
            return parseSelectStatementNoParentheses(it)
        }
        val selectParens = selectstmtContext.select_with_parens()!!
        return parseSelectStatementWithParentheses(selectParens)
    }

    private fun parseSelectStatementWithParentheses(selectContext: RedshiftSqlParser.Select_with_parensContext): Ast_SelectStatement {
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

    private fun parseSelectStatementNoParentheses(selectContext: RedshiftSqlParser.Select_no_parensContext): Ast_SelectStatement {
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

    private fun parseWithClause(withClauseContext: RedshiftSqlParser.With_clauseContext): List<Ast_Cte> =
        withClauseContext.cte_list().common_table_expr().map { cteContext ->
            val cteName = cteContext.name().text
            val selectStatement = parseSelectStatement(cteContext.selectstmt())
            Ast_Cte(cteContext, cteName, selectStatement)
        }

    private fun parseSortClause(sortClauseContext: RedshiftSqlParser.Sort_clauseContext): Ast_SortClause {
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

    private fun parseSelectClause(selectClause: RedshiftSqlParser.Select_clauseContext): Ast_SelectClause {
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

    private fun parseSelectCombineOperator(selectCombOp: RedshiftSqlParser.Select_comb_opContext): Ast_SelectCombineOperator {
        val operatorType = when (selectCombOp.text) {
            "UNION" -> SelectCombineOperatorType.UNION
            "EXCEPT" -> SelectCombineOperatorType.EXCEPT
            else -> throw IllegalArgumentException("Unknown select combine operator: ${selectCombOp.text}")
        }
        return Ast_SelectCombineOperator(selectCombOp, operatorType)
    }

    private fun parseSelectCombineOperatorModifier(
        all_or_distinct_Context: RedshiftSqlParser.All_or_distinctContext
    ): SelectCombineOperatorModifier {
        val modifierType = when (all_or_distinct_Context.text) {
            "ALL" -> SelectCombineOperatorModifier.ALL
            "DISTINCT" -> SelectCombineOperatorModifier.DISTINCT
            else -> throw IllegalArgumentException("Unknown select combine operator modifier: ${all_or_distinct_Context.text}")
        }
        return modifierType
    }

    private fun parseSimpleSelect(simpleSelectContext: RedshiftSqlParser.Simple_selectContext): Ast_SelectClause {
        return when (simpleSelectContext) {
            is RedshiftSqlParser.StandardSimpleSelectContext -> parseStandardSelect(simpleSelectContext)
            is RedshiftSqlParser.ValuesSimpleSelectContext -> parseValuesSelect(simpleSelectContext)
            else -> throw IllegalArgumentException("Unknown simple select type: ${simpleSelectContext.javaClass.simpleName}")
        }
    }

    private fun parseStandardSelect(selectContext: RedshiftSqlParser.StandardSimpleSelectContext): Ast_CoreSelectClause {
        val isDistinct = selectContext.distinct_clause() != null
        val targets = selectContext.target_list()!!.target_el().map { targetElContext ->
            when (targetElContext) {
                is RedshiftSqlParser.Target_starContext -> Ast_StarSelectTarget(targetElContext)
                is RedshiftSqlParser.Target_labelContext -> parseTargetLabelContext(targetElContext)
                else -> throw IllegalArgumentException("Unknown select target type: ${targetElContext.javaClass.simpleName}")
            }
        }
        val from: Ast_From? = selectContext.from_clause()?.let { parseFromClause(it) }
        val into: Ast_OptTempTableName? = selectContext.into_clause()?.let { parseIntoClause(it) }
        val where: Ast_Expression? = selectContext.where_clause()?.let { whereContext ->
            parseExpression(whereContext.a_expr())
        }
        return Ast_CoreSelectClause(selectContext, isDistinct, targets, from, into, where)
    }

    private fun parseValuesSelect(valuesContext: RedshiftSqlParser.ValuesSimpleSelectContext): Ast_ValuesSelectClause {
        val valuesList = valuesContext.values_clause().expr_list().map { exprListContext ->
            exprListContext.a_expr().map { exprContext ->
                parseExpression(exprContext)
            }
        }
        return Ast_ValuesSelectClause(valuesContext, valuesList)
    }

    private fun parseTargetLabelContext(targetLabelcontext: RedshiftSqlParser.Target_labelContext): Ast_ColumnSelectTarget {
        val expression = parseExpression(targetLabelcontext.a_expr())
        val alias = targetLabelcontext.colLabel()?.text ?: targetLabelcontext.bareColLabel()?.text
        return Ast_ColumnSelectTarget(targetLabelcontext, expression, alias)
    }

    private fun parseExpression(exprContext: RedshiftSqlParser.A_exprContext): Ast_Expression {
        val aExprLessLessContext: RedshiftSqlParser.A_expr_lesslessContext = exprContext.a_expr_qual().a_expr_lessless()
        // TODO: what is the parent a_expr_qual context?
        return parseLessLessExpression(aExprLessLessContext)
    }

    private fun parseLessLessExpression(
        lessLessContext: RedshiftSqlParser.A_expr_lesslessContext
    ): Ast_Expression {
        val left = parseOrExpression(lessLessContext.a_expr_or())
        return lessLessContext.a_expr_lessless_rest_()?.let {
            var res = left
            for (i in it.a_expr_or().indices) {
                val right = parseOrExpression(it.a_expr_or(i)!!)
                val operator = if (it.LESS_LESS(i) != null) BinaryOperator.BIT_SHIFT_LEFT else BinaryOperator.BIT_SHIFT_RIGHT
                res = Ast_BinaryOperatorExpression(
                    lessLessContext,
                    res,
                    right,
                    operator
                )
            }
            res

        } ?: left

    }

    private fun parseOrExpression(orContext: RedshiftSqlParser.A_expr_orContext): Ast_Expression {
        var res = parseAndExpression(orContext.a_expr_and(0)!!)
        for (i in 1 until orContext.a_expr_and().size) {
            val right = parseAndExpression(orContext.a_expr_and(i)!!)
            res = Ast_BinaryOperatorExpression(
                orContext,
                res,
                right,
                BinaryOperator.OR
            )
        }
        return res
    }

    private fun parseAndExpression(andContext: RedshiftSqlParser.A_expr_andContext): Ast_Expression {
        var res = parseBetweenExpression(andContext.a_expr_between(0)!!)
        for (i in 1 until andContext.a_expr_between().size) {
            val right = parseBetweenExpression(andContext.a_expr_between(i)!!)
            res = Ast_BinaryOperatorExpression(
                andContext,
                res,
                right,
                BinaryOperator.AND
            )
        }
        return res
    }

    private fun parseBetweenExpression(betweenContext: RedshiftSqlParser.A_expr_betweenContext): Ast_Expression {
        val target = parseInExpression(betweenContext.a_expr_in())
        return betweenContext.a_expr_between_rest_()?.let { restContext ->
            val lowerBound = parseInExpression(restContext.a_expr_in(0)!!)
            val upperBound = parseInExpression(restContext.a_expr_in(1)!!)
            val isNot = restContext.NOT() != null
            val isSymmetric = restContext.SYMMETRIC() != null
            Ast_BetweenExpression(
                betweenContext,
                target,
                lowerBound,
                upperBound,
                isNot,
                isSymmetric
            )
        } ?: target
    }

    private fun parseInExpression(inContext: RedshiftSqlParser.A_expr_inContext): Ast_Expression {
        val target = parseUnaryNotExpression(inContext.a_expr_unary_not())
        return if (inContext.IN_P() != null) {
            val isNot = inContext.NOT() != null
            val values = parseInSourceExpression(inContext.in_expr()!!)
            Ast_InExpression(
                inContext,
                target,
                values,
                isNot
            )
        } else {
            target
        }
    }

    private fun parseInSourceExpression(inValuesContext: RedshiftSqlParser.In_exprContext) : Ast_InSource {
        when (inValuesContext) {
            is RedshiftSqlParser.In_expr_selectContext -> {
                val selectStatement = parseSelectStatementWithParentheses(inValuesContext.select_with_parens())
                return Ast_InSelectSource(inValuesContext, selectStatement)
            }
            is RedshiftSqlParser.In_expr_listContext -> {
                val exprList = inValuesContext.expr_list().a_expr().map { exprContext ->
                    parseExpression(exprContext)
                }
                return Ast_InValuesSource(inValuesContext, exprList)
            }
            else -> throw IllegalArgumentException("Unknown IN source type: ${inValuesContext.javaClass.simpleName}")
        }
    }

    private fun parseUnaryNotExpression(unaryNotContext: RedshiftSqlParser.A_expr_unary_notContext): Ast_Expression {
        val expression = parseIsNullExpression(unaryNotContext.a_expr_isnull())
        if (unaryNotContext.NOT() != null) {
            return Ast_UnaryOperatorExpression(
                unaryNotContext,
                expression,
                UnaryOperator.NOT
            )
        }
        return expression
    }

    private fun parseIsNullExpression(isNullContext: RedshiftSqlParser.A_expr_isnullContext): Ast_Expression {
        val expression = parseIsNotExpression(isNullContext.a_expr_is_not())
        val operator = if (isNullContext.ISNULL() != null) {
            UnaryOperator.IS_NULL
        } else if (isNullContext.NOTNULL() != null) {
            UnaryOperator.IS_NOT_NULL
        } else {
            null
        }
        return operator?.let {
            Ast_UnaryOperatorExpression(
                isNullContext,
                expression,
                it
            )
        } ?: expression
    }

    private fun parseIsNotExpression(isNotContext: RedshiftSqlParser.A_expr_is_notContext): Ast_Expression {
        val expression = parseCompareExpression(isNotContext.a_expr_compare())
        if (isNotContext.IS() == null) {
            return expression
        }
        val isNot = isNotContext.NOT() != null
        return if (isNotContext.NULL_P() != null) {
            if (isNot) {
                Ast_UnaryOperatorExpression(
                    isNotContext,
                    expression,
                    UnaryOperator.IS_NOT_NULL
                )
            } else {
                Ast_UnaryOperatorExpression(
                    isNotContext,
                    expression,
                    UnaryOperator.IS_NULL
                )
            }
        } else if (isNotContext.TRUE_P() != null) {
            if (isNot) {
                Ast_UnaryOperatorExpression(
                    isNotContext,
                    expression,
                    UnaryOperator.IS_FALSE
                )
            } else {
                Ast_UnaryOperatorExpression(
                    isNotContext,
                    expression,
                    UnaryOperator.IS_TRUE
                )
            }
        } else if (isNotContext.UNKNOWN() != null) {
            if (isNot) {
                Ast_UnaryOperatorExpression(
                    isNotContext,
                    expression,
                    UnaryOperator.IS_NOT_UNKNOWN
                )
            } else {
                Ast_UnaryOperatorExpression(
                    isNotContext,
                    expression,
                    UnaryOperator.IS_UNKNOWN
                )
            }
        } else if (isNotContext.DISTINCT() != null) {
            val right = parseExpression(isNotContext.a_expr()!!)
            val operator = if (isNot) {
                BinaryOperator.NOT_DISTINCT_FROM
            } else {
                BinaryOperator.DISTINCT_FROM
            }
            Ast_BinaryOperatorExpression(
                isNotContext,
                expression,
                right,
                operator
            )
        } else {
            throw IllegalArgumentException("Unsupported IS (NOT)? expression: ${isNotContext.text}")
        }
    }

    private fun parseCompareExpression(compareContext: RedshiftSqlParser.A_expr_compareContext): Ast_Expression {
        val left = parseLikeExpression(compareContext.a_expr_like(0)!!)
        if (compareContext.a_expr_like().size == 1) {
            return left
        }
        val right = parseLikeExpression(compareContext.a_expr_like(1)!!)
        val op = if (compareContext.EQUAL() != null) {
            BinaryOperator.EQUALS
        } else if (compareContext.NOT_EQUALS() != null) {
            BinaryOperator.NOT_EQUALS
        } else if (compareContext.GT() != null) {
            BinaryOperator.GREATER_THAN
        } else if (compareContext.LT() != null) {
            BinaryOperator.LESS_THAN
        } else if (compareContext.GREATER_EQUALS() != null) {
            BinaryOperator.GREATER_THAN_OR_EQUAL
        } else if (compareContext.LESS_EQUALS() != null) {
            BinaryOperator.LESS_THAN_OR_EQUAL
        } else {
            throw IllegalArgumentException("Unknown comparison operator: ${compareContext.text}")
        }
        return Ast_BinaryOperatorExpression(
            compareContext,
            left,
            right,
            op
        )
    }

    private fun parseLikeExpression(likeContext: RedshiftSqlParser.A_expr_likeContext): Ast_Expression {
        val left = parseQualOpExpression(likeContext.a_expr_qual_op(0)!!)
        if (likeContext.a_expr_qual_op().size == 1) {
            return left
        }
        val right = parseQualOpExpression(likeContext.a_expr_qual_op(1)!!)
        val isNot = likeContext.NOT() != null
        val operator = if (likeContext.LIKE() != null) {
            LikeOperatorType.LIKE
        } else if (likeContext.ILIKE() != null) {
            LikeOperatorType.ILIKE
        } else if (likeContext.SIMILAR() != null) {
            LikeOperatorType.SIMILAR_TO
        } else {
            throw IllegalArgumentException("Unknown LIKE operator: ${likeContext.text}")
        }
        val escape = likeContext.escape_()?.a_expr()?.let { parseExpression(it) }
        return Ast_LikeExpression(
            likeContext,
            left,
            right,
            operator,
            isNot,
            escape
        )
    }

    private fun parseQualOpExpression(qualOpContextext: RedshiftSqlParser.A_expr_qual_opContext): Ast_Expression {
        val left = parseUnaryQualOpExpression(qualOpContextext.a_expr_unary_qualop(0)!!)
        if (qualOpContextext.a_expr_unary_qualop().size != 1) {
            throw UnsupportedOperationException("Multiple unary qualified operators are not supported yet: ${qualOpContextext.text}")
        }
        return left

    }

    private fun parseUnaryQualOpExpression(unaryQualOpContext: RedshiftSqlParser.A_expr_unary_qualopContext): Ast_Expression {
        val left = parseAddExpression(unaryQualOpContext.a_expr_add())
        if (unaryQualOpContext.qual_op() != null) {
            throw UnsupportedOperationException("Qualified operators are not supported yet: ${unaryQualOpContext.text}")
        }
        return left
    }

    private fun parseAddExpression(addContext: RedshiftSqlParser.A_expr_addContext): Ast_Expression {
        var res = parseMulExpression(addContext.a_expr_mul())
        addContext.a_expr_add_rest_()?.let {
            it.a_expr_add_term_().forEach { term ->
                val right = parseMulExpression(term.a_expr_mul())
                val operator = if (term.PLUS() != null) {
                    BinaryOperator.ADD
                } else if (term.MINUS() != null) {
                    BinaryOperator.SUBTRACT
                } else {
                    throw IllegalArgumentException("Unknown add operator: ${it.text}")
                }
                res = Ast_BinaryOperatorExpression(
                    addContext,
                    res,
                    right,
                    operator
                )
            }
        }
        return res
    }

    fun parseMulExpression(mulContext: RedshiftSqlParser.A_expr_mulContext): Ast_Expression {
        var res = parseCaretExpression(mulContext.a_expr_caret())
        mulContext.a_expr_mul_rest_()?.let {
            it.a_expr_mul_term_().forEach { term ->
                val right = parseCaretExpression(term.a_expr_caret())
                val operator = if (term.STAR() != null) {
                    BinaryOperator.MULTIPLY
                } else if (term.SLASH() != null) {
                    BinaryOperator.DIVIDE
                } else if (term.PERCENT() != null) {
                    BinaryOperator.MODULO
                } else {
                    throw IllegalArgumentException("Unknown multiplicative operator: ${it.text}")
                }
                res = Ast_BinaryOperatorExpression(
                    mulContext,
                    res,
                    right,
                    operator
                )
            }
        }
        return res
    }

    fun parseCaretExpression(caretContext: RedshiftSqlParser.A_expr_caretContext): Ast_Expression {
        val left = parseUnarySignExpression(caretContext.a_expr_unary_sign(0)!!)
        return if (caretContext.a_expr_unary_sign().size > 1) {
            val right = parseUnarySignExpression(caretContext.a_expr_unary_sign(1)!!)
            Ast_BinaryOperatorExpression(
                caretContext,
                left,
                right,
                BinaryOperator.CARET
            )
        } else {
            left
        }
    }

    private fun parseUnarySignExpression(unarySignContext: RedshiftSqlParser.A_expr_unary_signContext): Ast_Expression {
        val expression = parseAtTimezoneExpression(unarySignContext.a_expr_at_time_zone())
        val operator = if (unarySignContext.PLUS() != null) {
            UnaryOperator.PLUS
        } else if (unarySignContext.MINUS() != null) {
            UnaryOperator.MINUS
        } else {
            null
        }
        return operator?.let {
            Ast_UnaryOperatorExpression(
                unarySignContext,
                expression,
                it
            )
        } ?: expression
    }

    private fun parseAtTimezoneExpression(atTimezoneContext: RedshiftSqlParser.A_expr_at_time_zoneContext): Ast_Expression {
        val left = parseCollateExpression(atTimezoneContext.a_expr_collate())
        atTimezoneContext.a_expr()?.let {
            val right = parseExpression(it)
            return Ast_BinaryOperatorExpression(
                atTimezoneContext,
                left,
                right,
                BinaryOperator.AT_TIME_ZONE
            )
        }
        return left
    }

    private fun parseCollateExpression(collateContext: RedshiftSqlParser.A_expr_collateContext): Ast_Expression {
        val expression = parseTypeCastExpression(collateContext.a_expr_typecast())
        return collateContext.any_name()?.let {
            val collationName = it.text
            return Ast_CollateExpression(
                collateContext,
                expression,
                collationName
            )
        } ?: expression
    }

    private fun parseTypeCastExpression(typeCastContext: RedshiftSqlParser.A_expr_typecastContext): Ast_Expression {
        var res = parseCExpression(typeCastContext.c_expr())
        typeCastContext.typename().forEach {
            val typeName = it.text
            res = Ast_CastExpression(
                typeCastContext,
                res,
                typeName
            )
        }
        return res
    }

    private fun parseCExpression(cContext: RedshiftSqlParser.C_exprContext): Ast_Expression {
        return when (cContext) {
            is RedshiftSqlParser.C_expr_existsContext -> parseExistsExpression(cContext)
            is RedshiftSqlParser.C_expr_columnrefContext -> parseColumnRefExpression(cContext.columnref())
            is RedshiftSqlParser.C_expr_constContext -> parseConstExpression(cContext.aexprconst())
            is RedshiftSqlParser.C_expr_in_parensContext -> parseInParensExpression(cContext)
            is RedshiftSqlParser.C_expr_caseContext -> parseCaseExpression(cContext.case_expr())
            is RedshiftSqlParser.C_expr_funcContext -> parseFunctionExpression(cContext.func_expr())
            is RedshiftSqlParser.C_expr_implicit_rowContext -> parseImplicitRowExpression(cContext)
            is RedshiftSqlParser.C_expr_selectContext -> parseSelectInParenthesesWithIndirection(cContext)
            else -> throw IllegalArgumentException("Unknown C expression type: ${cContext.javaClass.simpleName}")
        }
    }

    private fun parseExistsExpression(existContext: RedshiftSqlParser.C_expr_existsContext): Ast_ExistsExpression {
        val selectStatement = parseSelectStatementWithParentheses(existContext.select_with_parens())
        return Ast_ExistsExpression(
            existContext,
            selectStatement
        )
    }

    private fun parseColumnRefExpression(columnRefContext: RedshiftSqlParser.ColumnrefContext): Ast_ColumnReference {
        val names: MutableList<String> = mutableListOf()
        names.add(columnRefContext.colid().text)
        columnRefContext.indirection()?.let {
            it.indirection_el().forEach { indirection ->
                if (indirection.OPEN_BRACKET() != null) {
                    throw UnsupportedOperationException("Array indexing is not supported yet: ${indirection.text}")
                } else if (indirection.DOT() != null) {
                    val namePart = indirection.attr_name()?.text ?: indirection.STAR()?.let { "*" }
                        ?: throw IllegalArgumentException("Unexpected column reference part: ${indirection.text}")
                    names.add(namePart)
                } else {
                    throw IllegalArgumentException("Unexpected column reference part: ${indirection.text}")
                }
            }
        }
        return Ast_ColumnReference(
            columnRefContext,
            names
        )
    }

    private fun parseConstExpression(constContext: RedshiftSqlParser.AexprconstContext): Ast_ConstantExpression {
        // TODO: Implement parsing for different constant types
        return Ast_ConstantExpression(
            constContext,
            constContext.text
        )
//        return if (constContext.iconst() != null) {
//            parseIntegerConstant(constContext.iconst()!!)
//        } else if (constContext.fconst() != null) {
//            Ast_FloatConstant(
//                constContext.fconst()!!,
//                constContext.fconst()!!.text.toDouble()
//            )
//        } else if (constContext.sconst() != null) {
//            Ast_StringConstant(
//                constContext.sconst()!!,
//                constContext.sconst()!!.text.trim('\'')
//            )
//        } else if (constContext.bconst() != null) {
//            throw UnsupportedOperationException("Binary string constants are not supported yet: ${constContext.bconst()!!.text}")
//        } else if (constContext.xconst() != null) {
//            throw UnsupportedOperationException("Hexadecimal string constants are not supported yet: ${constContext.xconst()!!.text}")
//        } else if (constContext.func_name() != null) {
//            throw UnsupportedOperationException("Literal constructors are not supported yet: ${constContext.text}")
//        } else {
//            throw IllegalArgumentException("Unknown constant type: ${constContext.javaClass.simpleName}")
//        }
    }
/*
    private fun parseIntegerConstant(constContext: RedshiftSqlParser.IconstContext): Ast_IntegerConstant {
        val value: Long = constContext.Integral()?.let {
            it.text.toLong()
        } ?: constContext.OctalIntegral()?.let {
            it.text.substring(2).toLong(8) // Remove the leading '0o'
        } ?: constContext.HexadecimalIntegral()?.let {
            it.text.substring(2).toLong(16) // Remove the leading '0x'
        } ?: constContext.BinaryIntegral()?.let {
            it.text.substring(2).toLong(2) // Remove the leading '0b'
        } ?: throw IllegalArgumentException("Unknown integer literal: ${constContext.text}")
        return Ast_IntegerConstant(constContext, value)
    }
*/
    private fun parseInParensExpression(inParensContext: RedshiftSqlParser.C_expr_in_parensContext): Ast_Expression {
        val expressions = parseExpression(inParensContext.a_expr_in_parens!!)
        // TODO: add support for indirection
        return expressions
    }

    private fun parseCaseExpression(caseContext: RedshiftSqlParser.Case_exprContext): Ast_Expression {
        val caseExpr = caseContext.case_arg()?.let { parseExpression(it.a_expr()) }
        val whenClauses = caseContext.when_clause_list().when_clause().map { whenClauseContext ->
            val condition = parseExpression(whenClauseContext.when_expr!!)
            val result = parseExpression(whenClauseContext.then_expr!!)
            Ast_WhenClause(whenClauseContext, condition, result)
        }
        val defaultExpr = caseContext.case_default()?.let { parseExpression(it.a_expr()) }
        return Ast_CaseExpression(caseContext, caseExpr, whenClauses, defaultExpr)
    }

    private fun parseFunctionExpression(funcContext: RedshiftSqlParser.Func_exprContext): Ast_Expression {
        funcContext.func_application()?.let { funcAppContext ->
            val functionName = funcAppContext.func_name().text
            val arguments = funcAppContext.func_arg_list()?.func_arg_expr()?.map { exprContext ->
                if (exprContext.param_name() != null) {
                    throw UnsupportedOperationException("Named parameters are not supported yet: ${exprContext.text}")
                }
                parseExpression(exprContext.a_expr())
            } ?: emptyList()
            val isDistinct = funcAppContext.DISTINCT() != null
            val isAll = funcAppContext.ALL() != null
            val isStar = funcAppContext.STAR() != null
            if (funcAppContext.VARIADIC() != null) {
                throw UnsupportedOperationException("Variadic functions are not supported yet: ${funcAppContext.text}")
            }
            val sort = funcAppContext.sort_clause()?.let { parseSortClause(it) }
            val withinGroup = funcContext.within_group_clause()?.let { parseSortClause(it.sort_clause()) }
            val filter = funcContext.filter_clause()?.let { parseExpression(it.a_expr()) }
            val over = funcContext.over_clause()?.let { parseOverClause(it) }
            return Ast_FunctionCallExpression(
                funcContext,
                functionName,
                arguments,
                isAll,
                isDistinct,
                isStar,
                sort,
                withinGroup,
                filter,
                over
            )
        }
        funcContext.func_expr_common_subexpr()?.let {
            val subExpressions: MutableList<Ast_Expression> = mutableListOf()
            it.a_expr().forEach { exprContext ->
                subExpressions.add(parseExpression(exprContext))
            }
            it.expr_list()?.let { exprListContext ->
                exprListContext.a_expr().forEach { exprContext ->
                    subExpressions.add(parseExpression(exprContext))
                }
            }
            it.extract_list()?.let { extractList ->
                subExpressions.add(parseExpression(extractList.a_expr()))
            }
            return Ast_CommonFunctionCallExpression(
                funcContext,
                it.text,
                subExpressions
            )
        }
        throw IllegalArgumentException("Unknown function expression type: ${funcContext.text}")
    }

    private fun parseOverClause(overContext: RedshiftSqlParser.Over_clauseContext): Ast_OverClause {
        overContext.colid()?.let {
            val windowName = it.text
            return Ast_OverWindowName(overContext, windowName)
        }
        val windowSpecContext = overContext.window_specification()!!
        val windowName = windowSpecContext.existing_window_name_()?.text
        val partitionBy: List<Ast_Expression>? = windowSpecContext.partition_clause_()?.expr_list()?.a_expr()?.map { exprContext ->
            parseExpression(exprContext)
        }
        val orderBy: Ast_SortClause? = windowSpecContext.sort_clause()?.let { parseSortClause(it) }
        val frameClause: Ast_FrameClause? = windowSpecContext.frame_clause_()?.let { frameContext ->
            Ast_FrameClause(frameContext, frameContext.text)
        }
        return Ast_OverWindowSpecification(
            overContext,
            windowName,
            partitionBy,
            orderBy,
            frameClause
        )
    }

    private fun parseImplicitRowExpression(implicitRowContext: RedshiftSqlParser.C_expr_implicit_rowContext): Ast_Expression {
        // TODO
        throw UnsupportedOperationException("Implicit row expressions are not supported yet: ${implicitRowContext.text}")
    }

    private fun parseSelectInParenthesesWithIndirection(selectIndirectionContext: RedshiftSqlParser.C_expr_selectContext): Ast_Expression {
        val selectStatement = parseSelectStatementWithParentheses(selectIndirectionContext.select_with_parens())
        val indirections: List<Ast_Indirection>? = selectIndirectionContext.indirection()?.indirection_el()?.map {
            if (it.OPEN_BRACKET() != null) {
                throw UnsupportedOperationException("Array indexing is not supported yet: ${it.text}")
            } else if (it.DOT() != null) {
                Ast_Indirection(it, it.attr_name()!!.text)
            } else {
                throw IllegalArgumentException("Unexpected column reference part: ${it.text}")
            }
        }
        return Ast_SelectExpression(
            selectIndirectionContext,
            selectStatement,
            indirections
        )
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
            val selectStatement = parseSelectStatementWithParentheses(it)
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

    fun parseDropStatement(dropStatementContext: RedshiftSqlParser.DropstmtContext): Ast_DropStatement {
        val ifExists = dropStatementContext.EXISTS() != null
        val dropBehavior: DropBehavior? = dropStatementContext.drop_behavior_()?.let { dropBehaviorContext ->
            when (dropBehaviorContext.text.uppercase()) {
                "CASCADE" -> DropBehavior.CASCADE
                "RESTRICT" -> DropBehavior.RESTRICT
                else -> throw IllegalArgumentException("Unknown drop behavior: ${dropBehaviorContext.text}")
            }
        }
        val names = dropStatementContext.any_name_list_().any_name().map { it.text }
        return Ast_DropStatement(dropStatementContext, names, ifExists, dropBehavior)
    }

    fun parseInsertStatement(insertStmntContext: RedshiftSqlParser.InsertstmtContext): Ast_InsertStatement {
        val with: List<Ast_Cte> = insertStmntContext.with_clause()?.let { parseWithClause(it) } ?: emptyList()
        val into: Ast_InsertTarget = parseInsertTarget(insertStmntContext.insert_target())
        val selectStatement = parseSelectStatement(insertStmntContext.insert_rest().selectstmt())
        return Ast_InsertStatement(
            insertStmntContext,
            with,
            into,
            selectStatement
        )
    }

    fun parseInsertTarget(insertTargetContext: RedshiftSqlParser.Insert_targetContext): Ast_InsertTarget {
        val tableFqn = insertTargetContext.qualified_name().text
        val alias = insertTargetContext.colid()?.text
        val columns = insertTargetContext.target_columns()?.colid()?.map { it.text }
        return Ast_InsertTarget(insertTargetContext, tableFqn, alias, columns)
    }

    fun parseDeleteStatement(deleteStatementContext: RedshiftSqlParser.DeletestmtContext): Ast_DeleteStatement {
        val with: List<Ast_Cte> = deleteStatementContext.with_clause()?.let { parseWithClause(it) } ?: emptyList()
        val from: Ast_SimpleFromTableRef = deleteStatementContext.relation_expr_opt_alias().let {
            val tableFqn = it.relation_expr().qualified_name().text
            val alias = it.colid()?.text
            Ast_SimpleFromTableRef(deleteStatementContext.relation_expr_opt_alias(), tableFqn, alias)
        }
        val where: Ast_Expression? = deleteStatementContext.where_clause()?.let { parseExpression(it.a_expr()) }
        return Ast_DeleteStatement(
            deleteStatementContext,
            with,
            from,
            where
        )
    }

    private fun parseCreateStatement(createTableContext: RedshiftSqlParser.CreatestmtContext): Ast_CreateTableStatement {
        val ifNotExists = createTableContext.EXISTS() != null
        val isTemporary = createTableContext.opttemp() != null
        val tableFqn = createTableContext.qualified_name().text
        val rest = createTableContext.createstmt_rest_()
        return when (rest) {
            is RedshiftSqlParser.CreateStmtColumnsContext -> {
                val colDefs = rest.opttableelementlist()?.tableelementlist()?.tableelement()?.map { Ast_ColumnDefinition(it, it.text) } ?: emptyList()
                Ast_CreateTableWithColumnDefinitions(
                    createTableContext,
                    tableFqn,
                    ifNotExists,
                    isTemporary,
                    colDefs
                )
            }
            is RedshiftSqlParser.CreateStmtAsSelectContext -> {
                val select = parseSelectStatement(rest.selectstmt())
                Ast_CreateTableAsSelect(
                    createTableContext,
                    tableFqn,
                    ifNotExists,
                    isTemporary,
                    select
                )
            }
            else -> throw IllegalArgumentException("Unknown create table statement type: ${rest.javaClass.simpleName}")
        }
    }
}
