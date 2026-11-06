package com.aistudio.smartcostpro.app.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.aistudio.smartcostpro.app.domain.model.VehicleOil
import com.aistudio.smartcostpro.app.ui.components.AmountTextField
import com.aistudio.smartcostpro.app.ui.forms.DatePickerField
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.DateFormatter

/**
 * Oils / Filters record add / edit dialog.
 *
 * Per the spec §8.3 — must preserve ALL of:
 *  - serviceType, brand (default "Mobil"), productName
 *  - quantity, price, odometer
 *  - suggestedIntervalKm (default 5000), notes, date
 *
 * These details must NOT be reduced to a single expense amount — they
 * are kept in their own table (vehicle_oils).
 */
@Composable
fun OilFormDialog(
    initial: VehicleOil? = null,
    onDismiss: () -> Unit,
    onSave: (VehicleOil) -> Unit
) {
    val locale = AppLocale.Arabic

    var serviceType by remember { mutableStateOf(initial?.serviceType ?: "oil_change") }
    var brand by remember { mutableStateOf(initial?.brand ?: "Mobil") }
    var productName by remember { mutableStateOf(initial?.productName ?: "") }
    var quantityStr by remember { mutableStateOf(initial?.quantity?.toString()?.let { trimZero(it) } ?: "1") }
    var priceStr by remember { mutableStateOf(initial?.price?.toString()?.let { trimZero(it) } ?: "") }
    var odometerStr by remember { mutableStateOf(initial?.odometer?.toString()?.let { trimZero(it) } ?: "") }
    var intervalStr by remember { mutableStateOf(initial?.suggestedIntervalKm?.toString() ?: "5000") }
    var date by remember { mutableStateOf(initial?.date ?: DateFormatter.todayIso()) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

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
                        else stringResource(R.string.action_edit) + " — " + stringResource(R.string.expense_type_oil),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = serviceType,
                    onValueChange = { serviceType = it },
                    label = { Text(stringResource(R.string.field_service_type)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text(stringResource(R.string.field_brand)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text(stringResource(R.string.field_product_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                AmountTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = stringResource(R.string.field_quantity)
                )

                AmountTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = stringResource(R.string.field_price)
                )

                OutlinedTextField(
                    value = odometerStr,
                    onValueChange = { odometerStr = it },
                    label = { Text(stringResource(R.string.field_odometer)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = intervalStr,
                    onValueChange = { intervalStr = it },
                    label = { Text(stringResource(R.string.field_interval_km)) },
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
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.form_notes_label)) },
                    minLines = 2, maxLines = 4,
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
                        val record = VehicleOil(
                            id = initial?.id ?: "",
                            serviceType = serviceType.ifBlank { "oil_change" },
                            brand = brand.ifBlank { "Mobil" },
                            productName = productName.ifBlank { null },
                            quantity = quantityStr.toDoubleOrNull() ?: 1.0,
                            price = priceStr.toDoubleOrNull() ?: 0.0,
                            odometer = odometerStr.toDoubleOrNull(),
                            suggestedIntervalKm = intervalStr.toIntOrNull(),
                            notes = notes.ifBlank { null },
                            date = date,
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
