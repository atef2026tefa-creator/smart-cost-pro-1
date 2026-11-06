package com.aistudio.smartcostpro.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User-set monthly budget.
 *
 * Stored under `smartcost_budget` in the reference app.
 */
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["month"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey val month: String, // YYYY-MM
    val total: Double = 0.0,
    val house: Double = 0.0,
    val medical: Double = 0.0,
    val work: Double = 0.0,
    val personal: Double = 0.0,
    val fuel: Double = 0.0,
    val maintenance: Double = 0.0,
    val oils: Double = 0.0,
    val education: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Persisted sections ordering. Stored under
 * `smartcost_expenses_sections_order` in the reference app.
 *
 * The `order` column is the user's chosen sequence; the rows are stored
 * one per section for easy drag-and-drop updates (we just bump the
 * `order` value when the user reorders).
 */
@Entity(
    tableName = "section_orders",
    indices = [Index(value = ["sectionId"], unique = true)]
)
data class SectionOrderEntity(
    @PrimaryKey val sectionId: String,
    val order: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
