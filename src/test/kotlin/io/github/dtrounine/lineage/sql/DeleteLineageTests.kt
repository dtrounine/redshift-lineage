package io.github.dtrounine.lineage.sql

import kotlin.test.Test

class DeleteLineageTests {

    @Test
    fun testDeleteLineage_noSource() = assertLineage(
        """
            DELETE FROM users WHERE age < 18;
        """,
        """
            lineage:
                users: []
            sources: []
        """
    )

    @Test
    fun testDeleteLineage() = assertLineage(
        """
            DELETE FROM users 
            WHERE age < 18
            OR id IN (SELECT id FROM churned_users);
        """,
        """
            lineage:
                users:
                    - churned_users
            sources:
                - churned_users
        """
    )
}