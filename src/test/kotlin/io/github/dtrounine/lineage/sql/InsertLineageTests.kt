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
