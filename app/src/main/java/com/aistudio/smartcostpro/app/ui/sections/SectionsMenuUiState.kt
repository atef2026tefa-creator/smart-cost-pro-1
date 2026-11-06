package com.aistudio.smartcostpro.app.ui.sections

import com.aistudio.smartcostpro.app.domain.model.SectionType

/**
 * UI state for the Sections menu screen.
 *
 * Sections are draggable; the [isReordering] flag toggles the drag handles
 * and the Save / Reset buttons.
 */
data class SectionsMenuUiState(
    val sections: List<SectionType> = SectionType.DEFAULT_ORDER,
    val isReordering: Boolean = false,
    val isLoading: Boolean = true
)
