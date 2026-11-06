package com.aistudio.smartcostpro.app.domain.calculations

import com.aistudio.smartcostpro.app.domain.model.Expense
import com.aistudio.smartcostpro.app.domain.model.ExpenseType
import com.aistudio.smartcostpro.app.domain.model.PaymentMethod
import com.aistudio.smartcostpro.app.domain.model.RecentOperation
import com.aistudio.smartcostpro.app.util.DateFormatter

/**
 * Pure functions for the unified expense table.
 *
 * **Spec compliance:**
 * - The dashboard's expense totals filter expenses by `date.slice(0, 7) == month`.
 * - Empty dates never belong to a month.
 * - All eight expense types are summed independently.
 * - Recent operations are built from this table only — no current account,
 *   no certificates, no car income, no accidents, no association rows.
 */
object ExpenseCalculator {

    /**
     * Filters the list down to expenses that fall in the given YYYY-MM month.
     */
    fun filterByMonth(expenses: List<Expense>, month: String): List<Expense> {
        return expenses.filter { DateFormatter.isDateInMonth(it.date, month) }
    }

    /**
     * Sums all expenses in the list for a specific [ExpenseType].
     */
    fun sumByType(expenses: List<Expense>, type: ExpenseType): Double {
        return expenses.filter { it.type == type }.sumOf { it.amount }
    }

    /**
     * Computes the eight per-type totals for the dashboard, then derives
     * `homeAndMedicalTotal` and `vehicleTotal` per the spec.
     */
    fun totalsForMonth(expenses: List<Expense>, month: String): com.aistudio.smartcostpro.app.domain.model.DashboardExpenseTotals {
        val monthExpenses = filterByMonth(expenses, month)
        return com.aistudio.smartcostpro.app.domain.model.DashboardExpenseTotals(
            houseTotal = sumByType(monthExpenses, ExpenseType.House),
            medicalTotal = sumByType(monthExpenses, ExpenseType.Medical),
            workTotal = sumByType(monthExpenses, ExpenseType.Work),
            personalTotal = sumByType(monthExpenses, ExpenseType.Personal),
            fuelTotal = sumByType(monthExpenses, ExpenseType.VehicleFuel),
            maintTotal = sumByType(monthExpenses, ExpenseType.VehicleMaint),
            oilTotal = sumByType(monthExpenses, ExpenseType.VehicleOil),
            educationTotal = sumByType(monthExpenses, ExpenseType.Student)
        )
    }

    /**
     * Builds the recent operations list. The spec limits it to the unified
     * expense table, sorted newest first, with a sensible cap (we default to
     * 10 items — the spec does not pin a number).
     */
    fun recentOperations(expenses: List<Expense>, limit: Int = 10): List<RecentOperation> {
        return expenses
            .sortedWith(compareByDescending<Expense> { it.date }
                .thenByDescending { it.createdAt })
            .take(limit)
            .map { e ->
                RecentOperation(
                    id = e.id,
                    title = e.title,
                    amount = e.amount,
                    type = e.type,
                    category = e.category,
                    date = e.date,
                    paymentMethod = e.paymentMethod
                )
            }
    }

    /**
     * Validates an expense before persistence.
     *
     * Per the spec:
     * - amount must be > 0
     * - amount must be a number (handled by the input field)
     * - date is mandatory
     * - title / category are mandatory (server-side requirement)
     *
     * Returns null when valid, or a [ValidationError] when something is wrong.
     */
    fun validate(
        title: String,
        amount: Double,
        type: ExpenseType,
        category: String,
        date: String,
        notes: String?
    ): ValidationError? {
        if (title.isBlank()) return ValidationError.TitleRequired
        if (amount <= 0.0) return ValidationError.AmountInvalid
        if (date.isBlank()) return ValidationError.DateRequired
        if (category.isBlank()) return ValidationError.CategoryRequired
        if ((notes?.length ?: 0) > com.aistudio.smartcostpro.app.util.Constants.MAX_NOTES_LENGTH)
            return ValidationError.NotesTooLong
        return null
    }

    /** Validation errors surfaced to the UI. */
    enum class ValidationError {
        TitleRequired,
        AmountInvalid,
        DateRequired,
        CategoryRequired,
        NotesTooLong
    }
}
