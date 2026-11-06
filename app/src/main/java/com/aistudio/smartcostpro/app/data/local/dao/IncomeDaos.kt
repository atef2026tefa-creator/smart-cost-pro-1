package com.aistudio.smartcostpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aistudio.smartcostpro.app.data.local.entity.BankCertificateEntity
import com.aistudio.smartcostpro.app.data.local.entity.CarTripEntity
import com.aistudio.smartcostpro.app.data.local.entity.MonthlyIncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {

    // ────────────────── Monthly income ──────────────────

    @Query("SELECT * FROM monthly_income ORDER BY month DESC")
    fun observeAllIncome(): Flow<List<MonthlyIncomeEntity>>

    @Query("SELECT * FROM monthly_income WHERE month = :month LIMIT 1")
    suspend fun getByMonth(month: String): MonthlyIncomeEntity?

    @Query("SELECT * FROM monthly_income WHERE month = :month LIMIT 1")
    fun observeByMonth(month: String): Flow<MonthlyIncomeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MonthlyIncomeEntity)

    @Update
    suspend fun update(entity: MonthlyIncomeEntity)

    @Query("DELETE FROM monthly_income WHERE month = :month")
    suspend fun deleteByMonth(month: String)

    @Query("DELETE FROM monthly_income")
    suspend fun deleteAllIncome()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllIncome(entities: List<MonthlyIncomeEntity>)
}

@Dao
interface CertificateDao {

    @Query("SELECT * FROM bank_certificates ORDER BY issueDate DESC")
    fun observeAll(): Flow<List<BankCertificateEntity>>

    @Query("SELECT * FROM bank_certificates ORDER BY issueDate DESC")
    suspend fun getAll(): List<BankCertificateEntity>

    @Query("SELECT * FROM bank_certificates WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BankCertificateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BankCertificateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<BankCertificateEntity>)

    @Update
    suspend fun update(entity: BankCertificateEntity)

    @Query("DELETE FROM bank_certificates WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM bank_certificates")
    suspend fun deleteAll()
}

@Dao
interface CarTripDao {

    @Query("SELECT * FROM car_income_trips ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<CarTripEntity>>

    @Query("SELECT * FROM car_income_trips WHERE substr(date, 1, 7) = :month ORDER BY date DESC")
    fun observeByMonth(month: String): Flow<List<CarTripEntity>>

    @Query("SELECT * FROM car_income_trips WHERE date = :date ORDER BY createdAt DESC")
    fun observeByDate(date: String): Flow<List<CarTripEntity>>

    @Query("SELECT * FROM car_income_trips")
    suspend fun getAll(): List<CarTripEntity>

    @Query("SELECT * FROM car_income_trips WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CarTripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CarTripEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CarTripEntity>)

    @Query("DELETE FROM car_income_trips WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM car_income_trips")
    suspend fun deleteAll()
}
