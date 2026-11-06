package com.aistudio.smartcostpro.app.ui.car_income

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalTaxi
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.smartcostpro.app.R
import com.aistudio.smartcostpro.app.domain.model.CarTrip
import com.aistudio.smartcostpro.app.domain.model.TripType
import com.aistudio.smartcostpro.app.ui.components.AmountTextField
import com.aistudio.smartcostpro.app.ui.components.ConfirmDialog
import com.aistudio.smartcostpro.app.ui.components.EmptyState
import com.aistudio.smartcostpro.app.ui.components.MonthSelector
import com.aistudio.smartcostpro.app.ui.forms.DropdownField
import com.aistudio.smartcostpro.app.util.CurrencyFormatter
import com.aistudio.smartcostpro.app.util.DateFormatter

/**
 * Car income screen.
 *
 * Per the spec §13:
 *  - Statistics: todayTotal, weekTotal, monthTotal, totalIncome, count, avg, min, max
 *  - Trip types: All / Cash / Uber / DiDi / inDrive / Private
 *  - **Car trip income is NOT auto-merged into the dashboard's monthly income** (spec §13.3)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarIncomeScreen(
    viewModel: CarIncomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showTripDialog by remember { mutableStateOf(false) }
    var editTrip by remember { mutableStateOf<CarTrip?>(null) }
    var deleteTrip by remember { mutableStateOf<CarTrip?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.section_car_income),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editTrip = null
                showTripDialog = true
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
                .verticalScroll(rememberScrollState()),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MonthSelector(
                monthIso = uiState.month,
                locale = uiState.locale,
                onPrevious = { viewModel.setMonth(shiftMonth(uiState.month, -1)) },
                onNext = { viewModel.setMonth(shiftMonth(uiState.month, +1)) }
            )

            // Stats grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(stringResource(R.string.stat_today), CurrencyFormatter.format(uiState.statistics.todayTotal, uiState.currency, uiState.locale), Modifier.weight(1f))
                StatCard(stringResource(R.string.stat_week), CurrencyFormatter.format(uiState.statistics.weekTotal, uiState.currency, uiState.locale), Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(stringResource(R.string.stat_month), CurrencyFormatter.format(uiState.statistics.monthTotal, uiState.currency, uiState.locale), Modifier.weight(1f))
                StatCard(stringResource(R.string.stat_total), CurrencyFormatter.format(uiState.statistics.totalIncome, uiState.currency, uiState.locale), Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(stringResource(R.string.stat_count), uiState.statistics.tripCount.toString(), Modifier.weight(1f))
                StatCard(stringResource(R.string.stat_avg), CurrencyFormatter.format(uiState.statistics.avgFare, uiState.currency, uiState.locale), Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(stringResource(R.string.stat_min), CurrencyFormatter.format(uiState.statistics.minFare, uiState.currency, uiState.locale), Modifier.weight(1f))
                StatCard(stringResource(R.string.stat_max), CurrencyFormatter.format(uiState.statistics.maxFare, uiState.currency, uiState.locale), Modifier.weight(1f))
            }

            Text(
                text = stringResource(R.string.recent_trips),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (uiState.trips.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.no_trips_yet),
                    hint = stringResource(R.string.no_trips_hint),
                    icon = Icons.Filled.LocalTaxi
                )
            } else {
                uiState.trips.take(50).forEach { trip ->
                    TripRow(
                        trip = trip,
                        locale = uiState.locale,
                        currency = uiState.currency,
                        onEdit = { editTrip = it },
                        onDelete = { deleteTrip = it }
                    )
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showTripDialog) {
        TripFormDialog(
            initial = editTrip,
            onDismiss = {
                showTripDialog = false
                editTrip = null
            },
            onSave = {
                viewModel.upsert(it)
                showTripDialog = false
                editTrip = null
            }
        )
    }

    if (deleteTrip != null) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_message),
            confirmLabel = stringResource(R.string.confirm_delete_yes),
            cancelLabel = stringResource(R.string.confirm_delete_no),
            onConfirm = {
                viewModel.delete(deleteTrip!!.id)
                deleteTrip = null
            },
            onDismiss = { deleteTrip = null }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TripRow(
    trip: CarTrip,
    locale: com.aistudio.smartcostpro.app.util.AppLocale,
    currency: com.aistudio.smartcostpro.app.util.AppCurrency,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(trip.tripType.code, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(DateFormatter.formatForDisplay(trip.date, locale), style = MaterialTheme.typography.bodyMedium)
                trip.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text(
                text = CurrencyFormatter.format(trip.amount, currency, locale),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = null) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = null) }
        }
    }
}

@Composable
private fun TripFormDialog(
    initial: CarTrip? = null,
    onDismiss: () -> Unit,
    onSave: (CarTrip) -> Unit
) {
    var amountStr by remember { mutableStateOf(initial?.amount?.toString()?.let { trimZero(it) } ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: DateFormatter.todayIso()) }
    var typeIndex by remember { mutableStateOf(TripType.entries.indexOf(initial?.tripType ?: TripType.Cash).coerceAtLeast(0)) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    val types = TripType.entries.map { it.code }

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
                    text = if (initial == null) stringResource(R.string.add_trip)
                        else stringResource(R.string.edit_trip),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                AmountTextField(
                    value = amountStr, onValueChange = { amountStr = it },
                    label = stringResource(R.string.form_amount_label)
                )

                com.aistudio.smartcostpro.app.ui.forms.DatePickerField(
                    value = date, onValueChange = { date = it },
                    label = stringResource(R.string.field_date),
                    locale = com.aistudio.smartcostpro.app.util.AppLocale.Arabic
                )

                DropdownField(
                    label = stringResource(R.string.field_trip_type),
                    options = types,
                    selectedIndex = typeIndex,
                    onSelected = { typeIndex = it }
                )

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.form_notes_label)) },
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
                        if (amount <= 0) return@Button
                        val trip = CarTrip(
                            id = initial?.id ?: "",
                            date = date,
                            amount = amount,
                            tripType = TripType.entries[typeIndex],
                            notes = notes.ifBlank { null },
                            createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(trip)
                    }) { Text(stringResource(R.string.form_save)) }
                }
            }
        }
    }
}

private fun trimZero(s: String): String =
    if (s.endsWith(".0")) s.dropLast(2) else s

private fun shiftMonth(monthIso: String, delta: Int): String {
    val ym = DateFormatter.yearMonthFromIso(monthIso) ?: return DateFormatter.currentMonthIso()
    val new = ym.plusMonths(delta.toLong())
    return new.format(DateFormatter.isoMonthFormatter)
}
