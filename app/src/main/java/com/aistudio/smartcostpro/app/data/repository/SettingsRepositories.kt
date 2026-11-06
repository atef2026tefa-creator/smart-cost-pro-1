package com.aistudio.smartcostpro.app.data.repository

import androidx.room.withTransaction
import com.aistudio.smartcostpro.app.data.local.AppDatabase
import com.aistudio.smartcostpro.app.data.local.Mappers.toDomain
import com.aistudio.smartcostpro.app.data.local.Mappers.toEntities
import com.aistudio.smartcostpro.app.data.local.Mappers.toEntity
import com.aistudio.smartcostpro.app.data.local.dao.BudgetDao
import com.aistudio.smartcostpro.app.data.local.dao.SectionOrderDao
import com.aistudio.smartcostpro.app.domain.calculations.BackupCalculator
import com.aistudio.smartcostpro.app.domain.model.BackupData
import com.aistudio.smartcostpro.app.domain.model.BackupImportSummary
import com.aistudio.smartcostpro.app.domain.model.Budget
import com.aistudio.smartcostpro.app.domain.model.SectionOrder
import com.aistudio.smartcostpro.app.domain.model.SectionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {
    fun observeAll(): Flow<List<Budget>> =
        budgetDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observeByMonth(month: String): Flow<Budget?> =
        budgetDao.observeByMonth(month).map { it?.toDomain() }

    suspend fun getByMonth(month: String): Budget? = budgetDao.getByMonth(month)?.toDomain()

    suspend fun upsert(budget: Budget) {
        budgetDao.upsert(budget.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteByMonth(month: String) = budgetDao.deleteByMonth(month)

    suspend fun deleteAll() = budgetDao.deleteAll()

    suspend fun upsertAll(rows: List<Budget>) {
        // Use the DAO directly via reflection — for simplicity, we deleteAll
        // then re-insert one by one. Stage 3.1 can add a bulk DAO method.
        rows.forEach { upsert(it) }
    }
}

@Singleton
class SectionOrderRepository @Inject constructor(
    private val sectionOrderDao: SectionOrderDao
) {
    fun observeAll(): Flow<List<SectionType>> =
        sectionOrderDao.observeAll().map { rows -> rows.toSectionOrder().sections }

    suspend fun getCurrent(): SectionOrder {
        val rows = sectionOrderDao.getAll()
        return rows.toSectionOrder()
    }

    suspend fun getAll(): List<SectionType> = getCurrent().sections

    /**
     * Replaces the entire ordering with [order] (full replacement, not a merge).
     *
     * The spec allows the user to drag-and-drop sections; we persist the new
     * order wholesale so reordering is atomic.
     */
    suspend fun setOrder(order: List<SectionType>) {
        sectionOrderDao.deleteAll()
        sectionOrderDao.upsertAll(order.toEntities())
    }

    /** Convenience: resets to the spec's default order. */
    suspend fun resetToDefault() = setOrder(SectionType.DEFAULT_ORDER)
}

/**
 * Backup & restore repository — Stage 3 schema v2.
 *
 * **Stage 3 changes (per code review):**
 *  - Backup now covers ALL 13 tables (not just 4) so a phone-loss scenario
 *    doesn't lose students, associations, vehicles, current account, budgets
 *    or section ordering.
 *  - Restore is wrapped in `appDatabase.withTransaction { }` so the database
 *    is never left half-written. If any insert throws, the entire transaction
 *    rolls back and the previous data is preserved.
 *  - Schema v1 backups still parse — the new fields default to empty lists.
 *
 * **Spec compliance:** restore is a full replacement, not a merge (§19.2).
 */
@Singleton
class BackupRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val certificateRepository: CertificateRepository,
    private val carTripRepository: CarTripRepository,
    private val studentRepository: StudentRepository,
    private val associationRepository: AssociationRepository,
    private val vehicleFuelRepository: VehicleFuelRepository,
    private val vehicleMaintenanceRepository: VehicleMaintenanceRepository,
    private val vehicleOilRepository: VehicleOilRepository,
    private val vehicleAccidentRepository: VehicleAccidentRepository,
    private val currentAccountRepository: CurrentAccountRepository,
    private val budgetRepository: BudgetRepository,
    private val sectionOrderRepository: SectionOrderRepository
) {
    /**
     * Builds a [BackupData] snapshot of all 13 collections.
     */
    suspend fun export(): BackupData {
        val allExpenses = expenseRepository.getByDateRange("1970-01-01", "2999-12-31")
        val allIncome = incomeRepository.observeAllIncome().first()
        val allCertificates = certificateRepository.getAll()
        val allCarTrips = carTripRepository.getAll()
        val allStudents = studentRepository.getAll()
        val allAssociations = associationRepository.getAll()
        val allFuel = vehicleFuelRepository.getAll()
        val allMaint = vehicleMaintenanceRepository.getAll()
        val allOils = vehicleOilRepository.getAll()
        val allAccidents = vehicleAccidentRepository.getAll()
        val allCurrentAccount = currentAccountRepository.getAll()
        val allBudgets = budgetRepository.observeAll().first()
        val sectionOrder = sectionOrderRepository.getAll()
        return BackupData(
            schemaVersion = com.aistudio.smartcostpro.app.util.Constants.BACKUP_SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            expenses = allExpenses,
            income = allIncome,
            certificates = allCertificates,
            carIncome = allCarTrips,
            students = allStudents,
            associations = allAssociations,
            vehicleFuel = allFuel,
            vehicleMaintenance = allMaint,
            vehicleOils = allOils,
            vehicleAccidents = allAccidents,
            currentAccount = allCurrentAccount,
            budgets = allBudgets,
            sectionOrder = sectionOrder
        )
    }

    /** Serializes the [BackupData] to JSON for sharing / saving to a file. */
    fun encode(data: BackupData): String = BackupCalculator.encode(data)

    /** Parses a JSON string into a [BackupData], returns null on failure. */
    fun parse(jsonString: String): BackupData? = BackupCalculator.parse(jsonString)

    /** Produces a [BackupImportSummary] for the UI to display before confirming. */
    fun summary(data: BackupData): BackupImportSummary = BackupCalculator.summary(data)

    /**
     * Replaces all 13 collections with the contents of [data].
     *
     * Runs inside a Room `withTransaction { }` so the database is never left
     * half-written. Each collection is wiped before inserting the new rows;
     * if any insert throws, the entire transaction rolls back and the
     * previous data is preserved.
     *
     * **Spec compliance:** restore is a full replacement, not a merge (§19.2).
     */
    suspend fun restore(data: BackupData) = appDatabase.withTransaction {
        // ─── Wipe ───
        expenseRepository.deleteAllExpenses()
        incomeRepository.deleteAllIncome()
        certificateRepository.deleteAll()
        carTripRepository.deleteAll()
        studentRepository.deleteAll()
        associationRepository.deleteAll()
        vehicleFuelRepository.deleteAll()
        vehicleMaintenanceRepository.deleteAll()
        vehicleOilRepository.deleteAll()
        vehicleAccidentRepository.deleteAll()
        currentAccountRepository.deleteAll()
        budgetRepository.deleteAll()
        sectionOrderRepository.setOrder(emptyList())

        // ─── Rewrite (only when the backup actually has rows) ───
        if (data.expenses.isNotEmpty()) expenseRepository.upsertAllExpenses(data.expenses)
        if (data.income.isNotEmpty()) incomeRepository.upsertAll(data.income)
        if (data.certificates.isNotEmpty()) certificateRepository.upsertAll(data.certificates)
        if (data.carIncome.isNotEmpty()) carTripRepository.upsertAll(data.carIncome)
        if (data.students.isNotEmpty()) studentRepository.upsertAll(data.students)
        if (data.associations.isNotEmpty()) associationRepository.upsertAll(data.associations)
        if (data.vehicleFuel.isNotEmpty()) data.vehicleFuel.forEach { vehicleFuelRepository.upsert(it) }
        if (data.vehicleMaintenance.isNotEmpty()) data.vehicleMaintenance.forEach { vehicleMaintenanceRepository.upsert(it) }
        if (data.vehicleOils.isNotEmpty()) data.vehicleOils.forEach { vehicleOilRepository.upsert(it) }
        if (data.vehicleAccidents.isNotEmpty()) data.vehicleAccidents.forEach { vehicleAccidentRepository.upsert(it) }
        if (data.currentAccount.isNotEmpty()) currentAccountRepository.upsertAll(data.currentAccount)
        if (data.budgets.isNotEmpty()) budgetRepository.upsertAll(data.budgets)
        if (data.sectionOrder.isNotEmpty()) sectionOrderRepository.setOrder(data.sectionOrder)
    }
}
