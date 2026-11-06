package com.aistudio.smartcostpro.app.data.di

import android.content.Context
import com.aistudio.smartcostpro.app.data.local.AppDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that exposes the Room database and every DAO to the rest of the
 * app. All bindings are [Singleton] so the database is opened once for the
 * lifetime of the process.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.build(context)

    @Provides fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideIncomeDao(db: AppDatabase): IncomeDao = db.incomeDao()
    @Provides fun provideCertificateDao(db: AppDatabase): CertificateDao = db.certificateDao()
    @Provides fun provideCarTripDao(db: AppDatabase): CarTripDao = db.carTripDao()
    @Provides fun provideCurrentAccountDao(db: AppDatabase): CurrentAccountDao = db.currentAccountDao()
    @Provides fun provideStudentDao(db: AppDatabase): StudentDao = db.studentDao()
    @Provides fun provideAssociationDao(db: AppDatabase): AssociationDao = db.associationDao()
    @Provides fun provideVehicleFuelDao(db: AppDatabase): VehicleFuelDao = db.vehicleFuelDao()
    @Provides fun provideVehicleMaintenanceDao(db: AppDatabase): VehicleMaintenanceDao = db.vehicleMaintenanceDao()
    @Provides fun provideVehicleOilDao(db: AppDatabase): VehicleOilDao = db.vehicleOilDao()
    @Provides fun provideVehicleAccidentDao(db: AppDatabase): VehicleAccidentDao = db.vehicleAccidentDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideSectionOrderDao(db: AppDatabase): SectionOrderDao = db.sectionOrderDao()
}
