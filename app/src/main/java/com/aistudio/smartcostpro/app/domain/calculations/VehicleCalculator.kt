package com.aistudio.smartcostpro.app.domain.calculations

import com.aistudio.smartcostpro.app.domain.model.VehicleAccident
import com.aistudio.smartcostpro.app.domain.model.VehicleFuel
import com.aistudio.smartcostpro.app.domain.model.VehicleMaintenance
import com.aistudio.smartcostpro.app.domain.model.VehicleOil
import com.aistudio.smartcostpro.app.util.DateFormatter

/**
 * Vehicle expenses calculations.
 *
 * **Spec compliance:**
 *
 * 1. `fuelCost = quantity × unitPrice` (quantity must be > 0).
 * 2. `maintenanceTotal = supplyPrice + laborPrice` (the spec warns this
 *    must NOT be double-counted in the dashboard's expense totals — the
 *    dashboard reads from the unified expenses table only).
 * 3. `vehicleTotal = fuelTotal + maintTotal + oilTotal` (sum of the three
 *    vehicle types only — accident `estimatedDamage` is NOT included).
 * 4. Accident `estimatedDamage` is an *estimate*, never a paid expense.
 */
object VehicleCalculator {

    /**
     * Per-fuel cost = quantity × unitPrice.
     */
    fun fuelCost(record: VehicleFuel): Double = record.quantity * record.unitPrice

    /**
     * Total fuel cost for a given month.
     */
    fun fuelTotalForMonth(records: List<VehicleFuel>, month: String): Double {
        return records
            .filter { DateFormatter.isDateInMonth(it.date, month) }
            .sumOf { fuelCost(it) }
    }

    /**
     * Maintenance total = supplyPrice + laborPrice.
     */
    fun maintenanceTotal(record: VehicleMaintenance): Double {
        // Prefer the explicit total field if it is positive; otherwise compute.
        return if (record.total > 0.0) record.total else record.supplyPrice + record.laborPrice
    }

    /**
     * Total maintenance for a given month.
     */
    fun maintenanceTotalForMonth(records: List<VehicleMaintenance>, month: String): Double {
        return records
            .filter { DateFormatter.isDateInMonth(it.date, month) }
            .sumOf { maintenanceTotal(it) }
    }

    /**
     * Total oil / filter cost for a given month.
     */
    fun oilTotalForMonth(records: List<VehicleOil>, month: String): Double {
        return records
            .filter { DateFormatter.isDateInMonth(it.date, month) }
            .sumOf { it.price }
    }

    /**
     * `vehicleTotal = fuelTotal + maintTotal + oilTotal` (spec §8.4).
     *
     * The accident list is passed in only to compute the operation *count*
     * for the vehicle section UI — its `estimatedDamage` is NEVER added to
     * the total (spec §8.4 / §15.2).
     */
    fun vehicleTotalForMonth(
        fuel: List<VehicleFuel>,
        maintenance: List<VehicleMaintenance>,
        oils: List<VehicleOil>,
        month: String
    ): Double {
        return fuelTotalForMonth(fuel, month) +
            maintenanceTotalForMonth(maintenance, month) +
            oilTotalForMonth(oils, month)
    }

    /**
     * Operation count includes fuel + maintenance + oil + accidents (spec §8.4
     * says the count "may include accidents" even though they don't enter the
     * total). Useful for the vehicle section's UI summary.
     */
    fun operationCountForMonth(
        fuel: List<VehicleFuel>,
        maintenance: List<VehicleMaintenance>,
        oils: List<VehicleOil>,
        accidents: List<VehicleAccident>,
        month: String
    ): Int {
        val fuelCount = fuel.count { DateFormatter.isDateInMonth(it.date, month) }
        val maintCount = maintenance.count { DateFormatter.isDateInMonth(it.date, month) }
        val oilCount = oils.count { DateFormatter.isDateInMonth(it.date, month) }
        val accidentCount = accidents.count { DateFormatter.isDateInMonth(it.date, month) }
        return fuelCount + maintCount + oilCount + accidentCount
    }
}
