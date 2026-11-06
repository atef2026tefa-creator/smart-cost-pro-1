package com.aistudio.smartcostpro.app.domain.model

import kotlinx.serialization.Serializable

/**
 * A student profile.
 *
 * The spec requires:
 *  - National ID must be exactly 14 digits, no letters.
 *  - When a student is deleted, their expenses must be removed too
 *    (no orphan expense rows).
 *
 * @param id Stable identifier (the reference used `std_${Date.now()}`).
 * @param name Mandatory.
 * @param stage Education stage (defaults to prep).
 * @param age Mandatory, > 0.
 * @param nationalId Mandatory, exactly 14 numeric characters.
 */
@Serializable
data class Student(
    val id: String,
    val name: String,
    val stage: EducationStage = EducationStage.Prep,
    val age: Int,
    val nationalId: String, // 14 digits
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * A student expense is just an [Expense] of type [ExpenseType.Student] with
 * the [StudentExpenseLink] payload stored inside `detailsJson`. We expose a
 * typed wrapper here so the UI doesn't have to deserialize by hand.
 */
@Serializable
data class StudentExpense(
    val expense: Expense,
    val studentId: String,
    val subCategory: String
)
