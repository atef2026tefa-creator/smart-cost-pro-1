package com.aistudio.smartcostpro.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Backup file payload — **schema v2** (Stage 3 fix).
 *
 * The original spec limited the backup to four collections (expenses, income,
 * certificates, carIncome). Stage 3 fixes this so a phone-loss scenario
 * doesn't lose students, associations, vehicles, current account, budgets
 * or section ordering.
 *
 * The schema is backward-compatible: a v1 backup can still be parsed (the
 * new fields default to empty lists), and a v2 backup written by a v1-only
 * client will simply ignore the unknown fields when the JSON decoder is
 * configured with `ignoreUnknownKeys = true` (which it is).
 *
 * @param schemaVersion Backup schema version (currently 2).
 * @param exportedAt Epoch millis when the backup was created.
 * @param expenses List of [Expense] rows (unified expenses table).
 * @param income List of [MonthlyIncome] rows.
 * @param certificates List of [BankCertificate] rows.
 * @param carIncome List of [CarTrip] rows.
 * @param students List of [Student] rows (new in v2).
 * @param associations List of [Association] rows (new in v2).
 * @param vehicleFuel List of [VehicleFuel] rows (new in v2).
 * @param vehicleMaintenance List of [VehicleMaintenance] rows (new in v2).
 * @param vehicleOils List of [VehicleOil] rows (new in v2).
 * @param vehicleAccidents List of [VehicleAccident] rows (new in v2).
 * @param currentAccount List of [CurrentAccountTransaction] rows (new in v2).
 * @param budgets List of [Budget] rows (new in v2).
 * @param sectionOrder List of [SectionType] in the user's chosen order (new in v2).
 */
@Serializable
data class BackupData(
    val schemaVersion: Int = SCHEMA_VERSION,
    val exportedAt: Long,
    val expenses: List<Expense> = emptyList(),
    val income: List<MonthlyIncome> = emptyList(),
    val certificates: List<BankCertificate> = emptyList(),
    val carIncome: List<CarTrip> = emptyList(),
    // Stage 3 additions — default to empty so v1 backups still parse.
    val students: List<Student> = emptyList(),
    val associations: List<Association> = emptyList(),
    val vehicleFuel: List<VehicleFuel> = emptyList(),
    val vehicleMaintenance: List<VehicleMaintenance> = emptyList(),
    val vehicleOils: List<VehicleOil> = emptyList(),
    val vehicleAccidents: List<VehicleAccident> = emptyList(),
    val currentAccount: List<CurrentAccountTransaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val sectionOrder: List<SectionType> = emptyList()
) {
    companion object {
        /** Current backup schema version. Bump on breaking schema changes. */
        const val SCHEMA_VERSION = 2
    }
}

/**
 * Summary shown to the user before the restore is confirmed.
 *
 * Per the spec, restore is a full replacement (not a merge) for each
 * collection included in the file, and the user must confirm before any
 * data is touched.
 */
data class BackupImportSummary(
    val schemaVersion: Int,
    val exportedAt: Long,
    val expensesCount: Int,
    val incomeCount: Int,
    val certificatesCount: Int,
    val carIncomeCount: Int,
    val studentsCount: Int = 0,
    val associationsCount: Int = 0,
    val vehicleFuelCount: Int = 0,
    val vehicleMaintenanceCount: Int = 0,
    val vehicleOilsCount: Int = 0,
    val vehicleAccidentsCount: Int = 0,
    val currentAccountCount: Int = 0,
    val budgetsCount: Int = 0,
    val sectionOrderCount: Int = 0
)
