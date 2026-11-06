package com.aistudio.smartcostpro.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Vehicle accident record.
 *
 * Per the spec, accidents are stored separately from the unified expense
 * table and `estimatedDamage` is an *estimate*, NOT a paid expense — it must
 * never be added automatically to the vehicle or expense totals.
 *
 * @param id Stable identifier.
 * @param vehicleId Optional vehicle reference (multi-vehicle support).
 * @param title Mandatory accident title.
 * @param photoUrl Optional photo URI (camera capture in future stages).
 * @param date ISO date (YYYY-MM-DD).
 * @param time ISO time (HH:mm) or null.
 * @param location Optional.
 * @param estimatedDamage Optional estimate — used for display only.
 * @param notes Optional.
 * @param createdAt Epoch millis.
 */
@Serializable
data class VehicleAccident(
    val id: String,
    val vehicleId: String? = null,
    val title: String,
    val photoUrl: String? = null,
    val date: String,
    val time: String? = null,
    val location: String? = null,
    val estimatedDamage: Double? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
