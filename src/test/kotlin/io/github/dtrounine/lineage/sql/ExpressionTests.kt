package io.github.dtrounine.lineage.sql

import kotlin.test.Test


class ExpressionTests {

    @Test
    fun testExtract() = assertLineage(
        """
            SELECT
                EXTRACT('year' FROM birth_date) AS birth_year
            INTO birth_years
            FROM users WHERE age > 18;
        """,
        """
            lineage:
              birth_years:
                - users
            sources:
                - users
        """
    )


    @Test
    fun testExtractLineage() = assertLineage(
        """
            SELECT
                EXTRACT('year' FROM (SELECT birth_date FROM birth_dates WHERE user_id = u.id)) AS birth_year
            INTO birth_years
            FROM users AS u 
            WHERE age > 18;
        """,
        """
            lineage:
              birth_years:
                - users
                - birth_dates
            sources:
                - users
                - birth_dates
        """
    )
}