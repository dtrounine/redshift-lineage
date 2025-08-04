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

interface AstVisitor {
    fun visitAst_Cte(cte: Ast_Cte)
    fun visitAst_SelectStatement(select: Ast_SelectStatement)
    fun visitAst_SelectCombineOperator(op: Ast_SelectCombineOperator)
    fun visitAst_CombineSelectClause(clause: Ast_CombineSelectClause)
    fun visitAst_CoreSelectClause(clause: Ast_CoreSelectClause)
    fun visitAst_ValuesSelectClause(clause: Ast_ValuesSelectClause)
    fun visitAst_StarSelectTarget(target: Ast_StarSelectTarget)
    fun visitAst_ColumnSelectTarget(target: Ast_ColumnSelectTarget)
    fun visitAst_SortClause(sort: Ast_SortClause)
    fun visitAst_SortOrder(order: Ast_SortOrder)
    fun visitAst_From(from: Ast_From)
    fun visitAst_FromElement(fromElement: Ast_FromElement)
    fun visitAst_SimpleFromTableRef(fromTable: Ast_SimpleFromTableRef)
    fun visitAst_SimpleFromSubQueryRef(subQuery: Ast_SimpleFromSubQuery)
    fun visitAst_SimpleNamedFromElement(named: Ast_SimpleNamedFromElement)
    fun visitAst_QualifiedJoinOperator(operator: Ast_QualifiedJoinOperator)
    fun visitAst_CrossJoinOperator(operator: Ast_CrossJoinOperator)
    fun visitAst_CrossJoin(join: Ast_CrossJoin)
    fun visitAst_QualifiedJoin(join: Ast_QualifiedJoin)
    fun visitAst_JoinOnCondition(condition: Ast_JoinOnCondition)
    fun visitAst_JoinUsingCondition(condition: Ast_JoinUsingCondition)
    fun visitAst_OptTempTableName(opt: Ast_OptTempTableName)
    fun visitAst_Expression(expr: Ast_Expression)
    fun visitAst_BinaryOperatorExpression(expr: Ast_BinaryOperatorExpression)
    fun visitAst_BetweenExpression(expr: Ast_BetweenExpression)
    fun visitAst_InExpression(expr: Ast_InExpression)
    fun visitAst_InValuesSource(source: Ast_InValuesSource)
    fun visitAst_InSelectSource(source: Ast_InSelectSource)
    fun visitAst_UnaryOperatorExpression(expr: Ast_UnaryOperatorExpression)
    fun visitAst_LikeExpression(expr: Ast_LikeExpression)
    fun visitAst_CollateExpression(expr: Ast_CollateExpression)
    fun visitAst_CastExpression(expr: Ast_CastExpression)
    fun visitAst_ExistsExpression(expr: Ast_ExistsExpression)
    fun visitAst_SelectExpression(expr: Ast_SelectExpression)
    fun visitAst_ColumnReference(expr: Ast_ColumnReference)
    fun visitAst_ConstantExpression(expr: Ast_ConstantExpression)
    fun visitAst_ImplicitRowExpression(expr: Ast_ImplicitRowExpression)
    fun visitAst_CaseExpression(expr: Ast_CaseExpression)
    fun visitAst_WhenClause(clause: Ast_WhenClause)
    fun visitAst_FunctionCallExpression(expr: Ast_FunctionCallExpression)
    fun visitAst_CommonFunctionCallExpression(expr: Ast_CommonFunctionCallExpression)
    fun visitAst_OverWindowName(name: Ast_OverWindowName)
    fun visitAst_OverWindowSpecification(spec: Ast_OverWindowSpecification)
    fun visitAst_FrameClause(clause: Ast_FrameClause)
    fun visitAst_DropStatement(stmt: Ast_DropStatement)
    fun visitAst_InsertStatement(stmt: Ast_InsertStatement)
    fun visitAst_InsertTarget(target: Ast_InsertTarget)
    fun visitAst_DeleteStatement(stmt: Ast_DeleteStatement)
    fun visitAst_CreateTableWithColumnDefinitions(stmt: Ast_CreateTableWithColumnDefinitions)
    fun visitAst_CreateTableAsSelect(stmt: Ast_CreateTableAsSelect)
    fun visitAst_ColumnDefinition(def: Ast_ColumnDefinition)
    fun visitAst_Indirection(ind: Ast_Indirection)
    fun visitAst_ReservedBinaryOperator(op: Ast_ReservedBinaryOperator)
    fun visitAst_QualifiedBinaryOperator(op: Ast_QualifiedBinaryOperator)
    fun visitAst_NestedSelectStatementClause(stmt: Ast_NestedSelectStatementClause)
    fun visitAst_WithClause(with: Ast_WithClause)
    fun visitAst_AlterTableRenameStatement(stmt: Ast_AlterRenameTableStatement)
    fun visitAst_CreateViewAsSelect(stmt: Ast_CreateViewAsSelect)
}
