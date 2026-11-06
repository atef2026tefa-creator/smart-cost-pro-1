package com.aistudio.smartcostpro.app.domain.model

import kotlinx.serialization.Serializable

/**
 * JSON payload stored inside [Expense.detailsJson] when the type is
 * [ExpenseType.Medical].
 *
 * The spec requires every one of these fields to be preserved during
 * migration — none of them may be silently dropped.
 *
 * See `§5.2` of the SMART-COST Android Logic Spec for the canonical
 * field list. Stage 2 introduces this typed representation so the
 * medical form can edit it without leaking JSON serialization to the UI.
 */
@Serializable
data class MedicalDetails(
    val familyMember: String? = null,
    val facilityName: String? = null,
    val facilityType: String? = null,
    val condition: String? = null,
    val diagnosis: String? = null,
    val examCost: Double = 0.0,
    val medicationCost: Double = 0.0,
    val date: String? = null,
    val medicalNotes: String? = null
) {
    /**
     * The "amount" the parent [Expense] should carry for a medical record.
     * Per the spec, the medical record's contribution to the dashboard's
     * expense total is `examCost + medicationCost`.
     */
    val totalAmount: Double get() = examCost + medicationCost
}

/**
 * JSON payload stored inside [Expense.detailsJson] when the type is
 * [ExpenseType.VehicleMaint].
 *
 * Per the spec: `total = supplyPrice + laborPrice`. The parent
 * [Expense].amount carries this total — the dashboard's expense totals
 * read from `amount`, so the supply/labor breakdown must NOT be summed
 * again into the dashboard (spec §8.2).
 */
@Serializable
data class MaintenanceDetails(
    val maintenanceType: String? = null,
    val supplyName: String? = null,
    val supplyPrice: Double = 0.0,
    val laborDescription: String? = null,
    val laborPrice: Double = 0.0,
    val date: String? = null
) {
    val total: Double get() = supplyPrice + laborPrice
}

/**
 * JSON payload stored inside [Expense.detailsJson] when the type is
 * [ExpenseType.Student].
 *
 * The spec requires [studentId] and [subCategory] to be preserved when
 * migrating — the Education section reconstructs the student link from
 * these fields.
 */
@Serializable
data class StudentExpenseLink(
    val studentId: String,
    val subCategory: String // lessons | personal | transport | school | books
)

/**
 * Student expense sub-categories per spec §9.2.
 */
object StudentSubCategories {
    const val LESSONS = "lessons"
    const val PERSONAL = "personal"
    const val TRANSPORT = "transport"
    const val SCHOOL = "school"
    const val BOOKS = "books"

    val ALL = listOf(LESSONS, PERSONAL, TRANSPORT, SCHOOL, BOOKS)
}
