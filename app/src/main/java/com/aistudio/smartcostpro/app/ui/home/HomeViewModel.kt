package com.aistudio.smartcostpro.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.smartcostpro.app.data.auth.AuthRepository
import com.aistudio.smartcostpro.app.data.auth.AuthSession
import com.aistudio.smartcostpro.app.data.repository.AssociationRepository
import com.aistudio.smartcostpro.app.data.repository.BudgetRepository
import com.aistudio.smartcostpro.app.data.repository.CertificateRepository
import com.aistudio.smartcostpro.app.data.repository.CurrentAccountRepository
import com.aistudio.smartcostpro.app.data.repository.ExpenseRepository
import com.aistudio.smartcostpro.app.data.repository.IncomeRepository
import com.aistudio.smartcostpro.app.domain.calculations.DashboardCalculator
import com.aistudio.smartcostpro.app.domain.model.DashboardSummary
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.CurrencyFormatter
import com.aistudio.smartcostpro.app.util.DateFormatter
import com.aistudio.smartcostpro.app.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home screen state — combines session + dashboard summary + recent operations
 * + the marquee news items (computed from the user's activity + financial tips).
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val session: AuthSession? = null,
    val month: String = DateFormatter.currentMonthIso(),
    val summary: DashboardSummary? = null,
    val marqueeItems: List<String> = emptyList(),
    val locale: AppLocale = AppLocale.Arabic,
    val currency: com.aistudio.smartcostpro.app.util.AppCurrency = com.aistudio.smartcostpro.app.util.AppCurrency.EGP
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val certificateRepository: CertificateRepository,
    private val currentAccountRepository: CurrentAccountRepository,
    private val associationRepository: AssociationRepository,
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = authRepository.session.flatMapLatest { session ->
        if (session == null) {
            flowOf(HomeUiState(isLoading = false, session = null))
        } else {
            // Combine the dashboard inputs (same pattern as DashboardViewModel)
            // with the session + settings.
            combine(
                expenseRepository.observeAll(),
                certificateRepository.observeAll(),
                currentAccountRepository.observeAll(),
                associationRepository.observeAll(),
                settingsRepository.settings
            ) { expenses, certs, currentAccount, associations, settings ->
                val month = DateFormatter.currentMonthIso()
                // Synchronously read the income + budget for the active month.
                // These are cached by Room so `first()` is cheap.
                val currentIncome = incomeRepository.observeByMonth(month).first()
                val currentBudget = budgetRepository.observeByMonth(month).first()
                val summary = DashboardCalculator.build(
                    month = month,
                    expenses = expenses,
                    monthlyIncome = currentIncome,
                    certificates = certs,
                    currentAccount = currentAccount,
                    associations = associations,
                    budget = currentBudget
                )
                val marqueeItems = buildMarqueeItems(summary, settings.locale, settings.currency, expenses)
                HomeUiState(
                    isLoading = false,
                    session = session,
                    month = month,
                    summary = summary,
                    marqueeItems = marqueeItems,
                    locale = settings.locale,
                    currency = settings.currency
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    /**
     * Builds the marquee content: a mix of the user's recent activity + financial
     * tips + month-to-date totals.
     *
     * Each item is already-formatted (localized + currency) so the UI just renders it.
     */
    private fun buildMarqueeItems(
        summary: DashboardSummary,
        locale: AppLocale,
        currency: com.aistudio.smartcostpro.app.util.AppCurrency,
        expenses: List<com.aistudio.smartcostpro.app.domain.model.Expense>
    ): List<String> {
        val items = mutableListOf<String>()
        // Recent activity (most recent 3 expenses).
        expenses.take(3).forEach { e ->
            val amount = CurrencyFormatter.format(e.amount, currency, locale, fractionDigits = 0)
            items.add("أضفت $amount مصروف ${e.title} اليوم")
        }
        // Financial tips + month-to-date facts.
        items.add("💡 وفّر 20% من دخلك الشهري")
        val netIncome = CurrencyFormatter.format(summary.monthlyIncome, currency, locale, fractionDigits = 0)
        items.add("📈 صافي دخل الشهر: $netIncome")
        items.add("💡 الشهادات بأعلى عائد: 18% سنوياً")
        items.add("💰 نسبة الادخار: ${summary.savingsRate}%")
        if (summary.isExpenseRatioCritical) {
            items.add("⚠️ نسبة المصروفات تجاوزت 90% من الدخل")
        }
        return items
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
