package com.aistudio.smartcostpro.app.domain.calculations

import com.aistudio.smartcostpro.app.domain.model.BackupData
import com.aistudio.smartcostpro.app.domain.model.BackupImportSummary
import com.aistudio.smartcostpro.app.util.Constants
import kotlinx.serialization.json.Json

/**
 * Backup validation & parsing.
 *
 * Stage 3 (schema v2) extends the backup to cover ALL 13 tables — a phone-loss
 * scenario no longer loses students, associations, vehicles, current account,
 * budgets or section ordering.
 *
 * Restore is a full replacement (not a merge). Schema v1 backups still parse
 * because the new fields default to empty lists.
 */
object BackupCalculator {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    /**
     * Parses the JSON string into a [BackupData] object. Returns null when
     * the JSON is invalid or the schema version is unsupported (newer than
     * what we know).
     */
    fun parse(jsonString: String): BackupData? {
        return try {
            val data = json.decodeFromString<BackupData>(jsonString)
            // Reject backups written by a future schema we don't understand.
            if (data.schemaVersion > Constants.BACKUP_SCHEMA_VERSION) null
            else data
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Produces a summary the UI shows before asking for confirmation.
     */
    fun summary(data: BackupData): BackupImportSummary = BackupImportSummary(
        schemaVersion = data.schemaVersion,
        exportedAt = data.exportedAt,
        expensesCount = data.expenses.size,
        incomeCount = data.income.size,
        certificatesCount = data.certificates.size,
        carIncomeCount = data.carIncome.size,
        studentsCount = data.students.size,
        associationsCount = data.associations.size,
        vehicleFuelCount = data.vehicleFuel.size,
        vehicleMaintenanceCount = data.vehicleMaintenance.size,
        vehicleOilsCount = data.vehicleOils.size,
        vehicleAccidentsCount = data.vehicleAccidents.size,
        currentAccountCount = data.currentAccount.size,
        budgetsCount = data.budgets.size,
        sectionOrderCount = data.sectionOrder.size
    )

    /**
     * Serializes a [BackupData] into a JSON string for export.
     */
    fun encode(data: BackupData): String = json.encodeToString(BackupData.serializer(), data)
}
