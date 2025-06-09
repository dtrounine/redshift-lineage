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
package io.github.dtrounine.lineage.util

import io.github.dtrounine.lineage.model.LineageInfo
import io.github.dtrounine.lineage.model.SourcePosition
import io.github.dtrounine.lineage.model.TextPosition
import io.github.dtrounine.lineage.sql.ast.Ast_Node

fun mergeLineage(left: Map<String, Set<String>>, right: Map<String, Set<String>>): Map<String, Set<String>> {
    val merged: MutableMap<String, Set<String>> = mutableMapOf()

    for ((key, value) in left) {
        merged[key] = value.toMutableSet()
    }

    for ((key, value) in right) {
        merged.merge(key, value.toMutableSet()) { oldValue, newValue -> oldValue union newValue }
    }

    return merged
}

/**
 * Merges two [LineageInfo] instances by combining their lineage maps and their lists of sources.
 */
fun LineageInfo.mergeAll(other: LineageInfo): LineageInfo = LineageInfo(
    lineage = mergeLineage(this.lineage, other.lineage),
    sources = this.sources union other.sources,
    context = this.context
)

/**
 * Merges two [LineageInfo] instances by combining their lineage maps,
 * but does not merge the sources.
 */
fun LineageInfo.mergeOnlyLineage(other: LineageInfo): LineageInfo = LineageInfo(
    lineage = mergeLineage(this.lineage, other.lineage),
    sources = this.sources, // Sources are not merged,
    context = this.context
)

fun LineageInfo.println() {
    println("LineageInfo:")
    println("  lineage:")
    for ((key, value) in lineage) {
        println("    - $key -> ${ value.joinToString(", ")}")
    }
    println("  sources:")
    for (source in sources) {
        println("    - $source")
    }
}

fun sourcePositionFromAst(ast: Ast_Node): SourcePosition? {
    val sourcePosition: SourcePosition? = ast.context.start?.let { start ->
        ast.context.stop?.let { stop ->
            SourcePosition(
                start = TextPosition(start.line, start.charPositionInLine),
                stop = TextPosition(stop.line, stop.charPositionInLine + (stop.text?.length ?: 0))
            )
        }
    }
    return sourcePosition
}

fun mergeSourcePositions(
    left: SourcePosition?,
    right: SourcePosition?
): SourcePosition? {
    if (left == null) return right
    if (right == null) return left

    val start = minOf(left.start, right.start)
    val stop = maxOf(left.stop, right.stop)

    return SourcePosition(
        start = start,
        stop = stop
    )
}
