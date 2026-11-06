package com.aistudio.smartcostpro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aistudio.smartcostpro.app.data.local.entity.BudgetEntity
import com.aistudio.smartcostpro.app.data.local.entity.SectionOrderEntity
import com.aistudio.smartcostpro.app.data.local.entity.VehicleAccidentEntity
import com.aistudio.smartcostpro.app.data.local.entity.VehicleFuelEntity
import com.aistudio.smartcostpro.app.data.local.entity.VehicleMaintenanceEntity
import com.aistudio.smartcostpro.app.data.local.entity.VehicleOilEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleFuelDao {

    @Query("SELECT * FROM vehicle_fuel ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<VehicleFuelEntity>>

    @Query("SELECT * FROM vehicle_fuel WHERE substr(date, 1, 7) = :month ORDER BY date DESC")
    fun observeByMonth(month: String): Flow<List<VehicleFuelEntity>>

    @Query("SELECT * FROM vehicle_fuel")
    suspend fun getAll(): List<VehicleFuelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VehicleFuelEntity)

    @Update
    suspend fun update(entity: VehicleFuelEntity)

    @Query("DELETE FROM vehicle_fuel WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vehicle_fuel")
    suspend fun deleteAll()
}

@Dao
interface VehicleMaintenanceDao {

    @Query("SELECT * FROM vehicle_maintenance ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<VehicleMaintenanceEntity>>

    @Query("SELECT * FROM vehicle_maintenance")
    suspend fun getAll(): List<VehicleMaintenanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VehicleMaintenanceEntity)

    @Update
    suspend fun update(entity: VehicleMaintenanceEntity)

    @Query("DELETE FROM vehicle_maintenance WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vehicle_maintenance")
    suspend fun deleteAll()
}

@Dao
interface VehicleOilDao {

    @Query("SELECT * FROM vehicle_oils ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<VehicleOilEntity>>

    @Query("SELECT * FROM vehicle_oils")
    suspend fun getAll(): List<VehicleOilEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VehicleOilEntity)

    @Update
    suspend fun update(entity: VehicleOilEntity)

    @Query("DELETE FROM vehicle_oils WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vehicle_oils")
    suspend fun deleteAll()
}

@Dao
interface VehicleAccidentDao {

    @Query("SELECT * FROM vehicle_accidents ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<VehicleAccidentEntity>>

    @Query("SELECT * FROM vehicle_accidents")
    suspend fun getAll(): List<VehicleAccidentEntity>

    @Query("SELECT * FROM vehicle_accidents WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): VehicleAccidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VehicleAccidentEntity)

    @Update
    suspend fun update(entity: VehicleAccidentEntity)

    @Query("DELETE FROM vehicle_accidents WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vehicle_accidents")
    suspend fun deleteAll()
}

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets ORDER BY month DESC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE month = :month LIMIT 1")
    fun observeByMonth(month: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE month = :month LIMIT 1")
    suspend fun getByMonth(month: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BudgetEntity)

    @Query("DELETE FROM budgets WHERE month = :month")
    suspend fun deleteByMonth(month: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}

@Dao
interface SectionOrderDao {

    @Query("SELECT * FROM section_orders ORDER BY `order` ASC")
    fun observeAll(): Flow<List<SectionOrderEntity>>

    @Query("SELECT * FROM section_orders ORDER BY `order` ASC")
    suspend fun getAll(): List<SectionOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SectionOrderEntity>)

    @Query("DELETE FROM section_orders")
    suspend fun deleteAll()
}
