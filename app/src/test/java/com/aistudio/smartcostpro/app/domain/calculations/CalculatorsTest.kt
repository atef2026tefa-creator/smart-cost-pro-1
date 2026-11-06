package com.aistudio.smartcostpro.app.domain.calculations

import com.aistudio.smartcostpro.app.domain.model.Association
import com.aistudio.smartcostpro.app.domain.model.BankCertificate
import com.aistudio.smartcostpro.app.domain.model.Expense
import com.aistudio.smartcostpro.app.domain.model.ExpenseType
import com.aistudio.smartcostpro.app.domain.model.PaymentMethod
import com.aistudio.smartcostpro.app.domain.model.ProfitFrequency
import com.aistudio.smartcostpro.app.util.DateFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the spec's most critical financial calculators.
 *
 * These tests run as part of the `testDebugUnitTest` task — they don't need
 * Robolectric or an emulator because the calculators are pure functions
 * over domain models.
 *
 * Coverage (per spec §22 acceptance scenarios):
 *  - Expense: filter by month, sum by type, totals
 *  - Association: installment formula, active-in-month, payout detection
 *  - Certificate: profit priority chain, duration parser, date window
 *  - Report: range cap < 370 days
 */
class CalculatorsTest {

    // ─────────────────────────── Expenses ───────────────────────────

    @Test
    fun `expense filter by month respects ISO month`() {
        val expenses = listOf(
            expense("e1", "Lunch", 50.0, ExpenseType.House, "2025-03-14"),
            expense("e2", "Dinner", 80.0, ExpenseType.House, "2025-03-15"),
            expense("e3", "Lunch", 60.0, ExpenseType.House, "2025-04-01")
        )
        val march = ExpenseCalculator.filterByMonth(expenses, "2025-03")
        assertEquals(2, march.size)
        assertEquals(50.0 + 80.0, march.sumOf { it.amount }, 0.001)
    }

    @Test
    fun `expense with blank date does not belong to any month`() {
        val expenses = listOf(
            expense("e1", "Lunch", 50.0, ExpenseType.House, ""),
            expense("e2", "Lunch", 50.0, ExpenseType.House, "2025-03-14")
        )
        val march = ExpenseCalculator.filterByMonth(expenses, "2025-03")
        assertEquals(1, march.size)
    }

    @Test
    fun `expense totals sum each type independently`() {
        val expenses = listOf(
            expense("e1", "Lunch", 50.0, ExpenseType.House, "2025-03-14"),
            expense("e2", "Doctor", 200.0, ExpenseType.Medical, "2025-03-14"),
            expense("e3", "Gas", 100.0, ExpenseType.VehicleFuel, "2025-03-15")
        )
        val totals = ExpenseCalculator.totalsForMonth(expenses, "2025-03")
        assertEquals(50.0, totals.houseTotal, 0.001)
        assertEquals(200.0, totals.medicalTotal, 0.001)
        assertEquals(100.0, totals.fuelTotal, 0.001)
        assertEquals(0.0, totals.workTotal, 0.001)
        // composite totals
        assertEquals(250.0, totals.homeAndMedicalTotal, 0.001)
        assertEquals(100.0, totals.vehicleTotal, 0.001)
        // grand total = sum of all 8
        assertEquals(350.0, totals.grandTotal, 0.001)
    }

    // ─────────────────────────── Associations ───────────────────────────

    @Test
    fun `association installment equals value divided by member count`() {
        // Spec example: 12,000 / 12 = 1,000
        val installment = AssociationCalculator.computeInstallment(12_000.0, 12)
        assertEquals(1_000.0, installment, 0.001)
    }

    @Test
    fun `association is active only when monthIndex is in 0 until memberCount`() {
        val a = association("a1", 12_000.0, 12, 1, "2025-01-01", "2025-06-01")
        // 2025-01 → monthIndex 0 → active
        assertTrue(AssociationCalculator.isActiveInMonth(a, "2025-01"))
        // 2025-12 → monthIndex 11 → still within 12-member cycle → active
        assertTrue(AssociationCalculator.isActiveInMonth(a, "2025-12"))
        // 2026-01 → monthIndex 12 → past cycle → NOT active
        assertEquals(false, AssociationCalculator.isActiveInMonth(a, "2026-01"))
        // 2024-12 → monthIndex -1 → before start → NOT active
        assertEquals(false, AssociationCalculator.isActiveInMonth(a, "2024-12"))
    }

    @Test
    fun `association payout is added to earned when month matches payoutDate`() {
        val a = association("a1", 12_000.0, 12, 1, "2025-01-01", "2025-06-01")
        val fin = AssociationCalculator.financialsForMonth(listOf(a), "2025-06")
        assertEquals(12_000.0, fin.earned, 0.001)
    }

    @Test
    fun `association validation rejects role greater than member count`() {
        val err = AssociationCalculator.validate(
            name = "Test",
            value = 1000.0,
            memberCount = 5,
            roleNumber = 6,
            startDate = "2025-01-01",
            payoutDate = "2025-06-01"
        )
        assertEquals(AssociationCalculator.ValidationError.RoleNumberInvalid, err)
    }

    // ─────────────────────────── Certificates ───────────────────────────

    @Test
    fun `certificate annual profit equals amount times rate over 100`() {
        // Spec example: 100,000 × 18% = 18,000
        val cert = certificate(amount = 100_000.0, annualRate = 18.0)
        assertEquals(18_000.0, CertificateCalculator.annualProfit(cert), 0.001)
    }

    @Test
    fun `certificate monthly equivalent uses monthlyEquivalentProfit when positive`() {
        // Priority 1: monthlyEquivalentProfit wins.
        val cert = certificate(
            amount = 100_000.0,
            annualRate = 18.0,
            monthlyEquivalentProfit = 1234.0
        )
        assertEquals(1234.0, CertificateCalculator.computeMonthlyProfit(cert), 0.001)
    }

    @Test
    fun `certificate monthly profit falls back to amount times rate when no explicit equivalent`() {
        // Priority 2: derive from amount × annualRate / 12
        val cert = certificate(amount = 100_000.0, annualRate = 18.0)
        assertEquals(1_500.0, CertificateCalculator.computeMonthlyProfit(cert), 0.001)
    }

    @Test
    fun `certificate on-maturity monthly equivalent divides total profit by months`() {
        // 100,000 × 18% × 1 year = 18,000 over 12 months = 1,500/month
        val cert = certificate(
            amount = 100_000.0,
            annualRate = 18.0,
            duration = "1 year",
            profitFrequency = ProfitFrequency.OnMaturity
        )
        assertEquals(1_500.0, CertificateCalculator.computeMonthlyProfit(cert), 0.001)
    }

    @Test
    fun `certificate duration parser converts months to years`() {
        assertEquals(0.5, CertificateCalculator.parseDurationYears("6 months"), 0.001)
        assertEquals(1.0, CertificateCalculator.parseDurationYears("1 year"), 0.001)
        assertEquals(2.0, CertificateCalculator.parseDurationYears("2 years"), 0.001)
        // Fallback for unparseable strings
        assertEquals(1.0, CertificateCalculator.parseDurationYears("xyz"), 0.001)
        assertEquals(1.0, CertificateCalculator.parseDurationYears(null), 0.001)
    }

    @Test
    fun `certificate date window excludes months outside issue and maturity`() {
        val cert = certificate(
            amount = 100_000.0,
            annualRate = 18.0,
            issueDate = "2025-01-01",
            maturityDate = "2025-12-01"
        )
        assertTrue(CertificateCalculator.isCertActiveInMonth(cert, "2025-06"))
        assertEquals(false, CertificateCalculator.isCertActiveInMonth(cert, "2024-12"))
        assertEquals(false, CertificateCalculator.isCertActiveInMonth(cert, "2026-01"))
    }

    // ─────────────────────────── Reports ───────────────────────────

    @Test
    fun `range report returns empty when range exceeds 370 days`() {
        val expenses = listOf(
            expense("e1", "Test", 100.0, ExpenseType.House, "2024-01-01")
        )
        // 400 days range → should return empty
        val rows = ReportCalculator.rangeReport(expenses, "2024-01-01", "2025-02-04")
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `range report includes expenses within the bounds`() {
        val expenses = listOf(
            expense("e1", "A", 100.0, ExpenseType.House, "2025-03-01"),
            expense("e2", "B", 200.0, ExpenseType.House, "2025-03-15"),
            expense("e3", "C", 300.0, ExpenseType.House, "2025-04-01") // outside
        )
        val rows = ReportCalculator.rangeReport(expenses, "2025-03-01", "2025-03-31")
        assertEquals(2, rows.size)
    }

    // ─────────────────────────── Helpers ───────────────────────────

    private fun expense(
        id: String, title: String, amount: Double, type: ExpenseType, date: String
    ): Expense = Expense(
        id = id, title = title, amount = amount, type = type,
        category = "test", date = date, paymentMethod = PaymentMethod.Cash,
        notes = null, detailsJson = null, createdAt = 0L, updatedAt = 0L
    )

    private fun association(
        id: String, value: Double, members: Int, role: Int,
        startDate: String, payoutDate: String
    ): Association = Association(
        id = id, name = "Test", value = value, memberCount = members,
        installment = value / members, roleNumber = role,
        payoutDate = payoutDate, startDate = startDate,
        notes = null, createdAt = 0L, updatedAt = 0L
    )

    private fun certificate(
        amount: Double,
        annualRate: Double,
        duration: String = "1 year",
        profitFrequency: ProfitFrequency = ProfitFrequency.Monthly,
        issueDate: String = "2025-01-01",
        maturityDate: String? = null,
        monthlyEquivalentProfit: Double = 0.0
    ): BankCertificate = BankCertificate(
        id = "c1", bankName = "Test Bank", certificateNumber = null,
        amount = amount, annualRate = annualRate, duration = duration,
        profitFrequency = profitFrequency, issueDate = issueDate,
        maturityDate = maturityDate, profitDate = null,
        profitAmount = 0.0, monthlyEquivalentProfit = monthlyEquivalentProfit,
        notes = null, detailsJson = null, createdAt = 0L, updatedAt = 0L
    )
}
