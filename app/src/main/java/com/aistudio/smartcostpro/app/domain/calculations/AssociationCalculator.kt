package com.aistudio.smartcostpro.app.domain.calculations

import com.aistudio.smartcostpro.app.domain.model.Association
import com.aistudio.smartcostpro.app.domain.model.AssociationFinancials
import com.aistudio.smartcostpro.app.util.DateFormatter
import kotlin.math.floor
import kotlin.math.max

/**
 * Association (جمعية) calculations.
 *
 * **Spec compliance:**
 *
 * 1. `installment = value / memberCount` — no explicit rounding.
 * 2. For a target month:
 *        monthIndex = (targetYear - startYear) * 12 + (targetMonth - startMonth)
 *    The installment is added if `0 ≤ monthIndex < max(1, floor(memberCount || 1))`.
 * 3. The payout amount (`value`) is added to the month's income when the
 *    first 7 characters of `payoutDate` match the target month.
 * 4. The installment is NOT added to the unified expense total.
 * 5. The payout is added to the monthly income.
 *
 * The Personal section card may show `personalTotal + installments`, but the
 * dashboard's grand total does NOT include installments. This is a deliberate
 * difference — handled in [DashboardCalculator].
 */
object AssociationCalculator {

    /**
     * Computes the monthly installment. Per the spec:
     *     installment = value / memberCount
     */
    fun computeInstallment(value: Double, memberCount: Int): Double {
        require(memberCount >= 1) { "memberCount must be >= 1" }
        return value / memberCount
    }

    /**
     * Returns true when the given association is "active" for the target month
     * — i.e. the target month falls within the installments cycle.
     */
    fun isActiveInMonth(association: Association, month: String): Boolean {
        val startMonth = DateFormatter.monthOf(association.startDate) ?: return false
        val monthIndex = DateFormatter.monthsBetween(startMonth, month) ?: return false
        val cycleLength = max(1, floor(association.memberCount.toDouble()).toInt())
        return monthIndex >= 0 && monthIndex < cycleLength
    }

    /**
     * Computes the installments due for the target month across all associations,
     * plus the payouts earned in the target month.
     *
     * **Returns** an [AssociationFinancials] object so the UI can:
     * - Add `installments` to the Personal section card display.
     * - Add `earned` to the monthly income total.
     * - Pass the active associations list to the Personal section UI.
     */
    fun financialsForMonth(associations: List<Association>, month: String): AssociationFinancials {
        val active = associations.filter { isActiveInMonth(it, month) }
        val installments = active.sumOf { it.installment }
        val earned = associations.filter { belongsToMonth(it.payoutDate, month) }.sumOf { it.value }
        return AssociationFinancials(
            installments = installments,
            earned = earned,
            activeAssociations = active
        )
    }

    /**
     * Helper: returns true when the payout month matches the target month.
     * Per the spec, the comparison is on the first 7 characters of the ISO date.
     */
    private fun belongsToMonth(payoutDate: String, month: String): Boolean {
        if (payoutDate.length < 7) return false
        return payoutDate.substring(0, 7) == month
    }

    // ───────────────────────────── Validation ─────────────────────────────

    /**
     * Validates the association before persistence.
     *
     * Per the spec:
     * - name is required
     * - value > 0
     * - memberCount ≥ 1
     * - roleNumber 1 ≤ roleNumber ≤ memberCount
     * - startDate is required
     * - payoutDate is required
     */
    fun validate(
        name: String,
        value: Double,
        memberCount: Int,
        roleNumber: Int,
        startDate: String,
        payoutDate: String
    ): ValidationError? {
        if (name.isBlank()) return ValidationError.NameRequired
        if (value <= 0.0) return ValidationError.ValueInvalid
        if (memberCount < 1) return ValidationError.MemberCountInvalid
        if (roleNumber < 1 || roleNumber > memberCount) return ValidationError.RoleNumberInvalid
        if (startDate.isBlank()) return ValidationError.StartDateRequired
        if (payoutDate.isBlank()) return ValidationError.PayoutDateRequired
        return null
    }

    enum class ValidationError {
        NameRequired,
        ValueInvalid,
        MemberCountInvalid,
        RoleNumberInvalid,
        StartDateRequired,
        PayoutDateRequired
    }
}
