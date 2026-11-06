package com.aistudio.smartcostpro.app.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Currency / amount formatting helpers.
 *
 * We always print the ISO currency code *after* the number so the value
 * stays the source of truth; the code is a small secondary annotation.
 * Grouping uses the locale-appropriate thousands separator.
 *
 * **Important:** For Arabic, digits are kept as Western Arabic (0-9) —
 * not Eastern Arabic — to match the reference app and stay readable in
 * mixed-language totals. The grouping separator is locale-specific.
 */
object CurrencyFormatter {

    /**
     * Formats a numeric amount with the chosen currency code and locale.
     *
     * @param amount The number to format (may be negative).
     * @param currency Currency to display as a trailing code.
     * @param locale App locale (Arabic / English).
     * @param fractionDigits Decimal precision (defaults to 2).
     */
    fun format(
        amount: Double,
        currency: AppCurrency,
        locale: AppLocale,
        fractionDigits: Int = 2
    ): String {
        val formatted = formatNumber(amount, locale, fractionDigits)
        // For Arabic, prefix the amount with the LRM mark to keep numbers LTR
        // inside an RTL layout, then append the currency code.
        return if (locale == AppLocale.Arabic) {
            "\u200E$formatted ${currency.code}"
        } else {
            "$formatted ${currency.code}"
        }
    }

    /**
     * Format without currency code — useful for percentage / count displays.
     */
    fun formatNumber(
        value: Double,
        locale: AppLocale,
        fractionDigits: Int = 2
    ): String {
        val javaLocale = when (locale) {
            AppLocale.Arabic -> Locale("ar", "EG")
            AppLocale.English -> Locale.US
        }
        val symbols = DecimalFormatSymbols.getInstance(javaLocale)
        // Force Western digits even in Arabic locale for readability.
        symbols.digitCharsToWesternArabic()

        val pattern = buildPattern(fractionDigits)
        val formatter = DecimalFormat(pattern, symbols).apply {
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
            isGroupingUsed = true
            negativePrefix = "("
            negativeSuffix = ")"
            positivePrefix = ""
            positiveSuffix = ""
        }
        return formatter.format(value)
    }

    /**
     * Format an integer percentage with a trailing % sign.
     */
    fun formatPercentage(value: Int, locale: AppLocale): String {
        return "${formatNumber(value.toDouble(), locale, 0)}%"
    }

    private fun buildPattern(fractionDigits: Int): String {
        val sb = StringBuilder("#,##0")
        if (fractionDigits > 0) {
            sb.append(".")
            repeat(fractionDigits) { sb.append("0") }
        }
        return sb.toString()
    }

    /**
     * Helper: forces Western-Arabic digits on the [DecimalFormatSymbols].
     * DecimalFormatSymbols exposes per-character digit string since API 24+
     * via [DecimalFormatSymbols.getDigitStrings], but the setter requires
     * API 24+ too. We rebuild the symbols to use 0-9.
     */
    private fun DecimalFormatSymbols.digitCharsToWesternArabic() {
        // DecimalFormatSymbols.zeroDigit setter is API 24+, so we set the zero
        // digit and Java handles the rest of the per-character substitution.
        zeroDigit = '0'
    }
}
