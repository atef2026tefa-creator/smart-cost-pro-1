package com.aistudio.smartcostpro.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.smartcostpro.app.data.repository.AssociationRepository
import com.aistudio.smartcostpro.app.data.repository.BudgetRepository
import com.aistudio.smartcostpro.app.data.repository.CertificateRepository
import com.aistudio.smartcostpro.app.data.repository.CurrentAccountRepository
import com.aistudio.smartcostpro.app.data.repository.ExpenseRepository
import com.aistudio.smartcostpro.app.data.repository.IncomeRepository
import com.aistudio.smartcostpro.app.domain.calculations.DashboardCalculator
import com.aistudio.smartcostpro.app.domain.model.Association
import com.aistudio.smartcostpro.app.domain.model.BankCertificate
import com.aistudio.smartcostpro.app.domain.model.CurrentAccountTransaction
import com.aistudio.smartcostpro.app.domain.model.Expense
import com.aistudio.smartcostpro.app.util.AppSettings
import com.aistudio.smartcostpro.app.util.DateFormatter
import com.aistudio.smartcostpro.app.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Dashboard ViewModel.
 *
 * Combines every source the [DashboardCalculator] needs into a single
 * [DashboardUiState]. The selected month is held as a [MutableStateFlow] so
 * the user can flip through months without re-subscribing to every Flow.
 *
 * **Spec compliance:**
 *  - Default month is the current YYYY-MM (not the previous month).
 *  - The dashboard aggregates:
 *      - Expenses (unified table) — recent operations + totals.
 *      - Monthly income (one row per month).
 *      - Certificates (every row — date-window check inside the calculator).
 *      - Current account transactions (filtered by month).
 *      - Associations (filtered by month inside the calculator).
 *      - Budget (single row for the active month, optional).
 *  - Car trips are NOT merged into the income total — spec §13.3.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val certificateRepository: CertificateRepository,
    private val currentAccountRepository: CurrentAccountRepository,
    private val associationRepository: AssociationRepository,
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /** The currently selected YYYY-MM month, exposed as a mutable flow. */
    private val selectedMonth = MutableStateFlow(DateFormatter.currentMonthIso())

    /** Currently-selected month — used by the screen's MonthSelector. */
    val month: StateFlow<String> = selectedMonth

    /**
     * Helper tuple used to thread the upstream Flows into [flatMapLatest].
     */
    private data class DashboardBase(
        val expenses: List<Expense>,
        val certificates: List<BankCertificate>,
        val currentAccount: List<CurrentAccountTransaction>,
        val associations: List<Association>,
        val settings: AppSettings
    )

    private data class DashboardInputs(
        val base: DashboardBase,
        val month: String
    )

    /**
     * The dashboard state. Built by combining the "always-on" upstreams
     * (expenses / certificates / current account / associations / settings /
     * selected month) into [DashboardInputs], then flatMapLatest'ing that
     * into a per-month pair of (income, budget) Flows.
     *
     * `kotlinx.coroutines` doesn't ship a 6-flow `combine` overload, so we
     * split into two steps: combine the first five flows into [DashboardBase],
     * then combine with the selected-month flow.
     */
    val uiState: StateFlow<DashboardUiState> = combine(
        expenseRepository.observeAll(),
        certificateRepository.observeAll(),
        currentAccountRepository.observeAll(),
        associationRepository.observeAll(),
        settingsRepository.settings
    ) { expenses, certificates, currentAccount, associations, settings ->
        DashboardBase(expenses, certificates, currentAccount, associations, settings)
    }.combine(selectedMonth) { base, month ->
        DashboardInputs(base, month)
    }.flatMapLatest { inputs ->
        combine(
            incomeRepository.observeByMonth(inputs.month),
            budgetRepository.observeByMonth(inputs.month)
        ) { income, budget ->
            DashboardUiState.Success(
                month = inputs.month,
                summary = DashboardCalculator.build(
                    month = inputs.month,
                    expenses = inputs.base.expenses,
                    monthlyIncome = income,
                    certificates = inputs.base.certificates,
                    currentAccount = inputs.base.currentAccount,
                    associations = inputs.base.associations,
                    budget = budget,
                    recentLimit = 10
                ),
                locale = inputs.base.settings.locale,
                currency = inputs.base.settings.currency
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState.Loading
    )

    /** Sets the active month to the previous one (used by the MonthSelector). */
    fun previousMonth() {
        selectedMonth.update { shift(it, -1) }
    }

    /** Sets the active month to the next one. Disallowed past the current month. */
    fun nextMonth() {
        val current = DateFormatter.currentMonthIso()
        val next = shift(selectedMonth.value, +1)
        if (next <= current) selectedMonth.value = next
    }

    /** Jumps to the current YYYY-MM month. */
    fun resetToCurrentMonth() {
        selectedMonth.value = DateFormatter.currentMonthIso()
    }

    /** True when the user can move to the next month. */
    fun canGoForward(): Boolean =
        shift(selectedMonth.value, +1) <= DateFormatter.currentMonthIso()

    private fun shift(monthIso: String, delta: Int): String {
        val ym = DateFormatter.yearMonthFromIso(monthIso) ?: return DateFormatter.currentMonthIso()
        val new = ym.plusMonths(delta.toLong())
        return new.format(DateFormatter.isoMonthFormatter)
    }
}
