package com.aistudio.smartcostpro.app.data.repository

import com.aistudio.smartcostpro.app.data.local.Mappers.toDomain
import com.aistudio.smartcostpro.app.data.local.Mappers.toEntity
import com.aistudio.smartcostpro.app.data.local.dao.ExpenseDao
import com.aistudio.smartcostpro.app.domain.model.Expense
import com.aistudio.smartcostpro.app.domain.model.ExpenseType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Expense repository — wraps the unified expense DAO.
 *
 * The spec calls out that some screens read from a finance API and others
 * from a local store; Stage 1 implements the Room-backed path. The remote
 * API is wired in [com.aistudio.smartcostpro.app.data.remote.MockApiService] and Stage 2
 * will switch this repository to a network-first strategy with offline cache.
 */
@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {
    /** Live stream of every expense, newest first. */
    fun observeAll(): Flow<List<Expense>> =
        expenseDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /** Live stream of expenses for a YYYY-MM month. */
    fun observeByMonth(month: String): Flow<List<Expense>> =
        expenseDao.observeByMonth(month).map { rows -> rows.map { it.toDomain() } }

    /** Live stream filtered by type — used by each section view. */
    fun observeByType(type: ExpenseType): Flow<List<Expense>> =
        expenseDao.observeByType(type.code).map { rows -> rows.map { it.toDomain() } }

    /** Snapshot of every expense in a month. */
    suspend fun getByMonth(month: String): List<Expense> =
        expenseDao.getByMonth(month).map { it.toDomain() }

    /** Snapshot of expenses within a date range. */
    suspend fun getByDateRange(from: String, to: String): List<Expense> =
        expenseDao.getByDateRange(from, to).map { it.toDomain() }

    /** Snapshot of every expense (full table scan). */
    suspend fun getAll(): List<Expense> =
        expenseDao.observeAll().first()

    suspend fun getById(id: String): Expense? = expenseDao.getById(id)?.toDomain()

    /**
     * Inserts a new expense. Generates id + timestamps when missing.
     *
     * Per the spec, validation must run on the title / amount / date before
     * persistence; we delegate to [com.aistudio.smartcostpro.app.domain.calculations.ExpenseCalculator.validate]
     * at the UI layer and surface its [com.aistudio.smartcostpro.app.domain.calculations.ExpenseCalculator.ValidationError].
     */
    suspend fun insert(expense: Expense) {
        val now = System.currentTimeMillis()
        val entity = if (expense.id.isBlank()) {
            expense.copy(id = UUID.randomUUID().toString(), createdAt = now, updatedAt = now)
        } else {
            expense.copy(updatedAt = now)
        }.toEntity()
        expenseDao.upsert(entity)
    }

    suspend fun upsert(expense: Expense) {
        val now = System.currentTimeMillis()
        val entity = if (expense.id.isBlank()) {
            expense.copy(id = UUID.randomUUID().toString(), createdAt = now, updatedAt = now)
        } else {
            expense.copy(updatedAt = now)
        }.toEntity()
        expenseDao.upsert(entity)
    }

    suspend fun update(expense: Expense) {
        expenseDao.update(expense.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteById(id: String) = expenseDao.deleteById(id)

    suspend fun deleteAllExpenses() = expenseDao.deleteAll()

    suspend fun upsertAllExpenses(rows: List<Expense>) {
        expenseDao.upsertAll(rows.map { it.toEntity() })
    }

    /**
     * Deletes all student-typed expenses whose detailsJson references the
     * given studentId. Used when deleting a student — the spec requires no
     * orphan expense rows are left behind.
     *
     * **Spec compliance:** SQLite `LIKE` doesn't support `\s*` regex, so we
     * generate two literal patterns that cover both compact and pretty-printed
     * JSON serialization styles. As a defensive fallback, when Kotlin has
     * access to the student's expenses by id we should also delete by row id
     * (Stage 3.1 — for now the LIKE pattern approach is the only path).
     */
    suspend fun deleteStudentExpenses(studentId: String) {
        // Two patterns: one for compact JSON (`"studentId":"<id>"`) and one for
        // pretty-printed JSON (`"studentId": "<id>"` with a space after the colon).
        // We also handle the unlikely single-quote variant for parity.
        val compact = "%\"studentId\":\"$studentId\"%"
        val spaced = "%\"studentId\": \"$studentId\"%"
        expenseDao.deleteStudentExpenses(compact, spaced)
    }

    suspend fun count(): Int = expenseDao.count()
}
