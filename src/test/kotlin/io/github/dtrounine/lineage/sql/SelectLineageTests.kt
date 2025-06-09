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

class SelectLineageTests {

    @Test
    fun testSelectLineage_noSink() = assertLineage(
        """
            SELECT id, name, age FROM users;
        """,
        """
            lineage: {}
            sources:
              - users
        """
    )

    @Test
    fun testSelectLineage() = assertLineage(
        """
            SELECT id, name, age
            INTO new_users
            FROM users;
        """,
        """
            lineage:
                new_users:
                - users
            sources:
              - users
        """
    )

    @Test
    fun testSelectLineage_withCte() = assertLineage(
        """
            WITH user_data AS (
                SELECT id, name, age FROM users
            )
            SELECT id, name, age
            INTO new_users
            FROM user_data;
        """,
        """
            lineage:
                new_users:
                - users
            sources:
              - users
        """
    )

    @Test
    fun testSelectLineage_withCte2() = assertLineage(
        """
            WITH user_data AS (
                SELECT id, name, age FROM users
            ),
            staging_users AS (
                SELECT id, name, age FROM user_data WHERE age > 20
            )
            SELECT id, name, age
            INTO new_users
            FROM staging_users;
        """,
        """
            lineage:
                new_users:
                - users
            sources:
              - users
        """
    )

    @Test
    fun testSelectLineage_withCteJoin() = assertLineage(
        """
            WITH user_data AS (
                SELECT id, name, age FROM users
            ),
            staging_users AS (
                SELECT id, name, age 
                FROM user_data 
                    JOIN other_data ON user_data.id = other_data.user_id
                WHERE age > 20
            )
            SELECT id, name, age
            INTO new_users
            FROM staging_users;
        """,
        """
            lineage:
                new_users:
                - users
                - other_data
            sources:
              - users
              - other_data
        """
    )

    @Test
    fun testSelectLineage_withUnion() = assertLineage(
        """
            SELECT id, name, age FROM users
            UNION ALL
            SELECT id, name, age FROM other_users;
        """,
        """
            lineage: {}
            sources:
              - users
              - other_users
        """
    )

    @Test
    fun testSelectLineage_WhereExpression() = assertLineage(
        """
            SELECT id, name, age 
            INTO new_users
            FROM users 
            WHERE age BETWEEN (SELECT MIN(age) FROM adult_users) AND (SELECT MAX(age) FROM retired_users);
        """,
        """
            lineage:
              new_users:
                - users
                - adult_users
                - retired_users
            sources:
              - users
              - adult_users
              - retired_users
        """
    )

    @Test
    fun testSelectLineage_TargetExpression() = assertLineage(
        """
                SELECT 
                    id, name, age,
                    age IN (
                        SELECT age FROM adult_users
                        UNION ALL
                        SELECT age FROM retired_users
                    ) AS is_adult_or_retired
                INTO new_users
                FROM users 
            """,
        """
                lineage:
                  new_users:
                    - users
                    - adult_users
                    - retired_users
                sources:
                  - users
                  - adult_users
                  - retired_users
            """
    )

    @Test
    fun testNestedSelectStatementLineage() = assertLineage(
        """
            SELECT id, name, age
            INTO new_users
            FROM (
                (
                    WITH preprocessed_users AS 
                    (
                        SELECT id, name, age FROM users
                    )
                    SELECT * FROM preprocessed_users 
                )
                UNION ALL
                SELECT id, name, age FROM other_users
            ) AS combined_users;
        """,
        """
            lineage:
              new_users:
                - users
                - other_users
            sources:
              - users
              - other_users
        """
    )

    @Test
    fun testSelectQualifyLineage() = assertLineage(
        """
            SELECT id, name, MAX(age) OVER(PARTITION BY id ORDER BY age) AS max_age
            INTO new_users
            FROM users
            QUALIFY max_age > (SELECT AVG(age) FROM ref_users);
        """,
        """
            lineage:
              new_users:
                - users
                - ref_users
            sources:
              - users
              - ref_users
        """
    )
}