package com.aistudio.smartcostpro.app.data.repository

import com.aistudio.smartcostpro.app.data.local.Mappers.toDomain
import com.aistudio.smartcostpro.app.data.local.Mappers.toEntity
import com.aistudio.smartcostpro.app.data.local.dao.CertificateDao
import com.aistudio.smartcostpro.app.data.local.dao.IncomeDao
import com.aistudio.smartcostpro.app.domain.model.BankCertificate
import com.aistudio.smartcostpro.app.domain.model.MonthlyIncome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomeRepository @Inject constructor(
    private val incomeDao: IncomeDao
) {
    fun observeAllIncome(): Flow<List<MonthlyIncome>> =
        incomeDao.observeAllIncome().map { rows -> rows.map { it.toDomain() } }

    fun observeByMonth(month: String): Flow<MonthlyIncome?> =
        incomeDao.observeByMonth(month).map { it?.toDomain() }

    suspend fun getByMonth(month: String): MonthlyIncome? = incomeDao.getByMonth(month)?.toDomain()

    suspend fun upsert(income: MonthlyIncome) {
        incomeDao.upsert(income.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteByMonth(month: String) = incomeDao.deleteByMonth(month)

    suspend fun upsertAll(rows: List<MonthlyIncome>) {
        incomeDao.upsertAllIncome(rows.map { it.toEntity() })
    }

    suspend fun deleteAllIncome() = incomeDao.deleteAllIncome()
}

@Singleton
class CertificateRepository @Inject constructor(
    private val certificateDao: CertificateDao
) {
    fun observeAll(): Flow<List<BankCertificate>> =
        certificateDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun getAll(): List<BankCertificate> =
        certificateDao.getAll().map { it.toDomain() }

    suspend fun getById(id: String): BankCertificate? = certificateDao.getById(id)?.toDomain()

    suspend fun upsert(cert: BankCertificate) {
        val now = System.currentTimeMillis()
        val withId = if (cert.id.isBlank()) cert.copy(id = UUID.randomUUID().toString()) else cert
        certificateDao.upsert(withId.copy(updatedAt = now).toEntity())
    }

    suspend fun update(cert: BankCertificate) {
        certificateDao.update(cert.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteById(id: String) = certificateDao.deleteById(id)

    suspend fun upsertAll(rows: List<BankCertificate>) {
        certificateDao.upsertAll(rows.map { it.toEntity() })
    }

    suspend fun deleteAll() = certificateDao.deleteAll()
}
