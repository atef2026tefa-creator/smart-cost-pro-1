package com.aistudio.smartcostpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aistudio.smartcostpro.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    /**
     * Live stream of ALL expenses, ordered by date descending then by
     * createdAt descending. Used by the dashboard's recent operations
     * and by the unified expense list.
     */
    @Query("SELECT * FROM expenses ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    /** All expenses that fall inside the given YYYY-MM month. */
    @Query("SELECT * FROM expenses WHERE substr(date, 1, 7) = :month ORDER BY date DESC")
    fun observeByMonth(month: String): Flow<List<ExpenseEntity>>

    /** Synchronous (snapshot) read of all expenses for a given month. */
    @Query("SELECT * FROM expenses WHERE substr(date, 1, 7) = :month ORDER BY date DESC, createdAt DESC")
    suspend fun getByMonth(month: String): List<ExpenseEntity>

    /** Synchronous (snapshot) read of all expenses for a date range. */
    @Query("SELECT * FROM expenses WHERE date >= :from AND date <= :to ORDER BY date DESC")
    suspend fun getByDateRange(from: String, to: String): List<ExpenseEntity>

    /** A single expense by id. */
    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExpenseEntity?

    /** Expenses filtered by type (used by the section views). */
    @Query("SELECT * FROM expenses WHERE type = :type ORDER BY date DESC, createdAt DESC")
    fun observeByType(type: String): Flow<List<ExpenseEntity>>

    /** Insert or replace (used by the API sync). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExpenseEntity)

    /** Bulk insert / replace for restore and API sync. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ExpenseEntity>)

    /** Update an existing row. */
    @Update
    suspend fun update(entity: ExpenseEntity)

    /** Delete a single row by id. */
    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Delete all rows (used by restore — full replacement). */
    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    /** Count rows, used by the import summary dialog. */
    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun count(): Int

    /**
     * Deletes all student-typed expenses whose `detailsJson` matches one of the
     * candidate patterns. SQLite `LIKE` doesn't support `\s*` regex, so the
     * caller generates two literal patterns per student id (compact and
     * spaced) to cover both serialization styles:
     *
     *   - `%"studentId":"<id>"%` — kotlinx.serialization compact output
     *   - `%"studentId": "<id>"%` — pretty-printed output (with a space)
     */
    @Query("DELETE FROM expenses WHERE type = 'student' AND (detailsJson LIKE :patternA OR detailsJson LIKE :patternB)")
    suspend fun deleteStudentExpenses(patternA: String, patternB: String)
}
