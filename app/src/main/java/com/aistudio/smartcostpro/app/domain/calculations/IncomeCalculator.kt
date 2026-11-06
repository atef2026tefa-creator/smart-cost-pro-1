package com.aistudio.smartcostpro.app.domain.calculations

import com.aistudio.smartcostpro.app.domain.model.CurrentAccountTransaction
import com.aistudio.smartcostpro.app.domain.model.CurrentAccountType
import com.aistudio.smartcostpro.app.domain.model.IncomeSource
import com.aistudio.smartcostpro.app.domain.model.IncomeType
import com.aistudio.smartcostpro.app.domain.model.MonthlyIncome
import com.aistudio.smartcostpro.app.util.DateFormatter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Income / current-account calculations.
 *
 * **Spec compliance:**
 *
 * 1. `getPrimaryIncomeForMonth` reads the detailed income sources JSON; if
 *    present, it sums every entry whose type is NOT `current_deposit` /
 *    `current_withdrawal` (those belong to the current account).
 * 2. If the details list is empty, it falls back to `salary + bonuses + otherIncome`.
 * 3. `currentAccountNet` is the sum of signed amounts (deposit = +, withdrawal = -).
 *    The sign is applied exactly once (the spec warns against applying it twice).
 */
object IncomeCalculator {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    /**
     * Returns the primary income for the given YYYY-MM month, following the
     * spec's two-tier algorithm.
     */
    fun primaryIncomeForMonth(income: MonthlyIncome?, month: String): Double {
        if (income == null || income.month != month) return 0.0

        // 1. Try the detailed list first.
        val details = parseDetails(income.detailsJson)
        if (details.isNotEmpty()) {
            return details
                .filter { it.type != IncomeType.Other || it.name?.contains("current", ignoreCase = true) == false }
                // current_deposit / current_withdrawal belong to the current account, not income.
                .filter { !isCurrentAccountMovement(it) }
                .sumOf { it.amount }
        }

        // 2. Fall back to the summary fields.
        return income.salary + income.bonuses + income.otherIncome
    }

    /**
     * Computes the current-account net for the given month:
     *   sum of `amount * type.sign` for every transaction whose date falls in the month.
     */
    fun currentAccountNetForMonth(transactions: List<CurrentAccountTransaction>, month: String): Double {
        return transactions
            .filter { DateFormatter.isDateInMonth(it.date, month) }
            .sumOf { it.signedAmount }
    }

    /**
     * Helper: serializes the income source list to JSON for storage.
     */
    fun encodeDetails(sources: List<IncomeSource>): String = json.encodeToString(sources)

    /**
     * Helper: deserializes the income source list, returning an empty list
     * when the string is null / blank / invalid.
     */
    fun parseDetails(detailsJson: String?): List<IncomeSource> {
        if (detailsJson.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<IncomeSource>>(detailsJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Returns true when the [IncomeSource] is actually a current-account
     * movement masquerading as income (the reference app's workaround).
     * We detect by name (case-insensitive) — the spec lists the keys as
     * `current_deposit` and `current_withdrawal`.
     */
    private fun isCurrentAccountMovement(source: IncomeSource): Boolean {
        val name = source.name.lowercase().trim()
        return name == "current_deposit" ||
            name == "current_withdrawal" ||
            source.type == IncomeType.Other && (name.startsWith("current_"))
    }
}
