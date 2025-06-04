package io.github.dtrounine.lineage.sql.ast

abstract class AbstractAstVisitor: AstVisitor {

    override fun visitAst_Cte(cte: Ast_Cte) {
    }

    override fun visitAst_SelectStatement(select: Ast_SelectStatement) {
    }

    override fun visitAst_SelectCombineOperator(op: Ast_SelectCombineOperator) {
    }

    override fun visitAst_CombineSelectClause(clause: Ast_CombineSelectClause) {
    }

    override fun visitAst_CoreSelectClause(clause: Ast_CoreSelectClause) {
    }

    override fun visitAst_ValuesSelectClause(clause: Ast_ValuesSelectClause) {
    }

    override fun visitAst_StarSelectTarget(target: Ast_StarSelectTarget) {
    }

    override fun visitAst_ColumnSelectTarget(target: Ast_ColumnSelectTarget) {
    }

    override fun visitAst_SortClause(sort: Ast_SortClause) {
    }

    override fun visitAst_SortOrder(order: Ast_SortOrder) {
    }

    override fun visitAst_From(from: Ast_From) {
    }

    override fun visitAst_FromElement(fromElement: Ast_FromElement) {
    }

    override fun visitAst_SimpleFromTableRef(fromTable: Ast_SimpleFromTableRef) {
    }

    override fun visitAst_SimpleFromSubQueryRef(subQuery: Ast_SimpleFromSubQuery) {
    }

    override fun visitAst_SimpleNamedFromElement(named: Ast_SimpleNamedFromElement) {
    }

    override fun visitAst_QualifiedJoinOperator(operator: Ast_QualifiedJoinOperator) {
    }

    override fun visitAst_CrossJoinOperator(operator: Ast_CrossJoinOperator) {
    }

    override fun visitAst_CrossJoin(join: Ast_CrossJoin) {
    }

    override fun visitAst_QualifiedJoin(join: Ast_QualifiedJoin) {
    }

    override fun visitAst_JoinOnCondition(condition: Ast_JoinOnCondition) {
    }

    override fun visitAst_JoinUsingCondition(condition: Ast_JoinUsingCondition) {
    }

    override fun visitAst_OptTempTableName(opt: Ast_OptTempTableName) {
    }

    override fun visitAst_Expression(expr: Ast_Expression) {
    }

    override fun visitAst_BinaryOperatorExpression(expr: Ast_BinaryOperatorExpression) {
    }

    override fun visitAst_BetweenExpression(expr: Ast_BetweenExpression) {
    }

    override fun visitAst_InExpression(expr: Ast_InExpression) {
    }

    override fun visitAst_InValuesSource(source: Ast_InValuesSource) {
    }

    override fun visitAst_InSelectSource(source: Ast_InSelectSource) {
    }

    override fun visitAst_UnaryOperatorExpression(expr: Ast_UnaryOperatorExpression) {
    }

    override fun visitAst_LikeExpression(expr: Ast_LikeExpression) {
    }

    override fun visitAst_CollateExpression(expr: Ast_CollateExpression) {
    }

    override fun visitAst_CastExpression(expr: Ast_CastExpression) {
    }

    override fun visitAst_ExistsExpression(expr: Ast_ExistsExpression) {
    }

    override fun visitAst_SelectExpression(expr: Ast_SelectExpression) {
    }

    override fun visitAst_ColumnReference(expr: Ast_ColumnReference) {
    }

    override fun visitAst_ConstantExpression(expr: Ast_ConstantExpression) {
    }

    override fun visitAst_CaseExpression(expr: Ast_CaseExpression) {
    }

    override fun visitAst_WhenClause(clause: Ast_WhenClause) {
    }

    override fun visitAst_FunctionCallExpression(expr: Ast_FunctionCallExpression) {
    }

    override fun visitAst_CommonFunctionCallExpression(expr: Ast_CommonFunctionCallExpression) {
    }

    override fun visitAst_OverWindowName(name: Ast_OverWindowName) {
    }

    override fun visitAst_OverWindowSpecification(spec: Ast_OverWindowSpecification) {
    }

    override fun visitAst_FrameClause(clause: Ast_FrameClause) {
    }

    override fun visitAst_DropStatement(stmt: Ast_DropStatement) {
    }

    override fun visitAst_InsertStatement(stmt: Ast_InsertStatement) {
    }

    override fun visitAst_InsertTarget(target: Ast_InsertTarget) {
    }

    override fun visitAst_DeleteStatement(stmt: Ast_DeleteStatement) {
    }

    override fun visitAst_CreateTableWithColumnDefinitions(stmt: Ast_CreateTableWithColumnDefinitions) {
    }

    override fun visitAst_CreateTableAsSelect(stmt: Ast_CreateTableAsSelect) {
    }

    override fun visitAst_ColumnDefinition(def: Ast_ColumnDefinition) {
    }

    override fun visitAst_Indirection(ind: Ast_Indirection) {
    }

    override fun visitAst_ReservedBinaryOperator(op: Ast_ReservedBinaryOperator) {
    }

    override fun visitAst_QualifiedBinaryOperator(op: Ast_QualifiedBinaryOperator) {
    }

    override fun visitAst_NestedSelectStatementClause(stmt: Ast_NestedSelectStatementClause) {
    }
}
