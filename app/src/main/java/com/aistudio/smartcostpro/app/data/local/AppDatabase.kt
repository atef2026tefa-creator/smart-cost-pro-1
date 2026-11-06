package com.aistudio.smartcostpro.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.aistudio.smartcostpro.app.data.local.dao.AssociationDao
import com.aistudio.smartcostpro.app.data.local.dao.BudgetDao
import com.aistudio.smartcostpro.app.data.local.dao.CarTripDao
import com.aistudio.smartcostpro.app.data.local.dao.CertificateDao
import com.aistudio.smartcostpro.app.data.local.dao.CurrentAccountDao
import com.aistudio.smartcostpro.app.data.local.dao.ExpenseDao
import com.aistudio.smartcostpro.app.data.local.dao.IncomeDao
import com.aistudio.smartcostpro.app.data.local.dao.SectionOrderDao
import com.aistudio.smartcostpro.app.data.local.dao.StudentDao
import com.aistudio.smartcostpro.app.data.local.dao.VehicleAccidentDao
import com.aistudio.smartcostpro.app.data.local.dao.VehicleFuelDao
import com.aistudio.smartcostpro.app.data.local.dao.VehicleMaintenanceDao
import com.aistudio.smartcostpro.app.data.local.dao.VehicleOilDao
import com.aistudio.smartcostpro.app.data.local.entity.AssociationEntity
import com.aistudio.smartcostpro.app.data.local.entity.BankCertificateEntity
import com.aistudio.smartcostpro.app.data.local.entity.BudgetEntity
import com.aistudio.smartcostpro.app.data.local.entity.CarTripEntity
import com.aistudio.smartcostpro.app.data.local.entity.CurrentAccountTransactionEntity
import com.aistudio.smartcostpro.app.data.local.entity.ExpenseEntity
import com.aistudio.smartcostpro.app.data.local.entity.MonthlyIncomeEntity
import com.aistudio.smartcostpro.app.data.local.entity.SectionOrderEntity
import com.aistudio.smartcostpro.app.data.local.entity.StudentEntity
import com.aistudio.smartcostpro.app.data.local.entity.VehicleAccidentEntity
import com.aistudio.smartcostpro.app.data.local.entity.VehicleFuelEntity
import com.aistudio.smartcostpro.app.data.local.entity.VehicleMaintenanceEntity
import com.aistudio.smartcostpro.app.data.local.entity.VehicleOilEntity
import com.aistudio.smartcostpro.app.util.Constants

/**
 * The Room database backing Cost App Pro.
 *
 * Stage 1 ships schema v1 with 13 tables. Stage 2 will add migration paths
 * (e.g. add notifications table) and bump the version.
 *
 * The schemas are exported to `app/schemas/` (configured via the Room
 * `schemaDirectory` block in `app/build.gradle.kts`) so future migrations
 * have a baseline to diff against.
 *
 * Each entity corresponds to a separate concern — the spec is explicit
 * about not collapsing any of them into the unified expenses table.
 */
@Database(
    entities = [
        ExpenseEntity::class,                    // unified expenses
        MonthlyIncomeEntity::class,              // monthly income
        BankCertificateEntity::class,            // bank certificates
        CarTripEntity::class,                    // car income trips
        CurrentAccountTransactionEntity::class,  // current account
        StudentEntity::class,                    // students
        AssociationEntity::class,                // associations
        VehicleFuelEntity::class,                // vehicle fuel
        VehicleMaintenanceEntity::class,         // vehicle maintenance
        VehicleOilEntity::class,                 // vehicle oils / filters
        VehicleAccidentEntity::class,            // vehicle accidents
        BudgetEntity::class,                     // monthly budget
        SectionOrderEntity::class                // section ordering
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun certificateDao(): CertificateDao
    abstract fun carTripDao(): CarTripDao
    abstract fun currentAccountDao(): CurrentAccountDao
    abstract fun studentDao(): StudentDao
    abstract fun associationDao(): AssociationDao
    abstract fun vehicleFuelDao(): VehicleFuelDao
    abstract fun vehicleMaintenanceDao(): VehicleMaintenanceDao
    abstract fun vehicleOilDao(): VehicleOilDao
    abstract fun vehicleAccidentDao(): VehicleAccidentDao
    abstract fun budgetDao(): BudgetDao
    abstract fun sectionOrderDao(): SectionOrderDao

    companion object {
        fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context = context.applicationContext,
            klass = AppDatabase::class.java,
            name = Constants.DATABASE_NAME
        )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }
}
