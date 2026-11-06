package com.aistudio.smartcostpro.app.util

import android.content.res.Configuration
import java.util.Locale

/**
 * Locale management helpers.
 *
 * We rely on the modern per-app language API (`AppCompatDelegate.setApplicationLocales`
 * on Android 13+, and a manual `Configuration` override on older versions)
 * rather than deprecated `updateConfiguration` to keep the code working on
 * minSdk 24 all the way to API 35.
 */
object LocaleManager {

    /**
     * Returns a [Configuration] updated with the requested locale, suitable for
     * being provided as a CompositionLocal. The original configuration is left
     * untouched — we copy first then set the locale.
     */
    fun applyLocaleToConfiguration(base: Configuration, locale: AppLocale): Configuration {
        val newConfig = Configuration(base)
        val javaLocale = when (locale) {
            AppLocale.Arabic -> Locale("ar")
            AppLocale.English -> Locale("en")
        }
        Locale.setDefault(javaLocale)
        newConfig.setLocale(javaLocale)
        // Force layout direction too so RTL / LTR is correct on API 17+.
        newConfig.setLayoutDirection(javaLocale)
        return newConfig
    }

    /**
     * Returns the BCP 47 tag of the active locale (used for
     * `androidx.appcompat.app.AppCompatDelegate.setApplicationLocales`).
     */
    fun bcp47Tag(locale: AppLocale): String = when (locale) {
        AppLocale.Arabic -> "ar-EG"
        AppLocale.English -> "en-US"
    }
}
