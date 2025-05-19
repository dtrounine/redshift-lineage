package io.github.dtrounine.lineage.sql.ast

import org.antlr.v4.kotlinruntime.ParserRuleContext

sealed class Ast_Node(open val context: ParserRuleContext)

sealed class Ast_Statement(override val context: ParserRuleContext) : Ast_Node(context)

data class Ast_Cte(
    override val context: ParserRuleContext,
    val name: String,
    val selectStatement: Ast_SelectStatement) : Ast_Node(context)

data class Ast_SelectStatement(
    override val context: ParserRuleContext,
    val with: List<Ast_Cte>,
    val selectClause: Ast_SelectClause,
    val orderByClause: Ast_Expression?
) : Ast_Statement(context)

sealed class Ast_SelectClause(override val context: ParserRuleContext) : Ast_Node(context)

enum class SelectCombineOperatorType {
    UNION,
    EXCEPT
}

enum class SelectCombineOperatorModifier {
    ALL,
    DISTINCT
}

data class Ast_SelectCombineOperator(
    override val context: ParserRuleContext,
    val operatorType: SelectCombineOperatorType
) : Ast_Node(context)

data class Ast_CombineSelectClause(
    override val context: ParserRuleContext,
    val operator: Ast_SelectCombineOperator,
    val modifier: SelectCombineOperatorModifier?,
    val left: Ast_SelectClause,
    val right: Ast_SelectClause
): Ast_SelectClause(context)


data class Ast_SimpleSelectClause(
    override val context: ParserRuleContext,
    val isDistinct: Boolean,
    val targets: List<Ast_SelectTarget>,
    val from: Ast_From?,
    val into: Ast_OptTempTableName?
): Ast_SelectClause(context)

sealed class Ast_SelectTarget(override val context: ParserRuleContext) : Ast_Node(context)

data class Ast_StarSelectTarget(
    override val context: ParserRuleContext
): Ast_SelectTarget(context)

data class Ast_ColumnSelectTarget(
    override val context: ParserRuleContext,
    val expression: Ast_Expression,
    val alias: String?
): Ast_SelectTarget(context)


data class Ast_SortClause(
    override val context: ParserRuleContext,
    val orderBy: List<Ast_SortOrder>
): Ast_Expression(context)

enum class SortOrderType {
    ASC,
    DESC
}

data class Ast_SortOrder(
    override val context: ParserRuleContext,
    val order: SortOrderType,
    val expression: Ast_Expression
): Ast_Node(context)


data class Ast_From(
    override val context: ParserRuleContext,
    val elements: List<Ast_FromElement>
): Ast_Expression(context)

class Ast_FromElement(
    override val context: ParserRuleContext,
    val src: Ast_SimpleFromElement,
    val joins: List<Ast_Join>
): Ast_Expression(context)

sealed class Ast_SimpleFromElement(
    override val context: ParserRuleContext,
    open val alias: String?
): Ast_Node(context)

data class Ast_SimpleFromTableRef(
    override val context: ParserRuleContext,
    val tableFqn: String,
    override val alias: String?
): Ast_SimpleFromElement(context, alias)

data class Ast_SimpleFromSubQuery(
    override val context: ParserRuleContext,
    val subQuery: Ast_SelectStatement,
    override val alias: String?
): Ast_SimpleFromElement(context, alias)

data class Ast_SimpleNamedFromElement(
    override val context: ParserRuleContext,
    val from: Ast_FromElement,
    override val alias: String?
): Ast_SimpleFromElement(context, alias)

enum class JoinType {
    INNER,
    LEFT,
    RIGHT,
    FULL
}

sealed class Ast_JoinOperator(override val context: ParserRuleContext): Ast_Expression(context)

data class Ast_QualifiedJoinOperator(
    override val context: ParserRuleContext,
    val joinType: JoinType?,
    val isOuter: Boolean
): Ast_JoinOperator(context)

data class Ast_CrossJoinOperator(
    override val context: ParserRuleContext
): Ast_JoinOperator(context)

sealed class Ast_Join(
    override val context: ParserRuleContext,
    open val joinOperator: Ast_JoinOperator?,
    open val joinTo: Ast_FromElement
): Ast_Node(context)

data class Ast_CrossJoin(
    override val context: ParserRuleContext,
    override val joinTo: Ast_FromElement
    // TODO: no context for CROSS keyword
): Ast_Join(context, Ast_CrossJoinOperator(context), joinTo)

data class Ast_QualifiedJoin(
    override val context: ParserRuleContext,
    override val joinOperator: Ast_QualifiedJoinOperator?,
    override val joinTo: Ast_FromElement,
    val condition: Ast_JoinCondition
): Ast_Join(context, joinOperator, joinTo)

sealed class Ast_JoinCondition(override val context: ParserRuleContext): Ast_Node(context)

data class Ast_JoinOnCondition(
    override val context: ParserRuleContext,
    val expression: Ast_Expression
): Ast_JoinCondition(context)

data class Ast_JoinUsingCondition(
    override val context: ParserRuleContext,
    val columns: List<String>
): Ast_JoinCondition(context)

data class Ast_OptTempTableName(
    override val context: ParserRuleContext,
    val tableFqn: String,
    val isTemporary: Boolean
): Ast_Expression(context)

open class Ast_Expression(override val context: ParserRuleContext) : Ast_Node(context)
