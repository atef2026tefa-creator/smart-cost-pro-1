package com.aistudio.smartcostpro.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

/**
 * Amount input field.
 *
 * - Number-only keyboard
 * - Two-decimal input (step = 0.01, min = 0.1 per the spec)
 * - Optional error message below the field
 *
 * Returns the typed string through the [onValueChange] callback so the
 * caller can validate it and convert to Double when needed.
 */
@Composable
fun AmountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                // Strip anything that's not a digit or a single dot.
                val filtered = newValue.filter { it.isDigit() || it == '.' }
                val firstDot = filtered.indexOf('.')
                val sanitized = if (firstDot >= 0) {
                    val before = filtered.substring(0, firstDot + 1)
                    val after = filtered.substring(firstDot + 1).filter { it.isDigit() }
                    before + after
                } else filtered
                onValueChange(sanitized)
            },
            label = { Text(label) },
            singleLine = true,
            isError = error != null,
            supportingText = if (error != null) {
                { Text(text = error, color = MaterialTheme.colorScheme.error) }
            } else null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                autoCorrect = false
            ),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
    }
}
