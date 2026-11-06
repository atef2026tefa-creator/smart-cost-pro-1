package com.aistudio.smartcostpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aistudio.smartcostpro.app.data.local.entity.AssociationEntity
import com.aistudio.smartcostpro.app.data.local.entity.CurrentAccountTransactionEntity
import com.aistudio.smartcostpro.app.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrentAccountDao {

    @Query("SELECT * FROM current_account_transactions ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<CurrentAccountTransactionEntity>>

    @Query("SELECT * FROM current_account_transactions WHERE substr(date, 1, 7) = :month ORDER BY date DESC")
    fun observeByMonth(month: String): Flow<List<CurrentAccountTransactionEntity>>

    @Query("SELECT * FROM current_account_transactions WHERE substr(date, 1, 7) = :month")
    suspend fun getByMonth(month: String): List<CurrentAccountTransactionEntity>

    @Query("SELECT * FROM current_account_transactions")
    suspend fun getAll(): List<CurrentAccountTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CurrentAccountTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CurrentAccountTransactionEntity>)

    @Update
    suspend fun update(entity: CurrentAccountTransactionEntity)

    @Query("DELETE FROM current_account_transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM current_account_transactions")
    suspend fun deleteAll()
}

@Dao
interface StudentDao {

    @Query("SELECT * FROM students ORDER BY name ASC")
    fun observeAll(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students")
    suspend fun getAll(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StudentEntity?

    @Query("SELECT * FROM students WHERE nationalId = :nationalId LIMIT 1")
    suspend fun getByNationalId(nationalId: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<StudentEntity>)

    @Update
    suspend fun update(entity: StudentEntity)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM students")
    suspend fun deleteAll()
}

@Dao
interface AssociationDao {

    @Query("SELECT * FROM associations ORDER BY startDate DESC, createdAt DESC")
    fun observeAll(): Flow<List<AssociationEntity>>

    @Query("SELECT * FROM associations")
    suspend fun getAll(): List<AssociationEntity>

    @Query("SELECT * FROM associations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AssociationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AssociationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AssociationEntity>)

    @Update
    suspend fun update(entity: AssociationEntity)

    @Query("DELETE FROM associations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM associations")
    suspend fun deleteAll()
}
