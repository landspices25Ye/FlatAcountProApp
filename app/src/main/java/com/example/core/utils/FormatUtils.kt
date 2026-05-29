package com.example.core.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object FormatUtils {

    /**
     * Format Double to proper Arabic-locale oriented Currency (default Saudi Riyal SAR)
     */
    fun formatCurrency(amount: Double, currencyCode: String = "SAR", locale: Locale = Locale("ar", "SA")): String {
        return try {
            val formatter = NumberFormat.getCurrencyInstance(locale)
            formatter.currency = Currency.getInstance(currencyCode)
            formatter.format(amount)
        } catch (e: Exception) {
            String.format(Locale.getDefault(), "%,.2f %s", amount, currencyCode)
        }
    }

    /**
     * Formats Long timestamp to a human-readable date representation
     */
    fun formatDate(timestamp: Long, format: String = "yyyy-MM-dd", locale: Locale = Locale("ar")): String {
        return try {
            val sdf = SimpleDateFormat(format, locale)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            "---"
        }
    }

    /**
     * Basic check for Tax/CR/email validations
     */
    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
        return email.isNotEmpty() && emailRegex.matches(email)
    }

    fun isValidPhone(phone: String): Boolean {
        return phone.isNotEmpty() && phone.length >= 8 && phone.all { it.isDigit() || it == '+' }
    }

    /**
     * Validates Saudi standard 15-digit Tax Identification Number (TIN)
     */
    fun isValidTaxNumber(taxNumber: String): Boolean {
        return taxNumber.trim().length == 15 && taxNumber.trim().all { it.isDigit() }
    }

    /**
     * Elegant truncate string helpers
     */
    fun truncateString(text: String, maxLength: Int = 50, suffix: String = "..."): String {
        if (text.length <= maxLength) return text
        return text.take(maxLength) + suffix
    }
}
