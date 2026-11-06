package com.aistudio.smartcostpro.app.domain.model

/**
 * Payment methods supported by the unified expense form.
 *
 * Matches the spec exactly — `cash` is the default in the AddExpenseModal.
 */
enum class PaymentMethod(val code: String) {
    Cash("cash"),
    Card("card"),
    Transfer("transfer"),
    Wallet("wallet"),
    Cheque("cheque");

    companion object {
        fun fromCode(code: String?): PaymentMethod? = code?.let { c ->
            entries.firstOrNull { it.code == c }
        }

        val DEFAULT: PaymentMethod = Cash
    }
}

/**
 * Income types as documented in the spec's "Sources of Income" section.
 *
 * `current_deposit` and `current_withdrawal` are deliberately NOT included
 * here — they belong to the current account and must NOT enter the primary
 * income total (the spec is explicit about this).
 */
enum class IncomeType(val code: String) {
    Salary("salary"),
    Extra("extra"),
    Bonus("bonus"),
    Investment("investment"),
    Other("other");

    companion object {
        fun fromCode(code: String?): IncomeType? = code?.let { c ->
            entries.firstOrNull { it.code == c }
        }

        val DEFAULT: IncomeType = Salary
    }
}

/**
 * Profit payout frequency for bank certificates.
 *
 * The spec maps each frequency to a divisor:
 *   - Monthly: annual / 12
 *   - Quarterly: annual / 4
 *   - Semi-annual: annual / 2
 *   - Annual: annual
 *   - On maturity: annual × years
 */
enum class ProfitFrequency(val code: String) {
    Monthly("monthly"),
    Quarterly("quarterly"),
    SemiAnnual("semi_annual"),
    Annual("annual"),
    OnMaturity("on_maturity");

    companion object {
        fun fromCode(code: String?): ProfitFrequency? = code?.let { c ->
            entries.firstOrNull { it.code == c }
        }
    }
}

/**
 * School / university stage for students.
 *
 * The reference app's default stage is `prep` (إعدادي).
 */
enum class EducationStage(val code: String) {
    Kindergarten("kindergarten"),
    Primary("primary"),
    Prep("prep"),
    Secondary("secondary"),
    University("university");

    companion object {
        fun fromCode(code: String?): EducationStage? = code?.let { c ->
            entries.firstOrNull { it.code == c }
        }

        val DEFAULT: EducationStage = Prep
    }
}

/**
 * Current account transaction direction.
 *
 * - `Deposit` adds to the monthly net (positive sign).
 * - `Withdrawal` subtracts (negative sign — applied once, not twice).
 */
enum class CurrentAccountType(val code: String, val sign: Int) {
    Deposit("current_deposit", +1),
    Withdrawal("current_withdrawal", -1);

    companion object {
        fun fromCode(code: String?): CurrentAccountType? = code?.let { c ->
            entries.firstOrNull { it.code == c }
        }
    }
}

/**
 * Car income trip types (the filter set documented in the spec).
 */
enum class TripType(val code: String) {
    All("all"),
    Cash("cash"),
    Uber("uber"),
    DiDi("didi"),
    InDrive("in_drive"),
    Private("private");

    companion object {
        fun fromCode(code: String?): TripType? = code?.let { c ->
            entries.firstOrNull { it.code == c }
        }
    }
}
