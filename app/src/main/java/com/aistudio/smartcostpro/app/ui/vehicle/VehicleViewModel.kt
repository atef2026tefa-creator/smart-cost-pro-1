package com.aistudio.smartcostpro.app.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.smartcostpro.app.data.repository.VehicleFuelRepository
import com.aistudio.smartcostpro.app.data.repository.VehicleMaintenanceRepository
import com.aistudio.smartcostpro.app.data.repository.VehicleOilRepository
import com.aistudio.smartcostpro.app.data.repository.VehicleAccidentRepository
import com.aistudio.smartcostpro.app.domain.model.VehicleFuel
import com.aistudio.smartcostpro.app.domain.model.VehicleMaintenance
import com.aistudio.smartcostpro.app.domain.model.VehicleOil
import com.aistudio.smartcostpro.app.domain.model.VehicleAccident
import com.aistudio.smartcostpro.app.util.AppCurrency
import com.aistudio.smartcostpro.app.util.AppLocale
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
 * Aggregated UI state for the Vehicle section screen.
 *
 * The spec defines four subsections:
 *  1. Fuel
 *  2. Maintenance
 *  3. Oils & Filters
 *  4. Accidents
 *
 * Stage 3 ships them as four tabs of the same screen so the user can flip
 * between them without leaving the section.
 */
data class VehicleUiState(
    val fuel: List<VehicleFuel> = emptyList(),
    val maintenance: List<VehicleMaintenance> = emptyList(),
    val oils: List<VehicleOil> = emptyList(),
    val accidents: List<VehicleAccident> = emptyList(),
    val locale: AppLocale = AppLocale.Arabic,
    val currency: AppCurrency = AppCurrency.EGP
)

@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val fuelRepo: VehicleFuelRepository,
    private val maintRepo: VehicleMaintenanceRepository,
    private val oilRepo: VehicleOilRepository,
    private val accidentRepo: VehicleAccidentRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<VehicleUiState> = combine(
        fuelRepo.observeAll(),
        maintRepo.observeAll(),
        oilRepo.observeAll(),
        accidentRepo.observeAll(),
        settingsRepository.settings
    ) { fuel, maint, oil, accidents, settings ->
        VehicleUiState(
            fuel = fuel,
            maintenance = maint,
            oils = oil,
            accidents = accidents,
            locale = settings.locale,
            currency = settings.currency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VehicleUiState()
    )

    // ─── Fuel CRUD ───
    fun upsertFuel(record: VehicleFuel) = viewModelScope.launch {
        fuelRepo.upsert(record.copy(id = record.id.ifBlank { UUID.randomUUID().toString() }))
    }
    fun deleteFuel(id: String) = viewModelScope.launch { fuelRepo.deleteById(id) }

    // ─── Maintenance CRUD ───
    fun upsertMaintenance(record: VehicleMaintenance) = viewModelScope.launch {
        maintRepo.upsert(record.copy(id = record.id.ifBlank { UUID.randomUUID().toString() }))
    }
    fun deleteMaintenance(id: String) = viewModelScope.launch { maintRepo.deleteById(id) }

    // ─── Oil CRUD ───
    fun upsertOil(record: VehicleOil) = viewModelScope.launch {
        oilRepo.upsert(record.copy(id = record.id.ifBlank { UUID.randomUUID().toString() }))
    }
    fun deleteOil(id: String) = viewModelScope.launch { oilRepo.deleteById(id) }

    // ─── Accident CRUD ───
    fun upsertAccident(record: VehicleAccident) = viewModelScope.launch {
        accidentRepo.upsert(record.copy(id = record.id.ifBlank { UUID.randomUUID().toString() }))
    }
    fun deleteAccident(id: String) = viewModelScope.launch { accidentRepo.deleteById(id) }
}
