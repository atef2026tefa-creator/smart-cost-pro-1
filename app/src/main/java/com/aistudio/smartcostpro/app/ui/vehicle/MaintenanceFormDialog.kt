package com.aistudio.smartcostpro.app.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.smartcostpro.app.R
import com.aistudio.smartcostpro.app.domain.model.VehicleMaintenance
import com.aistudio.smartcostpro.app.ui.components.AmountTextField
import com.aistudio.smartcostpro.app.ui.forms.DatePickerField
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.DateFormatter

/**
 * Maintenance record add / edit dialog.
 *
 * Per the spec §8.2:
 *  - `total = supplyPrice + laborPrice`
 *  - The spec warns this total must NOT be double-counted in the dashboard's
 *    expense totals — only the parent [Expense].amount goes into the unified total.
 */
@Composable
fun MaintenanceFormDialog(
    initial: VehicleMaintenance? = null,
    onDismiss: () -> Unit,
    onSave: (VehicleMaintenance) -> Unit
) {
    val locale = AppLocale.Arabic

    var maintType by remember { mutableStateOf(initial?.maintenanceType ?: "") }
    var supplyName by remember { mutableStateOf(initial?.supplyName ?: "") }
    var supplyPriceStr by remember { mutableStateOf(initial?.supplyPrice?.toString()?.let { trimZero(it) } ?: "") }
    var laborDesc by remember { mutableStateOf(initial?.laborDescription ?: "") }
    var laborPriceStr by remember { mutableStateOf(initial?.laborPrice?.toString()?.let { trimZero(it) } ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: DateFormatter.todayIso()) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    val totalPreview: String = remember(supplyPriceStr, laborPriceStr) {
        val s = supplyPriceStr.toDoubleOrNull() ?: 0.0
        val l = laborPriceStr.toDoubleOrNull() ?: 0.0
        String.format("%.2f", s + l)
    }

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
                    text = if (initial == null) stringResource(R.string.action_add)
                        else stringResource(R.string.action_edit) + " — " + stringResource(R.string.expense_type_maintenance),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = maintType,
                    onValueChange = { maintType = it },
                    label = { Text(stringResource(R.string.field_type)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = supplyName,
                    onValueChange = { supplyName = it },
                    label = { Text(stringResource(R.string.field_supply_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                AmountTextField(
                    value = supplyPriceStr,
                    onValueChange = { supplyPriceStr = it },
                    label = stringResource(R.string.field_supply_price)
                )

                OutlinedTextField(
                    value = laborDesc,
                    onValueChange = { laborDesc = it },
                    label = { Text(stringResource(R.string.field_labor_desc)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                AmountTextField(
                    value = laborPriceStr,
                    onValueChange = { laborPriceStr = it },
                    label = stringResource(R.string.field_labor_price)
                )

                DatePickerField(
                    value = date,
                    onValueChange = { date = it },
                    label = stringResource(R.string.field_date),
                    locale = locale
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.form_notes_label)) },
                    minLines = 2, maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "${stringResource(R.string.total_label)}: $totalPreview",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.form_cancel)) }
                    Spacer(modifier = androidx.compose.foundation.layout.width(8.dp))
                    Button(onClick = {
                        val record = VehicleMaintenance(
                            id = initial?.id ?: "",
                            maintenanceType = maintType.ifBlank { null },
                            supplyName = supplyName.ifBlank { null },
                            supplyPrice = supplyPriceStr.toDoubleOrNull() ?: 0.0,
                            laborDescription = laborDesc.ifBlank { null },
                            laborPrice = laborPriceStr.toDoubleOrNull() ?: 0.0,
                            date = date,
                            total = (supplyPriceStr.toDoubleOrNull() ?: 0.0) +
                                (laborPriceStr.toDoubleOrNull() ?: 0.0),
                            notes = notes.ifBlank { null },
                            createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(record)
                    }) { Text(stringResource(R.string.form_save)) }
                }
            }
        }
    }
}

private fun trimZero(s: String): String =
    if (s.endsWith(".0")) s.dropLast(2) else s
