package com.aistudio.smartcostpro.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Bank certificate record.
 *
 * The spec is explicit about the profit calculation priority when
 * computing the monthly equivalent profit:
 *
 *  1. If `monthlyEquivalentProfit > 0`, use it directly.
 *  2. Else if `amount` and `annualRate` are present, compute from those.
 *  3. Else if `profitAmount` and `profitFrequency` are present, derive from those.
 *  4. If no positive profit can be derived, the certificate contributes nothing.
 *
 * In addition, when both issue and maturity dates are valid, the target
 * month must lie within [issueDate, maturityDate] (inclusive).
 *
 * @param id Stable identifier.
 * @param bankName Mandatory.
 * @param certificateNumber Optional.
 * @param amount Original principal — must be > 0.
 * @param annualRate Annual profit rate in percent (e.g. 18 means 18%).
 * @param duration Either "6 months", "1 year", "2 years", "3 years", "5 years".
 *   Stored as a free string to match the spec, then parsed by
 *   [com.aistudio.smartcostpro.app.domain.calculations.CertificateCalculator].
 * @param profitFrequency See [ProfitFrequency].
 * @param issueDate ISO date (YYYY-MM-DD).
 * @param maturityDate ISO date (YYYY-MM-DD).
 * @param profitDate The date the profit was last paid (ISO).
 * @param profitAmount The fixed profit amount (when stored as such).
 * @param monthlyEquivalentProfit Pre-computed monthly equivalent, highest priority.
 * @param notes Optional free text.
 * @param createdAt Epoch millis when created.
 * @param updatedAt Epoch millis when last updated.
 */
@Serializable
data class BankCertificate(
    val id: String,
    val bankName: String,
    val certificateNumber: String? = null,
    val amount: Double,
    val annualRate: Double = 0.0,
    val duration: String = "1 year",
    val profitFrequency: ProfitFrequency = ProfitFrequency.Monthly,
    val issueDate: String,
    val maturityDate: String? = null,
    val profitDate: String? = null,
    val profitAmount: Double = 0.0,
    val monthlyEquivalentProfit: Double = 0.0,
    val notes: String? = null,
    val detailsJson: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
