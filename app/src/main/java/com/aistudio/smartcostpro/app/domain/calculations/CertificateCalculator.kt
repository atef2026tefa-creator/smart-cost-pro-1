package com.aistudio.smartcostpro.app.domain.calculations

import com.aistudio.smartcostpro.app.domain.model.BankCertificate
import com.aistudio.smartcostpro.app.domain.model.ProfitFrequency
import com.aistudio.smartcostpro.app.util.DateFormatter
import java.time.LocalDate
import java.time.YearMonth

/**
 * Bank certificate profit calculations.
 *
 * **Spec compliance — the priority order is strict:**
 *
 *  1. If `monthlyEquivalentProfit > 0`, use it directly.
 *  2. Else if `amount > 0` and `annualRate > 0`, compute from those.
 *  3. Else if `profitAmount > 0` and `profitFrequency` is set, derive from those.
 *  4. If no positive profit can be derived, the certificate contributes 0.
 *
 * The spec also requires:
 *  - If both issue and maturity dates are valid, the target month must lie
 *    within [issueDate, maturityDate] (inclusive) — otherwise the cert
 *    contributes nothing.
 *  - "On maturity" computes the *total* profit over the full duration and
 *    divides by the duration in months.
 *  - All results are rounded to 2 decimal places.
 *  - The duration is stored as a free string; we parse it carefully into months.
 */
object CertificateCalculator {

    /**
     * Returns the monthly profit contribution of [cert] for the [month] target month,
     * or `0.0` if the certificate is not active in that month or yields no positive profit.
     */
    fun monthlyProfitFor(cert: BankCertificate, month: String): Double {
        // Date-window check (spec §11.5 step 5).
        if (!isCertActiveInMonth(cert, month)) {
            // Spec §11.5 step 6: when dates cannot be compared, the reference app
            // falls back to including the profit. We keep that behavior for parity,
            // but only when the date window cannot be computed.
            val windowUnknown = cert.issueDate.isBlank() || cert.maturityDate.isNullOrBlank()
            if (!windowUnknown) return 0.0
        }

        val monthly = computeMonthlyProfit(cert)
        return if (monthly > 0.0) roundTo2(monthly) else 0.0
    }

    /**
     * Returns the sum of monthly profits from all certificates for the target month.
     */
    fun totalMonthlyProfit(certs: List<BankCertificate>, month: String): Double {
        return roundTo2(certs.sumOf { monthlyProfitFor(it, month) })
    }

    // ───────────────────────────── Annual profit ─────────────────────────────

    /**
     * `annualProfit = amount * annualRate / 100`
     */
    fun annualProfit(cert: BankCertificate): Double {
        if (cert.amount <= 0.0 || cert.annualRate <= 0.0) return 0.0
        return roundTo2(cert.amount * cert.annualRate / 100.0)
    }

    // ───────────────────────────── Periodic profit ─────────────────────────────

    /**
     * Maps the spec's periodic profit table to a per-period amount:
     *
     * | Frequency | Profit |
     * |-----------|--------|
     * | Monthly | annual / 12 |
     * | Quarterly | annual / 4 |
     * | SemiAnnual | annual / 2 |
     * | Annual | annual |
     * | OnMaturity | annual × years |
     */
    fun periodicProfit(cert: BankCertificate): Double {
        val annual = annualProfit(cert)
        if (annual <= 0.0) return 0.0
        return when (cert.profitFrequency) {
            ProfitFrequency.Monthly -> roundTo2(annual / 12.0)
            ProfitFrequency.Quarterly -> roundTo2(annual / 4.0)
            ProfitFrequency.SemiAnnual -> roundTo2(annual / 2.0)
            ProfitFrequency.Annual -> roundTo2(annual)
            ProfitFrequency.OnMaturity -> {
                val years = parseDurationYears(cert.duration)
                roundTo2(annual * years)
            }
        }
    }

    // ───────────────────────────── Monthly equivalent ─────────────────────────────

    /**
     * The monthly-equivalent profit, applying the spec's priority order.
     */
    fun computeMonthlyProfit(cert: BankCertificate): Double {
        // Step 1: use the explicit monthly equivalent if provided.
        if (cert.monthlyEquivalentProfit > 0.0) {
            return cert.monthlyEquivalentProfit
        }

        // Step 2: derive from amount × annualRate.
        if (cert.amount > 0.0 && cert.annualRate > 0.0) {
            val annual = cert.amount * cert.annualRate / 100.0
            return when (cert.profitFrequency) {
                ProfitFrequency.OnMaturity -> {
                    val years = parseDurationYears(cert.duration)
                    val totalProfit = annual * years
                    val totalMonths = (years * 12.0).coerceAtLeast(1.0)
                    roundTo2(totalProfit / totalMonths)
                }
                else -> roundTo2(annual / 12.0)
            }
        }

        // Step 3: derive from `profitAmount` + `profitFrequency`.
        if (cert.profitAmount > 0.0) {
            return when (cert.profitFrequency) {
                ProfitFrequency.Monthly -> roundTo2(cert.profitAmount)
                ProfitFrequency.Quarterly -> roundTo2(cert.profitAmount / 3.0)
                ProfitFrequency.SemiAnnual -> roundTo2(cert.profitAmount / 6.0)
                ProfitFrequency.Annual -> roundTo2(cert.profitAmount / 12.0)
                ProfitFrequency.OnMaturity -> {
                    val years = parseDurationYears(cert.duration)
                    val months = (years * 12.0).coerceAtLeast(1.0)
                    val totalProfit = cert.profitAmount
                    roundTo2(totalProfit / months)
                }
            }
        }

        // Step 4: no positive profit could be derived.
        return 0.0
    }

    // ───────────────────────────── Duration parsing ─────────────────────────────

    /**
     * Parses a duration string and returns the number of years as a Double.
     * Falls back to 1.0 when no number can be extracted (spec §11.3).
     *
     * **Stage 3 fix:** handles BOTH English and Arabic duration strings:
     *  - English: "6 months", "1 year", "3 years", "5 years"
     *  - Arabic:   "6 شهور", "سنة", "سنتان", "3 سنوات", "5 سنوات"
     *
     * Also tolerates missing spaces ("6months", "سنة1") — the regexes use
     * `\s*` between the number and the unit.
     */
    fun parseDurationYears(duration: String?): Double {
        if (duration.isNullOrBlank()) return 1.0
        val lower = duration.lowercase().trim()
        // English: "X months" / "X month"
        val monthsEn = Regex("""(\d+(?:\.\d+)?)\s*months?""")
        // English: "X year(s)"
        val yearsEn = Regex("""(\d+(?:\.\d+)?)\s*years?""")
        // Arabic: "X شهور/شهر/أشهر"
        val monthsAr = Regex("""(\d+(?:\.\d+)?)\s*(شهور|شهر|أشهر|اشهر)""")
        // Arabic: "X سنوات/سنة/سنتان"
        val yearsAr = Regex("""(\d+(?:\.\d+)?)\s*(سنوات|سنة|سنتان)""")
        // Bare Arabic unit (e.g. "سنة" → 1 year)
        val yearsArBare = Regex("""^(سنوات|سنة|سنتان)$""")
        val monthsArBare = Regex("""^(شهور|شهر|أشهر|اشهر)$""")

        monthsEn.find(lower)?.let { m ->
            m.groupValues[1].toDoubleOrNull()?.let { return it / 12.0 }
        }
        yearsEn.find(lower)?.let { m ->
            m.groupValues[1].toDoubleOrNull()?.let { return it }
        }
        monthsAr.find(lower)?.let { m ->
            m.groupValues[1].toDoubleOrNull()?.let { return it / 12.0 }
        }
        yearsAr.find(lower)?.let { m ->
            m.groupValues[1].toDoubleOrNull()?.let { return it }
        }
        yearsArBare.find(lower)?.let { return 1.0 }
        monthsArBare.find(lower)?.let { return 1.0 / 12.0 }

        // Plain integer fallback (e.g. "12" → 12 months → 1 year)
        lower.toDoubleOrNull()?.let { n ->
            return n / 12.0
        }
        return 1.0
    }

    // ───────────────────────────── Date window ─────────────────────────────

    /**
     * Returns true when the target YYYY-MM month falls in [issueDate, maturityDate]
     * inclusive. When dates are unparseable or missing, returns true (the spec
     * falls back to including the profit in that case).
     */
    fun isCertActiveInMonth(cert: BankCertificate, month: String): Boolean {
        val target = DateFormatter.yearMonthFromIso(month) ?: return true
        val issue = runCatching { YearMonth.from(LocalDate.parse(cert.issueDate, DateFormatter.isoDateFormatter)) }.getOrNull()
        val maturity = cert.maturityDate?.let {
            runCatching { YearMonth.from(LocalDate.parse(it, DateFormatter.isoDateFormatter)) }.getOrNull()
        }
        if (issue == null && maturity == null) return true
        if (issue != null && target < issue) return false
        if (maturity != null && target > maturity) return false
        return true
    }

    private fun roundTo2(value: Double): Double {
        val scaled = Math.round(value * 100.0) / 100.0
        return scaled
    }
}
