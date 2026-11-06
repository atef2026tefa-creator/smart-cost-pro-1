package com.aistudio.smartcostpro.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Vehicle fuel record.
 *
 * Per the spec:
 *   cost = quantity × unitPrice
 *   quantity must be > 0
 *
 * @param id Stable identifier.
 * @param fuelType Fuel code (gasoline_80, gasoline_92, gasoline_95, diesel, gas, electric_home, electric_ac, electric_dc).
 * @param quantity Liters / kWh (must be > 0).
 * @param unitPrice Price per unit.
 * @param odometer Optional odometer reading at fill-up.
 * @param date ISO date (YYYY-MM-DD).
 * @param time Optional time (HH:mm).
 * @param notes Optional.
 * @param createdAt Epoch millis.
 */
@Serializable
data class VehicleFuel(
    val id: String,
    val fuelType: String,
    val quantity: Double,
    val unitPrice: Double,
    val odometer: Double? = null,
    val date: String,
    val time: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** `cost = quantity × unitPrice` per the spec. */
    val cost: Double get() = quantity * unitPrice
}

/**
 * Vehicle maintenance record.
 *
 * Per the spec:
 *   total = supplyPrice + laborPrice
 *
 * The spec warns that the total must NOT be double-counted in the dashboard
 * expense totals — only the parent [Expense].amount goes into the unified total.
 */
@Serializable
data class VehicleMaintenance(
    val id: String,
    val maintenanceType: String? = null,
    val supplyName: String? = null,
    val supplyPrice: Double = 0.0,
    val laborDescription: String? = null,
    val laborPrice: Double = 0.0,
    val date: String,
    val total: Double = 0.0,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** `total = supplyPrice + laborPrice` per the spec. */
    val computedTotal: Double get() = supplyPrice + laborPrice
}

/**
 * Vehicle oils / filters record.
 *
 * The spec is explicit: oil & filter details must NOT be reduced to a single
 * expense amount. The complete record (brand, product, quantity, price,
 * odometer, suggested interval) must all be preserved.
 *
 * @param id Stable identifier.
 * @param serviceType Type of service (oil change, filter change, etc.).
 * @param brand Brand (default: "Mobil").
 * @param productName Product name.
 * @param quantity Quantity.
 * @param price Price.
 * @param odometer Reading at change time.
 * @param suggestedIntervalKm Optional suggested next-change interval (e.g. 5000).
 * @param notes Optional.
 * @param date ISO date.
 */
@Serializable
data class VehicleOil(
    val id: String,
    val serviceType: String,
    val brand: String = "Mobil",
    val productName: String? = null,
    val quantity: Double = 1.0,
    val price: Double = 0.0,
    val odometer: Double? = null,
    val suggestedIntervalKm: Int? = null,
    val notes: String? = null,
    val date: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
