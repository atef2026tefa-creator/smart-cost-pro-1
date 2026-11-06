package com.aistudio.smartcostpro.app.ui.reports

import android.content.Intent
import androidx.core.content.FileProvider
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.smartcostpro.app.R
import com.aistudio.smartcostpro.app.domain.calculations.ReportCalculator
import com.aistudio.smartcostpro.app.export.CsvExporter
import com.aistudio.smartcostpro.app.export.ExportFiles
import com.aistudio.smartcostpro.app.export.HtmlExporter
import com.aistudio.smartcostpro.app.ui.components.EmptyState
import com.aistudio.smartcostpro.app.ui.forms.DatePickerField
import com.aistudio.smartcostpro.app.ui.forms.DropdownField
import com.aistudio.smartcostpro.app.ui.forms.localizedCategoryLabel
import com.aistudio.smartcostpro.app.ui.forms.localizedExpenseType
import com.aistudio.smartcostpro.app.ui.forms.localizedPaymentMethod
import com.aistudio.smartcostpro.app.util.CurrencyFormatter
import com.aistudio.smartcostpro.app.util.DateFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reports screen — day / week / month / range + filter + CSV/HTML export.
 *
 * Per the spec §16, the rows used for preview AND export are the SAME list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.section_reports),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    if (uiState.rows.isNotEmpty()) {
                        IconButton(onClick = {
                            val csv = CsvExporter.writeRows(
                                headers = listOf("id", "date", "title", "amount", "type", "category", "payment"),
                                rows = uiState.rows.map { row ->
                                    listOf(row.id, row.date, row.title, row.amount.toString(),
                                        row.type?.code ?: "", row.category, row.paymentMethod ?: "")
                                }
                            )
                            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val file = ExportFiles.writeTextFile(context, "report_${ts}.csv", csv)
                            shareFile(context, file, "text/csv")
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = null)
                        }
                        IconButton(onClick = {
                            val html = HtmlExporter.expensesToHtml(
                                expenses = uiState.rows.map { row ->
                                    com.aistudio.smartcostpro.app.domain.model.Expense(
                                        id = row.id,
                                        title = row.title,
                                        amount = row.amount,
                                        type = row.type ?: com.aistudio.smartcostpro.app.domain.model.ExpenseType.House,
                                        category = row.category,
                                        date = row.date,
                                        paymentMethod = row.paymentMethod?.let {
                                            com.aistudio.smartcostpro.app.domain.model.PaymentMethod.fromCode(it)
                                                ?: com.aistudio.smartcostpro.app.domain.model.PaymentMethod.Cash
                                        } ?: com.aistudio.smartcostpro.app.domain.model.PaymentMethod.Cash,
                                        notes = null,
                                        detailsJson = null,
                                        createdAt = 0L,
                                        updatedAt = 0L
                                    )
                                },
                                title = stringResource(R.string.section_reports),
                                locale = uiState.locale,
                                typeLabel = { localizedExpenseType(it.code) },
                                paymentLabel = { localizedPaymentMethod(it) },
                                categoryLabel = { localizedCategoryLabel(it) }
                            )
                            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val file = ExportFiles.writeTextFile(context, "report_${ts}.html", html)
                            shareFile(context, file, "text/html")
                        }) {
                            Icon(Icons.Filled.Print, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Period type selector
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf(
                        PeriodType.Day to stringResource(R.string.period_day),
                        PeriodType.Week to stringResource(R.string.period_week),
                        PeriodType.Month to stringResource(R.string.period_month),
                        PeriodType.Range to stringResource(R.string.period_range)
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        options.forEachIndexed { index, (type, label) ->
                            SegmentedButton(
                                selected = uiState.periodType == type,
                                onClick = { viewModel.setPeriodType(type) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                            ) { Text(label) }
                        }
                    }

                    if (uiState.periodType == PeriodType.Day || uiState.periodType == PeriodType.Month) {
                        DatePickerField(
                            value = uiState.from,
                            onValueChange = viewModel::setFrom,
                            label = stringResource(R.string.field_date),
                            locale = uiState.locale
                        )
                    } else {
                        DatePickerField(
                            value = uiState.from,
                            onValueChange = viewModel::setFrom,
                            label = stringResource(R.string.field_from),
                            locale = uiState.locale
                        )
                        DatePickerField(
                            value = uiState.to,
                            onValueChange = viewModel::setTo,
                            label = stringResource(R.string.field_to),
                            locale = uiState.locale
                        )
                    }
                }
            }

            // Filter dropdown
            val filterOptions = listOf(
                                ReportCalculator.ReportFilter.All to stringResource(R.string.filter_all),
                ReportCalculator.ReportFilter.House to stringResource(R.string.section_house),
                ReportCalculator.ReportFilter.Work to stringResource(R.string.section_work),
                ReportCalculator.ReportFilter.Personal to stringResource(R.string.section_personal),
                ReportCalculator.ReportFilter.Vehicle to stringResource(R.string.section_vehicle),
                ReportCalculator.ReportFilter.Education to stringResource(R.string.section_education)
            )
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DropdownField(
                        label = stringResource(R.string.action_filter),
                        options = filterOptions.map { it.second },
                        selectedIndex = filterOptions.indexOfFirst { it.first == uiState.filter }.coerceAtLeast(0),
                        onSelected = { viewModel.setFilter(filterOptions[it].first) }
                    )
                }
            }

            // Total
            val total = uiState.rows.sumOf { it.amount }
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource(R.string.total_label)} (${uiState.rows.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = CurrencyFormatter.format(total, uiState.currency, uiState.locale),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }

            // Range-too-wide warning (Stage 3 fix: silent empty list was confusing).
            if (uiState.rangeTooWide) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = stringResource(R.string.report_range_too_wide),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (uiState.rows.isEmpty() && !uiState.rangeTooWide) {
                EmptyState(
                    title = stringResource(R.string.no_expenses_yet),
                    hint = stringResource(R.string.no_expenses_yet_hint)
                )
            } else if (uiState.rows.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(uiState.rows, key = { it.id }) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(row.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${DateFormatter.formatForDisplay(row.date, uiState.locale)} · " +
                                        (row.type?.let { localizedExpenseType(it.code) } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = CurrencyFormatter.format(row.amount, uiState.currency, uiState.locale),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun shareFile(context: android.content.Context, file: java.io.File, mimeType: String) {
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
