package com.aistudio.smartcostpro.app

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.smartcostpro.app.ui.navigation.RootNavGraph
import com.aistudio.smartcostpro.app.ui.settings.SettingsViewModel
import com.aistudio.smartcostpro.app.ui.theme.CostAppProTheme
import com.aistudio.smartcostpro.app.ui.theme.ThemeMode
import com.aistudio.smartcostpro.app.util.AppLocale
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Single-activity entry point.
 *
 * Stage 3: extends [AppCompatActivity] (instead of [ComponentActivity]) so
 * `AppCompatDelegate.setApplicationLocales` actually drives the resources
 * configuration — without this, switching language from Arabic to English
 * would only flip the layout direction but every `stringResource()` lookup
 * would still resolve to the system locale.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var biometricHelper: com.aistudio.smartcostpro.app.data.auth.BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the androidx.core.splashscreen before super.onCreate so the
        // system replaces the launcher window background smoothly.
        installSplashScreen()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )

        super.onCreate(savedInstanceState)

        try {
            setContent {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

                CompositionLocalProvider(
                    LocalAppThemeMode provides uiState.themeMode,
                    LocalAppLocale provides uiState.locale
                ) {
                    CostAppProTheme(
                        themeMode = uiState.themeMode,
                        locale = uiState.locale
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Unspecified),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            // Stage 3.2: start on the Login screen; the NavGraph
                            // handles the redirect to Home after sign-in.
                            RootNavGraph()
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Timber.e(t, "Failed to set Compose content")
            throw t
        }
    }
}

/**
 * Process-death-safe holder for the current app theme (light/dark/system).
 * Provided by [MainActivity] from [SettingsViewModel].
 */
val LocalAppThemeMode = staticCompositionLocalOf<ThemeMode> {
    error("LocalAppThemeMode not provided")
}

/**
 * Holder for the current app locale ("ar" / "en") used by all UI components
 * for date / currency formatting.
 */
val LocalAppLocale = staticCompositionLocalOf<AppLocale> {
    error("LocalAppLocale not provided")
}
