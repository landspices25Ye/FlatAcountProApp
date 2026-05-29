package com.example

import com.example.core.auth.AppModule
import com.example.core.auth.PermissionEngine
import com.example.core.auth.SystemRole
import com.example.core.auth.UserAction
import com.example.core.utils.FinancialUtils
import com.example.core.utils.FormatUtils
import com.example.core.utils.ITreeNode
import com.example.core.utils.TreeUtils
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class CoreUnitTest {

    @Test
    fun test_financial_rounding() {
        assertEquals(10.55, FinancialUtils.roundToDecimal(10.554), 0.0)
        assertEquals(10.56, FinancialUtils.roundToDecimal(10.556), 0.0)
        assertEquals(0.0, FinancialUtils.roundToDecimal(Double.NaN), 0.0)
    }

    @Test
    fun test_financial_calculate_line_total_tax_inclusive() {
        // Qty: 2, Price: 115, Tax Inclusive (VAT is 15%)
        // Total = 230 -> VAT Amount = 30 -> Base = 200
        val totals = FinancialUtils.calculateLineTotal(qty = 2.0, price = 115.0, isTaxInclusive = true)
        
        assertEquals(230.0, totals.totalAmount, 0.0)
        assertEquals(30.0, totals.taxAmount, 0.0)
        assertEquals(200.0, totals.baseAmount, 0.0)
    }

    @Test
    fun test_financial_calculate_line_total_tax_exclusive() {
        // Qty: 2, Price: 100, Tax Exclusive
        // Subtotal = 200 -> VAT = 30 -> Total = 230
        val totals = FinancialUtils.calculateLineTotal(qty = 2.0, price = 100.0, isTaxInclusive = false)
        
        assertEquals(230.0, totals.totalAmount, 0.0)
        assertEquals(30.0, totals.taxAmount, 0.0)
        assertEquals(200.0, totals.baseAmount, 0.0)
    }

    @Test
    fun test_financial_double_entry_balance() {
        val debits = listOf(100.0, 50.0)
        val credits = listOf(150.0)
        assertTrue(FinancialUtils.isEntryBalanced(debits, credits))

        val unbalancedDebits = listOf(100.0)
        val unbalancedCredits = listOf(150.0)
        assertFalse(FinancialUtils.isEntryBalanced(unbalancedDebits, unbalancedCredits))
    }

    @Test
    fun test_moving_weighted_average() {
        // Stock: 10 units at 5.0 cost. Purchase 10 units at 15.0 cost.
        // Expected Average Cost = ((10*5) + (10*15)) / 20 = 10.0
        val avgCost = FinancialUtils.calculateMovingWeightedAverage(
            currentStockQty = 10.0,
            currentAvgCost = 5.0,
            incomingQty = 10.0,
            incomingUnitCost = 15.0
        )
        assertEquals(10.0, avgCost, 0.0)
    }

    // Dummy TreeNode for testing
    data class TestNode(
        override val id: Long,
        override val parentId: Long?,
        val value: String
    ) : ITreeNode

    @Test
    fun test_tree_builder() {
        val flatList = listOf(
            TestNode(1, null, "أصول متداولة"),
            TestNode(2, 1, "الصندوق"),
            TestNode(3, 1, "البنك"),
            TestNode(4, null, "خصوم متداولة")
        )

        val tree = TreeUtils.buildTree(flatList)
        assertEquals(2, tree.size) // roots: Root 1 & Root 4

        val assetsRoot = tree.find { it.data.id == 1L }
        assertNotNull(assetsRoot)
        assertEquals(2, assetsRoot!!.children.size) // children: Node 2 & Node 3
    }

    @Test
    fun test_format_currency() {
        val formatted = FormatUtils.formatCurrency(1500.0, "SAR", Locale.US)
        assertTrue(formatted.contains("SAR") || formatted.contains("1,500"))
    }

    @Test
    fun test_format_date() {
        val timestamp = 1779836400000L // arbitrary timestamp
        val dateStr = FormatUtils.formatDate(timestamp, "yyyy-MM-dd", Locale.US)
        assertNotNull(dateStr)
    }

    @Test
    fun test_validations() {
        assertTrue(FormatUtils.isValidEmail("info@aistudio.com"))
        assertFalse(FormatUtils.isValidEmail("invalid-email"))

        assertTrue(FormatUtils.isValidPhone("+966500000000"))
        assertFalse(FormatUtils.isValidPhone("short"))

        assertTrue(FormatUtils.isValidTaxNumber("310123456789013"))
        assertFalse(FormatUtils.isValidTaxNumber("123"))
    }

    @Test
    fun test_permission_engine() {
        // Admin rule
        assertTrue(PermissionEngine.canUserPerformAction(SystemRole.ADMIN, AppModule.ACCOUNTING, UserAction.DELETE))

        // Accountant rule
        assertTrue(PermissionEngine.canUserPerformAction(SystemRole.ACCOUNTANT, AppModule.ACCOUNTING, UserAction.VIEW))
        assertTrue(PermissionEngine.canUserPerformAction(SystemRole.ACCOUNTANT, AppModule.REPORTS, UserAction.EXPORT))
        assertFalse(PermissionEngine.canUserPerformAction(SystemRole.ACCOUNTANT, AppModule.PAYROLL, UserAction.DELETE))

        // HR rules
        assertTrue(PermissionEngine.canUserPerformAction(SystemRole.HR_MANAGER, AppModule.PAYROLL, UserAction.CREATE))
        assertFalse(PermissionEngine.canUserPerformAction(SystemRole.HR_MANAGER, AppModule.ACCOUNTING, UserAction.POST_JOURNAL))
    }
}
