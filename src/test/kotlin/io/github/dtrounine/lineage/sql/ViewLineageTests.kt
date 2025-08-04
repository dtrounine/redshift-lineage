/**
 * MIT License with Commons Clause v1.0
 *
 * Copyright © 2025 Dmitrii Trunin (dtrounine@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
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
 *    For purposes of this condition, **"Sell"** means practicing any or all of
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
 * 3. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *    THE SOFTWARE.
 */
package io.github.dtrounine.lineage.sql

import org.junit.jupiter.api.Test

class ViewLineageTests {

    @Test
    fun testCreateViewSimpleSelect() = assertLineage(
        """
            CREATE VIEW simple_view AS
            SELECT * FROM source_table
        """,
        """
            lineage:
              simple_view:
                - source_table
            sources:
              - source_table
        """)

    @Test
    fun testCreateOrReplaceViewSimpleSelect() = assertLineage(
        """
            CREATE OR REPLACE VIEW simple_view AS
            SELECT * FROM source_table
        """,
        """
            lineage:
              simple_view:
                - source_table
            sources:
              - source_table
        """)

    @Test
    fun testCreateViewWithQualifiedNames() = assertLineage(
        """
            CREATE VIEW analytics.weather_summary AS
            SELECT * FROM climate_data.temperature_readings
        """,
        """
            lineage:
              analytics.weather_summary:
                - climate_data.temperature_readings
            sources:
              - climate_data.temperature_readings
        """)

    @Test
    fun testCreateOrReplaceViewComplexQuery() = assertLineage(
        """
            CREATE OR REPLACE VIEW analytics.weather_summary AS
            SELECT 
                date_trunc('month', measurement_date) as month,
                avg(temperature) as avg_temp,
                count(*) as measurement_count
            FROM climate_data.temperature_readings
            WHERE measurement_date >= '2023-01-01'
            GROUP BY date_trunc('month', measurement_date)
        """,
        """
            lineage:
              analytics.weather_summary:
                - climate_data.temperature_readings
            sources:
              - climate_data.temperature_readings
        """)

    @Test
    fun testCreateViewWithJoin() = assertLineage(
        """
            CREATE VIEW customer_orders AS
            SELECT 
                c.customer_id,
                c.customer_name,
                o.order_id,
                o.order_total
            FROM customers c
            JOIN orders o ON c.customer_id = o.customer_id
        """,
        """
            lineage:
              customer_orders:
                - customers
                - orders
            sources:
              - customers
              - orders
        """)

    @Test
    fun testCreateOrReplaceViewWithMultipleJoins() = assertLineage(
        """
            CREATE OR REPLACE VIEW sales_summary AS
            SELECT 
                c.customer_name,
                p.product_name,
                SUM(oi.quantity * oi.unit_price) as total_sales
            FROM customers c
            JOIN orders o ON c.customer_id = o.customer_id
            JOIN order_items oi ON o.order_id = oi.order_id
            JOIN products p ON oi.product_id = p.product_id
            GROUP BY c.customer_name, p.product_name
        """,
        """
            lineage:
              sales_summary:
                - customers
                - orders
                - order_items
                - products
            sources:
              - customers
              - orders
              - order_items
              - products
        """)

    @Test
    fun testCreateViewWithSubquery() = assertLineage(
        """
            CREATE VIEW high_value_customers AS
            SELECT customer_id, customer_name
            FROM customers
            WHERE customer_id IN (
                SELECT customer_id 
                FROM orders 
                WHERE order_total > 1000
            )
        """,
        """
            lineage:
              high_value_customers:
                - customers
                - orders
            sources:
              - customers
              - orders
        """)

    @Test
    fun testCreateViewWithCTE() = assertLineage(
        """
            CREATE VIEW monthly_sales AS
            WITH monthly_totals AS (
                SELECT 
                    DATE_TRUNC('month', order_date) as month,
                    SUM(order_total) as total
                FROM orders
                GROUP BY DATE_TRUNC('month', order_date)
            )
            SELECT month, total
            FROM monthly_totals
            WHERE total > 10000
        """,
        """
            lineage:
              monthly_sales:
                - orders
            sources:
              - orders
        """)

    @Test
    fun testCreateOrReplaceViewWithMultipleCTEs() = assertLineage(
        """
            CREATE OR REPLACE VIEW customer_analytics AS
            WITH customer_totals AS (
                SELECT customer_id, SUM(order_total) as total_spent
                FROM orders
                GROUP BY customer_id
            ),
            customer_details AS (
                SELECT 
                    c.customer_id,
                    c.customer_name,
                    ct.total_spent
                FROM customers c
                JOIN customer_totals ct ON c.customer_id = ct.customer_id
            )
            SELECT customer_id, customer_name, total_spent
            FROM customer_details
            WHERE total_spent > 5000
        """,
        """
            lineage:
              customer_analytics:
                - orders
                - customers
            sources:
              - orders
              - customers
        """)

    @Test
    fun testCreateViewWithUnion() = assertLineage(
        """
            CREATE VIEW all_transactions AS
            SELECT transaction_id, amount, 'sales' as type
            FROM sales_transactions
            UNION ALL
            SELECT transaction_id, amount, 'returns' as type
            FROM return_transactions
        """,
        """
            lineage:
              all_transactions:
                - sales_transactions
                - return_transactions
            sources:
              - sales_transactions
              - return_transactions
        """)

    @Test
    fun testCreateViewFromAnotherView() = assertLineage(
        """
            CREATE VIEW summary_view AS
            SELECT * FROM existing_view
            WHERE active = true
        """,
        """
            lineage:
              summary_view:
                - existing_view
            sources:
              - existing_view
        """)
}