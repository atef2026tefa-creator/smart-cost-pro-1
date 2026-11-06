package com.aistudio.smartcostpro.app.ui.car_income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.smartcostpro.app.data.repository.CarTripRepository
import com.aistudio.smartcostpro.app.domain.calculations.CarTripCalculator
import com.aistudio.smartcostpro.app.domain.model.CarTrip
import com.aistudio.smartcostpro.app.domain.model.CarTripStatistics
import com.aistudio.smartcostpro.app.domain.model.TripType
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

data class CarIncomeUiState(
    val trips: List<CarTrip> = emptyList(),
    val statistics: CarTripStatistics = CarTripStatistics(0.0, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0),
    val locale: AppLocale = AppLocale.Arabic,
    val currency: AppCurrency = AppCurrency.EGP,
    val month: String = DateFormatter.currentMonthIso()
)

@HiltViewModel
class CarIncomeViewModel @Inject constructor(
    private val carTripRepository: CarTripRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val month = kotlinx.coroutines.flow.MutableStateFlow(DateFormatter.currentMonthIso())

    val uiState: StateFlow<CarIncomeUiState> = combine(
        carTripRepository.observeAll(),
        settingsRepository.settings,
        month
    ) { trips, settings, m ->
        CarIncomeUiState(
            trips = trips,
            statistics = CarTripCalculator.statistics(trips, m),
            locale = settings.locale,
            currency = settings.currency,
            month = m
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CarIncomeUiState()
    )

    fun setMonth(value: String) { month.value = value }

    fun upsert(trip: CarTrip) {
        viewModelScope.launch {
            carTripRepository.upsert(trip.copy(id = trip.id.ifBlank { UUID.randomUUID().toString() }))
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { carTripRepository.deleteById(id) }
    }
}
