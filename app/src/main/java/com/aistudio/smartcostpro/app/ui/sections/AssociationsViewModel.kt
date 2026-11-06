package com.aistudio.smartcostpro.app.ui.sections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.smartcostpro.app.data.repository.AssociationRepository
import com.aistudio.smartcostpro.app.domain.calculations.AssociationCalculator
import com.aistudio.smartcostpro.app.domain.model.Association
import com.aistudio.smartcostpro.app.util.AppCurrency
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Associations screen.
 */
data class AssociationsUiState(
    val items: List<Association> = emptyList(),
    val locale: AppLocale = AppLocale.Arabic,
    val currency: AppCurrency = AppCurrency.EGP,
    val isLoading: Boolean = true
)

@HiltViewModel
class AssociationsViewModel @Inject constructor(
    private val associationRepository: AssociationRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<AssociationsUiState> = combine(
        associationRepository.observeAll(),
        settingsRepository.settings
    ) { items, settings ->
        AssociationsUiState(
            items = items,
            locale = settings.locale,
            currency = settings.currency,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AssociationsUiState()
    )

    fun upsert(association: Association) {
        viewModelScope.launch { associationRepository.upsert(association) }
    }

    fun delete(id: String) {
        viewModelScope.launch { associationRepository.deleteById(id) }
    }
}

/**
 * Form state for the Add / Edit association dialog.
 *
 * Per the spec:
 *  - `installment = value / memberCount` — auto-calculated.
 *  - Validation: name, value>0, memberCount≥1, 1≤roleNumber≤memberCount,
 *    startDate, payoutDate.
 */
data class AssociationFormState(
    val id: String? = null,
    val name: String = "",
    val value: String = "",
    val memberCount: String = "",
    val installment: String = "",
    val roleNumber: String = "",
    val startDate: String = com.aistudio.smartcostpro.app.util.DateFormatter.todayIso(),
    val payoutDate: String = com.aistudio.smartcostpro.app.util.DateFormatter.todayIso(),
    val notes: String = "",
    val nameError: String? = null,
    val valueError: String? = null,
    val membersError: String? = null,
    val roleError: String? = null,
    val dateError: String? = null,
    val isSaving: Boolean = false
) {
    val isEdit: Boolean get() = id != null
}
