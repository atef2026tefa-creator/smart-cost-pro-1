package com.aistudio.smartcostpro.app.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aistudio.smartcostpro.app.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPrefs by preferencesDataStore(name = Constants.PREFS_NAME)

/**
 * Tiny DataStore-backed wrapper for user preferences.
 *
 * Stage 3 fix: when the user changes the language, we now also push the
 * locale to the system via [AppCompatDelegate.setApplicationLocales] so
 * `stringResource()` lookups honor the new locale (not just the layout
 * direction). This is the Android-recommended per-app language API and
 * works on all supported API levels (24+).
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey(Constants.PREF_KEY_LANGUAGE)
        val CURRENCY = stringPreferencesKey(Constants.PREF_KEY_CURRENCY)
        val THEME = stringPreferencesKey(Constants.PREF_KEY_THEME)
        val DEFAULT_SCREEN = stringPreferencesKey(Constants.PREF_KEY_DEFAULT_SCREEN)
    }

    val language: Flow<AppLocale> = context.appPrefs.data
        .map { AppLocale.fromCode(it[Keys.LANGUAGE]) }

    val currency: Flow<AppCurrency> = context.appPrefs.data
        .map { AppCurrency.fromCode(it[Keys.CURRENCY]) }

    val themeMode: Flow<ThemeMode> = context.appPrefs.data
        .map { parseThemeMode(it[Keys.THEME]) }

    val defaultScreen: Flow<String> = context.appPrefs.data
        .map { it[Keys.DEFAULT_SCREEN] ?: Constants.DEFAULT_SCREEN }

    /** Aggregated preferences snapshot, useful for the Settings UI. */
    val settings: Flow<AppSettings> = context.appPrefs.data.map { prefs ->
        AppSettings(
            locale = AppLocale.fromCode(prefs[Keys.LANGUAGE]),
            currency = AppCurrency.fromCode(prefs[Keys.CURRENCY]),
            themeMode = parseThemeMode(prefs[Keys.THEME]),
            defaultScreen = prefs[Keys.DEFAULT_SCREEN] ?: Constants.DEFAULT_SCREEN
        )
    }

    /**
     * Sets the active language. Persists the choice AND pushes it to the
     * system via [AppCompatDelegate.setApplicationLocales] so every
     * `stringResource()` lookup picks up the new locale immediately.
     */
    suspend fun setLanguage(locale: AppLocale) {
        context.appPrefs.edit { it[Keys.LANGUAGE] = locale.code }
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(LocaleManager.bcp47Tag(locale))
        )
    }

    suspend fun setCurrency(currency: AppCurrency) {
        context.appPrefs.edit { it[Keys.CURRENCY] = currency.code }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appPrefs.edit { it[Keys.THEME] = mode.name.lowercase() }
    }

    suspend fun setDefaultScreen(screen: String) {
        context.appPrefs.edit { it[Keys.DEFAULT_SCREEN] = screen }
    }

    private fun parseThemeMode(value: String?): ThemeMode = when (value?.lowercase()) {
        "light" -> ThemeMode.Light
        "dark" -> ThemeMode.Dark
        else -> ThemeMode.System
    }
}

/**
 * Snapshot of all user-configurable settings. Used by the SettingsViewModel
 * to emit a single immutable state to the UI.
 */
data class AppSettings(
    val locale: AppLocale = AppLocale.Arabic,
    val currency: AppCurrency = AppCurrency.EGP,
    val themeMode: ThemeMode = ThemeMode.Dark,
    val defaultScreen: String = Constants.DEFAULT_SCREEN
)
