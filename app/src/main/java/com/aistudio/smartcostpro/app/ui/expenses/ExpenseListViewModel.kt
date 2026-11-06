package com.aistudio.smartcostpro.app.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.smartcostpro.app.data.repository.ExpenseRepository
import com.aistudio.smartcostpro.app.domain.calculations.ExpenseCalculator
import com.aistudio.smartcostpro.app.domain.model.Expense
import com.aistudio.smartcostpro.app.domain.model.ExpenseType
import com.aistudio.smartcostpro.app.util.AppCurrency
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the unified expense list screen.
 *
 * Each section screen (House / Work / Personal) is built on top of
 * [ExpenseListScreen] using this state — the only difference is the
 * [ExpenseType] filter applied.
 */
data class ExpenseListUiState(
    val items: List<Expense> = emptyList(),
    val search: String = "",
    val categoryFilter: String? = null, // null = all categories
    val locale: AppLocale = AppLocale.Arabic,
    val currency: AppCurrency = AppCurrency.EGP,
    val total: Double = 0.0,
    val isExporting: Boolean = false,
    val exportMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _search = MutableStateFlow("")
    private val _categoryFilter = MutableStateFlow<String?>(null)
    private val _exportMessage = MutableStateFlow<String?>(null)
    private val _isExporting = MutableStateFlow(false)

    /**
     * Builds the UI state for a given expense type. The screen passes the
     * type once at composition time and we subscribe to the appropriate Flow.
     */
    fun stateFor(type: ExpenseType): StateFlow<ExpenseListUiState> {
        val expenses = expenseRepository.observeByType(type)
        return combine(
            expenses,
            _search,
            _categoryFilter,
            settingsRepository.settings,
            _exportMessage,
            _isExporting
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val items = values[0] as List<Expense>
            val search = values[1] as String
            val catFilter = values[2] as String?
            val settings = values[3] as com.aistudio.smartcostpro.app.util.AppSettings
            val exportMsg = values[4] as String?
            val isExporting = values[5] as Boolean

            val filtered = ExpenseFilter.apply(items, search, type, catFilter)
            val total = filtered.sumOf { it.amount }

            ExpenseListUiState(
                items = filtered,
                search = search,
                categoryFilter = catFilter,
                locale = settings.locale,
                currency = settings.currency,
                total = total,
                isExporting = isExporting,
                exportMessage = exportMsg
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExpenseListUiState()
        )
    }

    fun onSearchChange(value: String) { _search.value = value }
    fun onCategoryFilterChange(value: String?) { _categoryFilter.value = value }

    fun deleteExpense(id: String) {
        viewModelScope.launch { expenseRepository.deleteById(id) }
    }

    fun clearExportMessage() { _exportMessage.value = null }

    /**
     * Builds a CSV string for the current filtered list and stores it in
     * the app's `exports/` directory. The result (file name) is exposed
     * via [exportMessage].
     *
     * The actual file writing is performed by the screen so it can pass
     * the [android.content.Context] — the ViewModel only produces the CSV
     * string and decides the file name.
     */
    fun exportCsv(items: List<Expense>, locale: AppLocale, currency: AppCurrency, onResult: (String?, String?) -> Unit) {
        if (items.isEmpty()) {
            onResult(null, "EXPORT_NO_DATA")
            return
        }
        _isExporting.value = true
        viewModelScope.launch {
            try {
                val csv = com.aistudio.smartcostpro.app.export.CsvExporter.expensesToCsv(items, locale)
                onResult(csv, null)
                _exportMessage.value = "EXPORT_DONE"
            } catch (t: Throwable) {
                onResult(null, "EXPORT_FAILED")
                _exportMessage.value = "EXPORT_FAILED"
            } finally {
                _isExporting.value = false
            }
        }
    }
}
