package com.aistudio.smartcostpro.app.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.smartcostpro.app.data.repository.ExpenseRepository
import com.aistudio.smartcostpro.app.domain.calculations.ExpenseCalculator
import com.aistudio.smartcostpro.app.domain.model.Expense
import com.aistudio.smartcostpro.app.domain.model.ExpenseType
import com.aistudio.smartcostpro.app.domain.model.PaymentMethod
import com.aistudio.smartcostpro.app.util.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One-shot events emitted by the form (used by the UI to show a snackbar or
 * dismiss the dialog).
 */
sealed interface ExpenseFormEvent {
    data object Saved : ExpenseFormEvent
    data class Failed(val message: String) : ExpenseFormEvent
}

/**
 * View state for both Add and Edit forms.
 *
 * Stage 2 unifies the two flows into a single ViewModel — the only
 * difference is that the Edit screen pre-populates the fields from the
 * existing [Expense] and the Save path calls [ExpenseRepository.update]
 * instead of [ExpenseRepository.insert].
 */
data class ExpenseFormState(
    val id: String? = null, // null = new expense
    val sectionIndex: Int = 0,
    val categoryIndex: Int = 0,
    val title: String = "",
    val amount: String = "",
    val date: String = DateFormatter.todayIso(),
    val paymentMethodIndex: Int = 0,
    val notes: String = "",
    val medicalFamilyMember: String = "",
    val medicalFacilityName: String = "",
    val medicalFacilityType: String = "",
    val medicalCondition: String = "",
    val medicalDiagnosis: String = "",
    val medicalExamCost: String = "",
    val medicalMedicationCost: String = "",
    val medicalNotes: String = "",
    val isMedical: Boolean = false,
    val amountError: String? = null,
    val dateError: String? = null,
    val titleError: String? = null,
    val isSaving: Boolean = false
) {
    val isEdit: Boolean get() = id != null
}

@HiltViewModel
class ExpenseFormViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseFormState())
    val state: StateFlow<ExpenseFormState> = _state

    private val _events = MutableSharedFlow<ExpenseFormEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ExpenseFormEvent> = _events.asSharedFlow()

    /**
     * Pre-populates the form for editing. Stage 2 only allows editing
     * existing expenses — there is no separate "view" mode.
     */
    fun loadForEdit(expense: Expense) {
        val sectionList = sectionCodes()
        val sectionIndex = sectionList.indexOf(expense.type.code).coerceAtLeast(0)
        val catList = com.aistudio.smartcostpro.app.domain.model.Categories.forType(expense.type)
        val categoryIndex = catList.indexOf(expense.category).coerceAtLeast(0)
        val paymentIndex = PaymentMethod.entries.indexOf(expense.paymentMethod).coerceAtLeast(0)

        // Try to decode medical details if present.
        var medical = com.aistudio.smartcostpro.app.domain.model.MedicalDetails()
        if (expense.type == ExpenseType.Medical && !expense.detailsJson.isNullOrBlank()) {
            runCatching {
                kotlinx.serialization.json.Json.decodeFromString<
                    com.aistudio.smartcostpro.app.domain.model.MedicalDetails
                >(expense.detailsJson)
            }.getOrNull()?.let { medical = it }
        }

        _state.value = ExpenseFormState(
            id = expense.id,
            sectionIndex = sectionIndex,
            categoryIndex = categoryIndex,
            title = expense.title,
            amount = expense.amount.toPlainString(),
            date = expense.date,
            paymentMethodIndex = paymentIndex,
            notes = expense.notes.orEmpty(),
            medicalFamilyMember = medical.familyMember.orEmpty(),
            medicalFacilityName = medical.facilityName.orEmpty(),
            medicalFacilityType = medical.facilityType.orEmpty(),
            medicalCondition = medical.condition.orEmpty(),
            medicalDiagnosis = medical.diagnosis.orEmpty(),
            medicalExamCost = medical.examCost.takeIf { it > 0 }?.toPlainString().orEmpty(),
            medicalMedicationCost = medical.medicationCost.takeIf { it > 0 }?.toPlainString().orEmpty(),
            medicalNotes = medical.medicalNotes.orEmpty(),
            isMedical = expense.type == ExpenseType.Medical
        )
    }

    /** Updates the section, also resetting the category to the first of the new section. */
    fun onSectionChange(index: Int) {
        _state.value = _state.value.copy(
            sectionIndex = index,
            categoryIndex = 0,
            isMedical = sectionCodes().getOrNull(index) == ExpenseType.Medical.code
        )
    }

    fun onCategoryChange(index: Int) {
        _state.value = _state.value.copy(categoryIndex = index)
    }

    fun onTitleChange(value: String) {
        _state.value = _state.value.copy(title = value, titleError = null)
    }

    fun onAmountChange(value: String) {
        _state.value = _state.value.copy(amount = value, amountError = null)
    }

    fun onDateChange(value: String) {
        _state.value = _state.value.copy(date = value, dateError = null)
    }

    fun onPaymentMethodChange(index: Int) {
        _state.value = _state.value.copy(paymentMethodIndex = index)
    }

    fun onNotesChange(value: String) {
        _state.value = _state.value.copy(notes = value)
    }

    // Medical field setters
    fun onMedicalFamilyMember(value: String) {
        _state.value = _state.value.copy(medicalFamilyMember = value)
    }

    fun onMedicalFacilityName(value: String) {
        _state.value = _state.value.copy(medicalFacilityName = value)
    }

    fun onMedicalFacilityType(value: String) {
        _state.value = _state.value.copy(medicalFacilityType = value)
    }

    fun onMedicalCondition(value: String) {
        _state.value = _state.value.copy(medicalCondition = value)
    }

    fun onMedicalDiagnosis(value: String) {
        _state.value = _state.value.copy(medicalDiagnosis = value)
    }

    fun onMedicalExamCost(value: String) {
        _state.value = _state.value.copy(medicalExamCost = value)
    }

    fun onMedicalMedicationCost(value: String) {
        _state.value = _state.value.copy(medicalMedicationCost = value)
    }

    fun onMedicalNotes(value: String) {
        _state.value = _state.value.copy(medicalNotes = value)
    }

    /**
     * Persists the expense. The validation matches the spec §4.1:
     *  - amount > 0
     *  - date not blank
     *  - title not blank (server-side requirement; we enforce it too)
     * For medical, also validates examCost/medicationCost are non-negative.
     *
     * Emits [ExpenseFormEvent.Saved] on success, [ExpenseFormEvent.Failed] on error.
     */
    fun save() {
        val s = _state.value
        if (s.isSaving) return

        val amount = s.amount.toDoubleOrNull() ?: 0.0
        if (amount <= 0.0) {
            _state.value = s.copy(amountError = "AMOUNT_INVALID")
            return
        }
        if (s.date.isBlank()) {
            _state.value = s.copy(dateError = "DATE_REQUIRED")
            return
        }
        if (s.title.isBlank()) {
            _state.value = s.copy(titleError = "TITLE_REQUIRED")
            return
        }

        val sectionCode = sectionCodes().getOrNull(s.sectionIndex) ?: ExpenseType.House.code
        val type = ExpenseType.fromCode(sectionCode) ?: ExpenseType.House
        val category = com.aistudio.smartcostpro.app.domain.model.Categories.forType(type).getOrNull(s.categoryIndex)
            ?: com.aistudio.smartcostpro.app.domain.model.Categories.defaultFor(type)
        val paymentMethod = PaymentMethod.entries.getOrNull(s.paymentMethodIndex) ?: PaymentMethod.DEFAULT

        // Build the medical JSON if applicable.
        val detailsJson: String? = if (type == ExpenseType.Medical) {
            val exam = s.medicalExamCost.toDoubleOrNull() ?: 0.0
            val medication = s.medicalMedicationCost.toDoubleOrNull() ?: 0.0
            val medical = com.aistudio.smartcostpro.app.domain.model.MedicalDetails(
                familyMember = s.medicalFamilyMember.ifBlank { null },
                facilityName = s.medicalFacilityName.ifBlank { null },
                facilityType = s.medicalFacilityType.ifBlank { null },
                condition = s.medicalCondition.ifBlank { null },
                diagnosis = s.medicalDiagnosis.ifBlank { null },
                examCost = exam,
                medicationCost = medication,
                date = s.date,
                medicalNotes = s.medicalNotes.ifBlank { null }
            )
            kotlinx.serialization.json.Json.encodeToString(medical)
        } else null

        // For medical records, the spec's amount = examCost + medicationCost.
        val finalAmount = if (type == ExpenseType.Medical) {
            (s.medicalExamCost.toDoubleOrNull() ?: 0.0) + (s.medicalMedicationCost.toDoubleOrNull() ?: 0.0)
        } else amount

        if (finalAmount <= 0.0 && type == ExpenseType.Medical) {
            _state.value = s.copy(amountError = "AMOUNT_INVALID")
            return
        }

        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                // Preserve the original `createdAt` on update — otherwise the
                // row would jump to epoch (1970) and break "recent operations"
                // sorting in the dashboard. Done inside the coroutine so we
                // don't block the main thread.
                val existingCreatedAt: Long = if (s.id != null) {
                    expenseRepository.getById(s.id)?.createdAt ?: now
                } else now
                val expense = Expense(
                    id = s.id ?: java.util.UUID.randomUUID().toString(),
                    title = s.title.trim(),
                    amount = finalAmount,
                    type = type,
                    category = category,
                    date = s.date,
                    paymentMethod = paymentMethod,
                    notes = s.notes.ifBlank { null },
                    detailsJson = detailsJson,
                    createdAt = existingCreatedAt,
                    updatedAt = now
                )

                if (s.id == null) expenseRepository.insert(expense)
                else expenseRepository.update(expense)
                _events.emit(ExpenseFormEvent.Saved)
                _state.value = _state.value.copy(isSaving = false)
            } catch (t: Throwable) {
                _events.emit(ExpenseFormEvent.Failed(t.message ?: "UNKNOWN"))
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }

    /** Returns the list of section codes available in the AddExpenseModal. */
    fun sectionCodes(): List<String> = listOf(
        ExpenseType.House.code,
        ExpenseType.Medical.code,
        ExpenseType.Work.code,
        ExpenseType.Personal.code,
        ExpenseType.VehicleFuel.code,
        ExpenseType.Student.code
    )
}

/**
 * Pure helper used by the list screen's "search + filter" pipeline. Kept here
 * so the form ViewModel doesn't need to expose state for filtering.
 */
object ExpenseFilter {
    fun apply(
        expenses: List<Expense>,
        search: String,
        typeFilter: ExpenseType?,
        categoryFilter: String?
    ): List<Expense> {
        return expenses
            .filter { typeFilter == null || it.type == typeFilter }
            .filter { categoryFilter.isNullOrBlank() || it.category == categoryFilter }
            .filter {
                search.isBlank() ||
                    it.title.contains(search, ignoreCase = true) ||
                    it.notes?.contains(search, ignoreCase = true) == true ||
                    it.category.contains(search, ignoreCase = true)
            }
    }
}
