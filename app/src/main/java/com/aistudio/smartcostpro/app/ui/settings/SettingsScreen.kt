package com.aistudio.smartcostpro.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.smartcostpro.app.BuildConfig
import com.aistudio.smartcostpro.app.R
import com.aistudio.smartcostpro.app.ui.theme.ThemeMode
import com.aistudio.smartcostpro.app.util.AppCurrency
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.Constants

/**
 * Settings screen — language, currency, theme, default screen, backup/restore entry points.
 *
 * Each setting is a single [Card] with a list of radio options. The layout
 * uses Material 3 components only and respects the active locale's
 * layout direction (RTL/LTR) automatically because the theme forces it.
 */
@Composable
fun SettingsScreen(
    onNavigateToBackup: () -> Unit = {},
    onNavigateToRestore: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            SettingsGroup(
                title = stringResource(R.string.settings_language),
                options = listOf(
                    SettingsOption(stringResource(R.string.settings_language_ar)) {
                        viewModel.setLanguage(AppLocale.Arabic)
                    },
                    SettingsOption(stringResource(R.string.settings_language_en)) {
                        viewModel.setLanguage(AppLocale.English)
                    }
                ),
                selectedIndex = if (state.locale == AppLocale.Arabic) 0 else 1
            )
        }

        item {
            SettingsGroup(
                title = stringResource(R.string.settings_currency),
                options = AppCurrency.entries.map { currency ->
                    SettingsOption(currency.code) { viewModel.setCurrency(currency) }
                },
                selectedIndex = AppCurrency.entries.indexOf(state.currency)
            )
        }

        item {
            SettingsGroup(
                title = stringResource(R.string.settings_theme),
                options = listOf(
                    SettingsOption(stringResource(R.string.settings_theme_system)) {
                        viewModel.setThemeMode(ThemeMode.System)
                    },
                    SettingsOption(stringResource(R.string.settings_theme_light)) {
                        viewModel.setThemeMode(ThemeMode.Light)
                    },
                    SettingsOption(stringResource(R.string.settings_theme_dark)) {
                        viewModel.setThemeMode(ThemeMode.Dark)
                    }
                ),
                selectedIndex = when (state.themeMode) {
                    ThemeMode.System -> 0
                    ThemeMode.Light -> 1
                    ThemeMode.Dark -> 2
                }
            )
        }

        item {
            SettingsGroup(
                title = stringResource(R.string.settings_default_screen),
                options = listOf(
                    SettingsOption(stringResource(R.string.settings_default_screen_expenses)) {
                        viewModel.setDefaultScreen(Constants.DEFAULT_SCREEN)
                    },
                    SettingsOption(stringResource(R.string.settings_default_screen_dashboard)) {
                        viewModel.setDefaultScreen("dashboard")
                    }
                ),
                selectedIndex = if (state.defaultScreen == "dashboard") 1 else 0
            )
        }

        item { HorizontalDivider() }

        // Backup / Restore entry points
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.action_backup) + " / " + stringResource(R.string.action_restore),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Button(
                        onClick = onNavigateToBackup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.screen_backup_title))
                    }
                    OutlinedButton(
                        onClick = onNavigateToRestore,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.screen_restore_title))
                    }
                }
            }
        }

        item { HorizontalDivider() }

        item {
            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    options: List<SettingsOption>,
    selectedIndex: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            options.forEachIndexed { index, option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = index == selectedIndex,
                            onClick = option.onClick
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = index == selectedIndex,
                        onClick = option.onClick
                    )
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private data class SettingsOption(val label: String, val onClick: () -> Unit)
