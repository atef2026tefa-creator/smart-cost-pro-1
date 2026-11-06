package com.aistudio.smartcostpro.app.data.repository

import com.aistudio.smartcostpro.app.data.local.Mappers.toDomain
import com.aistudio.smartcostpro.app.data.local.Mappers.toEntity
import com.aistudio.smartcostpro.app.data.local.dao.VehicleAccidentDao
import com.aistudio.smartcostpro.app.data.local.dao.VehicleFuelDao
import com.aistudio.smartcostpro.app.data.local.dao.VehicleMaintenanceDao
import com.aistudio.smartcostpro.app.data.local.dao.VehicleOilDao
import com.aistudio.smartcostpro.app.domain.model.VehicleAccident
import com.aistudio.smartcostpro.app.domain.model.VehicleFuel
import com.aistudio.smartcostpro.app.domain.model.VehicleMaintenance
import com.aistudio.smartcostpro.app.domain.model.VehicleOil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleFuelRepository @Inject constructor(
    private val vehicleFuelDao: VehicleFuelDao
) {
    fun observeAll(): Flow<List<VehicleFuel>> =
        vehicleFuelDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observeByMonth(month: String): Flow<List<VehicleFuel>> =
        vehicleFuelDao.observeByMonth(month).map { rows -> rows.map { it.toDomain() } }

    suspend fun getAll(): List<VehicleFuel> = vehicleFuelDao.getAll().map { it.toDomain() }

    suspend fun upsert(record: VehicleFuel) {
        val now = System.currentTimeMillis()
        val withId = if (record.id.isBlank()) record.copy(id = UUID.randomUUID().toString()) else record
        vehicleFuelDao.upsert(withId.copy(updatedAt = now).toEntity())
    }

    suspend fun update(record: VehicleFuel) {
        vehicleFuelDao.update(record.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteById(id: String) = vehicleFuelDao.deleteById(id)

    suspend fun deleteAll() = vehicleFuelDao.deleteAll()
}

@Singleton
class VehicleMaintenanceRepository @Inject constructor(
    private val vehicleMaintenanceDao: VehicleMaintenanceDao
) {
    fun observeAll(): Flow<List<VehicleMaintenance>> =
        vehicleMaintenanceDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun getAll(): List<VehicleMaintenance> =
        vehicleMaintenanceDao.getAll().map { it.toDomain() }

    suspend fun upsert(record: VehicleMaintenance) {
        val now = System.currentTimeMillis()
        val withId = if (record.id.isBlank()) record.copy(id = UUID.randomUUID().toString()) else record
        vehicleMaintenanceDao.upsert(withId.copy(updatedAt = now).toEntity())
    }

    suspend fun update(record: VehicleMaintenance) {
        vehicleMaintenanceDao.update(record.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteById(id: String) = vehicleMaintenanceDao.deleteById(id)

    suspend fun deleteAll() = vehicleMaintenanceDao.deleteAll()
}

@Singleton
class VehicleOilRepository @Inject constructor(
    private val vehicleOilDao: VehicleOilDao
) {
    fun observeAll(): Flow<List<VehicleOil>> =
        vehicleOilDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun getAll(): List<VehicleOil> = vehicleOilDao.getAll().map { it.toDomain() }

    suspend fun upsert(record: VehicleOil) {
        val now = System.currentTimeMillis()
        val withId = if (record.id.isBlank()) record.copy(id = UUID.randomUUID().toString()) else record
        vehicleOilDao.upsert(withId.copy(updatedAt = now).toEntity())
    }

    suspend fun update(record: VehicleOil) {
        vehicleOilDao.update(record.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteById(id: String) = vehicleOilDao.deleteById(id)

    suspend fun deleteAll() = vehicleOilDao.deleteAll()
}

@Singleton
class VehicleAccidentRepository @Inject constructor(
    private val vehicleAccidentDao: VehicleAccidentDao
) {
    fun observeAll(): Flow<List<VehicleAccident>> =
        vehicleAccidentDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun getAll(): List<VehicleAccident> =
        vehicleAccidentDao.getAll().map { it.toDomain() }

    suspend fun getById(id: String): VehicleAccident? = vehicleAccidentDao.getById(id)?.toDomain()

    suspend fun upsert(record: VehicleAccident) {
        val now = System.currentTimeMillis()
        val withId = if (record.id.isBlank()) record.copy(id = UUID.randomUUID().toString()) else record
        vehicleAccidentDao.upsert(withId.copy(updatedAt = now).toEntity())
    }

    suspend fun update(record: VehicleAccident) {
        vehicleAccidentDao.update(record.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteById(id: String) = vehicleAccidentDao.deleteById(id)

    suspend fun deleteAll() = vehicleAccidentDao.deleteAll()
}
