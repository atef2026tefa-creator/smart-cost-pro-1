package com.aistudio.smartcostpro.app.ui.education

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.smartcostpro.app.R
import com.aistudio.smartcostpro.app.domain.model.EducationStage
import com.aistudio.smartcostpro.app.domain.model.Student
import com.aistudio.smartcostpro.app.ui.components.AmountTextField
import com.aistudio.smartcostpro.app.ui.components.ConfirmDialog
import com.aistudio.smartcostpro.app.ui.components.EmptyState
import com.aistudio.smartcostpro.app.ui.forms.DropdownField
import com.aistudio.smartcostpro.app.util.Constants
import com.aistudio.smartcostpro.app.util.CurrencyFormatter
import com.aistudio.smartcostpro.app.util.DateFormatter

/**
 * Education screen.
 *
 * Stage 3 ships a single list view of students with their summed expenses;
 * tapping a student opens an Add-student-expense dialog scoped to them.
 *
 * Per the spec §9.1 / §9.4:
 *  - National ID must be 14 digits, no letters.
 *  - Deleting a student removes their expenses (handled in repository).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationScreen(
    viewModel: EducationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showStudentDialog by remember { mutableStateOf(false) }
    var editStudent by remember { mutableStateOf<Student?>(null) }
    var deleteStudent by remember { mutableStateOf<Student?>(null) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var expenseForStudent by remember { mutableStateOf<Student?>(null) }
    var deleteExpenseId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.section_education),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editStudent = null
                showStudentDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.students.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.no_students_yet),
                    hint = stringResource(R.string.no_students_hint),
                    icon = Icons.Filled.School
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.studentExpenses, key = { it.student.id }) { item ->
                        StudentCard(
                            item = item,
                            locale = uiState.locale,
                            currency = uiState.currency,
                            onEdit = {
                                editStudent = it.student
                                showStudentDialog = true
                            },
                            onDelete = { deleteStudent = it.student },
                            onAddExpense = {
                                expenseForStudent = it.student
                                showExpenseDialog = true
                            },
                            onDeleteExpense = { deleteExpenseId = it }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showStudentDialog) {
        StudentFormDialog(
            initial = editStudent,
            onDismiss = {
                showStudentDialog = false
                editStudent = null
            },
            onSave = { id, name, age, nationalId, stage ->
                viewModel.upsertStudent(id, name, age, nationalId, stage) { error ->
                    if (error == null) {
                        showStudentDialog = false
                        editStudent = null
                    }
                }
            }
        )
    }

    if (showExpenseDialog && expenseForStudent != null) {
        StudentExpenseDialog(
            student = expenseForStudent!!,
            onDismiss = {
                showExpenseDialog = false
                expenseForStudent = null
            },
            onSave = { title, amount, date, subCat, notes ->
                viewModel.upsertStudentExpense(
                    id = null,
                    studentId = expenseForStudent!!.id,
                    title = title,
                    amount = amount,
                    date = date,
                    subCategory = subCat,
                    notes = notes
                )
                showExpenseDialog = false
                expenseForStudent = null
            }
        )
    }

    if (deleteStudent != null) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_message),
            confirmLabel = stringResource(R.string.confirm_delete_yes),
            cancelLabel = stringResource(R.string.confirm_delete_no),
            onConfirm = {
                viewModel.deleteStudent(deleteStudent!!.id)
                deleteStudent = null
            },
            onDismiss = { deleteStudent = null }
        )
    }
    if (deleteExpenseId != null) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_message),
            confirmLabel = stringResource(R.string.confirm_delete_yes),
            cancelLabel = stringResource(R.string.confirm_delete_no),
            onConfirm = {
                viewModel.deleteStudentExpense(deleteExpenseId!!)
                deleteExpenseId = null
            },
            onDismiss = { deleteExpenseId = null }
        )
    }
}

@Composable
private fun StudentCard(
    item: StudentExpenseItem,
    locale: com.aistudio.smartcostpro.app.util.AppLocale,
    currency: com.aistudio.smartcostpro.app.util.AppCurrency,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddExpense: () -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.student.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${stageLabel(item.student.stage)} · ${item.student.age} ${stringResource(R.string.field_age_short)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.student.nationalId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = CurrencyFormatter.format(item.total, currency, locale),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = null) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = null) }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (item.expenses.isNotEmpty()) {
                item.expenses.forEach { e ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                e.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${DateFormatter.formatForDisplay(e.date, locale)} · ${subCategoryLabel(e.category)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = CurrencyFormatter.format(e.amount, currency, locale),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { onDeleteExpense(e.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Button(
                onClick = onAddExpense,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.add_expense_for_student))
            }
        }
    }
}

@Composable
private fun stageLabel(stage: EducationStage): String = when (stage) {
    EducationStage.Kindergarten -> stringResource(R.string.stage_kindergarten)
    EducationStage.Primary -> stringResource(R.string.stage_primary)
    EducationStage.Prep -> stringResource(R.string.stage_prep)
    EducationStage.Secondary -> stringResource(R.string.stage_secondary)
    EducationStage.University -> stringResource(R.string.stage_university)
}

@Composable
private fun subCategoryLabel(code: String): String = when (code) {
    "lessons" -> stringResource(R.string.cat_edu_lessons)
    "personal" -> stringResource(R.string.cat_edu_allowance)
    "transport" -> stringResource(R.string.cat_personal_transport)
    "school" -> stringResource(R.string.cat_edu_school_fees)
    "books" -> stringResource(R.string.cat_edu_books)
    else -> code
}

@Composable
private fun StudentFormDialog(
    initial: Student? = null,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, age: Int, nationalId: String, stage: EducationStage) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var ageStr by remember { mutableStateOf(initial?.age?.toString() ?: "") }
    var nationalId by remember { mutableStateOf(initial?.nationalId ?: "") }
    var stageIndex by remember { mutableStateOf(EducationStage.entries.indexOf(initial?.stage ?: EducationStage.Prep)) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var ageError by remember { mutableStateOf<String?>(null) }
    var idError by remember { mutableStateOf<String?>(null) }

    val stages = EducationStage.entries.map { stageLabelFor(it) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initial == null) stringResource(R.string.add_student)
                        else stringResource(R.string.edit_student),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = name, onValueChange = { name = it; nameError = null },
                    label = { Text(stringResource(R.string.field_name)) },
                    singleLine = true,
                    isError = nameError != null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ageStr, onValueChange = { ageStr = it; ageError = null },
                    label = { Text(stringResource(R.string.field_age)) },
                    singleLine = true,
                    isError = ageError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nationalId, onValueChange = { nationalId = it; idError = null },
                    label = { Text("${stringResource(R.string.field_national_id)} (${Constants.NATIONAL_ID_LENGTH})") },
                    singleLine = true,
                    isError = idError != null,
                    supportingText = if (idError != null) ({ Text(idError!!) }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownField(
                    label = stringResource(R.string.field_stage),
                    options = stages,
                    selectedIndex = stageIndex,
                    onSelected = { stageIndex = it }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.form_cancel)) }
                    Spacer(modifier = androidx.compose.foundation.layout.width(8.dp))
                    Button(onClick = {
                        onSave(
                            initial?.id,
                            name,
                            ageStr.toIntOrNull() ?: 0,
                            nationalId,
                            EducationStage.entries[stageIndex]
                        )
                    }) { Text(stringResource(R.string.form_save)) }
                }
            }
        }
    }
}

@Composable
private fun stageLabelFor(stage: EducationStage): String = when (stage) {
    EducationStage.Kindergarten -> stringResource(R.string.stage_kindergarten)
    EducationStage.Primary -> stringResource(R.string.stage_primary)
    EducationStage.Prep -> stringResource(R.string.stage_prep)
    EducationStage.Secondary -> stringResource(R.string.stage_secondary)
    EducationStage.University -> stringResource(R.string.stage_university)
}

@Composable
private fun StudentExpenseDialog(
    student: Student,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, date: String, subCategory: String, notes: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(DateFormatter.todayIso()) }
    var subCatIndex by remember { mutableStateOf(0) }
    var notes by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }
    val subCats = listOf(
        stringResource(R.string.cat_edu_lessons),
        stringResource(R.string.cat_edu_allowance),
        stringResource(R.string.cat_personal_transport),
        stringResource(R.string.cat_edu_school_fees),
        stringResource(R.string.cat_edu_books)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${stringResource(R.string.add_expense_for_student)} — ${student.name}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = title, onValueChange = { title = it; titleError = null },
                    label = { Text(stringResource(R.string.field_title)) },
                    singleLine = true,
                    isError = titleError != null,
                    modifier = Modifier.fillMaxWidth()
                )

                AmountTextField(
                    value = amountStr, onValueChange = { amountStr = it; amountError = null },
                    label = stringResource(R.string.form_amount_label),
                    error = amountError
                )

                com.aistudio.smartcostpro.app.ui.forms.DatePickerField(
                    value = date, onValueChange = { date = it },
                    label = stringResource(R.string.field_date),
                    locale = com.aistudio.smartcostpro.app.util.AppLocale.Arabic
                )

                DropdownField(
                    label = stringResource(R.string.form_category_label),
                    options = subCats,
                    selectedIndex = subCatIndex,
                    onSelected = { subCatIndex = it }
                )

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.form_notes_label)) },
                    minLines = 2, maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.form_cancel)) }
                    Spacer(modifier = androidx.compose.foundation.layout.width(8.dp))
                    Button(onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (title.isBlank()) { titleError = "TITLE_REQUIRED"; return@Button }
                        if (amount <= 0) { amountError = "AMOUNT_INVALID"; return@Button }
                        val subCode = listOf("lessons", "personal", "transport", "school", "books")[subCatIndex]
                        onSave(title, amount, date, subCode, notes.ifBlank { null })
                    }) { Text(stringResource(R.string.form_save)) }
                }
            }
        }
    }
}
