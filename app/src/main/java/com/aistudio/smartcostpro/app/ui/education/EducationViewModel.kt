package com.aistudio.smartcostpro.app.ui.education

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.smartcostpro.app.data.repository.ExpenseRepository
import com.aistudio.smartcostpro.app.data.repository.StudentRepository
import com.aistudio.smartcostpro.app.domain.calculations.StudentCalculator
import com.aistudio.smartcostpro.app.domain.model.EducationStage
import com.aistudio.smartcostpro.app.domain.model.Expense
import com.aistudio.smartcostpro.app.domain.model.ExpenseType
import com.aistudio.smartcostpro.app.domain.model.Student
import com.aistudio.smartcostpro.app.domain.model.StudentExpenseLink
import com.aistudio.smartcostpro.app.util.AppCurrency
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.DateFormatter
import com.aistudio.smartcostpro.app.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * UI state for the Education screen.
 *
 * Combines students list + their linked expenses. The spec requires:
 *  - 14-digit national ID validation (no letters)
 *  - Deleting a student removes their expenses (no orphan rows)
 *  - Student expense carries studentId + subCategory inside detailsJson
 */
data class EducationUiState(
    val students: List<Student> = emptyList(),
    val studentExpenses: List<StudentExpenseItem> = emptyList(),
    val locale: AppLocale = AppLocale.Arabic,
    val currency: AppCurrency = AppCurrency.EGP
)

/** Student + their summed expenses (used in the list view). */
data class StudentExpenseItem(
    val student: Student,
    val expenses: List<Expense>,
    val total: Double
)

@HiltViewModel
class EducationViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<EducationUiState> = combine(
        studentRepository.observeAll(),
        expenseRepository.observeByType(ExpenseType.Student),
        settingsRepository.settings
    ) { students, expenses, settings ->
        val byStudent = expenses.groupBy { extractStudentId(it) ?: "" }
        val items = students.map { s ->
            val list = byStudent[s.id].orEmpty()
            StudentExpenseItem(s, list, list.sumOf { it.amount })
        }
        EducationUiState(
            students = students,
            studentExpenses = items,
            locale = settings.locale,
            currency = settings.currency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EducationUiState()
    )

    // ─── Student CRUD ───
    fun upsertStudent(
        id: String?,
        name: String,
        age: Int,
        nationalId: String,
        stage: EducationStage,
        onResult: (String?) -> Unit
    ) {
        val err = StudentCalculator.validate(name, age, nationalId, stage.code)
        if (err != null) {
            onResult(errorCode(err))
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // Preserve createdAt on update by reading the existing row first.
            // For new rows, use the spec's `std_${Date.now()}` id format.
            val existing = id?.let { studentRepository.getById(it) }
            val student = Student(
                id = id ?: "std_${System.currentTimeMillis()}",
                name = name.trim(),
                stage = stage,
                age = age,
                nationalId = nationalId.trim(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            if (existing == null) studentRepository.upsert(student)
            else studentRepository.update(student)
            onResult(null)
        }
    }

    fun deleteStudent(id: String) {
        viewModelScope.launch { studentRepository.deleteById(id) }
    }

    // ─── Student expense CRUD ───
    fun upsertStudentExpense(
        id: String?,
        studentId: String,
        title: String,
        amount: Double,
        date: String,
        subCategory: String,
        notes: String?
    ) {
        viewModelScope.launch {
            val link = StudentExpenseLink(studentId = studentId, subCategory = subCategory)
            val detailsJson = kotlinx.serialization.json.Json.encodeToString(link)
            val now = System.currentTimeMillis()
            // Preserve createdAt on update.
            val existing = id?.let { expenseRepository.getById(it) }
            val expense = Expense(
                id = id ?: UUID.randomUUID().toString(),
                title = title.trim(),
                amount = amount,
                type = ExpenseType.Student,
                category = subCategory,
                date = date,
                paymentMethod = com.aistudio.smartcostpro.app.domain.model.PaymentMethod.Cash,
                notes = notes,
                detailsJson = detailsJson,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            if (existing == null) expenseRepository.insert(expense)
            else expenseRepository.update(expense)
        }
    }

    fun deleteStudentExpense(id: String) {
        viewModelScope.launch { expenseRepository.deleteById(id) }
    }

    private fun extractStudentId(expense: Expense): String? {
        if (expense.detailsJson.isNullOrBlank()) return null
        return runCatching {
            kotlinx.serialization.json.Json.decodeFromString<StudentExpenseLink>(expense.detailsJson).studentId
        }.getOrNull()
    }

    private fun errorCode(err: StudentCalculator.ValidationError): String = when (err) {
        StudentCalculator.ValidationError.NameRequired -> "NAME_REQUIRED"
        StudentCalculator.ValidationError.AgeInvalid -> "AGE_INVALID"
        StudentCalculator.ValidationError.NationalIdLength -> "NATIONAL_ID_LENGTH"
        StudentCalculator.ValidationError.NationalIdNumericOnly -> "NATIONAL_ID_NUMERIC"
    }
}
