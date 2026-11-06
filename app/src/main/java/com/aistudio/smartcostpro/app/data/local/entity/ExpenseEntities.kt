package com.aistudio.smartcostpro.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Unified expense row — the single source of truth for the dashboard's
 * expense totals and recent operations.
 *
 * The column list matches the SQLite schema documented in the spec:
 *  `id, title, amount, type, category, date, notes, createdAt, updatedAt`.
 *
 * We add `paymentMethod` and `detailsJson` columns to preserve the data
 * that the reference app keeps inside JSON blobs (medical / maintenance /
 * student link) without losing any information on the migration.
 */
@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["type"]),
        Index(value = ["date"]),
        Index(value = ["type", "date"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val type: String,           // ExpenseType.code
    val category: String,
    val date: String,           // YYYY-MM-DD
    val paymentMethod: String = "cash",
    val notes: String? = null,
    val detailsJson: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Monthly income row. The schema matches the spec:
 *  `month, salary, bonuses, otherIncome, otherIncomeNote, detailsJson, updatedAt`.
 */
@Entity(
    tableName = "monthly_income",
    indices = [Index(value = ["month"], unique = true)]
)
data class MonthlyIncomeEntity(
    @PrimaryKey val month: String, // YYYY-MM
    val salary: Double = 0.0,
    val bonuses: Double = 0.0,
    val otherIncome: Double = 0.0,
    val otherIncomeNote: String? = null,
    val detailsJson: String? = null,
    val updatedAt: Long
)

/**
 * Bank certificate row.
 *
 * Columns match the spec's `bank_certificates` table:
 *  `id, bankName, certificateNumber, duration, amount, issueDate, maturityDate,
 *   profitDate, profitAmount, profitFrequency, notes, detailsJson, createdAt, updatedAt`.
 */
@Entity(
    tableName = "bank_certificates",
    indices = [Index(value = ["bankName"])]
)
data class BankCertificateEntity(
    @PrimaryKey val id: String,
    val bankName: String,
    val certificateNumber: String? = null,
    val duration: String = "1 year",
    val amount: Double,
    val annualRate: Double = 0.0,
    val issueDate: String,
    val maturityDate: String? = null,
    val profitDate: String? = null,
    val profitAmount: Double = 0.0,
    val monthlyEquivalentProfit: Double = 0.0,
    val profitFrequency: String = "monthly", // ProfitFrequency.code
    val notes: String? = null,
    val detailsJson: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Car trip row — separate from expenses. The schema matches the spec:
 *  `id, date, amount, tripJson, createdAt`.
 *
 * `tripJson` holds the JSON-encoded [com.aistudio.smartcostpro.app.domain.model.CarTrip]
 * (with tripType, notes, updatedAt) so the table stays minimal.
 */
@Entity(
    tableName = "car_income_trips",
    indices = [Index(value = ["date"])]
)
data class CarTripEntity(
    @PrimaryKey val id: String,
    val date: String, // YYYY-MM-DD
    val amount: Double,
    val tripType: String = "cash", // TripType.code
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
