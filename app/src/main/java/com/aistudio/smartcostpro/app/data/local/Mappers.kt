package com.aistudio.smartcostpro.app.data.local

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
import com.aistudio.smartcostpro.app.domain.model.Association
import com.aistudio.smartcostpro.app.domain.model.BankCertificate
import com.aistudio.smartcostpro.app.domain.model.Budget
import com.aistudio.smartcostpro.app.domain.model.CarTrip
import com.aistudio.smartcostpro.app.domain.model.CurrentAccountTransaction
import com.aistudio.smartcostpro.app.domain.model.CurrentAccountType
import com.aistudio.smartcostpro.app.domain.model.EducationStage
import com.aistudio.smartcostpro.app.domain.model.Expense
import com.aistudio.smartcostpro.app.domain.model.ExpenseType
import com.aistudio.smartcostpro.app.domain.model.MonthlyIncome
import com.aistudio.smartcostpro.app.domain.model.PaymentMethod
import com.aistudio.smartcostpro.app.domain.model.ProfitFrequency
import com.aistudio.smartcostpro.app.domain.model.SectionOrder
import com.aistudio.smartcostpro.app.domain.model.SectionType
import com.aistudio.smartcostpro.app.domain.model.Student
import com.aistudio.smartcostpro.app.domain.model.TripType
import com.aistudio.smartcostpro.app.domain.model.VehicleAccident
import com.aistudio.smartcostpro.app.domain.model.VehicleFuel
import com.aistudio.smartcostpro.app.domain.model.VehicleMaintenance
import com.aistudio.smartcostpro.app.domain.model.VehicleOil

/**
 * Bidirectional mappers between Room entities and domain models.
 *
 * Keeping these in one place makes it easy to verify that no domain field
 * is lost when persisting — which is the spec's #1 rule.
 */
object Mappers {

    // ─────────────────────────── Expenses ───────────────────────────

    fun ExpenseEntity.toDomain(): Expense = Expense(
        id = id,
        title = title,
        amount = amount,
        type = ExpenseType.fromCode(type) ?: ExpenseType.House,
        category = category,
        date = date,
        paymentMethod = PaymentMethod.fromCode(paymentMethod) ?: PaymentMethod.Cash,
        notes = notes,
        detailsJson = detailsJson,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
        id = id,
        title = title,
        amount = amount,
        type = type.code,
        category = category,
        date = date,
        paymentMethod = paymentMethod.code,
        notes = notes,
        detailsJson = detailsJson,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    // ─────────────────────────── Income ───────────────────────────

    fun MonthlyIncomeEntity.toDomain(): MonthlyIncome = MonthlyIncome(
        month = month,
        salary = salary,
        bonuses = bonuses,
        otherIncome = otherIncome,
        otherIncomeNote = otherIncomeNote,
        detailsJson = detailsJson,
        updatedAt = updatedAt
    )

    fun MonthlyIncome.toEntity(): MonthlyIncomeEntity = MonthlyIncomeEntity(
        month = month,
        salary = salary,
        bonuses = bonuses,
        otherIncome = otherIncome,
        otherIncomeNote = otherIncomeNote,
        detailsJson = detailsJson,
        updatedAt = updatedAt
    )

    // ─────────────────────────── Certificates ───────────────────────────

    fun BankCertificateEntity.toDomain(): BankCertificate = BankCertificate(
        id = id,
        bankName = bankName,
        certificateNumber = certificateNumber,
        amount = amount,
        annualRate = annualRate,
        duration = duration,
        profitFrequency = ProfitFrequency.fromCode(profitFrequency) ?: ProfitFrequency.Monthly,
        issueDate = issueDate,
        maturityDate = maturityDate,
        profitDate = profitDate,
        profitAmount = profitAmount,
        monthlyEquivalentProfit = monthlyEquivalentProfit,
        notes = notes,
        detailsJson = detailsJson,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun BankCertificate.toEntity(): BankCertificateEntity = BankCertificateEntity(
        id = id,
        bankName = bankName,
        certificateNumber = certificateNumber,
        duration = duration,
        amount = amount,
        annualRate = annualRate,
        issueDate = issueDate,
        maturityDate = maturityDate,
        profitDate = profitDate,
        profitAmount = profitAmount,
        monthlyEquivalentProfit = monthlyEquivalentProfit,
        profitFrequency = profitFrequency.code,
        notes = notes,
        detailsJson = detailsJson,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    // ─────────────────────────── Car trips ───────────────────────────

    fun CarTripEntity.toDomain(): CarTrip = CarTrip(
        id = id,
        date = date,
        amount = amount,
        tripType = TripType.fromCode(tripType) ?: TripType.Cash,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun CarTrip.toEntity(): CarTripEntity = CarTripEntity(
        id = id,
        date = date,
        amount = amount,
        tripType = tripType.code,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    // ─────────────────────────── Current account ───────────────────────────

    fun CurrentAccountTransactionEntity.toDomain(): CurrentAccountTransaction =
        CurrentAccountTransaction(
            id = id,
            type = CurrentAccountType.fromCode(type) ?: CurrentAccountType.Deposit,
            description = description,
            amount = amount,
            date = date,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    fun CurrentAccountTransaction.toEntity(): CurrentAccountTransactionEntity =
        CurrentAccountTransactionEntity(
            id = id,
            type = type.code,
            description = description,
            amount = amount,
            date = date,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    // ─────────────────────────── Students ───────────────────────────

    fun StudentEntity.toDomain(): Student = Student(
        id = id,
        name = name,
        stage = EducationStage.fromCode(stage) ?: EducationStage.Prep,
        age = age,
        nationalId = nationalId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun Student.toEntity(): StudentEntity = StudentEntity(
        id = id,
        name = name,
        stage = stage.code,
        age = age,
        nationalId = nationalId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    // ─────────────────────────── Associations ───────────────────────────

    fun AssociationEntity.toDomain(): Association = Association(
        id = id,
        name = name,
        value = value,
        memberCount = memberCount,
        installment = installment,
        roleNumber = roleNumber,
        payoutDate = payoutDate,
        startDate = startDate,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun Association.toEntity(): AssociationEntity = AssociationEntity(
        id = id,
        name = name,
        value = value,
        memberCount = memberCount,
        installment = installment,
        roleNumber = roleNumber,
        payoutDate = payoutDate,
        startDate = startDate,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    // ─────────────────────────── Vehicles ───────────────────────────

    fun VehicleFuelEntity.toDomain(): VehicleFuel = VehicleFuel(
        id = id,
        fuelType = fuelType,
        quantity = quantity,
        unitPrice = unitPrice,
        odometer = odometer,
        date = date,
        time = time,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun VehicleFuel.toEntity(): VehicleFuelEntity = VehicleFuelEntity(
        id = id,
        fuelType = fuelType,
        quantity = quantity,
        unitPrice = unitPrice,
        odometer = odometer,
        date = date,
        time = time,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun VehicleMaintenanceEntity.toDomain(): VehicleMaintenance = VehicleMaintenance(
        id = id,
        maintenanceType = maintenanceType,
        supplyName = supplyName,
        supplyPrice = supplyPrice,
        laborDescription = laborDescription,
        laborPrice = laborPrice,
        date = date,
        total = total,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun VehicleMaintenance.toEntity(): VehicleMaintenanceEntity = VehicleMaintenanceEntity(
        id = id,
        maintenanceType = maintenanceType,
        supplyName = supplyName,
        supplyPrice = supplyPrice,
        laborDescription = laborDescription,
        laborPrice = laborPrice,
        date = date,
        total = total,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun VehicleOilEntity.toDomain(): VehicleOil = VehicleOil(
        id = id,
        serviceType = serviceType,
        brand = brand,
        productName = productName,
        quantity = quantity,
        price = price,
        odometer = odometer,
        suggestedIntervalKm = suggestedIntervalKm,
        notes = notes,
        date = date,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun VehicleOil.toEntity(): VehicleOilEntity = VehicleOilEntity(
        id = id,
        serviceType = serviceType,
        brand = brand,
        productName = productName,
        quantity = quantity,
        price = price,
        odometer = odometer,
        suggestedIntervalKm = suggestedIntervalKm,
        notes = notes,
        date = date,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun VehicleAccidentEntity.toDomain(): VehicleAccident = VehicleAccident(
        id = id,
        vehicleId = vehicleId,
        title = title,
        photoUrl = photoUrl,
        date = date,
        time = time,
        location = location,
        estimatedDamage = estimatedDamage,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun VehicleAccident.toEntity(): VehicleAccidentEntity = VehicleAccidentEntity(
        id = id,
        vehicleId = vehicleId,
        title = title,
        photoUrl = photoUrl,
        date = date,
        time = time,
        location = location,
        estimatedDamage = estimatedDamage,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    // ─────────────────────────── Budget ───────────────────────────

    fun BudgetEntity.toDomain(): Budget = Budget(
        month = month,
        total = total,
        house = house,
        medical = medical,
        work = work,
        personal = personal,
        fuel = fuel,
        maintenance = maintenance,
        oils = oils,
        education = education,
        updatedAt = updatedAt
    )

    fun Budget.toEntity(): BudgetEntity = BudgetEntity(
        month = month,
        total = total,
        house = house,
        medical = medical,
        work = work,
        personal = personal,
        fuel = fuel,
        maintenance = maintenance,
        oils = oils,
        education = education,
        updatedAt = updatedAt
    )

    // ─────────────────────────── Section order ───────────────────────────

    fun List<SectionOrderEntity>.toSectionOrder(): SectionOrder {
        // Section IDs sorted ascending by their `order` value.
        val sections = sortedBy { it.order }
            .mapNotNull { SectionType.fromId(it.sectionId) }
            .ifEmpty { SectionType.DEFAULT_ORDER }
        return SectionOrder(sections)
    }

    fun List<SectionType>.toEntities(): List<SectionOrderEntity> =
        mapIndexed { index, section ->
            SectionOrderEntity(sectionId = section.id, order = index)
        }
}
