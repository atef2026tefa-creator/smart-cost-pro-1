package com.aistudio.smartcostpro.app.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.smartcostpro.app.R
import com.aistudio.smartcostpro.app.domain.model.Categories
import com.aistudio.smartcostpro.app.domain.model.ExpenseType
import com.aistudio.smartcostpro.app.domain.model.PaymentMethod
import com.aistudio.smartcostpro.app.ui.components.AmountTextField
import com.aistudio.smartcostpro.app.ui.forms.DatePickerField
import com.aistudio.smartcostpro.app.ui.forms.DropdownField
import com.aistudio.smartcostpro.app.ui.forms.localizedCategoryLabel
import com.aistudio.smartcostpro.app.ui.forms.localizedExpenseType
import com.aistudio.smartcostpro.app.ui.forms.localizedPaymentMethod
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.DateFormatter

/**
 * Unified Add / Edit expense modal.
 *
 * Matches the spec §4.1 flow:
 *   1. Pick a section → category list resets to the first category.
 *   2. Pick a category.
 *   3. Enter amount (validated > 0).
 *   4. Pick a date (defaults to today).
 *   5. Pick a payment method (defaults to cash).
 *   6. Optional notes.
 *   7. For Medical, the form expands to show medical fields; the amount
 *      is then derived from `examCost + medicationCost`.
 *
 * The dialog dismisses itself when [ExpenseFormEvent.Saved] is received.
 */
@Composable
fun ExpenseFormDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    editExpense: com.aistudio.smartcostpro.app.domain.model.Expense? = null,
    viewModel: ExpenseFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = AppLocale.Arabic // The composition locale is provided by the theme; we mirror it here.

    LaunchedEffect(editExpense?.id) {
        if (editExpense != null) viewModel.loadForEdit(editExpense)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ExpenseFormEvent.Saved -> onSaved()
                is ExpenseFormEvent.Failed -> { /* snackbar handled by caller */ }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.material3.MaterialTheme.shapes.large,
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            FormContent(
                state = state,
                locale = locale,
                onSectionChange = viewModel::onSectionChange,
                onCategoryChange = viewModel::onCategoryChange,
                onTitleChange = viewModel::onTitleChange,
                onAmountChange = viewModel::onAmountChange,
                onDateChange = viewModel::onDateChange,
                onPaymentMethodChange = viewModel::onPaymentMethodChange,
                onNotesChange = viewModel::onNotesChange,
                onMedicalFamilyMember = viewModel::onMedicalFamilyMember,
                onMedicalFacilityName = viewModel::onMedicalFacilityName,
                onMedicalFacilityType = viewModel::onMedicalFacilityType,
                onMedicalCondition = viewModel::onMedicalCondition,
                onMedicalDiagnosis = viewModel::onMedicalDiagnosis,
                onMedicalExamCost = viewModel::onMedicalExamCost,
                onMedicalMedicationCost = viewModel::onMedicalMedicationCost,
                onMedicalNotes = viewModel::onMedicalNotes,
                onSave = viewModel::save,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun FormContent(
    state: ExpenseFormState,
    locale: AppLocale,
    onSectionChange: (Int) -> Unit,
    onCategoryChange: (Int) -> Unit,
    onTitleChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onPaymentMethodChange: (Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onMedicalFamilyMember: (String) -> Unit,
    onMedicalFacilityName: (String) -> Unit,
    onMedicalFacilityType: (String) -> Unit,
    onMedicalCondition: (String) -> Unit,
    onMedicalDiagnosis: (String) -> Unit,
    onMedicalExamCost: (String) -> Unit,
    onMedicalMedicationCost: (String) -> Unit,
    onMedicalNotes: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val sectionCodes = listOf(
        ExpenseType.House.code,
        ExpenseType.Medical.code,
        ExpenseType.Work.code,
        ExpenseType.Personal.code,
        ExpenseType.VehicleFuel.code,
        ExpenseType.Student.code
    )
    val sectionLabels = sectionCodes.map { localizedExpenseType(it) }
    val sectionCode = sectionCodes.getOrNull(state.sectionIndex) ?: ExpenseType.House.code
    val sectionType = ExpenseType.fromCode(sectionCode) ?: ExpenseType.House
    val categoryCodes = Categories.forType(sectionType)
    val categoryLabels = categoryCodes.map { localizedCategoryLabel(it) }
    val paymentLabels = PaymentMethod.entries.map { localizedPaymentMethod(it.code) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (state.isEdit) stringResource(R.string.form_edit_expense)
                else stringResource(R.string.form_add_expense),
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        )

        // Section
        DropdownField(
            label = stringResource(R.string.form_section_label),
            options = sectionLabels,
            selectedIndex = state.sectionIndex,
            onSelected = onSectionChange
        )

        // Category
        DropdownField(
            label = stringResource(R.string.form_category_label),
            options = categoryLabels,
            selectedIndex = state.categoryIndex.coerceIn(0, categoryLabels.lastIndex),
            onSelected = onCategoryChange
        )

        // Title
        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.form_title_label)) },
            singleLine = true,
            isError = state.titleError != null,
            supportingText = if (state.titleError != null) ({
                Text(stringResource(R.string.form_error_title_required))
            }) else null,
            modifier = Modifier.fillMaxWidth()
        )

        // Amount — hidden when medical (we use exam + medication cost).
        if (!state.isMedical) {
            AmountTextField(
                value = state.amount,
                onValueChange = onAmountChange,
                label = stringResource(R.string.form_amount_label),
                error = state.amountError?.let { stringResource(R.string.form_error_amount_zero) }
            )
        }

        // Date
        DatePickerField(
            value = state.date,
            onValueChange = onDateChange,
            label = stringResource(R.string.form_date_label),
            locale = locale,
            error = state.dateError?.let { stringResource(R.string.form_error_date_required) }
        )

        // Payment method
        DropdownField(
            label = stringResource(R.string.form_payment_label),
            options = paymentLabels,
            selectedIndex = state.paymentMethodIndex,
            onSelected = onPaymentMethodChange
        )

        // Notes
        OutlinedTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(R.string.form_notes_label)) },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        // Medical fields
        if (state.isMedical) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.expense_type_medical),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = state.medicalFamilyMember,
                onValueChange = onMedicalFamilyMember,
                label = { Text(stringResource(R.string.form_medical_family_member)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.medicalFacilityName,
                onValueChange = onMedicalFacilityName,
                label = { Text(stringResource(R.string.form_medical_facility_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.medicalFacilityType,
                onValueChange = onMedicalFacilityType,
                label = { Text(stringResource(R.string.form_medical_facility_type)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.medicalCondition,
                onValueChange = onMedicalCondition,
                label = { Text(stringResource(R.string.form_medical_condition)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.medicalDiagnosis,
                onValueChange = onMedicalDiagnosis,
                label = { Text(stringResource(R.string.form_medical_diagnosis)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            AmountTextField(
                value = state.medicalExamCost,
                onValueChange = onMedicalExamCost,
                label = stringResource(R.string.form_medical_exam_cost),
                error = state.amountError?.let { stringResource(R.string.form_error_amount_zero) }
            )
            AmountTextField(
                value = state.medicalMedicationCost,
                onValueChange = onMedicalMedicationCost,
                label = stringResource(R.string.form_medical_medication_cost)
            )
            OutlinedTextField(
                value = state.medicalNotes,
                onValueChange = onMedicalNotes,
                label = { Text(stringResource(R.string.form_medical_notes)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action buttons
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.form_cancel)) }
            Spacer(modifier = androidx.compose.foundation.layout.width(8.dp))
            Button(
                onClick = onSave,
                enabled = !state.isSaving
            ) { Text(stringResource(R.string.form_save)) }
        }
    }
}

private fun Double.toPlainString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()
