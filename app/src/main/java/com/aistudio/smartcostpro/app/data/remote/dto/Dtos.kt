package com.aistudio.smartcostpro.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * DTOs for the (mocked) REST API.
 *
 * Stage 1 ships a [com.aistudio.smartcostpro.app.data.remote.MockApiService] that returns
 * canned data so the networking layer is fully wired. Stage 2 will swap the
 * mock for a real Retrofit call against a real backend without touching the
 * repository / ViewModel layer.
 *
 * The spec mentions that the production API was disabled in the reference
 * app "until authentication is configured". We mirror that behavior with a
 * `auth_disabled` mock response so the code path is exercised without
 * leaking any data.
 */

@Serializable
data class ExpenseDto(
    val id: String,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val date: String,
    val paymentMethod: String? = null,
    val notes: String? = null,
    val detailsJson: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class IncomeDto(
    val month: String,
    val salary: Double,
    val bonuses: Double,
    val otherIncome: Double,
    val otherIncomeNote: String? = null,
    val detailsJson: String? = null,
    val updatedAt: Long
)

@Serializable
data class CertificateDto(
    val id: String,
    val bankName: String,
    val certificateNumber: String? = null,
    val duration: String = "1 year",
    val amount: Double,
    val annualRate: Double = 0.0,
    val issueDate: String,
    val maturityDate: String? = null,
    val profitDate: String? = null,
    val profitAmount: Double = 0.0,
    val monthlyEquivalentProfit: Double = 0.0,
    val profitFrequency: String = "monthly",
    val notes: String? = null,
    val detailsJson: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BackupDto(
    val schemaVersion: Int,
    val exportedAt: Long,
    val expenses: List<ExpenseDto>,
    val income: List<IncomeDto>,
    val certificates: List<CertificateDto>,
    val carIncome: List<com.aistudio.smartcostpro.app.domain.model.CarTrip>
)
