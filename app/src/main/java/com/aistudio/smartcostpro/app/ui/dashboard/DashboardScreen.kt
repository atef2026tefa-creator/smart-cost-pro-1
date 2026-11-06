package com.aistudio.smartcostpro.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.smartcostpro.app.R
import com.aistudio.smartcostpro.app.domain.model.DashboardSummary
import com.aistudio.smartcostpro.app.domain.model.ExpenseType
import com.aistudio.smartcostpro.app.domain.model.RecentOperation
import com.aistudio.smartcostpro.app.ui.components.CompactSummaryCard
import com.aistudio.smartcostpro.app.ui.components.EmptyState
import com.aistudio.smartcostpro.app.ui.components.ErrorState
import com.aistudio.smartcostpro.app.ui.components.LoadingState
import com.aistudio.smartcostpro.app.ui.components.MonthSelector
import com.aistudio.smartcostpro.app.ui.components.SummaryCard
import com.aistudio.smartcostpro.app.ui.theme.BrandColors
import com.aistudio.smartcostpro.app.util.CurrencyFormatter
import com.aistudio.smartcostpro.app.util.DateFormatter

/**
 * Dashboard screen — the financial overview.
 *
 * Layout:
 *   1. Month selector (prev / next).
 *   2. Top row: three big SummaryCards (income / expenses / net).
 *   3. Secondary row: savings rate + expense ratio (with warning when ratio > 90%).
 *   4. Per-type totals grid (FlowRow) — house, medical, work, personal, fuel, etc.
 *   5. Recent operations list (unified expenses only).
 */
@Composable
fun DashboardScreen(
    onSectionClick: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val month by viewModel.month.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is DashboardUiState.Loading -> LoadingState()
        is DashboardUiState.Error -> ErrorState(message = state.message)
        is DashboardUiState.Success -> DashboardContent(
            summary = state.summary,
            monthIso = state.month,
            locale = state.locale,
            currency = state.currency,
            onPreviousMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            canGoForward = viewModel.canGoForward(),
            onSectionClick = onSectionClick
        )
    }
}

@Composable
private fun DashboardContent(
    summary: DashboardSummary,
    monthIso: String,
    locale: com.aistudio.smartcostpro.app.util.AppLocale,
    currency: com.aistudio.smartcostpro.app.util.AppCurrency,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    canGoForward: Boolean,
    onSectionClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Title block
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.dashboard_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.dashboard_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            MonthSelector(
                monthIso = monthIso,
                locale = locale,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                canGoForward = canGoForward
            )
        }

        // Top row — the three "headline" cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    title = stringResource(R.string.card_total_income),
                    value = CurrencyFormatter.format(summary.monthlyIncome, currency, locale),
                    icon = Icons.Filled.TrendingUp,
                    accent = BrandColors.Income,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = stringResource(R.string.card_total_expenses),
                    value = CurrencyFormatter.format(summary.monthlyExpenses, currency, locale),
                    icon = Icons.Filled.TrendingDown,
                    accent = BrandColors.Expense,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SummaryCard(
                title = stringResource(R.string.card_net_income),
                value = CurrencyFormatter.format(summary.netIncome, currency, locale),
                icon = if (summary.netIncome >= 0) Icons.Filled.Savings else Icons.Filled.ErrorOutline,
                accent = if (summary.netIncome >= 0) BrandColors.Primary else BrandColors.ErrorLight,
                subtitle = when {
                    summary.netIncome < 0 -> stringResource(R.string.dashboard_warning_negative_net)
                    else -> null
                }
            )
        }

        // Secondary row — savings rate + expense ratio
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    title = stringResource(R.string.card_savings_rate),
                    value = CurrencyFormatter.formatPercentage(summary.savingsRate, locale),
                    icon = Icons.Filled.Savings,
                    accent = BrandColors.Primary,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = stringResource(R.string.card_expense_ratio),
                    value = CurrencyFormatter.formatPercentage(summary.expenseRatio, locale),
                    icon = if (summary.isExpenseRatioCritical)
                        Icons.Filled.WarningAmber else Icons.Filled.LocalAtm,
                    accent = if (summary.isExpenseRatioCritical)
                        BrandColors.Warning else MaterialTheme.colorScheme.primary,
                    subtitle = if (summary.isExpenseRatioCritical)
                        stringResource(R.string.dashboard_warning_expense_ratio) else null,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item { HorizontalDivider() }

        // Per-type totals grid
        item {
            Text(
                text = stringResource(R.string.sections_menu_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        item {
            PerTypeTotalsGrid(summary, currency, locale)
        }

        item { HorizontalDivider() }

        // Recent operations
        item {
            Text(
                text = stringResource(R.string.recent_operations),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (summary.recentOperations.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.no_recent_operations),
                    hint = null,
                    icon = Icons.Filled.ShoppingBag
                )
            }
        } else {
            items(summary.recentOperations, key = { it.id }) { op ->
                RecentOperationRow(op, locale, currency)
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PerTypeTotalsGrid(
    summary: DashboardSummary,
    currency: com.aistudio.smartcostpro.app.util.AppCurrency,
    locale: com.aistudio.smartcostpro.app.util.AppLocale
) {
    val totals = summary.expenses
    val items = listOf(
        Pair(stringResource(R.string.card_house_medical), totals.homeAndMedicalTotal) to
            Triple(Icons.Filled.Home, BrandColors.Primary, ExpenseType.House),
        Pair(stringResource(R.string.card_work), totals.workTotal) to
            Triple(Icons.Filled.Work, BrandColors.AccentDark, ExpenseType.Work),
        Pair(stringResource(R.string.card_personal), totals.personalTotal) to
            Triple(Icons.Filled.ShoppingBag, BrandColors.Info, ExpenseType.Personal),
        Pair(stringResource(R.string.card_education), totals.educationTotal) to
            Triple(Icons.Filled.School, BrandColors.Success, ExpenseType.Student),
        Pair(stringResource(R.string.card_fuel), totals.fuelTotal) to
            Triple(Icons.Filled.Bolt, BrandColors.Warning, ExpenseType.VehicleFuel),
        Pair(stringResource(R.string.card_maintenance), totals.maintTotal) to
            Triple(Icons.Filled.Build, BrandColors.ErrorLight, ExpenseType.VehicleMaint),
        Pair(stringResource(R.string.card_oils), totals.oilTotal) to
            Triple(Icons.Filled.CarRepair, BrandColors.PrimaryDark, ExpenseType.VehicleOil),
        Pair(stringResource(R.string.card_vehicle), totals.vehicleTotal) to
            Triple(Icons.Filled.DirectionsCar, BrandColors.Primary, ExpenseType.VehicleFuel),
        Pair(stringResource(R.string.section_income_certs), summary.income.certificatesProfit) to
            Triple(Icons.Filled.AccountBalance, BrandColors.IncomeDark, ExpenseType.House),
        Pair(stringResource(R.string.card_associations), summary.associationFinancials.earned) to
            Triple(Icons.Filled.CurrencyExchange, BrandColors.Accent, ExpenseType.House)
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (labelAndAmount, iconAndAccent) ->
            val (label, amount) = labelAndAmount
            val (icon, accent, _) = iconAndAccent
            CompactSummaryCard(
                title = label,
                value = CurrencyFormatter.format(amount, currency, locale, fractionDigits = 0),
                accent = accent,
                icon = icon,
                modifier = Modifier
                    .padding(2.dp)
                    .fillMaxWidth(0.5f)
            )
        }
    }
}

@Composable
private fun RecentOperationRow(
    op: RecentOperation,
    locale: com.aistudio.smartcostpro.app.util.AppLocale,
    currency: com.aistudio.smartcostpro.app.util.AppCurrency
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForType(op.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = op.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${DateFormatter.formatForDisplay(op.date, locale)} · ${labelForType(op.type)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = CurrencyFormatter.format(op.amount, currency, locale),
                style = MaterialTheme.typography.titleMedium,
                color = BrandColors.Expense,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun labelForType(type: ExpenseType): String = when (type) {
    ExpenseType.House -> stringResource(R.string.expense_type_house)
    ExpenseType.Medical -> stringResource(R.string.expense_type_medical)
    ExpenseType.Work -> stringResource(R.string.expense_type_work)
    ExpenseType.Personal -> stringResource(R.string.expense_type_personal)
    ExpenseType.Student -> stringResource(R.string.expense_type_student)
    ExpenseType.VehicleFuel -> stringResource(R.string.expense_type_fuel)
    ExpenseType.VehicleMaint -> stringResource(R.string.expense_type_maintenance)
    ExpenseType.VehicleOil -> stringResource(R.string.expense_type_oil)
}

private fun iconForType(type: ExpenseType): ImageVector = when (type) {
    ExpenseType.House -> Icons.Filled.Home
    ExpenseType.Medical -> Icons.Filled.MedicalServices
    ExpenseType.Work -> Icons.Filled.Work
    ExpenseType.Personal -> Icons.Filled.ShoppingBag
    ExpenseType.Student -> Icons.Filled.School
    ExpenseType.VehicleFuel -> Icons.Filled.Bolt
    ExpenseType.VehicleMaint -> Icons.Filled.Build
    ExpenseType.VehicleOil -> Icons.Filled.CarRepair
}
