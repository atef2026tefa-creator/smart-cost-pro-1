package com.aistudio.smartcostpro.app.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.smartcostpro.app.data.repository.CertificateRepository
import com.aistudio.smartcostpro.app.data.repository.IncomeRepository
import com.aistudio.smartcostpro.app.domain.calculations.CertificateCalculator
import com.aistudio.smartcostpro.app.domain.calculations.IncomeCalculator
import com.aistudio.smartcostpro.app.domain.model.BankCertificate
import com.aistudio.smartcostpro.app.domain.model.MonthlyIncome
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

/**
 * UI state for the Income & Certificates screen.
 *
 * Combines the active month's monthly income row + all certificates. The
 * calculator's monthly-equivalent profit per certificate is exposed to the
 * UI as a "preview" so the user can see how each certificate will contribute
 * to the dashboard's income total.
 */
data class IncomeUiState(
    val month: String = DateFormatter.currentMonthIso(),
    val monthlyIncome: MonthlyIncome? = null,
    val certificates: List<CertificatePreview> = emptyList(),
    val locale: AppLocale = AppLocale.Arabic,
    val currency: AppCurrency = AppCurrency.EGP
)

/** Certificate + its computed monthly profit for the active month. */
data class CertificatePreview(
    val certificate: BankCertificate,
    val monthlyProfit: Double
)

@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
    private val certificateRepository: CertificateRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val month = kotlinx.coroutines.flow.MutableStateFlow(DateFormatter.currentMonthIso())

    val uiState: StateFlow<IncomeUiState> = combine(
        incomeRepository.observeAllIncome(),
        certificateRepository.observeAll(),
        settingsRepository.settings,
        month
    ) { incomeRows, certs, settings, m ->
        val current = incomeRows.firstOrNull { it.month == m }
        IncomeUiState(
            month = m,
            monthlyIncome = current,
            certificates = certs.map {
                CertificatePreview(it, CertificateCalculator.monthlyProfitFor(it, m))
            },
            locale = settings.locale,
            currency = settings.currency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = IncomeUiState()
    )

    fun setMonth(value: String) { month.value = value }

    // ─── Income CRUD ───
    fun saveMonthlyIncome(
        month: String,
        salary: Double,
        bonuses: Double,
        otherIncome: Double,
        otherIncomeNote: String?
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = incomeRepository.getByMonth(month)
            val updated = (existing?.copy(
                salary = salary,
                bonuses = bonuses,
                otherIncome = otherIncome,
                otherIncomeNote = otherIncomeNote,
                updatedAt = now
            ) ?: MonthlyIncome(
                month = month,
                salary = salary,
                bonuses = bonuses,
                otherIncome = otherIncome,
                otherIncomeNote = otherIncomeNote,
                updatedAt = now
            ))
            incomeRepository.upsert(updated)
        }
    }

    // ─── Certificate CRUD ───
    fun upsertCertificate(cert: BankCertificate) {
        viewModelScope.launch {
            certificateRepository.upsert(cert.copy(id = cert.id.ifBlank { UUID.randomUUID().toString() }))
        }
    }

    fun deleteCertificate(id: String) {
        viewModelScope.launch { certificateRepository.deleteById(id) }
    }
}
