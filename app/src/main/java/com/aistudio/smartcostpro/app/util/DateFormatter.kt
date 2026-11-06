package com.aistudio.smartcostpro.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Date formatting / parsing helpers.
 *
 * - The reference app stores dates as `YYYY-MM-DD` (ISO-8601) strings in the
 *   DB and uses `YYYY-MM` to identify a month; this helper keeps both formats
 *   stable across the app.
 * - For display, the helper uses the user's locale for the human-readable
 *   string, but the underlying stored format is always ISO.
 */
object DateFormatter {

    /** ISO date: 2025-03-14 */
    val isoDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    /** ISO month: 2025-03 */
    val isoMonthFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)

    /** ISO date-time: 2025-03-14T15:30 */
    val isoDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm", Locale.US)

    /** Compact date for chips: 14/03/2025 */
    private fun compactDateFormatter(locale: AppLocale): DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US)

    /** Human-readable long date: 14 March 2025 */
    private fun longDateFormatter(locale: AppLocale): DateTimeFormatter {
        val javaLocale = when (locale) {
            AppLocale.Arabic -> Locale("ar", "EG")
            AppLocale.English -> Locale.US
        }
        return DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .withLocale(javaLocale)
            .withZone(ZoneId.systemDefault())
    }

    /** Month-year for headers: March 2025 / مارس 2025 */
    private fun monthYearFormatter(locale: AppLocale): DateTimeFormatter {
        val javaLocale = when (locale) {
            AppLocale.Arabic -> Locale("ar", "EG")
            AppLocale.English -> Locale.US
        }
        return DateTimeFormatter.ofPattern("MMMM yyyy", javaLocale)
    }

    // ───────────────────────── Conversions ─────────────────────────

    fun todayIso(): String = LocalDate.now().format(isoDateFormatter)

    fun currentMonthIso(): String = LocalDate.now().format(isoMonthFormatter)

    fun monthOf(dateIso: String?): String? = dateIso?.takeIf { it.length >= 7 }?.substring(0, 7)

    fun yearMonthFromIso(monthIso: String): YearMonth? = try {
        YearMonth.parse(monthIso, isoMonthFormatter)
    } catch (_: DateTimeParseException) {
        null
    }

    fun isoDateToEpochMillis(dateIso: String, zone: ZoneId = ZoneId.systemDefault()): Long? {
        return try {
            LocalDate.parse(dateIso, isoDateFormatter)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    fun epochMillisToIsoDate(epoch: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        return Instant.ofEpochMilli(epoch)
            .atZone(zone)
            .toLocalDate()
            .format(isoDateFormatter)
    }

    fun formatForDisplay(dateIso: String?, locale: AppLocale, longStyle: Boolean = false): String {
        if (dateIso.isNullOrBlank()) return ""
        return try {
            val date = LocalDate.parse(dateIso, isoDateFormatter)
            if (longStyle) date.format(longDateFormatter(locale)) else date.format(compactDateFormatter(locale))
        } catch (_: DateTimeParseException) {
            dateIso
        }
    }

    fun formatMonthForDisplay(monthIso: String?, locale: AppLocale): String {
        if (monthIso.isNullOrBlank()) return ""
        val ym = yearMonthFromIso(monthIso) ?: return monthIso
        return ym.format(monthYearFormatter(locale))
    }

    fun isDateInMonth(dateIso: String?, monthIso: String): Boolean {
        if (dateIso.isNullOrBlank()) return false
        // Per the spec: `date.slice(0, 7) == month`
        return dateIso.length >= 7 && dateIso.substring(0, 7) == monthIso
    }

    /**
     * Returns the difference in months between two YYYY-MM values, in the
     * order `target - start`. This matches the spec's:
     *
     *     monthIndex = (targetYear - startYear) * 12 + (targetMonth - startMonth)
     */
    fun monthsBetween(startMonthIso: String, targetMonthIso: String): Int? {
        val start = yearMonthFromIso(startMonthIso) ?: return null
        val target = yearMonthFromIso(targetMonthIso) ?: return null
        return (target.year - start.year) * 12 + (target.monthValue - start.monthValue)
    }
}
