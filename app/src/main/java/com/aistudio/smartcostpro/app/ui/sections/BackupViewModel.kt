package com.aistudio.smartcostpro.app.ui.sections

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.smartcostpro.app.data.repository.BackupRepository
import com.aistudio.smartcostpro.app.domain.model.BackupData
import com.aistudio.smartcostpro.app.domain.model.BackupImportSummary
import com.aistudio.smartcostpro.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * UI state for the Backup / Restore screen.
 *
 * The two operations share a state because the screens are visually
 * similar (single-button flows with progress + result) and the user is
 * likely to use both back-to-back when migrating devices.
 */
sealed interface BackupUiState {
    data object Idle : BackupUiState
    data object Working : BackupUiState
    data class ExportSuccess(val file: File) : BackupUiState
    data class RestoreSummaryReady(val summary: BackupImportSummary, val data: BackupData) : BackupUiState
    data class RestoreSuccess(val message: String) : BackupUiState
    data class Error(val message: String) : BackupUiState
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _state = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    /**
     * Exports the four spec-defined collections to a JSON file in the
     * app's private `backups/` directory, then emits [BackupUiState.ExportSuccess]
     * so the screen can share it via FileProvider.
     */
    fun export() {
        _state.value = BackupUiState.Working
        viewModelScope.launch {
            try {
                val data = backupRepository.export()
                val json = backupRepository.encode(data)
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "${Constants.BACKUP_FILE_PREFIX}${timestamp}${Constants.BACKUP_FILE_EXTENSION}"
                val dir = File(context.filesDir, "backups").apply { mkdirs() }
                val file = File(dir, fileName)
                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
                _state.value = BackupUiState.ExportSuccess(file)
            } catch (t: Throwable) {
                _state.value = BackupUiState.Error(t.message ?: "EXPORT_FAILED")
            }
        }
    }

    /**
     * Reads a backup file pointed to by [uri] and parses it. If successful,
     * emits [BackupUiState.RestoreSummaryReady] so the user can review
     * the contents before confirming.
     */
    fun parseRestoreFile(uri: Uri) {
        _state.value = BackupUiState.Working
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                } ?: throw IllegalStateException("CANNOT_READ_FILE")
                val data = backupRepository.parse(json)
                    ?: throw IllegalStateException("err_invalid_file")
                _state.value = BackupUiState.RestoreSummaryReady(
                    summary = backupRepository.summary(data),
                    data = data
                )
            } catch (t: Throwable) {
                _state.value = BackupUiState.Error(t.message ?: "INVALID_FILE")
            }
        }
    }

    /**
     * Restores the previously-parsed [BackupData] into the database.
     * The repository wraps the operation in a Room transaction so the
     * database is never left half-written.
     */
    fun confirmRestore(data: BackupData) {
        _state.value = BackupUiState.Working
        viewModelScope.launch {
            try {
                backupRepository.restore(data)
                _state.value = BackupUiState.RestoreSuccess("RESTORE_SUCCESS")
            } catch (t: Throwable) {
                _state.value = BackupUiState.Error(t.message ?: "RESTORE_FAILED")
            }
        }
    }

    fun resetToIdle() { _state.value = BackupUiState.Idle }
}
