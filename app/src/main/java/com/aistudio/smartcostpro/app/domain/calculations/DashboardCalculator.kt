package com.aistudio.smartcostpro.app.domain.calculations

import com.aistudio.smartcostpro.app.domain.model.Association
import com.aistudio.smartcostpro.app.domain.model.BankCertificate
import com.aistudio.smartcostpro.app.domain.model.Budget
import com.aistudio.smartcostpro.app.domain.model.CarTrip
import com.aistudio.smartcostpro.app.domain.model.CurrentAccountTransaction
import com.aistudio.smartcostpro.app.domain.model.DashboardExpenseTotals
import com.aistudio.smartcostpro.app.domain.model.DashboardIncomeTotals
import com.aistudio.smartcostpro.app.domain.model.DashboardSummary
import com.aistudio.smartcostpro.app.domain.model.Expense
import com.aistudio.smartcostpro.app.domain.model.MonthlyIncome
import com.aistudio.smartcostpro.app.domain.model.RecentOperation

/**
 * Aggregates all per-section calculators into the [DashboardSummary] used
 * by the dashboard screen.
 *
 * **Spec compliance:**
 *
 *     monthlyIncome = primaryIncome + certificatesProfit + currentAccountNet + associationFinancials.earned
 *     netIncome = monthlyIncome - monthlyExpenses
 *     savingsRate = monthlyIncome > 0 ? clamp(round(netIncome / monthlyIncome * 100), 0, 100) : 0
 *     expenseRatio = monthlyIncome > 0 ? min(round(monthlyExpenses / monthlyIncome * 100), 100) : 100
 *
 * Car trip income is NOT in the unified monthly income (spec is explicit).
 * Association installments are NOT in the unified expense total.
 * Vehicle accident `estimatedDamage` is NOT in the unified expense total.
 */
object DashboardCalculator {

    /**
     * Builds the dashboard summary for the given month, given every input
     * the dashboard needs. The caller is expected to have already loaded the
     * appropriate data — the calculator itself is pure.
     */
    fun build(
        month: String,
        expenses: List<Expense>,
        monthlyIncome: MonthlyIncome?,
        certificates: List<BankCertificate>,
        currentAccount: List<CurrentAccountTransaction>,
        associations: List<Association>,
        budget: Budget?,
        recentLimit: Int = 10
    ): DashboardSummary {
        // 1. Expense totals — pure call into ExpenseCalculator.
        val expenseTotals: DashboardExpenseTotals =
            ExpenseCalculator.totalsForMonth(expenses, month)

        // 2. Income totals — primary + certificates + current account + associations.
        val primaryIncome = IncomeCalculator.primaryIncomeForMonth(monthlyIncome, month)
        val certificatesProfit = CertificateCalculator.totalMonthlyProfit(certificates, month)
        val currentAccountNet = IncomeCalculator.currentAccountNetForMonth(currentAccount, month)
        val associationFinancials = AssociationCalculator.financialsForMonth(associations, month)
        val incomeTotals = DashboardIncomeTotals(
            primaryIncome = primaryIncome,
            certificatesProfit = certificatesProfit,
            currentAccountNet = currentAccountNet,
            associationEarned = associationFinancials.earned
        )

        // 3. Recent operations — built from the unified expense table only.
        val monthExpenses = ExpenseCalculator.filterByMonth(expenses, month)
        val recentOps: List<RecentOperation> = ExpenseCalculator.recentOperations(monthExpenses, recentLimit)

        return DashboardSummary(
            month = month,
            expenses = expenseTotals,
            income = incomeTotals,
            associationFinancials = associationFinancials,
            recentOperations = recentOps,
            budget = budget
        )
    }
}
