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
}
