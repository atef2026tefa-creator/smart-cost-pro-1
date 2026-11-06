package com.aistudio.smartcostpro.app.domain.model

import kotlinx.serialization.Serializable

/**
 * A current account transaction (deposit or withdrawal).
 *
 * Per the spec:
 *  - `currentAccountNet = sum of all movements in the month`
 *  - Deposit contributes +amount; withdrawal contributes -amount.
 *  - The sign must be applied exactly once — not twice — when the user
 *    enters a withdrawal, so we store `amount` as a positive number and use
 *    [CurrentAccountType.sign] when computing the net.
 *
 * **Important:** deposits and withdrawals of this kind do NOT enter the
 * primary income total. They have their own line in the dashboard's
 * `monthlyIncome` calculation: `monthlyIncome = primaryIncome + certificatesProfit + currentAccountNet + associationFinancials.earned`.
 */
@Serializable
data class CurrentAccountTransaction(
    val id: String,
    val type: CurrentAccountType,
    val description: String,
    val amount: Double, // always > 0; sign comes from `type.sign`
    val date: String,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Signed amount (+ for deposit, − for withdrawal). */
    val signedAmount: Double get() = amount * type.sign
}
