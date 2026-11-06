package com.aistudio.smartcostpro.app.ui.dashboard

import com.aistudio.smartcostpro.app.domain.model.DashboardSummary
import com.aistudio.smartcostpro.app.util.AppCurrency
import com.aistudio.smartcostpro.app.util.AppLocale

/**
 * Single UI state emitted by [DashboardViewModel].
 *
 * The dashboard never holds more than one of these at a time — Compose
 * recomposes the screen whenever this state changes.
 */
sealed interface DashboardUiState {

    /** Initial loading state — emitted while the Flow is being collected. */
    data object Loading : DashboardUiState

    /** Successful state with a [DashboardSummary] computed for the active month. */
    data class Success(
        val month: String,
        val summary: DashboardSummary,
        val locale: AppLocale,
        val currency: AppCurrency
    ) : DashboardUiState

    /** Error state — shown when the underlying Flow throws. */
    data class Error(val message: String) : DashboardUiState
}
