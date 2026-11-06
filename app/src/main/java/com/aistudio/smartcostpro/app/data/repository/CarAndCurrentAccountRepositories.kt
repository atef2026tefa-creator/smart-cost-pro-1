package com.aistudio.smartcostpro.app.data.repository

import com.aistudio.smartcostpro.app.data.local.Mappers.toDomain
import com.aistudio.smartcostpro.app.data.local.Mappers.toEntity
import com.aistudio.smartcostpro.app.data.local.dao.CarTripDao
import com.aistudio.smartcostpro.app.data.local.dao.CurrentAccountDao
import com.aistudio.smartcostpro.app.domain.model.CarTrip
import com.aistudio.smartcostpro.app.domain.model.CurrentAccountTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarTripRepository @Inject constructor(
    private val carTripDao: CarTripDao
) {
    fun observeAll(): Flow<List<CarTrip>> =
        carTripDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observeByMonth(month: String): Flow<List<CarTrip>> =
        carTripDao.observeByMonth(month).map { rows -> rows.map { it.toDomain() } }

    fun observeByDate(date: String): Flow<List<CarTrip>> =
        carTripDao.observeByDate(date).map { rows -> rows.map { it.toDomain() } }

    suspend fun getAll(): List<CarTrip> = carTripDao.getAll().map { it.toDomain() }

    suspend fun getById(id: String): CarTrip? = carTripDao.getById(id)?.toDomain()

    suspend fun upsert(trip: CarTrip) {
        val now = System.currentTimeMillis()
        val withId = if (trip.id.isBlank()) trip.copy(id = UUID.randomUUID().toString()) else trip
        carTripDao.upsert(withId.copy(updatedAt = now).toEntity())
    }

    suspend fun deleteById(id: String) = carTripDao.deleteById(id)

    suspend fun upsertAll(rows: List<CarTrip>) {
        carTripDao.upsertAll(rows.map { it.toEntity() })
    }

    suspend fun deleteAll() = carTripDao.deleteAll()
}

@Singleton
class CurrentAccountRepository @Inject constructor(
    private val currentAccountDao: CurrentAccountDao
) {
    fun observeAll(): Flow<List<CurrentAccountTransaction>> =
        currentAccountDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observeByMonth(month: String): Flow<List<CurrentAccountTransaction>> =
        currentAccountDao.observeByMonth(month).map { rows -> rows.map { it.toDomain() } }

    suspend fun getByMonth(month: String): List<CurrentAccountTransaction> =
        currentAccountDao.getByMonth(month).map { it.toDomain() }

    suspend fun getAll(): List<CurrentAccountTransaction> =
        currentAccountDao.getAll().map { it.toDomain() }

    suspend fun upsert(transaction: CurrentAccountTransaction) {
        val now = System.currentTimeMillis()
        val withId = if (transaction.id.isBlank()) transaction.copy(id = UUID.randomUUID().toString()) else transaction
        currentAccountDao.upsert(withId.copy(updatedAt = now).toEntity())
    }

    suspend fun update(transaction: CurrentAccountTransaction) {
        currentAccountDao.update(transaction.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteById(id: String) = currentAccountDao.deleteById(id)

    suspend fun upsertAll(rows: List<CurrentAccountTransaction>) {
        currentAccountDao.upsertAll(rows.map { it.toEntity() })
    }

    suspend fun deleteAll() = currentAccountDao.deleteAll()
}
