package com.aistudio.smartcostpro.app.ui.expenses

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.smartcostpro.app.R
import com.aistudio.smartcostpro.app.domain.model.Expense
import com.aistudio.smartcostpro.app.domain.model.ExpenseType
import com.aistudio.smartcostpro.app.export.CsvExporter
import com.aistudio.smartcostpro.app.export.ExportFiles
import com.aistudio.smartcostpro.app.export.HtmlExporter
import com.aistudio.smartcostpro.app.ui.components.ConfirmDialog
import com.aistudio.smartcostpro.app.ui.components.EmptyState
import com.aistudio.smartcostpro.app.ui.forms.categoryLabelRes
import com.aistudio.smartcostpro.app.ui.forms.localizedCategoryLabel
import com.aistudio.smartcostpro.app.ui.forms.localizedExpenseType
import com.aistudio.smartcostpro.app.ui.forms.localizedPaymentMethod
import com.aistudio.smartcostpro.app.util.CurrencyFormatter
import com.aistudio.smartcostpro.app.util.DateFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generic expense list screen used by House / Work / Personal.
 *
 * Per the spec each section screen provides:
 *  - List of expenses filtered by the section's [ExpenseType].
 *  - Search box.
 *  - Category filter dropdown.
 *  - Add / Edit / Delete actions.
 *  - CSV export + Print.
 *
 * The Medical records live inside the House section screen — they're shown
 * as separate rows with their own icon so the user can distinguish them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    type: ExpenseType,
    screenTitle: String,
    screenSubtitle: String,
    allowMedical: Boolean = false,
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    val uiState by viewModel.stateFor(type).collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Expense?>(null) }
    var deleteTarget by remember { mutableStateOf<Expense?>(null) }

    // Track the last section selection used by the Add/Edit dialog so the
    // dialog opens to the right section by default.
    var addInitialSection by remember(type) { mutableStateOf(type) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = screenTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = screenSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    ExportMenu(
                        items = uiState.items,
                        title = screenTitle,
                        locale = uiState.locale,
                        onExported = { msg ->
                            // Show a snackbar with the export result.
                        }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                addInitialSection = type
                showAddDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search row
            OutlinedTextField(
                value = uiState.search,
                onValueChange = viewModel::onSearchChange,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category filter (if categories available)
            CategoryFilterRow(
                type = type,
                selected = uiState.categoryFilter,
                onSelected = viewModel::onCategoryFilterChange
            )

            // Total card
            TotalCard(
                total = uiState.total,
                count = uiState.items.size,
                currency = uiState.currency,
                locale = uiState.locale
            )

            // List
            if (uiState.items.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.no_expenses_yet),
                    hint = stringResource(R.string.no_expenses_yet_hint),
                    icon = Icons.Filled.Storage
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.items, key = { it.id }) { expense ->
                        ExpenseRow(
                            expense = expense,
                            locale = uiState.locale,
                            currency = uiState.currency,
                            onEdit = {
                                editTarget = expense
                            },
                            onDelete = {
                                deleteTarget = expense
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        ExpenseFormDialog(
            onDismiss = { showAddDialog = false },
            onSaved = { showAddDialog = false }
        )
    }

    // Edit dialog
    if (editTarget != null) {
        ExpenseFormDialog(
            onDismiss = { editTarget = null },
            onSaved = { editTarget = null },
            editExpense = editTarget
        )
    }

    // Delete confirmation
    if (deleteTarget != null) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_message),
            confirmLabel = stringResource(R.string.confirm_delete_yes),
            cancelLabel = stringResource(R.string.confirm_delete_no),
            onConfirm = {
                viewModel.deleteExpense(deleteTarget!!.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

/**
 * Top-bar dropdown with the spec's export options: CSV, Print, Share.
 */
@Composable
private fun ExportMenu(
    items: List<Expense>,
    title: String,
    locale: com.aistudio.smartcostpro.app.util.AppLocale,
    onExported: (String) -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_export))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.export_csv)) },
                onClick = {
                    expanded = false
                    val csv = CsvExporter.expensesToCsv(items, locale)
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val file = ExportFiles.writeTextFile(context, "expenses_${timestamp}.csv", csv)
                    shareFile(context, file, "text/csv")
                    onExported("EXPORT_DONE")
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.export_print)) },
                onClick = {
                    expanded = false
                    val html = HtmlExporter.expensesToHtml(
                        items = items,
                        title = title,
                        locale = locale,
                        typeLabel = { type -> localizedExpenseType(type.code) },
                        paymentLabel = { code -> localizedPaymentMethod(code) },
                        categoryLabel = { code -> localizedCategoryLabel(code) }
                    )
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val file = ExportFiles.writeTextFile(context, "print_${timestamp}.html", html)
                    shareFile(context, file, "text/html")
                    onExported("EXPORT_DONE")
                }
            )
        }
    }
}

/**
 * Shares a file via the configured FileProvider.
 *
 * The provider authority is `${applicationId}.fileprovider` — see the
 * manifest entry and `xml/file_paths.xml` for the configured directories.
 */
private fun shareFile(context: android.content.Context, file: java.io.File, mimeType: String) {
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}

@Composable
private fun CategoryFilterRow(
    type: ExpenseType,
    selected: String?,
    onSelected: (String?) -> Unit
) {
    val categories = com.aistudio.smartcostpro.app.domain.model.Categories.forType(type)
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = if (selected == null) stringResource(R.string.filter_all_categories)
                else localizedCategoryLabel(selected),
            onValueChange = { /* read-only */ },
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.form_category_label)) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.filter_all_categories)) },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            categories.forEach { code ->
                DropdownMenuItem(
                    text = { Text(localizedCategoryLabel(code)) },
                    onClick = {
                        onSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TotalCard(
    total: Double,
    count: Int,
    currency: com.aistudio.smartcostpro.app.util.AppCurrency,
    locale: com.aistudio.smartcostpro.app.util.AppLocale
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.total_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (count == 1) stringResource(R.string.count_one_record)
                        else stringResource(R.string.count_many_records, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = CurrencyFormatter.format(total, currency, locale),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    locale: com.aistudio.smartcostpro.app.util.AppLocale,
    currency: com.aistudio.smartcostpro.app.util.AppCurrency,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                val subtitle = buildString {
                    append(DateFormatter.formatForDisplay(expense.date, locale))
                    append(" · ")
                    append(localizedCategoryLabel(expense.category))
                    if (!expense.notes.isNullOrBlank()) {
                        append(" · ")
                        append(expense.notes)
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = CurrencyFormatter.format(expense.amount, currency, locale),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_button_label))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_button_label))
            }
        }
    }
}
