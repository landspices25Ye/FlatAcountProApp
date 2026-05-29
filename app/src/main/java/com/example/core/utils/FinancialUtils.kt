package com.example.core.utils

import java.math.BigDecimal
import java.math.RoundingMode

object FinancialUtils {

    const val VAT_RATE = 0.15

    /**
     * Round value to standard decimal precision (default 2 for currency, 4 for rates)
     */
    fun roundToDecimal(value: Double, decimals: Int = 2): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal(value)
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Compute total for a line item including discount and tax
     */
    fun calculateLineTotal(qty: Double, price: Double, discountPercentage: Double = 0.0, isTaxInclusive: Boolean = true): LineTotals {
        val rawSubtotal = qty * price
        val discountAmount = roundToDecimal(rawSubtotal * (discountPercentage / 100.0))
        val subtotal = roundToDecimal(rawSubtotal - discountAmount)
        
        val taxAmount = if (isTaxInclusive) {
            roundToDecimal(subtotal - (subtotal / (1.0 + VAT_RATE)))
        } else {
            roundToDecimal(subtotal * VAT_RATE)
        }

        val baseAmount = if (isTaxInclusive) {
            roundToDecimal(subtotal - taxAmount)
        } else {
            subtotal
        }

        val totalAmount = if (isTaxInclusive) {
            subtotal
        } else {
            roundToDecimal(subtotal + taxAmount)
        }

        return LineTotals(
            rawAmount = rawSubtotal,
            discountAmount = discountAmount,
            baseAmount = baseAmount,
            taxAmount = taxAmount,
            totalAmount = totalAmount
        )
    }

    data class LineTotals(
        val rawAmount: Double,
        val discountAmount: Double,
        val baseAmount: Double, // before tax
        val taxAmount: Double,
        val totalAmount: Double // after discount & tax
    )

    /**
     * Balanced check for double ledger lines
     */
    fun isEntryBalanced(debits: List<Double>, credits: List<Double>): Boolean {
        val totalDebit = roundToDecimal(debits.sum())
        val totalCredit = roundToDecimal(credits.sum())
        return totalDebit == totalCredit
    }

    /**
     * Computes moving weighted average cost of stock on purchase
     */
    fun calculateMovingWeightedAverage(
        currentStockQty: Double,
        currentAvgCost: Double,
        incomingQty: Double,
        incomingUnitCost: Double
    ): Double {
        val newQty = currentStockQty + incomingQty
        if (newQty <= 0.0) return incomingUnitCost
        val totalVal = (currentStockQty * currentAvgCost) + (incomingQty * incomingUnitCost)
        return roundToDecimal(totalVal / newQty, decimals = 4)
    }
}
