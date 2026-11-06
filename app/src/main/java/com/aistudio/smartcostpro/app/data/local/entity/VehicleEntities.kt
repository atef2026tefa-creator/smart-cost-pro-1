package com.aistudio.smartcostpro.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Vehicle fuel record.
 *
 * Per the spec: `cost = quantity × unitPrice`, with quantity strictly > 0.
 */
@Entity(
    tableName = "vehicle_fuel",
    indices = [Index(value = ["date"]), Index(value = ["fuelType"])]
)
data class VehicleFuelEntity(
    @PrimaryKey val id: String,
    val fuelType: String,
    val quantity: Double,
    val unitPrice: Double,
    val odometer: Double? = null,
    val date: String,
    val time: String? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Vehicle maintenance record.
 *
 * Per the spec: `total = supplyPrice + laborPrice`. Stored separately from
 * the unified expense table to preserve the supply / labor breakdown.
 *
 * Note: the dashboard's expense totals read from the unified [ExpenseEntity]
 * table only — these detail rows feed the vehicle section's UI.
 */
@Entity(
    tableName = "vehicle_maintenance",
    indices = [Index(value = ["date"])]
)
data class VehicleMaintenanceEntity(
    @PrimaryKey val id: String,
    val maintenanceType: String? = null,
    val supplyName: String? = null,
    val supplyPrice: Double = 0.0,
    val laborDescription: String? = null,
    val laborPrice: Double = 0.0,
    val date: String,
    val total: Double = 0.0,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Vehicle oils / filters record.
 *
 * The spec is explicit: this row must preserve brand, product name,
 * quantity, price, odometer and suggested interval — they cannot be
 * reduced to a single amount in the unified expense table.
 */
@Entity(
    tableName = "vehicle_oils",
    indices = [Index(value = ["date"])]
)
data class VehicleOilEntity(
    @PrimaryKey val id: String,
    val serviceType: String,
    val brand: String = "Mobil",
    val productName: String? = null,
    val quantity: Double = 1.0,
    val price: Double = 0.0,
    val odometer: Double? = null,
    val suggestedIntervalKm: Int? = null,
    val notes: String? = null,
    val date: String,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Vehicle accident record.
 *
 * Per the spec:
 *  - Stored separately from the unified expense table.
 *  - `estimatedDamage` is an *estimate*, NOT a paid expense.
 *  - It must NOT be added automatically to the vehicle total.
 */
@Entity(
    tableName = "vehicle_accidents",
    indices = [Index(value = ["date"]), Index(value = ["vehicleId"])]
)
data class VehicleAccidentEntity(
    @PrimaryKey val id: String,
    val vehicleId: String? = null,
    val title: String,
    val photoUrl: String? = null,
    val date: String,
    val time: String? = null,
    val location: String? = null,
    val estimatedDamage: Double? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
