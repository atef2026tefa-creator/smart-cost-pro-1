package com.aistudio.smartcostpro.app.ui.sections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.smartcostpro.app.data.repository.SectionOrderRepository
import com.aistudio.smartcostpro.app.domain.model.SectionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sections menu ViewModel.
 *
 * Loads the persisted section order from [SectionOrderRepository], exposes
 * a [MutableStateFlow] so the UI can drag-and-drop to reorder, and persists
 * the new order atomically when the user taps Save.
 *
 * **Spec compliance:**
 *  - Default order: Vehicle, IncomeCerts, Education, House, Personal, Reports
 *  - The order is stored under `smartcost_expenses_sections_order`.
 *  - Reordering is atomic (delete all + insert all in one transaction).
 *  - Reset to default restores [SectionType.DEFAULT_ORDER].
 */
@HiltViewModel
class SectionsMenuViewModel @Inject constructor(
    private val sectionOrderRepository: SectionOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SectionsMenuUiState(isLoading = true))
    val uiState: StateFlow<SectionsMenuUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val initial = sectionOrderRepository.observeAll()
            initial.collect { persisted ->
                _uiState.update { it.copy(sections = persisted, isLoading = false) }
            }
        }
    }

    /** Toggles the drag-and-drop mode. */
    fun toggleReordering() {
        _uiState.update { it.copy(isReordering = !it.isReordering) }
    }

    /**
     * Reorders the list by moving [from] index to [to] index. Called by the
     * reorderable list during a drag.
     */
    fun moveSection(from: Int, to: Int) {
        val current = _uiState.value.sections.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        val moved = current.removeAt(from)
        current.add(to, moved)
        _uiState.update { it.copy(sections = current) }
    }

    /**
     * Persists the current order. Per the spec, the spec keeps the order
     * saved across launches, so we always write to the repository.
     */
    fun saveOrder() {
        viewModelScope.launch {
            sectionOrderRepository.setOrder(_uiState.value.sections)
            _uiState.update { it.copy(isReordering = false) }
        }
    }

    /** Resets to the spec's default order (also persists). */
    fun resetToDefault() {
        viewModelScope.launch {
            sectionOrderRepository.resetToDefault()
            _uiState.update { it.copy(sections = SectionType.DEFAULT_ORDER, isReordering = false) }
        }
    }
}
