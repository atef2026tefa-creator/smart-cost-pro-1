package com.aistudio.smartcostpro.app.domain.calculations

import com.aistudio.smartcostpro.app.domain.model.Student
import com.aistudio.smartcostpro.app.util.Constants

/**
 * Student validation.
 *
 * The spec requires:
 *  - name is mandatory
 *  - age is mandatory
 *  - nationalId is mandatory and exactly 14 numeric characters
 *  - no letters accepted in the national id
 */
object StudentCalculator {

    /**
     * Validates a student before persistence.
     */
    fun validate(
        name: String,
        age: Int,
        nationalId: String,
        stage: String? = null
    ): ValidationError? {
        if (name.isBlank()) return ValidationError.NameRequired
        if (age <= 0) return ValidationError.AgeInvalid
        if (nationalId.length != Constants.NATIONAL_ID_LENGTH) return ValidationError.NationalIdLength
        if (!nationalId.all { it.isDigit() }) return ValidationError.NationalIdNumericOnly
        return null
    }

    enum class ValidationError {
        NameRequired,
        AgeInvalid,
        NationalIdLength,
        NationalIdNumericOnly
    }

    /**
     * Generates the student id the way the reference app does:
     *   `std_${Date.now()}`
     */
    fun generateId(): String = "std_${System.currentTimeMillis()}"
}
