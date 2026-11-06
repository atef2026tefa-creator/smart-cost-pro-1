package com.aistudio.smartcostpro.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.LocaleManager

/**
 * Theme mode selected by the user. Falls back to system when [System] is chosen.
 */
enum class ThemeMode { System, Light, Dark }

/**
 * Top-level composable theme.
 *
 * @param themeMode User preference (system / light / dark)
 * @param locale Current app locale (Arabic = RTL, English = LTR)
 *
 * It composes the right Material 3 color scheme, forces the correct layout
 * direction (so RTL / LTR switches the moment the user changes language),
 * and exposes the locale through a CompositionLocal for downstream formatters.
 */
@Composable
fun CostAppProTheme(
    themeMode: ThemeMode = ThemeMode.System,
    locale: AppLocale = AppLocale.Arabic,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme

    // Force the layout direction based on the active locale so the whole UI
    // mirrors immediately when the user switches Arabic <-> English.
    val layoutDirection = when (locale) {
        AppLocale.Arabic -> LayoutDirection.Rtl
        AppLocale.English -> LayoutDirection.Ltr
    }

    val context = LocalContext.current
    val newConfig = LocaleManager.applyLocaleToConfiguration(context.resources.configuration, locale)

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        androidx.compose.ui.platform.LocalConfiguration provides newConfig
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CostAppProTypography,
            shapes = CostAppProShapes,
            content = content
        )
    }
}
