package com.aistudio.smartcostpro.app.ui.sections

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PeopleAlt
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
import androidx.compose.ui.Alignment
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
import com.aistudio.smartcostpro.app.domain.model.Association
import com.aistudio.smartcostpro.app.ui.components.AmountTextField
import com.aistudio.smartcostpro.app.ui.components.ConfirmDialog
import com.aistudio.smartcostpro.app.ui.components.EmptyState
import com.aistudio.smartcostpro.app.ui.forms.DatePickerField
import com.aistudio.smartcostpro.app.util.CurrencyFormatter
import com.aistudio.smartcostpro.app.util.DateFormatter
import java.util.UUID

/**
 * Associations screen.
 *
 * Per the spec §7.2:
 *  - List of associations.
 *  - Add / Edit / Delete.
 *  - Validation per [AssociationCalculator.validate].
 *  - `installment = value / memberCount` is auto-calculated as the user types.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssociationsScreen(
    viewModel: AssociationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Association?>(null) }
    var deleteTarget by remember { mutableStateOf<Association?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.screen_associations_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.screen_associations_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.items.isEmpty() && !uiState.isLoading) {
                EmptyState(
                    title = stringResource(R.string.no_associations_yet),
                    hint = stringResource(R.string.no_associations_hint),
                    icon = Icons.Filled.PeopleAlt
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.items, key = { it.id }) { association ->
                        AssociationRow(
                            association = association,
                            locale = uiState.locale,
                            currency = uiState.currency,
                            onEdit = { editTarget = association },
                            onDelete = { deleteTarget = association }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AssociationFormDialog(
            onDismiss = { showAddDialog = false },
            onSave = { viewModel.upsert(it); showAddDialog = false }
        )
    }
    if (editTarget != null) {
        AssociationFormDialog(
            initial = editTarget,
            onDismiss = { editTarget = null },
            onSave = { viewModel.upsert(it); editTarget = null }
        )
    }
    if (deleteTarget != null) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_message),
            confirmLabel = stringResource(R.string.confirm_delete_yes),
            cancelLabel = stringResource(R.string.confirm_delete_no),
            onConfirm = {
                viewModel.delete(deleteTarget!!.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun AssociationRow(
    association: Association,
    locale: com.aistudio.smartcostpro.app.util.AppLocale,
    currency: com.aistudio.smartcostpro.app.util.AppCurrency,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = association.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LabelValue(stringResource(R.string.form_association_value),
                    CurrencyFormatter.format(association.value, currency, locale))
                LabelValue(stringResource(R.string.form_association_members),
                    association.memberCount.toString())
                LabelValue(stringResource(R.string.form_association_installment),
                    CurrencyFormatter.format(association.installment, currency, locale))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LabelValue(stringResource(R.string.form_association_role_number),
                    association.roleNumber.toString())
                LabelValue(stringResource(R.string.form_association_start_date),
                    DateFormatter.formatForDisplay(association.startDate, locale))
                LabelValue(stringResource(R.string.form_association_payout_date),
                    DateFormatter.formatForDisplay(association.payoutDate, locale))
            }
            if (!association.notes.isNullOrBlank()) {
                Text(
                    text = association.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Add / Edit association dialog.
 *
 * The `installment` field is auto-computed as the user types the value
 * and member count — it's shown as a read-only field with a hint.
 */
@Composable
private fun AssociationFormDialog(
    initial: Association? = null,
    onDismiss: () -> Unit,
    onSave: (Association) -> Unit
) {
    val locale = com.aistudio.smartcostpro.app.util.AppLocale.Arabic

    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var valueStr by remember { mutableStateOf(initial?.value?.toString()?.let { it } ?: "") }
    var membersStr by remember { mutableStateOf(initial?.memberCount?.toString().orEmpty()) }
    var roleStr by remember { mutableStateOf(initial?.roleNumber?.toString().orEmpty()) }
    var startDate by remember { mutableStateOf(initial?.startDate ?: DateFormatter.todayIso()) }
    var payoutDate by remember { mutableStateOf(initial?.payoutDate ?: DateFormatter.todayIso()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var valueError by remember { mutableStateOf<String?>(null) }
    var membersError by remember { mutableStateOf<String?>(null) }
    var roleError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }

    val installment: String = remember(valueStr, membersStr) {
        val v = valueStr.toDoubleOrNull() ?: 0.0
        val m = membersStr.toIntOrNull() ?: 0
        if (v > 0 && m > 0) {
            // Per the spec, no explicit rounding is applied at storage time.
            String.format("%.2f", v / m)
        } else ""
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initial == null) stringResource(R.string.action_add)
                        else stringResource(R.string.action_edit),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text(stringResource(R.string.form_association_name)) },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = if (nameError != null) ({ Text(nameError!!) }) else null,
                    modifier = Modifier.fillMaxWidth()
                )

                AmountTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it; valueError = null },
                    label = stringResource(R.string.form_association_value),
                    error = valueError
                )

                OutlinedTextField(
                    value = membersStr,
                    onValueChange = { membersStr = it; membersError = null },
                    label = { Text(stringResource(R.string.form_association_members)) },
                    singleLine = true,
                    isError = membersError != null,
                    supportingText = if (membersError != null) ({ Text(membersError!!) }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = roleStr,
                    onValueChange = { roleStr = it; roleError = null },
                    label = { Text(stringResource(R.string.form_association_role_number)) },
                    singleLine = true,
                    isError = roleError != null,
                    supportingText = if (roleError != null) ({ Text(roleError!!) }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = installment,
                    onValueChange = { /* read-only */ },
                    label = { Text(stringResource(R.string.form_association_installment)) },
                    readOnly = true,
                    singleLine = true,
                    supportingText = { Text(stringResource(R.string.form_association_installment_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    value = startDate,
                    onValueChange = { startDate = it; dateError = null },
                    label = stringResource(R.string.form_association_start_date),
                    locale = locale,
                    error = dateError
                )

                DatePickerField(
                    value = payoutDate,
                    onValueChange = { payoutDate = it; dateError = null },
                    label = stringResource(R.string.form_association_payout_date),
                    locale = locale,
                    error = dateError
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.form_association_notes)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.form_cancel)) }
                    Spacer(modifier = androidx.compose.foundation.layout.width(8.dp))
                    Button(onClick = {
                        // Validate
                        val value = valueStr.toDoubleOrNull() ?: 0.0
                        val members = membersStr.toIntOrNull() ?: 0
                        val role = roleStr.toIntOrNull() ?: 0

                        val err = AssociationCalculator.validate(
                            name = name,
                            value = value,
                            memberCount = members,
                            roleNumber = role,
                            startDate = startDate,
                            payoutDate = payoutDate
                        )
                        if (err != null) {
                            when (err) {
                                AssociationCalculator.ValidationError.NameRequired ->
                                    nameError = "Name required"
                                AssociationCalculator.ValidationError.ValueInvalid ->
                                    valueError = "Value must be > 0"
                                AssociationCalculator.ValidationError.MemberCountInvalid ->
                                    membersError = "Members must be ≥ 1"
                                AssociationCalculator.ValidationError.RoleNumberInvalid ->
                                    roleError = "Role must be 1…members"
                                AssociationCalculator.ValidationError.StartDateRequired,
                                AssociationCalculator.ValidationError.PayoutDateRequired ->
                                    dateError = "Dates required"
                            }
                            return@Button
                        }
                        val now = System.currentTimeMillis()
                        val association = Association(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            value = value,
                            memberCount = members,
                            installment = value / members,
                            roleNumber = role,
                            payoutDate = payoutDate,
                            startDate = startDate,
                            notes = notes.ifBlank { null },
                            createdAt = initial?.createdAt ?: now,
                            updatedAt = now
                        )
                        onSave(association)
                    }) { Text(stringResource(R.string.form_save)) }
                }
            }
        }
    }
}
