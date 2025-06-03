package io.github.dtrounine.lineage.sql

import kotlin.test.Test

class SelectTests {

    @Test
    fun testLimit() = assertLineage(
        """
            SELECT id, name, age
            FROM users
            LIMIT 10;
        """,
        """
            lineage: {}
            sources:
              - users
        """
    )

    @Test
    fun testTop() = assertLineage(
        """
            SELECT TOP 10 id, name, age
            FROM users;
        """,
        """
            lineage: {}
            sources:
              - users
        """
    )
}