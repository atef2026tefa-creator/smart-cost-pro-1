package com.aistudio.smartcostpro.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.smartcostpro.app.R
import com.aistudio.smartcostpro.app.domain.model.DashboardSummary
import com.aistudio.smartcostpro.app.ui.components.LoadingState
import com.aistudio.smartcostpro.app.ui.theme.BrandColors
import com.aistudio.smartcostpro.app.util.CurrencyFormatter
import com.aistudio.smartcostpro.app.util.DateFormatter

/**
 * Home screen — Stage 3.2 design with the new Smart Cost Pro identity.
 *
 * Layout (top → bottom):
 *  1. TopAppBar (gradient): avatar + greeting + email + bell (with badge) + gear
 *  2. Marquee news bar: dark surface, app icon on the right, text scrolling
 *     toward the icon
 *  3. Summary card: net income for the active month
 *  4. "أقسام التكاليف" CTA card → opens the sections menu
 *  5. Quick stats grid: 4 cards (House, Vehicle, Education, Personal)
 *  6. Recent operations (compact, scrollable)
 *
 * The bottom navigation is provided by the parent NavGraph.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSections: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading || state.session == null) {
        LoadingState()
        return
    }

    val session = state.session!!
    val summary = state.summary
    val brandGradient = Brush.horizontalGradient(
        colors = listOf(BrandColors.Primary, BrandColors.PrimaryLight)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top app bar (gradient)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brandGradient)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Avatar + greeting (right side in RTL since Row is LTR by default
                    // — but we're in a RTL composition so this Row reads RTL).
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(BrandColors.Accent, BrandColors.AccentDark)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = session.displayName.firstOrNull()?.toString() ?: "G",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                text = if (session.isGuest)
                                    stringResource(R.string.home_greeting, stringResource(R.string.home_guest_name))
                                else
                                    stringResource(R.string.home_greeting, session.displayName),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = session.email,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    // Tools: bell + settings
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onOpenNotifications) {
                            Box {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                // Badge dot
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(BrandColors.Accent)
                                )
                            }
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }

                // Marquee news bar
                MarqueeNewsBar(items = state.marqueeItems)
            }
        }

        // Scrollable content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary card
            if (summary != null) {
                item { NetIncomeCard(summary = summary, monthIso = state.month, locale = state.locale, currency = state.currency) }
            }

            // Sections CTA card
            item {
                SectionsCtaCard(
                    onClick = onOpenSections,
                    locale = state.locale
                )
            }

            // Quick stats grid
            if (summary != null) {
                item {
                    QuickStatsGrid(
                        summary = summary,
                        currency = state.currency,
                        locale = state.locale
                    )
                }
            }
        }
    }
}

@Composable
private fun NetIncomeCard(
    summary: DashboardSummary,
    monthIso: String,
    locale: com.aistudio.smartcostpro.app.util.AppLocale,
    currency: com.aistudio.smartcostpro.app.util.AppCurrency
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(BrandColors.Primary, BrandColors.PrimaryLight)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_net_income_label, DateFormatter.formatMonthForDisplay(monthIso, locale)),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = CurrencyFormatter.format(summary.netIncome, currency, locale),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.home_total_income),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = CurrencyFormatter.format(summary.monthlyIncome, currency, locale, fractionDigits = 0),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.home_total_expenses),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = CurrencyFormatter.format(summary.monthlyExpenses, currency, locale, fractionDigits = 0),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionsCtaCard(
    onClick: () -> Unit,
    locale: com.aistudio.smartcostpro.app.util.AppLocale
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(BrandColors.PrimaryLight, BrandColors.Primary)
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_sections_card_title),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.home_sections_card_subtitle),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = stringResource(R.string.home_sections_card_meta),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatsGrid(
    summary: DashboardSummary,
    currency: com.aistudio.smartcostpro.app.util.AppCurrency,
    locale: com.aistudio.smartcostpro.app.util.AppLocale
) {
    val items = listOf(
        QuickStat(
            label = stringResource(R.string.card_house_medical),
            value = summary.expenses.homeAndMedicalTotal,
            icon = Icons.Filled.Balance,
            iconBg = Color(0xFFC8E6F5)
        ),
        QuickStat(
            label = stringResource(R.string.card_vehicle),
            value = summary.expenses.vehicleTotal,
            icon = Icons.Filled.CarRepair,
            iconBg = Color(0xFFD4E9F7)
        ),
        QuickStat(
            label = stringResource(R.string.card_education),
            value = summary.expenses.educationTotal,
            icon = Icons.Filled.School,
            iconBg = Color(0xFFBBDEFB)
        ),
        QuickStat(
            label = stringResource(R.string.card_personal),
            value = summary.expenses.personalTotal,
            icon = Icons.Filled.Person,
            iconBg = Color(0xFFFFCDD2)
        )
    )
    // 2x2 grid
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0..1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0..1) {
                    val item = items[row * 2 + col]
                    QuickStatCard(
                        item = item,
                        currency = currency,
                        locale = locale,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private data class QuickStat(
    val label: String,
    val value: Double,
    val icon: ImageVector,
    val iconBg: Color
)

@Composable
private fun QuickStatCard(
    item: QuickStat,
    currency: com.aistudio.smartcostpro.app.util.AppCurrency,
    locale: com.aistudio.smartcostpro.app.util.AppLocale,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(item.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = BrandColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = item.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    text = CurrencyFormatter.format(item.value, currency, locale, fractionDigits = 0),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
