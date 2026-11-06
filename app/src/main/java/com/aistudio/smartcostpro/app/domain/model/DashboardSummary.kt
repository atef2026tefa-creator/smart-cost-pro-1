package com.aistudio.smartcostpro.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Single recent operation row shown on the dashboard.
 *
 * Per the spec, "recent operations" is built ONLY from the unified expense
 * table — current account, certificates, car income, accidents and
 * associations are deliberately excluded from this list.
 */
@Serializable
data class RecentOperation(
    val id: String,
    val title: String,
    val amount: Double,
    val type: ExpenseType,
    val category: String,
    val date: String,
    val paymentMethod: PaymentMethod?
)

/**
 * Dashboard expense totals, broken down by type and with the two
 * composite totals (`homeAndMedical`, `vehicle`) used by the cards.
 *
 * Per the spec:
 *   - homeAndMedicalTotal = houseTotal + medicalTotal
 *   - vehicleTotal = fuelTotal + maintTotal + oilTotal
 *   - The grand total uses all eight types only.
 *   - Association installments are NOT added.
 *   - Vehicle accident estimatedDamage is NOT added.
 */
@Serializable
data class DashboardExpenseTotals(
    val houseTotal: Double,
    val medicalTotal: Double,
    val workTotal: Double,
    val personalTotal: Double,
    val fuelTotal: Double,
    val maintTotal: Double,
    val oilTotal: Double,
    val educationTotal: Double
) {
    val homeAndMedicalTotal: Double get() = houseTotal + medicalTotal
    val vehicleTotal: Double get() = fuelTotal + maintTotal + oilTotal
    val grandTotal: Double get() = houseTotal + medicalTotal + workTotal +
        personalTotal + fuelTotal + maintTotal + oilTotal + educationTotal
}

/**
 * Dashboard income totals.
 *
 * Per the spec:
 *
 *   monthlyIncome = primaryIncome + certificatesProfit + currentAccountNet + associationFinancials.earned
 *
 * Car trip income is deliberately NOT included here (unresolved per spec).
 */
@Serializable
data class DashboardIncomeTotals(
    val primaryIncome: Double,
    val certificatesProfit: Double,
    val currentAccountNet: Double,
    val associationEarned: Double
) {
    val monthlyIncome: Double get() =
        primaryIncome + certificatesProfit + currentAccountNet + associationEarned
}

/**
 * Final dashboard summary, combining expenses + income + derived ratios.
 *
 * Per the spec:
 *   netIncome = monthlyIncome - monthlyExpenses
 *   savingsRate = monthlyIncome > 0 ? clamp(round(netIncome / monthlyIncome * 100), 0, 100) : 0
 *   expenseRatio = monthlyIncome > 0 ? min(round(monthlyExpenses / monthlyIncome * 100), 100) : 100
 *   A warning shows up when expenseRatio > 90%.
 */
@Serializable
data class DashboardSummary(
    val month: String, // YYYY-MM
    val expenses: DashboardExpenseTotals,
    val income: DashboardIncomeTotals,
    val associationFinancials: AssociationFinancials,
    val recentOperations: List<RecentOperation>,
    val budget: Budget? = null
) {
    val monthlyExpenses: Double get() = expenses.grandTotal
    val monthlyIncome: Double get() = income.monthlyIncome
    val netIncome: Double get() = monthlyIncome - monthlyExpenses

    /** Savings rate, clamped 0..100. Zero when income is non-positive. */
    val savingsRate: Int
        get() = if (monthlyIncome > 0) {
            val raw = ((netIncome / monthlyIncome) * 100).toInt()
            raw.coerceIn(0, 100)
        } else 0

    /** Expense ratio as a percentage, capped at 100. 100 when income is non-positive. */
    val expenseRatio: Int
        get() = if (monthlyIncome > 0) {
            val raw = ((monthlyExpenses / monthlyIncome) * 100).toInt()
            raw.coerceAtMost(100)
        } else 100

    /** True when the expense ratio crosses the spec's 90% warning threshold. */
    val isExpenseRatioCritical: Boolean get() = expenseRatio > 90
}
