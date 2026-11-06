package com.aistudio.smartcostpro.app.ui.current_account

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
import com.aistudio.smartcostpro.app.domain.model.CurrentAccountTransaction
import com.aistudio.smartcostpro.app.domain.model.CurrentAccountType
import com.aistudio.smartcostpro.app.ui.components.AmountTextField
import com.aistudio.smartcostpro.app.ui.components.ConfirmDialog
import com.aistudio.smartcostpro.app.ui.components.EmptyState
import com.aistudio.smartcostpro.app.ui.forms.DatePickerField
import com.aistudio.smartcostpro.app.util.CurrencyFormatter
import com.aistudio.smartcostpro.app.util.DateFormatter

/**
 * Current account screen.
 *
 * Per the spec §12:
 *  - Two types: deposit / withdrawal
 *  - The signedAmount = amount × type.sign
 *  - The dashboard's `currentAccountNet` sums signedAmounts for the active month
 *  - Sign applied exactly once (NOT twice)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentAccountScreen(
    viewModel: CurrentAccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showTxnDialog by remember { mutableStateOf(false) }
    var editTxn by remember { mutableStateOf<CurrentAccountTransaction?>(null) }
    var deleteTxn by remember { mutableStateOf<CurrentAccountTransaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.section_current_account),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editTxn = null
                showTxnDialog = true
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
            // Net summary card
            val net = uiState.transactions.sumOf { it.signedAmount }
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.field_current_account_net),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = CurrencyFormatter.format(net, uiState.currency, uiState.locale),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.transactions.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.no_transactions_yet),
                    hint = stringResource(R.string.no_transactions_hint),
                    icon = Icons.Filled.Add
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.transactions, key = { it.id }) { txn ->
                        TxnRow(
                            txn = txn,
                            locale = uiState.locale,
                            currency = uiState.currency,
                            onEdit = { editTxn = it },
                            onDelete = { deleteTxn = it }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showTxnDialog) {
        TxnFormDialog(
            initial = editTxn,
            onDismiss = {
                showTxnDialog = false
                editTxn = null
            },
            onSave = { id, type, desc, amount, date, notes ->
                viewModel.upsert(id, type, desc, amount, date, notes)
                showTxnDialog = false
                editTxn = null
            }
        )
    }

    if (deleteTxn != null) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_message),
            confirmLabel = stringResource(R.string.confirm_delete_yes),
            cancelLabel = stringResource(R.string.confirm_delete_no),
            onConfirm = {
                viewModel.delete(deleteTxn!!.id)
                deleteTxn = null
            },
            onDismiss = { deleteTxn = null }
        )
    }
}

@Composable
private fun TxnRow(
    txn: CurrentAccountTransaction,
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
                Text(
                    text = if (txn.type == CurrentAccountType.Deposit)
                        stringResource(R.string.deposit_label) else stringResource(R.string.withdrawal_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (txn.type == CurrentAccountType.Deposit)
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = txn.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = DateFormatter.formatForDisplay(txn.date, locale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = CurrencyFormatter.format(txn.signedAmount, currency, locale),
                style = MaterialTheme.typography.titleMedium,
                color = if (txn.type == CurrentAccountType.Deposit)
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = null) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = null) }
        }
    }
}

@Composable
private fun TxnFormDialog(
    initial: CurrentAccountTransaction? = null,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        type: CurrentAccountType,
        description: String,
        amount: Double,
        date: String,
        notes: String?
    ) -> Unit
) {
    var isDeposit by remember { mutableStateOf(initial?.type == CurrentAccountType.Deposit) }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var amountStr by remember { mutableStateOf(initial?.amount?.toString()?.let { trimZero(it) } ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: DateFormatter.todayIso()) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var descError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

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
                    text = if (initial == null) stringResource(R.string.add_transaction)
                        else stringResource(R.string.edit_transaction),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(true, false).forEach { isDep ->
                        val selected = isDeposit == isDep
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = {
                                Text(
                                    text = if (isDep) stringResource(R.string.deposit_label)
                                        else stringResource(R.string.withdrawal_label),
                                    color = if (selected)
                                        MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                        )
                        // Toggle button
                        Button(
                            onClick = { isDeposit = isDep },
                            modifier = Modifier.weight(1f),
                            enabled = !selected
                        ) {
                            Text(
                                if (isDep) stringResource(R.string.deposit_label)
                                else stringResource(R.string.withdrawal_label)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description, onValueChange = { description = it; descError = null },
                    label = { Text(stringResource(R.string.field_description)) },
                    singleLine = true,
                    isError = descError != null,
                    modifier = Modifier.fillMaxWidth()
                )

                AmountTextField(
                    value = amountStr, onValueChange = { amountStr = it; amountError = null },
                    label = stringResource(R.string.form_amount_label),
                    error = amountError
                )

                DatePickerField(
                    value = date, onValueChange = { date = it },
                    label = stringResource(R.string.field_date),
                    locale = com.aistudio.smartcostpro.app.util.AppLocale.Arabic
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
                        if (description.isBlank()) { descError = "DESC_REQUIRED"; return@Button }
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount <= 0) { amountError = "AMOUNT_INVALID"; return@Button }
                        val type = if (isDeposit) CurrentAccountType.Deposit else CurrentAccountType.Withdrawal
                        onSave(initial?.id, type, description, amount, date, notes.ifBlank { null })
                    }) { Text(stringResource(R.string.form_save)) }
                }
            }
        }
    }
}

private fun trimZero(s: String): String =
    if (s.endsWith(".0")) s.dropLast(2) else s
