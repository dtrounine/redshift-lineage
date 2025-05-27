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


/**
 * Represents the most common core SELECT clause in SQL.
 *
 * SELECT DISTINCT
 *      target1, target2, ...
 * INTO sink_table
 * FROM source_table
 *      JOIN other_table ON condition
 *      JOIN another_table USING (column1, column2)
 * WHERE condition
 */
data class Ast_CoreSelectClause(
    override val context: ParserRuleContext,
    val isDistinct: Boolean,
    val targets: List<Ast_SelectTarget>,
    val from: Ast_From?,
    val into: Ast_OptTempTableName?
    // TODO: add WHERE clause, GROUP BY, HAVING, WINDOW etc.
): Ast_SelectClause(context)

/**
 * Represents a VALUES clause in SQL.
 * Note: grammatically, it is a SELECT clause.
 *
 * VALUES (value1, value2, ...), (value3, value4, ...)
 */
data class Ast_ValuesSelectClause(
    override val context: ParserRuleContext,
    val values: List<List<Ast_Expression>>
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

enum class BinaryOperator {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    MODULO,
    AND,
    OR,
    EQUALS,
    NOT_EQUALS,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    BIT_SHIFT_LEFT,
    BIT_SHIFT_RIGHT,
    DISTINCT_FROM,
    NOT_DISTINCT_FROM,
    CARET,
    AT_TIME_ZONE,
}

class Ast_BinaryOperatorExpression(
    override val context: ParserRuleContext,
    val left: Ast_Expression,
    val right: Ast_Expression,
    val operator: BinaryOperator
): Ast_Expression(context)

class Ast_BetweenExpression(
    override val context: ParserRuleContext,
    val target: Ast_Expression,
    val lowerBound: Ast_Expression,
    val upperBound: Ast_Expression,
    val isNot: Boolean,
    val isSymmetric: Boolean
): Ast_Expression(context)

class Ast_InExpression(
    override val context: ParserRuleContext,
    val target: Ast_Expression,
    val values: Ast_Expression,
    val isNot: Boolean
): Ast_Expression(context)

sealed class Ast_InSource(
    override val context: ParserRuleContext,
): Ast_Expression(context)

class Ast_InValuesSource(
    override val context: ParserRuleContext,
    val values: List<Ast_Expression>
): Ast_InSource(context)

class Ast_InSelectSource(
    override val context: ParserRuleContext,
    val selectStatement: Ast_SelectStatement
): Ast_InSource(context)

enum class UnaryOperator {
    NOT,
    IS_NULL,
    IS_NOT_NULL,
    IS_TRUE,
    IS_FALSE,
    IS_UNKNOWN,
    IS_NOT_UNKNOWN,
    MINUS,
    PLUS,
}

class Ast_UnaryOperatorExpression(
    override val context: ParserRuleContext,
    val expression: Ast_Expression,
    val operator: UnaryOperator
): Ast_Expression(context)

enum class LikeOperatorType {
    LIKE,
    ILIKE,
    SIMILAR_TO
}

class Ast_LikeExpression(
    override val context: ParserRuleContext,
    val target: Ast_Expression,
    val pattern: Ast_Expression,
    val operator: LikeOperatorType,
    val isNot: Boolean,
    val escape: Ast_Expression?
): Ast_Expression(context)

class Ast_CollateExpression(
    override val context: ParserRuleContext,
    val expression: Ast_Expression,
    val collation: String
): Ast_Expression(context)

class Ast_CastExpression(
    override val context: ParserRuleContext,
    val expression: Ast_Expression,
    val targetType: String
): Ast_Expression(context)

class Ast_ExistsExpression(
    override val context: ParserRuleContext,
    val selectStatement: Ast_SelectStatement
): Ast_Expression(context)

class Ast_ColumnReference(
    override val context: ParserRuleContext,
    val name: List<String>
): Ast_Expression(context)

class Ast_ConstantExpression(
    override val context: ParserRuleContext,
    val text: String
): Ast_Expression(context)

/*
class Ast_IntegerConstant(
    override val context: ParserRuleContext,
    val value: Long
): Ast_ConstantExpression(context)

class Ast_FloatConstant(
    override val context: ParserRuleContext,
    val value: Double
): Ast_ConstantExpression(context)

class Ast_StringConstant(
    override val context: ParserRuleContext,
    val value: String
): Ast_ConstantExpression(context)

class Ast_BooleanConstant(
    override val context: ParserRuleContext,
    val value: Boolean
): Ast_ConstantExpression(context)

class Ast_TypenameConstant(
    override val context: ParserRuleContext,
    val typename: String,
    val value: String
): Ast_ConstantExpression(context)
*/

data class Ast_CaseExpression(
    override val context: ParserRuleContext,
    val case: Ast_Expression?,
    val whenClauses: List<Ast_WhenClause>,
    val elseClause: Ast_Expression?
): Ast_Expression(context)

data class Ast_WhenClause(
    override val context: ParserRuleContext,
    val condition: Ast_Expression,
    val result: Ast_Expression
): Ast_Node(context)

data class Ast_FunctionCallExpression(
    override val context: ParserRuleContext,
    val functionName: String,
    val arguments: List<Ast_Expression>,
    val isAll: Boolean,
    val isDistinct: Boolean,
    val isStar: Boolean,
    val sort: Ast_SortClause?,
    val withinGroup: Ast_SortClause?,
    val filter: Ast_Expression?,
    val over: Ast_OverClause?
): Ast_Expression(context)

data class Ast_CommonFunctionCallExpression(
    override val context: ParserRuleContext,
    val text: String,
    val subExpressions: List<Ast_Expression>
): Ast_Expression(context)

sealed class Ast_OverClause(
    override val context: ParserRuleContext
): Ast_Node(context)

data class Ast_OverWindowName(
    override val context: ParserRuleContext,
    val windowName: String
): Ast_OverClause(context)

data class Ast_OverWindowSpecification(
    override val context: ParserRuleContext,
    val windowName: String?,
    val partitionBy: List<Ast_Expression>?,
    val orderBy: Ast_SortClause?,
    val frame: Ast_FrameClause?
) : Ast_OverClause(context)

data class Ast_FrameClause(
    override val context: ParserRuleContext,
    val text: String
): Ast_Node(context)

enum class DropBehavior {
    RESTRICT,
    CASCADE
}

data class Ast_DropStatement(
    override val context: ParserRuleContext,
    val names: List<String>,
    val ifExists: Boolean,
    val dropBehavior: DropBehavior? = null
): Ast_Statement(context)

data class Ast_InsertStatement(
    override val context: ParserRuleContext,
    val with: List<Ast_Cte>,
    val into: Ast_InsertTarget,
    val selectStatement: Ast_SelectStatement
): Ast_Statement(context)

data class Ast_InsertTarget(
    override val context: ParserRuleContext,
    val targetFqn: String,
    val alias: String?,
    val columns: List<String>?
): Ast_Node(context)

data class Ast_DeleteStatement(
    override val context: ParserRuleContext,
    val with: List<Ast_Cte>,
    val from: Ast_SimpleFromTableRef,
    val where: Ast_Expression?
): Ast_Statement(context)

sealed class Ast_CreateTableStatement(
    override val context: ParserRuleContext,
    open val tableName: String,
    open val ifNotExists: Boolean,
    open val isTemporary: Boolean
): Ast_Statement(context)

data class Ast_CreateTableWithColumnDefinitions(
    override val context: ParserRuleContext,
    override val tableName: String,
    override val ifNotExists: Boolean,
    override val isTemporary: Boolean,
    val columns: List<Ast_ColumnDefinition>
): Ast_CreateTableStatement(context, tableName, ifNotExists, isTemporary)

data class Ast_CreateTableAsSelect(
    override val context: ParserRuleContext,
    override val tableName: String,
    override val ifNotExists: Boolean,
    override val isTemporary: Boolean,
    val selectStatement: Ast_SelectStatement
): Ast_CreateTableStatement(context, tableName, ifNotExists, isTemporary)

data class Ast_ColumnDefinition(
    override val context: ParserRuleContext,
    // TODO: implement parsing column definition
    val text: String
): Ast_Node(context)
