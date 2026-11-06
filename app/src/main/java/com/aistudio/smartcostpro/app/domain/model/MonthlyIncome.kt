package com.aistudio.smartcostpro.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Monthly income record persisted to Room.
 *
 * The spec stores a monthly summary per `YYYY-MM` plus a JSON-encoded list of
 * detailed income sources. The dashboard's primary income reads from the
 * salary / bonuses / otherIncome fields if the details list is empty.
 *
 * @param month ISO month string (`YYYY-MM`).
 * @param salary Total salary for the month (≥ 0).
 * @param bonuses Total bonuses for the month (≥ 0).
 * @param otherIncome Other income total (≥ 0).
 * @param otherIncomeNote Optional description for the "other" bucket.
 * @param detailsJson JSON list of [IncomeSource] entries (salary / extra / bonus / investment / other).
 *   If non-empty, the dashboard's primary income uses the sum of these
 *   entries (excluding any entry typed as `current_deposit` / `current_withdrawal`,
 *   which belong to the current account, NOT to income).
 * @param updatedAt Epoch milliseconds of the last edit.
 */
@Serializable
data class MonthlyIncome(
    val month: String, // YYYY-MM
    val salary: Double = 0.0,
    val bonuses: Double = 0.0,
    val otherIncome: Double = 0.0,
    val otherIncomeNote: String? = null,
    val detailsJson: String? = null,
    val updatedAt: Long
)

/**
 * Detailed income source entry (one of several rows that can live inside
 * [MonthlyIncome.detailsJson]).
 */
@Serializable
data class IncomeSource(
    val id: String,
    val name: String,
    val amount: Double,
    val date: String, // YYYY-MM-DD
    val type: IncomeType = IncomeType.Other,
    val notes: String? = null
)
