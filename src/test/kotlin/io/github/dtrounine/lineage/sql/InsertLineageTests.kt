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
package io.github.dtrounine.lineage.sql

import org.junit.jupiter.api.Test

class InsertLineageTests {

    @Test
    fun testInsertLineage() = assertLineage(
        """
            INSERT INTO users (id, name, age)
            VALUES (1, 'Alice', 30), (2, 'Bob', 25);
        """,
        """
            lineage:
              users: []
            sources: []
        """)

    @Test
    fun testInsertLineageWithSelect() = assertLineage(
        """
            INSERT INTO users (id, name, age)
            SELECT id, name, age FROM temp_users;
        """,
        """
            lineage:
              users:
                - temp_users
            sources:
              - temp_users
        """)

    @Test
    fun testInsertLineageWithCte() = assertLineage(
        """
            WITH new_users AS (
                SELECT id, name, age FROM temp_users
            )
            INSERT INTO users (id, name, age)
            SELECT id, name, age FROM new_users;
        """,
        """
            lineage:
              users:
                - temp_users
            sources:
              - temp_users
        """)

    @Test
    fun testInsertLineageWithCte2() = assertLineage(
        """
            WITH new_users AS (
                SELECT id, name, age FROM temp_users
            )
            , staging_users AS (
                SELECT id, name, age FROM new_users WHERE age > 20
            )
            INSERT INTO users (id, name, age)
            SELECT id, name, age FROM staging_users;
        """,
        """
            lineage:
              users:
                - temp_users
            sources:
              - temp_users
        """)
}
