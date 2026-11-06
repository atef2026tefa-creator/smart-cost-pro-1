package com.aistudio.smartcostpro.app.ui.current_account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.smartcostpro.app.data.repository.CurrentAccountRepository
import com.aistudio.smartcostpro.app.domain.model.CurrentAccountTransaction
import com.aistudio.smartcostpro.app.domain.model.CurrentAccountType
import com.aistudio.smartcostpro.app.util.AppCurrency
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.DateFormatter
import com.aistudio.smartcostpro.app.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CurrentAccountUiState(
    val transactions: List<CurrentAccountTransaction> = emptyList(),
    val locale: AppLocale = AppLocale.Arabic,
    val currency: AppCurrency = AppCurrency.EGP
)

@HiltViewModel
class CurrentAccountViewModel @Inject constructor(
    private val currentAccountRepository: CurrentAccountRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<CurrentAccountUiState> = combine(
        currentAccountRepository.observeAll(),
        settingsRepository.settings
    ) { txns, settings ->
        CurrentAccountUiState(
            transactions = txns,
            locale = settings.locale,
            currency = settings.currency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CurrentAccountUiState()
    )

    /**
     * Inserts a transaction. The amount is stored as a positive number; the
     * sign comes from [CurrentAccountType.sign]. The spec warns against
     * applying the sign twice — we obey by NOT negating withdrawals here.
     *
     * On update, the original `createdAt` is preserved by reading the
     * existing row first. Setting it to 0L would put the row at epoch (1970)
     * and break "recent operations" sorting in the dashboard.
     *
     * Note: the simplest "find by id" call requires a dedicated DAO method.
     * Stage 3.1 can add `currentAccountDao.getById(id)`; for now we filter
     * the in-memory list which is small (current-account transactions are
     * infrequent).
     */
    fun upsert(
        id: String?,
        type: CurrentAccountType,
        description: String,
        amount: Double,
        date: String,
        notes: String?
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = id?.let { targetId ->
                currentAccountRepository.getAll().firstOrNull { it.id == targetId }
            }
            val txn = CurrentAccountTransaction(
                id = id ?: UUID.randomUUID().toString(),
                type = type,
                description = description.trim(),
                amount = amount,
                date = date,
                notes = notes?.ifBlank { null },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            if (existing == null) currentAccountRepository.upsert(txn)
            else currentAccountRepository.update(txn)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { currentAccountRepository.deleteById(id) }
    }
}
