package com.aistudio.smartcostpro.app.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.smartcostpro.app.R
import com.aistudio.smartcostpro.app.domain.model.VehicleFuel
import com.aistudio.smartcostpro.app.ui.components.AmountTextField
import com.aistudio.smartcostpro.app.ui.forms.DatePickerField
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.DateFormatter

/**
 * Fuel record add / edit dialog.
 *
 * Per the spec §8.1:
 *  - `cost = quantity × unitPrice`
 *  - `quantity` must be > 0
 *  - The form supports the spec's fuel types (gasoline 80/92/95, diesel, gas, electric home/AC/DC).
 */
@Composable
fun FuelFormDialog(
    initial: VehicleFuel? = null,
    onDismiss: () -> Unit,
    onSave: (VehicleFuel) -> Unit
) {
    val locale = AppLocale.Arabic

    var fuelType by remember { mutableStateOf(initial?.fuelType ?: "gasoline_92") }
    var quantityStr by remember { mutableStateOf(initial?.quantity?.toString()?.let { trimZero(it) } ?: "") }
    var unitPriceStr by remember { mutableStateOf(initial?.unitPrice?.toString()?.let { trimZero(it) } ?: "") }
    var odometerStr by remember { mutableStateOf(initial?.odometer?.toString()?.let { trimZero(it) } ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: DateFormatter.todayIso()) }
    var time by remember { mutableStateOf(initial?.time ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var qtyError by remember { mutableStateOf<String?>(null) }

    val costPreview: String = remember(quantityStr, unitPriceStr) {
        val q = quantityStr.toDoubleOrNull() ?: 0.0
        val p = unitPriceStr.toDoubleOrNull() ?: 0.0
        if (q > 0 && p > 0) String.format("%.2f", q * p) else "—"
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
                        else stringResource(R.string.action_edit) + " — " + stringResource(R.string.expense_type_fuel),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = fuelType,
                    onValueChange = { fuelType = it },
                    label = { Text(stringResource(R.string.field_type)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                AmountTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it; qtyError = null },
                    label = stringResource(R.string.field_quantity),
                    error = qtyError
                )

                AmountTextField(
                    value = unitPriceStr,
                    onValueChange = { unitPriceStr = it },
                    label = stringResource(R.string.field_unit_price)
                )

                OutlinedTextField(
                    value = odometerStr,
                    onValueChange = { odometerStr = it },
                    label = { Text(stringResource(R.string.field_odometer)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    value = date,
                    onValueChange = { date = it },
                    label = stringResource(R.string.field_date),
                    locale = locale
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text(stringResource(R.string.field_time_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.form_notes_label)) },
                    minLines = 2, maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // Cost preview
                Text(
                    text = "${stringResource(R.string.total_label)}: $costPreview",
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
                        val q = quantityStr.toDoubleOrNull() ?: 0.0
                        if (q <= 0) { qtyError = "QTY_INVALID"; return@Button }
                        val p = unitPriceStr.toDoubleOrNull() ?: 0.0
                        val record = VehicleFuel(
                            id = initial?.id ?: "",
                            fuelType = fuelType.ifBlank { "gasoline_92" },
                            quantity = q,
                            unitPrice = p,
                            odometer = odometerStr.toDoubleOrNull(),
                            date = date,
                            time = time.ifBlank { null },
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
