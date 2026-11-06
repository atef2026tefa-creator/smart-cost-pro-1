package com.aistudio.smartcostpro.app.domain.calculations

import com.aistudio.smartcostpro.app.domain.model.CarTrip
import com.aistudio.smartcostpro.app.domain.model.CarTripStatistics
import com.aistudio.smartcostpro.app.domain.model.TripType
import com.aistudio.smartcostpro.app.util.DateFormatter

/**
 * Car trip income calculations.
 *
 * **Spec compliance:**
 * - todayTotal = sum of amounts where date == today
 * - monthTotal = sum of amounts where date is in the selected month
 * - totalIncome = sum of all amounts
 * - avgFare = totalIncome / count (0 when count == 0)
 * - minFare / maxFare = min / max of amounts
 *
 * **Critical:** the spec explicitly states that car trip income is NOT
 * automatically merged into the monthly income total. The dashboard's
 * `monthlyIncome` formula is:
 *
 *     monthlyIncome = primaryIncome + certificatesProfit + currentAccountNet + associationFinancials.earned
 *
 * Stage 1 keeps this behavior. The merge is decided by the
 * `onSyncWithMonthlyIncome` callback, which is "unresolved" per the spec.
 */
object CarTripCalculator {

    fun statistics(trips: List<CarTrip>, month: String): CarTripStatistics {
        val today = DateFormatter.todayIso()
        val todayTotal = trips.filter { it.date == today }.sumOf { it.amount }
        val monthTrips = trips.filter { DateFormatter.isDateInMonth(it.date, month) }
        val monthTotal = monthTrips.sumOf { it.amount }
        val totalIncome = trips.sumOf { it.amount }
        val count = trips.size
        val avgFare = if (count == 0) 0.0 else totalIncome / count
        val maxFare = trips.maxOfOrNull { it.amount } ?: 0.0
        val minFare = trips.minOfOrNull { it.amount } ?: 0.0
        // Week total: trips in the last 7 days (best-effort).
        val todayDate = java.time.LocalDate.now()
        val weekStart = todayDate.minusDays(6).toString()
        val weekTotal = trips.filter { it.date >= weekStart && it.date <= today }.sumOf { it.amount }
        return CarTripStatistics(
            todayTotal = todayTotal,
            weekTotal = weekTotal,
            monthTotal = monthTotal,
            totalIncome = totalIncome,
            tripCount = count,
            avgFare = avgFare,
            maxFare = maxFare,
            minFare = minFare
        )
    }

    /**
     * Filters trips by type. [TripType.All] returns the original list.
     */
    fun filterByType(trips: List<CarTrip>, type: TripType): List<CarTrip> {
        if (type == TripType.All) return trips
        return trips.filter { it.tripType == type }
    }
}
