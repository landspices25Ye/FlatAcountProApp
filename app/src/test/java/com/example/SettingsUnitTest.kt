package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.errors.BusinessRuleError
import com.example.core.settings.CompanyProfile
import com.example.core.settings.SettingsManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsUnitTest {

    private lateinit var context: Context
    private lateinit var settingsManager: SettingsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Initialize real local manager
        settingsManager = SettingsManager.initialize(context)
    }

    @Test
    fun test_company_profile_default_and_save() {
        val original = settingsManager.getCompanyProfile()
        assertEquals("المحاسب المالي المبتكر", original.name)
        assertEquals("SAR", original.currency)
        assertEquals(15.0, original.taxRate, 0.0)

        val modified = original.copy(
            name = "شركة التجهيزات الجديدة",
            taxRate = 10.0,
            currency = "USD"
        )
        settingsManager.saveCompanyProfile(modified)

        val retrieved = settingsManager.getCompanyProfile()
        assertEquals("شركة التجهيزات الجديدة", retrieved.name)
        assertEquals("USD", retrieved.currency)
        assertEquals(10.0, retrieved.taxRate, 0.0)
    }

    @Test
    fun test_fiscal_period_rules() {
        val periods = settingsManager.getFiscalPeriods()
        assertTrue(periods.isNotEmpty())
        assertEquals(12, periods.size)

        val firstPeriod = periods.first()
        assertEquals("2026-01", firstPeriod.id)
        assertEquals("OPEN", firstPeriod.status)

        // Attempt to lock an open period should fail
        assertThrows(BusinessRuleError::class.java) {
            settingsManager.updateFiscalPeriodStatus(firstPeriod.id, "LOCKED")
        }

        // Close it first
        settingsManager.updateFiscalPeriodStatus(firstPeriod.id, "CLOSED")
        val closedPeriod = settingsManager.getFiscalPeriods().first()
        assertEquals("CLOSED", closedPeriod.status)

        // Now lock it
        settingsManager.updateFiscalPeriodStatus(firstPeriod.id, "LOCKED")
        val lockedPeriod = settingsManager.getFiscalPeriods().first()
        assertEquals("LOCKED", lockedPeriod.status)

        // Attempting to reopen a locked period should fail
        assertThrows(BusinessRuleError::class.java) {
            settingsManager.updateFiscalPeriodStatus(firstPeriod.id, "CLOSED")
        }
    }

    @Test
    fun test_number_sequence_increment() {
        val nextInvoiceNumber = settingsManager.getAndIncrementNextNumber("INVOICE")
        assertTrue(nextInvoiceNumber.startsWith("INV-"))
        assertTrue(nextInvoiceNumber.length >= 8)

        // Lower sequence number constraint check
        assertThrows(BusinessRuleError::class.java) {
            settingsManager.updateSequence("INVOICE", "INV-", 2)
        }
    }

    @Test
    fun test_unit_of_measure_operations() {
        val originalUnits = settingsManager.getUnitsOfMeasure()
        assertTrue(originalUnits.any { it.id == "box_12" })

        // Check adding custom unit of measure
        settingsManager.addUnitOfMeasure(
            name = "دسته (10 حبات)",
            symbol = "دسته",
            baseUnitId = "piece",
            factor = 10.0
        )

        val updatedUnits = settingsManager.getUnitsOfMeasure()
        assertTrue(updatedUnits.any { it.symbol == "دسته" })

        // Validate basic mathematical conversion using variables
        val pieceUnit = updatedUnits.find { it.id == "piece" }!!
        val boxUnit = updatedUnits.find { it.id == "box_12" }!!
        val dasteUnit = updatedUnits.find { it.symbol == "دسته" }!!

        // Convert 2 Boxes to pieces: 2 * 12 = 24 pieces
        val valueInPiece = 2.0 * boxUnit.factor
        assertEquals(24.0, valueInPiece, 0.0)

        // Convert that to daste: 24 / 10 = 2.4 dastes
        val valueInDaste = valueInPiece / dasteUnit.factor
        assertEquals(2.4, valueInDaste, 0.0)
    }

    @Test
    fun test_cost_center_structure() {
        val originalCenters = settingsManager.getCostCenters()
        assertTrue(originalCenters.any { it.code == "CC100" })

        // Adding a duplicate code should throw an error
        assertThrows(BusinessRuleError::class.java) {
            settingsManager.addCostCenter("CC100", "إعادة المحاولة", null)
        }
    }
}
