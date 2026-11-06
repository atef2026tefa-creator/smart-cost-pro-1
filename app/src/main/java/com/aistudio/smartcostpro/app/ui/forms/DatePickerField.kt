package com.aistudio.smartcostpro.app.ui.forms

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.DateFormatter
import java.util.Calendar

/**
 * Date picker field.
 *
 * Stage 2 uses the platform [DatePickerDialog] for parity with the Android
 * ecosystem; Stage 3 may swap to Material 3's `DatePicker` for a fully
 * Compose-native experience.
 *
 * The stored value is always ISO `YYYY-MM-DD`. The displayed value is
 * localized using [DateFormatter.formatForDisplay].
 *
 * @param value The current ISO date string (YYYY-MM-DD).
 * @param onValueChange Called with the new ISO date when the user picks one.
 * @param label The field label.
 * @param locale The active locale (for display formatting).
 * @param error Optional error message to show as supporting text.
 */
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    locale: AppLocale,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    if (value.isNotBlank()) {
        runCatching {
            val localDate = java.time.LocalDate.parse(value, DateFormatter.isoDateFormatter)
            calendar.set(localDate.year, localDate.monthValue - 1, localDate.dayOfMonth)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val display = if (value.isBlank()) "" else DateFormatter.formatForDisplay(value, locale, longStyle = false)

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = display,
            onValueChange = { /* read-only */ },
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            isError = error != null,
            supportingText = if (error != null) ({ Text(error) }) else null,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = {
                        showDatePicker(context, calendar, onValueChange)
                    }
                )
        )
    }
}

private fun showDatePicker(
    context: android.content.Context,
    calendar: Calendar,
    onDatePicked: (String) -> Unit
) {
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val localDate = java.time.LocalDate.of(year, month + 1, day)
            onDatePicked(localDate.format(DateFormatter.isoDateFormatter))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
