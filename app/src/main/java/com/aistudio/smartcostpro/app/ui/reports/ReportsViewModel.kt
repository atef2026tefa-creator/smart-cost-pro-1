package com.aistudio.smartcostpro.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.smartcostpro.app.data.repository.ExpenseRepository
import com.aistudio.smartcostpro.app.domain.calculations.ReportCalculator
import com.aistudio.smartcostpro.app.domain.calculations.ReportCalculator.ReportFilter
import com.aistudio.smartcostpro.app.domain.model.Expense
import com.aistudio.smartcostpro.app.util.AppCurrency
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.DateFormatter
import com.aistudio.smartcostpro.app.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * UI state for the Reports screen.
 *
 * Per the spec §16:
 *  - Day report: from == to == selected date
 *  - Range report: between [from, to]; capped at < 370 days
 *  - Filter set: All / Income / House / Personal / Associations / Vehicle / Education
 *  - Preview + export built from the SAME `rows` list
 */
data class ReportsUiState(
    val periodType: PeriodType = PeriodType.Month,
    val from: String = DateFormatter.todayIso(),
    val to: String = DateFormatter.todayIso(),
    val filter: ReportFilter = ReportFilter.All,
    val rows: List<ReportCalculator.ReportRow> = emptyList(),
    val locale: AppLocale = AppLocale.Arabic,
    val currency: AppCurrency = AppCurrency.EGP,
    val rangeTooWide: Boolean = false
)

enum class PeriodType { Day, Week, Month, Range }

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val periodType = MutableStateFlow(PeriodType.Month)
    private val from = MutableStateFlow(DateFormatter.todayIso())
    private val to = MutableStateFlow(DateFormatter.todayIso())
    private val filter = MutableStateFlow(ReportFilter.All)

    val uiState: StateFlow<ReportsUiState> = combine(
        expenseRepository.observeAll(),
        periodType,
        from,
        to,
        filter
    ) { expenses, period, from, to, filter ->
        // Pack the first 5 into a tuple so we can combine with settings as the 6th.
        ReportInputs(expenses, period, from, to, filter)
    }.combine(settingsRepository.settings) { inputs, settings ->
        // For the "Week" period, auto-compute the last 7 days ending today so
        // the user doesn't have to manually pick two dates. The user can still
        // switch to "Range" for an arbitrary window.
        val (effectiveFrom, effectiveTo, rangeTooWide) = when (inputs.period) {
            PeriodType.Week -> {
                val today = java.time.LocalDate.now()
                val start = today.minusDays(6)
                Triple(start.toString(), today.toString(), false)
            }
            PeriodType.Range -> {
                val days = runCatching {
                    java.time.temporal.ChronoUnit.DAYS.between(
                        java.time.LocalDate.parse(inputs.from, DateFormatter.isoDateFormatter),
                        java.time.LocalDate.parse(inputs.to, DateFormatter.isoDateFormatter)
                    ) + 1
                }.getOrDefault(0)
                Triple(inputs.from, inputs.to, days >= 370)
            }
            else -> Triple(inputs.from, inputs.to, false)
        }

        val rows = if (rangeTooWide) emptyList() else when (inputs.period) {
            PeriodType.Day -> ReportCalculator.dayReport(inputs.expenses, inputs.from)
            PeriodType.Week, PeriodType.Range -> ReportCalculator.rangeReport(inputs.expenses, effectiveFrom, effectiveTo)
            PeriodType.Month -> ReportCalculator.monthReport(inputs.expenses, DateFormatter.monthOf(inputs.from) ?: DateFormatter.currentMonthIso())
        }
        ReportsUiState(
            periodType = inputs.period,
            from = effectiveFrom,
            to = effectiveTo,
            filter = inputs.filter,
            rows = ReportCalculator.filterRows(rows, inputs.filter),
            locale = settings.locale,
            currency = settings.currency,
            rangeTooWide = rangeTooWide
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportsUiState()
    )

    /** Helper tuple so the 6-flow combine stays within kotlinx.coroutines' 5-flow overload. */
    private data class ReportInputs(
        val expenses: List<com.aistudio.smartcostpro.app.domain.model.Expense>,
        val period: PeriodType,
        val from: String,
        val to: String,
        val filter: ReportFilter
    )

    fun setPeriodType(type: PeriodType) { periodType.value = type }
    fun setFrom(value: String) { from.value = value }
    fun setTo(value: String) { to.value = value }
    fun setFilter(value: ReportFilter) { filter.value = value }
}
