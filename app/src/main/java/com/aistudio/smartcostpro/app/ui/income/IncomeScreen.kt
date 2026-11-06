package com.aistudio.smartcostpro.app.ui.income

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.smartcostpro.app.R
import com.aistudio.smartcostpro.app.domain.model.BankCertificate
import com.aistudio.smartcostpro.app.domain.model.ProfitFrequency
import com.aistudio.smartcostpro.app.ui.components.AmountTextField
import com.aistudio.smartcostpro.app.ui.components.ConfirmDialog
import com.aistudio.smartcostpro.app.ui.components.MonthSelector
import com.aistudio.smartcostpro.app.ui.forms.DropdownField
import com.aistudio.smartcostpro.app.util.CurrencyFormatter
import com.aistudio.smartcostpro.app.util.DateFormatter

/**
 * Income & Certificates screen.
 *
 * Two parts:
 *  1. Monthly income editor (salary + bonuses + other income + other note)
 *     for the selected YYYY-MM month.
 *  2. Certificates list with the spec's "preview" of monthly profit per cert.
 *
 * Per the spec §11.5, the profit priority is:
 *   1. monthlyEquivalentProfit > 0 → use directly
 *   2. amount × annualRate / 100 (periodic divisor) → use
 *   3. profitAmount / periodicity → use
 *   4. otherwise 0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(
    viewModel: IncomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCertDialog by remember { mutableStateOf(false) }
    var editCert by remember { mutableStateOf<BankCertificate?>(null) }
    var deleteCertId by remember { mutableStateOf<String?>(null) }

    var salary by remember(uiState.month, uiState.monthlyIncome) {
        mutableStateOf(uiState.monthlyIncome?.salary?.toString()?.let { trimZero(it) } ?: "0")
    }
    var bonuses by remember(uiState.month, uiState.monthlyIncome) {
        mutableStateOf(uiState.monthlyIncome?.bonuses?.toString()?.let { trimZero(it) } ?: "0")
    }
    var otherIncome by remember(uiState.month, uiState.monthlyIncome) {
        mutableStateOf(uiState.monthlyIncome?.otherIncome?.toString()?.let { trimZero(it) } ?: "0")
    }
    var otherNote by remember(uiState.month, uiState.monthlyIncome) {
        mutableStateOf(uiState.monthlyIncome?.otherIncomeNote ?: "")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.section_income_certs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editCert = null
                showCertDialog = true
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Month selector
            MonthSelector(
                monthIso = uiState.month,
                locale = uiState.locale,
                onPrevious = { viewModel.setMonth(shiftMonth(uiState.month, -1)) },
                onNext = { viewModel.setMonth(shiftMonth(uiState.month, +1)) }
            )

            // Monthly income editor
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.section_monthly_income),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    AmountTextField(
                        value = salary,
                        onValueChange = { salary = it },
                        label = stringResource(R.string.field_salary)
                    )
                    AmountTextField(
                        value = bonuses,
                        onValueChange = { bonuses = it },
                        label = stringResource(R.string.field_bonuses)
                    )
                    AmountTextField(
                        value = otherIncome,
                        onValueChange = { otherIncome = it },
                        label = stringResource(R.string.field_other_income)
                    )
                    OutlinedTextField(
                        value = otherNote, onValueChange = { otherNote = it },
                        label = { Text(stringResource(R.string.field_other_income_note)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            viewModel.saveMonthlyIncome(
                                uiState.month,
                                salary.toDoubleOrNull() ?: 0.0,
                                bonuses.toDoubleOrNull() ?: 0.0,
                                otherIncome.toDoubleOrNull() ?: 0.0,
                                otherNote.ifBlank { null }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.form_save))
                    }
                }
            }

            // Certificates list
            Text(
                text = stringResource(R.string.section_bank_certificates),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (uiState.certificates.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_certificates_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                uiState.certificates.forEach { preview ->
                    CertificateCard(
                        preview = preview,
                        locale = uiState.locale,
                        currency = uiState.currency,
                        onEdit = {
                            editCert = preview.certificate
                            showCertDialog = true
                        },
                        onDelete = { deleteCertId = preview.certificate.id }
                    )
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showCertDialog) {
        CertificateFormDialog(
            initial = editCert,
            onDismiss = {
                showCertDialog = false
                editCert = null
            },
            onSave = {
                viewModel.upsertCertificate(it)
                showCertDialog = false
                editCert = null
            }
        )
    }

    if (deleteCertId != null) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_message),
            confirmLabel = stringResource(R.string.confirm_delete_yes),
            cancelLabel = stringResource(R.string.confirm_delete_no),
            onConfirm = {
                viewModel.deleteCertificate(deleteCertId!!)
                deleteCertId = null
            },
            onDismiss = { deleteCertId = null }
        )
    }
}

@Composable
private fun CertificateCard(
    preview: CertificatePreview,
    locale: com.aistudio.smartcostpro.app.util.AppLocale,
    currency: com.aistudio.smartcostpro.app.util.AppCurrency,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cert = preview.certificate
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(cert.bankName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = null) }
                    IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = null) }
                }
            }
            cert.certificateNumber?.let {
                Text(
                    text = "${stringResource(R.string.field_certificate_number)}: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${stringResource(R.string.field_amount)}: ${CurrencyFormatter.format(cert.amount, currency, locale)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${stringResource(R.string.field_annual_rate)}: ${cert.annualRate}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${stringResource(R.string.field_duration)}: ${cert.duration}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${stringResource(R.string.field_profit_frequency)}: ${freqLabel(cert.profitFrequency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${stringResource(R.string.field_monthly_profit_preview)}: ${CurrencyFormatter.format(preview.monthlyProfit, currency, locale)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun freqLabel(freq: ProfitFrequency): String = when (freq) {
    ProfitFrequency.Monthly -> stringResource(R.string.freq_monthly)
    ProfitFrequency.Quarterly -> stringResource(R.string.freq_quarterly)
    ProfitFrequency.SemiAnnual -> stringResource(R.string.freq_semi_annual)
    ProfitFrequency.Annual -> stringResource(R.string.freq_annual)
    ProfitFrequency.OnMaturity -> stringResource(R.string.freq_on_maturity)
}

@Composable
private fun CertificateFormDialog(
    initial: BankCertificate? = null,
    onDismiss: () -> Unit,
    onSave: (BankCertificate) -> Unit
) {
    var bankName by remember { mutableStateOf(initial?.bankName ?: "") }
    var certNumber by remember { mutableStateOf(initial?.certificateNumber ?: "") }
    var amountStr by remember { mutableStateOf(initial?.amount?.toString()?.let { trimZero(it) } ?: "") }
    var annualRateStr by remember { mutableStateOf(initial?.annualRate?.toString()?.let { trimZero(it) } ?: "") }
    var durationIndex by remember { mutableStateOf(0) }
    var freqIndex by remember { mutableStateOf(ProfitFrequency.entries.indexOf(initial?.profitFrequency ?: ProfitFrequency.Monthly)) }
    var issueDate by remember { mutableStateOf(initial?.issueDate ?: DateFormatter.todayIso()) }
    var maturityDate by remember { mutableStateOf(initial?.maturityDate ?: "") }
    var monthlyEquivalentStr by remember { mutableStateOf(initial?.monthlyEquivalentProfit?.toString()?.let { trimZero(it) } ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var bankError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var issueError by remember { mutableStateOf<String?>(null) }

    val durations = listOf("6 months", "1 year", "2 years", "3 years", "5 years")
    val frequencies = ProfitFrequency.entries.map { freqLabel(it) }

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
                    text = if (initial == null) stringResource(R.string.add_certificate)
                        else stringResource(R.string.edit_certificate),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = bankName, onValueChange = { bankName = it; bankError = null },
                    label = { Text(stringResource(R.string.field_bank_name)) },
                    singleLine = true,
                    isError = bankError != null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = certNumber, onValueChange = { certNumber = it },
                    label = { Text(stringResource(R.string.field_certificate_number)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                AmountTextField(
                    value = amountStr, onValueChange = { amountStr = it; amountError = null },
                    label = stringResource(R.string.field_amount),
                    error = amountError
                )

                AmountTextField(
                    value = annualRateStr, onValueChange = { annualRateStr = it },
                    label = stringResource(R.string.field_annual_rate)
                )

                DropdownField(
                    label = stringResource(R.string.field_duration),
                    options = durations,
                    selectedIndex = durationIndex,
                    onSelected = { durationIndex = it }
                )

                DropdownField(
                    label = stringResource(R.string.field_profit_frequency),
                    options = frequencies,
                    selectedIndex = freqIndex,
                    onSelected = { freqIndex = it }
                )

                com.aistudio.smartcostpro.app.ui.forms.DatePickerField(
                    value = issueDate, onValueChange = { issueDate = it; issueError = null },
                    label = stringResource(R.string.field_issue_date),
                    locale = com.aistudio.smartcostpro.app.util.AppLocale.Arabic,
                    error = issueError
                )

                com.aistudio.smartcostpro.app.ui.forms.DatePickerField(
                    value = maturityDate, onValueChange = { maturityDate = it },
                    label = stringResource(R.string.field_maturity_date),
                    locale = com.aistudio.smartcostpro.app.util.AppLocale.Arabic
                )

                AmountTextField(
                    value = monthlyEquivalentStr, onValueChange = { monthlyEquivalentStr = it },
                    label = stringResource(R.string.field_monthly_equivalent)
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
                        if (bankName.isBlank()) { bankError = "BANK_REQUIRED"; return@Button }
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount <= 0) { amountError = "AMOUNT_INVALID"; return@Button }
                        if (issueDate.isBlank()) { issueError = "DATE_REQUIRED"; return@Button }
                        val cert = BankCertificate(
                            id = initial?.id ?: "",
                            bankName = bankName.trim(),
                            certificateNumber = certNumber.ifBlank { null },
                            amount = amount,
                            annualRate = annualRateStr.toDoubleOrNull() ?: 0.0,
                            duration = durations[durationIndex],
                            profitFrequency = ProfitFrequency.entries[freqIndex],
                            issueDate = issueDate,
                            maturityDate = maturityDate.ifBlank { null },
                            monthlyEquivalentProfit = monthlyEquivalentStr.toDoubleOrNull() ?: 0.0,
                            notes = notes.ifBlank { null },
                            createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(cert)
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

private fun androidx.compose.foundation.layout.ColumnScope.contentPadding(p: PaddingValues): Modifier =
    Modifier.padding(p)
