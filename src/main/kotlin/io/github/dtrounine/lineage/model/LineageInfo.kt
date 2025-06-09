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
package io.github.dtrounine.lineage.model

import kotlinx.serialization.Serializable

/**
 * Represents the lineage information extracted from some input by the lineage extractor tool.
 *
 * Note: this lineage information may relate to a single SQL statement or a whole SQL script, this is
 * determined by the context in which this information is used.
 */
@Serializable
data class LineageInfo(
    /**
     * Map of sinks to their sources (sources may be empty).
     */
    val lineage: Map<String, Set<String>>,

    /**
     * Set of all discovered sources including those not used for any named sink.
     */
    val sources: Set<String>,

    /**
     * Context information about the source of this lineage information, such as the file name and position
     * in the source text.
     */
    val context: SourceContextInfo? = null
) {
    fun withContext(context: SourceContextInfo): LineageInfo {
        return LineageInfo(
            lineage = this.lineage,
            sources = this.sources,
            context = context
        )
    }

    companion object {
        fun newEmpty(): LineageInfo {
            return LineageInfo(
                lineage = emptyMap(),
                sources = emptySet(),
                context = null
            )
        }
    }
}
