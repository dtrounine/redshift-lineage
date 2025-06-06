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