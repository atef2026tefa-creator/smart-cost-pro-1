package com.aistudio.smartcostpro.app.domain.model

import kotlinx.serialization.Serializable

/**
 * A single car income trip.
 *
 * Stored separately from vehicle expenses. The spec stores these in
 * `smartcost_car_income_trips` and the API has a `car_income_trips` table.
 *
 * Per the spec, **car trip income is NOT automatically merged into the
 * monthly income total** — the `onSyncWithMonthlyIncome` callback is the
 * place where this is decided by the parent component, and the spec
 * explicitly marks this as "unresolved".
 *
 * Statistics computed from a list of trips:
 *  - todayTotal = sum of trips where date == today
 *  - monthTotal = sum of trips where date belongs to selected month
 *  - totalIncome = sum of all trip amounts
 *  - avgFare = totalIncome / count (0 if no trips)
 *  - minFare / maxFare = min / max of trip amounts
 *
 * @param id Stable identifier.
 * @param date ISO date (YYYY-MM-DD).
 * @param amount Fare amount (≥ 0).
 * @param tripType Filter type — see [TripType].
 * @param notes Optional.
 * @param createdAt Epoch millis.
 */
@Serializable
data class CarTrip(
    val id: String,
    val date: String,
    val amount: Double,
    val tripType: TripType = TripType.Cash,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Trip statistics returned by [com.aistudio.smartcostpro.app.domain.calculations.CarTripCalculator].
 */
data class CarTripStatistics(
    val todayTotal: Double,
    val weekTotal: Double,
    val monthTotal: Double,
    val totalIncome: Double,
    val tripCount: Int,
    val avgFare: Double,
    val maxFare: Double,
    val minFare: Double
)
