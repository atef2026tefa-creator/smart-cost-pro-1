package com.aistudio.smartcostpro.app.domain.calculations

import com.aistudio.smartcostpro.app.domain.model.Expense
import com.aistudio.smartcostpro.app.domain.model.ExpenseType
import com.aistudio.smartcostpro.app.util.DateFormatter
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Report row — produced by [ReportCalculator] for day / week / month reports.
 *
 * The spec mentions a "rows" structure but does not pin every column, so
 * Stage 1 keeps this minimal and easy to extend in Stage 2.
 */
data class ReportRow(
    val id: String,
    val date: String,
    val title: String,
    val amount: Double,
    val type: ExpenseType?,
    val category: String,
    val paymentMethod: String?
)

/**
 * Reports calculator.
 *
 * **Spec compliance:**
 * - Day report: from == to == selected date.
 * - Range report: between [from] and [to] (inclusive).
 * - Long-range guard: spec mentions a < 370 days cap; we honor it.
 * - Filters: All / Income / House / Personal / Associations / Vehicle / Education.
 *
 * The spec notes that the new report builder uses the same `rows` list for
 * both preview and export, while the legacy ReportPreviewModal used
 * separate lists. Stage 1 implements the new "single rows" path.
 */
object ReportCalculator {

    /**
     * Builds the rows for a day report.
     */
    fun dayReport(expenses: List<Expense>, date: String): List<ReportRow> {
        return expenses
            .filter { it.date == date }
            .sortedByDescending { it.date }
            .map { it.toRow() }
    }

    /**
     * Builds the rows for a date-range report.
     *
     * The spec caps the range at < 370 days. Returns an empty list (and
     * logs nothing — the caller decides how to surface this) when the
     * range exceeds the cap.
     */
    fun rangeReport(
        expenses: List<Expense>,
        from: String,
        to: String
    ): List<ReportRow> {
        val start = runCatching { LocalDate.parse(from, DateFormatter.isoDateFormatter) }.getOrNull()
            ?: return emptyList()
        val end = runCatching { LocalDate.parse(to, DateFormatter.isoDateFormatter) }.getOrNull()
            ?: return emptyList()
        if (start > end) return emptyList()
        val days = ChronoUnit.DAYS.between(start, end) + 1
        if (days >= 370) return emptyList()

        return expenses
            .filter { it.date >= from && it.date <= to }
            .sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.createdAt })
            .map { it.toRow() }
    }

    /**
     * Builds the rows for a month report.
     */
    fun monthReport(expenses: List<Expense>, month: String): List<ReportRow> {
        return expenses
            .filter { DateFormatter.isDateInMonth(it.date, month) }
            .sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.createdAt })
            .map { it.toRow() }
    }

    /**
     * Filters rows by the spec's filter set:
     *  All / Income / House / Personal / Associations / Vehicle / Education
     *
     * Stage 1 maps the filter set to expense types; the "Income" / "Associations"
     * filters are placeholders until Stage 2 wires them to the income / association
     * sources, since those are not part of the unified expense table.
     */
    fun filterRows(rows: List<ReportRow>, filter: ReportFilter): List<ReportRow> {
        if (filter == ReportFilter.All) return rows
        return rows.filter { row ->
            when (filter) {
                ReportFilter.All -> true
                ReportFilter.House -> row.type == ExpenseType.House || row.type == ExpenseType.Medical
                ReportFilter.Personal -> row.type == ExpenseType.Personal
                ReportFilter.Vehicle -> row.type == ExpenseType.VehicleFuel ||
                    row.type == ExpenseType.VehicleMaint ||
                    row.type == ExpenseType.VehicleOil
                ReportFilter.Education -> row.type == ExpenseType.Student
                ReportFilter.Work -> row.type == ExpenseType.Work
                ReportFilter.Income -> false // not in the expense table
                ReportFilter.Associations -> false // not in the expense table
            }
        }
    }

    /** Spec filter set. */
    enum class ReportFilter {
        All, Income, House, Personal, Associations, Vehicle, Education, Work
    }

    private fun Expense.toRow() = ReportRow(
        id = id,
        date = date,
        title = title,
        amount = amount,
        type = type,
        category = category,
        paymentMethod = paymentMethod.code
    )
}
