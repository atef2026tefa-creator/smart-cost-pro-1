package com.aistudio.smartcostpro.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Smart Cost Pro brand palette (Stage 3.2 — redesigned identity).
 *
 * The palette is built around the app icon's color story:
 *  - Primary light blue gradient (#1a5a8a → #4a9bc7)
 *  - Secondary orange (#ff8c42) — pulled from the calculator "=" sign
 *  - Neutral warm surface (#faf8f2) for a softer financial look
 *
 * This replaces the original emerald-and-gold palette which didn't match
 * the new icon.
 */
object BrandColors {
    // Brand
    val Primary = Color(0xFF1A5A8A)
    val PrimaryDark = Color(0xFF0D3A5E)
    val PrimaryLight = Color(0xFF4FA3D9)
    val Accent = Color(0xFFFF8C42)
    val AccentDark = Color(0xFFD9622E)

    // Surfaces
    val SurfaceLight = Color(0xFFFAF8F2)
    val SurfaceDark = Color(0xFF0E1116)
    val SurfaceVariantLight = Color(0xFFF5F8FB)
    val SurfaceVariantDark = Color(0xFF1A1E24)
    val BackgroundLight = Color(0xFFF2F5F8)
    val BackgroundDark = Color(0xFF07090C)

    // Text
    val OnSurfaceLight = Color(0xFF1B1C1A)
    val OnSurfaceDark = Color(0xFFE6E3DD)
    val OnSurfaceVariantLight = Color(0xFF555048)
    val OnSurfaceVariantDark = Color(0xFFB5B0A6)
    val OutlineLight = Color(0xFFD7DDE3)
    val OutlineDark = Color(0xFF2A2E34)

    // Semantic
    val Income = Color(0xFF1F7A3A)
    val IncomeDark = Color(0xFF6BC97B)
    val Expense = Color(0xFFBA1A1A)
    val ExpenseDark = Color(0xFFFFB4AB)
    val Warning = Color(0xFFB45309)
    val Info = Color(0xFF005FB8)
    val Success = Color(0xFF1F7A3A)

    // Errors
    val ErrorLight = Color(0xFFBA1A1A)
    val ErrorDark = Color(0xFFFFB4AB)
    val OnErrorLight = Color(0xFFFFFFFF)
    val OnErrorDark = Color(0xFF690005)
}

/**
 * Material 3 dark color scheme (Stage 3.2 — blue brand identity).
 */
val DarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = BrandColors.PrimaryLight,
    onPrimary = Color(0xFF003355),
    primaryContainer = BrandColors.PrimaryDark,
    onPrimaryContainer = Color(0xFFBFE0F5),
    secondary = BrandColors.Accent,
    onSecondary = Color(0xFF3A1F00),
    secondaryContainer = BrandColors.AccentDark,
    onSecondaryContainer = Color(0xFFFFE08A),
    tertiary = Color(0xFFB5B0A6),
    onTertiary = Color(0xFF2A2E34),
    background = BrandColors.BackgroundDark,
    onBackground = BrandColors.OnSurfaceDark,
    surface = BrandColors.SurfaceDark,
    onSurface = BrandColors.OnSurfaceDark,
    surfaceVariant = BrandColors.SurfaceVariantDark,
    onSurfaceVariant = BrandColors.OnSurfaceVariantDark,
    outline = BrandColors.OutlineDark,
    outlineVariant = Color(0xFF42474E),
    error = BrandColors.ErrorDark,
    onError = BrandColors.OnErrorDark,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE6E3DD),
    inverseOnSurface = Color(0xFF2A2E34),
    inversePrimary = BrandColors.Primary
)

/**
 * Material 3 light color scheme (Stage 3.2 — blue brand identity).
 */
val LightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = BrandColors.Primary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8E6F5),
    onPrimaryContainer = Color(0xFF002133),
    secondary = BrandColors.AccentDark,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE08A),
    onSecondaryContainer = Color(0xFF2A1F00),
    tertiary = Color(0xFF555048),
    onTertiary = Color(0xFFFFFFFF),
    background = BrandColors.BackgroundLight,
    onBackground = BrandColors.OnSurfaceLight,
    surface = BrandColors.SurfaceLight,
    onSurface = BrandColors.OnSurfaceLight,
    surfaceVariant = BrandColors.SurfaceVariantLight,
    onSurfaceVariant = BrandColors.OnSurfaceVariantLight,
    outline = BrandColors.OutlineLight,
    outlineVariant = Color(0xFFC6D1DA),
    error = BrandColors.ErrorLight,
    onError = BrandColors.OnErrorLight,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF2A2E34),
    inverseOnSurface = Color(0xFFE6E3DD),
    inversePrimary = BrandColors.PrimaryLight
)
