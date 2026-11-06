package com.aistudio.smartcostpro.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Current account transaction (deposit / withdrawal).
 *
 * The spec stores these as `current_deposit` / `current_withdrawal` rows and
 * the dashboard's `currentAccountNet` is the sum of signed amounts for the
 * target month. Sign comes from the [type] column (deposit = +1, withdrawal = -1).
 */
@Entity(
    tableName = "current_account_transactions",
    indices = [Index(value = ["date"])]
)
data class CurrentAccountTransactionEntity(
    @PrimaryKey val id: String,
    val type: String,           // CurrentAccountType.code
    val description: String,
    val amount: Double,         // always > 0
    val date: String,           // YYYY-MM-DD
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Student row. The spec stores these under `smartcost_students` in the
 * reference app; we use a dedicated Room table.
 *
 * The nationalId must be exactly 14 numeric characters — enforced by the
 * validator in [com.aistudio.smartcostpro.app.domain.calculations.StudentCalculator].
 */
@Entity(
    tableName = "students",
    indices = [
        Index(value = ["nationalId"], unique = true)
    ]
)
data class StudentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val stage: String,           // EducationStage.code
    val age: Int,
    val nationalId: String,     // 14 digits
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Association row (جمعية).
 *
 * The spec stores these under `smartcost_personal_associations` in the
 * reference app; we use a dedicated Room table.
 */
@Entity(
    tableName = "associations",
    indices = [Index(value = ["startDate"]), Index(value = ["payoutDate"])]
)
data class AssociationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val value: Double,
    val memberCount: Int,
    val installment: Double,
    val roleNumber: Int,
    val payoutDate: String,
    val startDate: String,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
