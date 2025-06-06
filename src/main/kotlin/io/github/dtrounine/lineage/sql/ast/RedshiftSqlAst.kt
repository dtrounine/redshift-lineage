package io.github.dtrounine.lineage.sql.ast

import org.antlr.v4.kotlinruntime.ParserRuleContext

sealed class Ast_Node(open val context: ParserRuleContext) {
    abstract fun accept(visitor: AstVisitor)
}

sealed class Ast_Statement(override val context: ParserRuleContext) : Ast_Node(context)

data class Ast_Cte(
    override val context: ParserRuleContext,
    val name: String,
    val selectStatement: Ast_SelectStatement) : Ast_Node(context) {

    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_Cte(this)
        selectStatement.accept(visitor)
    }
}

data class Ast_SelectStatement(
    override val context: ParserRuleContext,
    val with: List<Ast_Cte>,
    val selectClause: Ast_SelectClause,
    val orderByClause: Ast_Expression?
) : Ast_Statement(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_SelectStatement(this)
        with.forEach { it.accept(visitor) }
        selectClause.accept(visitor)
        orderByClause?.accept(visitor)
    }
}

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
) : Ast_Node(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_SelectCombineOperator(this)
    }
}

data class Ast_CombineSelectClause(
    override val context: ParserRuleContext,
    val operator: Ast_SelectCombineOperator,
    val modifier: SelectCombineOperatorModifier?,
    val left: Ast_SelectClause,
    val right: Ast_SelectClause
): Ast_SelectClause(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_CombineSelectClause(this)
        operator.accept(visitor)
        left.accept(visitor)
        right.accept(visitor)
    }
}


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
    val into: Ast_OptTempTableName?,
    val where: Ast_Expression?,
    val having: Ast_Expression?,
    val qualify: Ast_Expression?
    // TODO: add GROUP BY, WINDOW etc.
): Ast_SelectClause(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_CoreSelectClause(this)
        targets.forEach { it.accept(visitor) }
        from?.accept(visitor)
        into?.accept(visitor)
    }
}

/**
 * Represents a nested SELECT statement as a clause.
 * This is used for subqueries in the FROM clause or as a value source.
 *
 * SELECT ... FROM (WITH ... SELECT ...)
 *                  ^^^^^^^^^^^^^^^^^^^
 */
data class Ast_NestedSelectStatementClause(
    override val context: ParserRuleContext,
    val selectStatement: Ast_SelectStatement
): Ast_SelectClause(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_NestedSelectStatementClause(this)
        selectStatement.accept(visitor)
    }
}

/**
 * Represents a VALUES clause in SQL.
 * Note: grammatically, it is a SELECT clause.
 *
 * VALUES (value1, value2, ...), (value3, value4, ...)
 */
data class Ast_ValuesSelectClause(
    override val context: ParserRuleContext,
    val values: List<List<Ast_Expression>>
): Ast_SelectClause(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_ValuesSelectClause(this)
        values.forEach { row ->
            row.forEach { it.accept(visitor) }
        }
    }
}

sealed class Ast_SelectTarget(override val context: ParserRuleContext) : Ast_Node(context)

data class Ast_StarSelectTarget(
    override val context: ParserRuleContext
): Ast_SelectTarget(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_StarSelectTarget(this)
    }
}

data class Ast_ColumnSelectTarget(
    override val context: ParserRuleContext,
    val expression: Ast_Expression,
    val alias: String?
): Ast_SelectTarget(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_ColumnSelectTarget(this)
        expression.accept(visitor)
    }
}


data class Ast_SortClause(
    override val context: ParserRuleContext,
    val orderBy: List<Ast_SortOrder>
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_SortClause(this)
        orderBy.forEach { it.accept(visitor) }
    }
}

enum class SortOrderType {
    ASC,
    DESC
}

data class Ast_SortOrder(
    override val context: ParserRuleContext,
    val order: SortOrderType,
    val expression: Ast_Expression
): Ast_Node(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_SortOrder(this)
        expression.accept(visitor)
    }
}


data class Ast_From(
    override val context: ParserRuleContext,
    val elements: List<Ast_FromElement>
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_From(this)
        elements.forEach { it.accept(visitor) }
    }
}

class Ast_FromElement(
    override val context: ParserRuleContext,
    val src: Ast_SimpleFromElement,
    val joins: List<Ast_Join>
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_FromElement(this)
        src.accept(visitor)
        joins.forEach { it.accept(visitor) }
    }
}

sealed class Ast_SimpleFromElement(
    override val context: ParserRuleContext,
    open val alias: String?
): Ast_Node(context)

data class Ast_SimpleFromTableRef(
    override val context: ParserRuleContext,
    val tableFqn: String,
    override val alias: String?
): Ast_SimpleFromElement(context, alias) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_SimpleFromTableRef(this)
    }
}

data class Ast_SimpleFromSubQuery(
    override val context: ParserRuleContext,
    val subQuery: Ast_SelectStatement,
    override val alias: String?
): Ast_SimpleFromElement(context, alias) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_SimpleFromSubQueryRef(this)
        subQuery.accept(visitor)
    }
}

data class Ast_SimpleNamedFromElement(
    override val context: ParserRuleContext,
    val from: Ast_FromElement,
    override val alias: String?
): Ast_SimpleFromElement(context, alias) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_SimpleNamedFromElement(this)
        from.accept(visitor)
    }
}

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
): Ast_JoinOperator(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_QualifiedJoinOperator(this)
    }
}

data class Ast_CrossJoinOperator(
    override val context: ParserRuleContext
): Ast_JoinOperator(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_CrossJoinOperator(this)
    }
}

sealed class Ast_Join(
    override val context: ParserRuleContext,
    open val joinOperator: Ast_JoinOperator?,
    open val joinTo: Ast_FromElement
): Ast_Node(context)

data class Ast_CrossJoin(
    override val context: ParserRuleContext,
    override val joinTo: Ast_FromElement
    // TODO: no context for CROSS keyword
): Ast_Join(context, Ast_CrossJoinOperator(context), joinTo) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_CrossJoin(this)
        joinOperator?.accept(visitor)
        joinTo.accept(visitor)
    }
}

data class Ast_QualifiedJoin(
    override val context: ParserRuleContext,
    override val joinOperator: Ast_QualifiedJoinOperator?,
    override val joinTo: Ast_FromElement,
    val condition: Ast_JoinCondition
): Ast_Join(context, joinOperator, joinTo) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_QualifiedJoin(this)
        joinOperator?.accept(visitor)
        joinTo.accept(visitor)
        condition.accept(visitor)
    }
}

sealed class Ast_JoinCondition(override val context: ParserRuleContext): Ast_Node(context)

data class Ast_JoinOnCondition(
    override val context: ParserRuleContext,
    val expression: Ast_Expression
): Ast_JoinCondition(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_JoinOnCondition(this)
        expression.accept(visitor)
    }
}

data class Ast_JoinUsingCondition(
    override val context: ParserRuleContext,
    val columns: List<String>
): Ast_JoinCondition(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_JoinUsingCondition(this)
    }
}

data class Ast_OptTempTableName(
    override val context: ParserRuleContext,
    val tableFqn: String,
    val isTemporary: Boolean
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_OptTempTableName(this)
    }
}

open class Ast_Expression(override val context: ParserRuleContext) : Ast_Node(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_Expression(this)
    }
}

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

sealed class Ast_BinaryOperator<T>(
    override val context: ParserRuleContext,
    open val operator: T
): Ast_Expression(context)

data class Ast_ReservedBinaryOperator(
    override val context: ParserRuleContext,
    override val operator: BinaryOperator
): Ast_BinaryOperator<BinaryOperator>(context, operator) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_ReservedBinaryOperator(this)
    }
}

data class Ast_QualifiedBinaryOperator(
    override val context: ParserRuleContext,
    override val operator: String
): Ast_BinaryOperator<String>(context, operator) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_QualifiedBinaryOperator(this)
    }
}

class Ast_BinaryOperatorExpression(
    override val context: ParserRuleContext,
    val left: Ast_Expression,
    val right: Ast_Expression,
    val operator: Ast_BinaryOperator<*>
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_BinaryOperatorExpression(this)
        left.accept(visitor)
        operator.accept(visitor)
        right.accept(visitor)
    }
}

class Ast_BetweenExpression(
    override val context: ParserRuleContext,
    val target: Ast_Expression,
    val lowerBound: Ast_Expression,
    val upperBound: Ast_Expression,
    val isNot: Boolean,
    val isSymmetric: Boolean
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_BetweenExpression(this)
        target.accept(visitor)
        lowerBound.accept(visitor)
        upperBound.accept(visitor)
    }
}

class Ast_InExpression(
    override val context: ParserRuleContext,
    val target: Ast_Expression,
    val values: Ast_Expression,
    val isNot: Boolean
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_InExpression(this)
        target.accept(visitor)
        values.accept(visitor)
    }
}

sealed class Ast_InSource(
    override val context: ParserRuleContext,
): Ast_Expression(context)

class Ast_InValuesSource(
    override val context: ParserRuleContext,
    val values: List<Ast_Expression>
): Ast_InSource(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_InValuesSource(this)
        values.forEach { it.accept(visitor) }
    }
}

class Ast_InSelectSource(
    override val context: ParserRuleContext,
    val selectStatement: Ast_SelectStatement
): Ast_InSource(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_InSelectSource(this)
        selectStatement.accept(visitor)
    }
}

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
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_UnaryOperatorExpression(this)
        expression.accept(visitor)
    }
}

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
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_LikeExpression(this)
        target.accept(visitor)
        pattern.accept(visitor)
        escape?.accept(visitor)
    }
}

class Ast_CollateExpression(
    override val context: ParserRuleContext,
    val expression: Ast_Expression,
    val collation: String
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_CollateExpression(this)
        expression.accept(visitor)
    }
}

class Ast_CastExpression(
    override val context: ParserRuleContext,
    val expression: Ast_Expression,
    val targetType: String
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_CastExpression(this)
        expression.accept(visitor)
    }
}

class Ast_ExistsExpression(
    override val context: ParserRuleContext,
    val selectStatement: Ast_SelectStatement
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_ExistsExpression(this)
        selectStatement.accept(visitor)
    }
}

class Ast_SelectExpression(
    override val context: ParserRuleContext,
    val selectStatement: Ast_SelectStatement,
    val indirections: List<Ast_Indirection>?
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_SelectExpression(this)
        selectStatement.accept(visitor)
        indirections?.forEach {
            it.accept(visitor)
        }
    }
}

class Ast_Indirection(
    override val context: ParserRuleContext,
    val attrName: String
): Ast_Node(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_Indirection(this)
    }
}

class Ast_ColumnReference(
    override val context: ParserRuleContext,
    val name: List<String>
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_ColumnReference(this)
    }
}

class Ast_ConstantExpression(
    override val context: ParserRuleContext,
    val text: String
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_ConstantExpression(this)
    }
}

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
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_CaseExpression(this)
        case?.accept(visitor)
        whenClauses.forEach { it.accept(visitor) }
        elseClause?.accept(visitor)
    }
}

data class Ast_WhenClause(
    override val context: ParserRuleContext,
    val condition: Ast_Expression,
    val result: Ast_Expression
): Ast_Node(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_WhenClause(this)
        condition.accept(visitor)
        result.accept(visitor)
    }
}

data class Ast_FunctionCallExpression(
    override val context: ParserRuleContext,
    val functionName: String,
    val arguments: List<Ast_Expression>,
    val isAll: Boolean,
    val isDistinct: Boolean,
    val isStar: Boolean,
    val isIgnoreNulls: Boolean,
    val sort: Ast_SortClause?,
    val withinGroup: Ast_SortClause?,
    val filter: Ast_Expression?,
    val over: Ast_OverClause?
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_FunctionCallExpression(this)
        arguments.forEach { it.accept(visitor) }
        sort?.accept(visitor)
        withinGroup?.accept(visitor)
        filter?.accept(visitor)
        over?.accept(visitor)
    }
}

data class Ast_CommonFunctionCallExpression(
    override val context: ParserRuleContext,
    val text: String,
    val subExpressions: List<Ast_Expression>
): Ast_Expression(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_CommonFunctionCallExpression(this)
        subExpressions.forEach { it.accept(visitor) }
    }
}

sealed class Ast_OverClause(
    override val context: ParserRuleContext
): Ast_Node(context)

data class Ast_OverWindowName(
    override val context: ParserRuleContext,
    val windowName: String
): Ast_OverClause(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_OverWindowName(this)
    }
}

data class Ast_OverWindowSpecification(
    override val context: ParserRuleContext,
    val windowName: String?,
    val partitionBy: List<Ast_Expression>?,
    val orderBy: Ast_SortClause?,
    val frame: Ast_FrameClause?
) : Ast_OverClause(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_OverWindowSpecification(this)
        partitionBy?.forEach { it.accept(visitor) }
        orderBy?.accept(visitor)
        frame?.accept(visitor)
    }
}

data class Ast_FrameClause(
    override val context: ParserRuleContext,
    val text: String
): Ast_Node(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_FrameClause(this)
    }
}

enum class DropBehavior {
    RESTRICT,
    CASCADE
}

data class Ast_DropStatement(
    override val context: ParserRuleContext,
    val names: List<String>,
    val ifExists: Boolean,
    val dropBehavior: DropBehavior? = null
): Ast_Statement(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_DropStatement(this)
    }
}

data class Ast_InsertStatement(
    override val context: ParserRuleContext,
    val with: List<Ast_Cte>,
    val into: Ast_InsertTarget,
    val selectStatement: Ast_SelectStatement
): Ast_Statement(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_InsertStatement(this)
        with.forEach { it.accept(visitor) }
        into.accept(visitor)
        selectStatement.accept(visitor)
    }
}

data class Ast_InsertTarget(
    override val context: ParserRuleContext,
    val targetFqn: String,
    val alias: String?,
    val columns: List<String>?
): Ast_Node(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_InsertTarget(this)
    }
}

data class Ast_DeleteStatement(
    override val context: ParserRuleContext,
    val with: List<Ast_Cte>,
    val from: Ast_SimpleFromTableRef,
    val where: Ast_Expression?
): Ast_Statement(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_DeleteStatement(this)
        with.forEach { it.accept(visitor) }
        from.accept(visitor)
        where?.accept(visitor)
    }
}

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
): Ast_CreateTableStatement(context, tableName, ifNotExists, isTemporary) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_CreateTableWithColumnDefinitions(this)
        columns.forEach { it.accept(visitor) }
    }
}

data class Ast_CreateTableAsSelect(
    override val context: ParserRuleContext,
    override val tableName: String,
    override val ifNotExists: Boolean,
    override val isTemporary: Boolean,
    val selectStatement: Ast_SelectStatement
): Ast_CreateTableStatement(context, tableName, ifNotExists, isTemporary) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_CreateTableAsSelect(this)
        selectStatement.accept(visitor)
    }
}

data class Ast_ColumnDefinition(
    override val context: ParserRuleContext,
    // TODO: implement parsing column definition
    val text: String
): Ast_Node(context) {
    override fun accept(visitor: AstVisitor) {
        visitor.visitAst_ColumnDefinition(this)
    }
}
