package io.github.dtrounine.lineage.sql

import org.junit.jupiter.api.Test

class AlterTableTests {
    @Test
    fun testRenameTable() = assertLineage(
        """
            ALTER TABLE users RENAME TO new_users;
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
    fun testRenameTableFqn() = assertLineage(
        """
            ALTER TABLE analytics.users RENAME TO new_users;
        """,
        """
            lineage:
                analytics.new_users:
                - analytics.users
            sources:
              - analytics.users
        """
    )
}
