package com.aistudio.smartcostpro.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.smartcostpro.app.ui.theme.ThemeMode
import com.aistudio.smartcostpro.app.util.AppCurrency
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.AppSettings
import com.aistudio.smartcostpro.app.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings ViewModel — exposes user preferences and provides setters for
 * every preference the user can change.
 *
 * Stage 1 settings:
 *  - Language (Arabic / English)
 *  - Currency (EGP / SAR / AED / USD / EUR)
 *  - Theme (System / Light / Dark)
 *  - Default screen (Expenses / Dashboard)
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings()
        )

    fun setLanguage(locale: AppLocale) {
        viewModelScope.launch { settingsRepository.setLanguage(locale) }
    }

    fun setCurrency(currency: AppCurrency) {
        viewModelScope.launch { settingsRepository.setCurrency(currency) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDefaultScreen(screen: String) {
        viewModelScope.launch { settingsRepository.setDefaultScreen(screen) }
    }
}
