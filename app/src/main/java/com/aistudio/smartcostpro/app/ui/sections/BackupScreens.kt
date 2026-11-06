package com.aistudio.smartcostpro.app.ui.sections

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.smartcostpro.app.R
import com.aistudio.smartcostpro.app.ui.components.ConfirmDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backup screen — single-button "export to JSON" flow that, on success,
 * shares the file via the configured FileProvider.
 *
 * **Spec compliance:**
 *  - The backup contains: schemaVersion, exportedAt, expenses, income,
 *    certificates, carIncome (spec §19.1).
 *  - Other collections (students, associations, accidents, budget,
 *    section order) are NOT part of the backup — the spec is explicit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        when (val s = state) {
            is BackupUiState.ExportSuccess -> {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    s.file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.backup_share_title)))
                viewModel.resetToIdle()
            }
            is BackupUiState.Error -> {
                val msg = translateError(s.message, context)
                snackbarHostState.showSnackbar(msg)
                viewModel.resetToIdle()
            }
            else -> { /* no-op */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_backup_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderCard(
                icon = Icons.Filled.Backup,
                title = stringResource(R.string.screen_backup_title),
                subtitle = stringResource(R.string.screen_backup_subtitle)
            )

            Text(
                text = stringResource(R.string.backup_export_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { viewModel.export() },
                enabled = state !is BackupUiState.Working,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state is BackupUiState.Working) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                } else {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(stringResource(R.string.backup_export_button))
            }
        }
    }
}

/**
 * Restore screen — uses the document picker to choose a JSON file, parses
 * it, shows the user a summary, and only restores after explicit confirmation.
 *
 * **Spec compliance:**
 *  - The restore is a full replacement, not a merge (spec §19.2).
 *  - The user must confirm before any data is touched (spec §19.3).
 *  - The schema version is checked; mismatch rejects the import.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreScreen(
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.parseRestoreFile(uri)
    }

    LaunchedEffect(state) {
        when (val s = state) {
            is BackupUiState.RestoreSuccess -> {
                snackbarHostState.showSnackbar(context.getString(R.string.restore_success))
                viewModel.resetToIdle()
            }
            is BackupUiState.Error -> {
                val msg = translateError(s.message, context)
                snackbarHostState.showSnackbar(msg)
                viewModel.resetToIdle()
            }
            else -> { /* no-op */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_restore_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderCard(
                icon = Icons.Filled.Restore,
                title = stringResource(R.string.screen_restore_title),
                subtitle = stringResource(R.string.screen_restore_subtitle)
            )

            Text(
                text = stringResource(R.string.restore_warning_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )

            Button(
                onClick = { pickFile.launch(arrayOf("application/json", "text/plain", "*/*")) },
                enabled = state !is BackupUiState.Working,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.restore_pick_file))
            }

            if (state is BackupUiState.Working) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Restore summary + confirmation
            val summaryState = state as? BackupUiState.RestoreSummaryReady
            if (summaryState != null) {
                RestoreSummaryCard(
                    summary = summaryState.summary,
                    onConfirm = { viewModel.confirmRestore(summaryState.data) },
                    onCancel = { viewModel.resetToIdle() }
                )
            }
        }
    }
}

@Composable
private fun HeaderCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun RestoreSummaryCard(
    summary: com.aistudio.smartcostpro.app.domain.model.BackupImportSummary,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }
    val exportedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(summary.exportedAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.restore_summary_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            SummaryRow(stringResource(R.string.restore_summary_schema, summary.schemaVersion))
            SummaryRow(stringResource(R.string.restore_summary_exported, exportedAt))
            SummaryRow(stringResource(R.string.restore_summary_expenses, summary.expensesCount))
            SummaryRow(stringResource(R.string.restore_summary_income, summary.incomeCount))
            SummaryRow(stringResource(R.string.restore_summary_certificates, summary.certificatesCount))
            SummaryRow(stringResource(R.string.restore_summary_car_income, summary.carIncomeCount))

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.restore_warning_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.form_cancel))
                }
                Button(onClick = { showConfirm = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.restore_confirm))
                }
            }
        }
    }

    if (showConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.restore_warning_title),
            message = stringResource(R.string.restore_warning_message),
            confirmLabel = stringResource(R.string.restore_confirm),
            cancelLabel = stringResource(R.string.form_cancel),
            onConfirm = {
                showConfirm = false
                onConfirm()
            },
            onDismiss = { showConfirm = false }
        )
    }
}

@Composable
private fun SummaryRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Translates internal error codes (thrown by the BackupViewModel) into
 * localized user-facing strings.
 *
 * Stage 3 fix — previously raw English strings like "Cannot read file"
 * were shown directly to the user in the snackbar.
 */
private fun translateError(code: String, context: android.content.Context): String {
    val resId = when (code) {
        "CANNOT_READ_FILE" -> R.string.err_cannot_read_file
        "err_invalid_file", "INVALID_FILE" -> R.string.err_invalid_file
        "EXPORT_FAILED" -> R.string.err_export_failed
        "RESTORE_FAILED" -> R.string.err_restore_failed
        else -> R.string.err_restore_failed
    }
    return context.getString(resId)
}
