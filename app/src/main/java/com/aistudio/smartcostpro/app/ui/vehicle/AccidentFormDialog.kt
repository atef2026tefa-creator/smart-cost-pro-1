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
import com.aistudio.smartcostpro.app.domain.model.VehicleAccident
import com.aistudio.smartcostpro.app.ui.components.AmountTextField
import com.aistudio.smartcostpro.app.ui.forms.DatePickerField
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.DateFormatter

/**
 * Vehicle accident record add / edit dialog.
 *
 * Per the spec §8.4:
 *  - `estimatedDamage` is an *estimate* — NOT a paid expense.
 *  - It must NEVER be added to vehicle / expense totals automatically.
 *  - All fields (title, photoUrl, date, time, location, estimatedDamage, notes)
 *    must be preserved.
 */
@Composable
fun AccidentFormDialog(
    initial: VehicleAccident? = null,
    onDismiss: () -> Unit,
    onSave: (VehicleAccident) -> Unit
) {
    val locale = AppLocale.Arabic

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var vehicleId by remember { mutableStateOf(initial?.vehicleId ?: "") }
    var photoUrl by remember { mutableStateOf(initial?.photoUrl ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: DateFormatter.todayIso()) }
    var time by remember { mutableStateOf(initial?.time ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var estimatedDamageStr by remember { mutableStateOf(initial?.estimatedDamage?.toString() ?: "") }
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
                        else stringResource(R.string.action_edit) + " — " + stringResource(R.string.section_accidents),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.field_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = vehicleId,
                    onValueChange = { vehicleId = it },
                    label = { Text(stringResource(R.string.field_vehicle_id_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(stringResource(R.string.field_location)) },
                    singleLine = true,
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

                AmountTextField(
                    value = estimatedDamageStr,
                    onValueChange = { estimatedDamageStr = it },
                    label = stringResource(R.string.field_estimated_damage)
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
                        val record = VehicleAccident(
                            id = initial?.id ?: "",
                            vehicleId = vehicleId.ifBlank { null },
                            title = title,
                            photoUrl = photoUrl.ifBlank { null },
                            date = date,
                            time = time.ifBlank { null },
                            location = location.ifBlank { null },
                            estimatedDamage = estimatedDamageStr.toDoubleOrNull(),
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
