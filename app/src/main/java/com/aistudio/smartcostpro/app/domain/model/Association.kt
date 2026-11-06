package com.aistudio.smartcostpro.app.domain.model

import kotlinx.serialization.Serializable

/**
 * An association (جمعية) — informal savings circle.
 *
 * Per the spec:
 *
 * - `installment = value / memberCount`
 * - The number of installments equals `memberCount`.
 * - When computing the monthly installment for a given target month:
 *       monthIndex = (targetYear - startYear) * 12 + (targetMonth - startMonth)
 *   The installment is added only if `0 ≤ monthIndex < max(1, memberCount)`.
 * - The payout amount (`value`) is added to the month's income when the
 *   first 7 characters of `payoutDate` match the target month.
 *
 * **Critical spec rules:**
 *   - The installment is NOT added to the unified expense total.
 *   - The installment MAY be added to the Personal section card display.
 *   - The payout amount MAY be added to the monthly income total.
 *
 * @param id Stable identifier.
 * @param name Mandatory.
 * @param value Mandatory, > 0.
 * @param memberCount Mandatory, ≥ 1.
 * @param installment Computed once as `value / memberCount`.
 * @param roleNumber The member's slot number, between 1 and memberCount inclusive.
 * @param payoutDate ISO date — when this member receives the pooled money.
 * @param startDate ISO date — when the association starts.
 * @param notes Optional.
 * @param createdAt Epoch millis.
 */
@Serializable
data class Association(
    val id: String,
    val name: String,
    val value: Double,
    val memberCount: Int,
    val installment: Double,
    val roleNumber: Int,
    val payoutDate: String,
    val startDate: String,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Result of running the association calculator for a given target month.
 *
 * `installments` is the sum of all monthly installments due (this goes
 * into the Personal card display, NOT the unified expense total).
 * `earned` is the sum of all payouts that fall in the target month (this
 * goes into the monthly income total).
 */
data class AssociationFinancials(
    val installments: Double,
    val earned: Double,
    val activeAssociations: List<Association>
)
