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
}