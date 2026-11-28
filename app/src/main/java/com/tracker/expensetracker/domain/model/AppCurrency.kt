package com.tracker.expensetracker.domain.model

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

data class AppCurrency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val locale: Locale = Locale.US
) {
    fun format(amount: Double): String = formatAmount(amount)

    fun format(amount: BigDecimal): String = formatAmount(amount)

    private fun formatAmount(amount: Number): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(locale)
            format.format(amount)
        } catch (e: Exception) {
            val symbols = DecimalFormatSymbols.getInstance(Locale.US).apply {
                currencySymbol = symbol
            }
            val df = DecimalFormat("$symbol #,##0.00", symbols)
            df.format(amount)
        }
    }

    companion object {
        val USD = AppCurrency("USD", "$", "USD - US Dollar ($)", Locale.US)
        val EUR = AppCurrency("EUR", "€", "EUR - Euro (€)", Locale.GERMANY)
        val GBP = AppCurrency("GBP", "£", "GBP - British Pound (£)", Locale.UK)
        val INR = AppCurrency("INR", "₹", "INR - Indian Rupee (₹)", Locale.forLanguageTag("en-IN"))
        val JPY = AppCurrency("JPY", "¥", "JPY - Japanese Yen (¥)", Locale.JAPAN)
        val CAD = AppCurrency("CAD", "CA$", "CAD - Canadian Dollar (CA$)", Locale.CANADA)
        val AUD = AppCurrency("AUD", "AU$", "AUD - Australian Dollar (AU$)", Locale.forLanguageTag("en-AU"))
        val CHF = AppCurrency("CHF", "CHF", "CHF - Swiss Franc (CHF)", Locale.forLanguageTag("de-CH"))
        val CNY = AppCurrency("CNY", "¥", "CNY - Chinese Yuan (¥)", Locale.CHINA)
        val BRL = AppCurrency("BRL", "R$", "BRL - Brazilian Real (R$)", Locale.forLanguageTag("pt-BR"))

        val SUPPORTED_CURRENCIES: List<AppCurrency> = listOf(
            USD, EUR, GBP, INR, JPY, CAD, AUD, CHF, CNY, BRL
        )

        fun fromCode(code: String): AppCurrency {
            return SUPPORTED_CURRENCIES.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: USD
        }
    }
}
