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

    override fun visitAst_ImplicitRowExpression(expr: Ast_ImplicitRowExpression) {
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

    override fun visitAst_WithClause(with: Ast_WithClause) {
    }

    override fun visitAst_AlterTableRenameStatement(stmt: Ast_AlterRenameTableStatement) {
    }

    override fun visitAst_CreateViewAsSelect(stmt: Ast_CreateViewAsSelect) {
    }
}
