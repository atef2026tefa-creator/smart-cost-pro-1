package com.aistudio.smartcostpro.app.ui.sections

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.aistudio.smartcostpro.app.domain.model.SectionType
import com.aistudio.smartcostpro.app.ui.components.LoadingState
import com.aistudio.smartcostpro.app.ui.navigation.Routes
import com.aistudio.smartcostpro.app.ui.theme.BrandColors
import com.aistudio.smartcostpro.app.ui.theme.CustomShapes
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Sections menu screen.
 *
 * Lists every section the user can navigate to. Stage 1 ships six sections
 * per the spec's default order:
 *   1. Vehicle
 *   2. Income & Certificates
 *   3. Education
 *   4. House
 *   5. Personal
 *   6. Reports
 *
 * Plus a top entry for the Dashboard.
 *
 * The user can drag-and-drop to reorder (toggle Reorder mode), Save to
 * persist the new order, or Reset to restore the default order.
 */
@Composable
fun SectionsMenuScreen(
    onSectionClick: (String) -> Unit,
    onDashboardClick: () -> Unit,
    viewModel: SectionsMenuViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        LoadingState()
        return
    }

    val lazyListState = rememberLazyListState()
    val reorderableState: ReorderableLazyListState = rememberReorderableLazyListState(
        lazyListState
    ) { from, to ->
        viewModel.moveSection(from.index, to.index)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Title block
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.sections_menu_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.sections_menu_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Action bar — toggle reordering / save / reset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(onClick = {
                if (uiState.isReordering) viewModel.saveOrder() else viewModel.toggleReordering()
            }) {
                Icon(
                    imageVector = if (uiState.isReordering) Icons.Filled.DragIndicator else Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (uiState.isReordering)
                        stringResource(R.string.action_save)
                    else stringResource(R.string.sections_reorder)
                )
            }
            if (uiState.isReordering) {
                TextButton(onClick = viewModel::resetToDefault) {
                    Icon(
                        imageVector = Icons.Filled.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(stringResource(R.string.sections_reset_order))
                }
            }
        }

        // Dashboard entry — always at the top, never draggable.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = CustomShapes.CardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            onClick = onDashboardClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CustomShapes.PillShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.nav_dashboard),  // "Home" / "الرئيسية"
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.dashboard_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Sections list — reorderable when in reordering mode.
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = uiState.sections,
                key = { it.id }
            ) { section ->
                ReorderableItem(reorderableState, key = section.id) { _ ->
                    val dragModifier = if (uiState.isReordering) {
                        Modifier.longPressDraggableHandle()
                    } else Modifier
                    SectionRow(
                        section = section,
                        onClick = {
                            if (!uiState.isReordering) onSectionClick(routeForSection(section))
                        },
                        showDragHandle = uiState.isReordering,
                        modifier = dragModifier
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionRow(
    section: SectionType,
    onClick: () -> Unit,
    showDragHandle: Boolean,
    modifier: Modifier = Modifier
) {
    val (titleRes, subtitleRes, icon, accent) = sectionInfo(section)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = CustomShapes.CardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.5.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = accent.copy(alpha = 0.16f),
                        shape = CustomShapes.PillShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(subtitleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showDragHandle) {
                Icon(
                    imageVector = Icons.Filled.DragIndicator,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private data class SectionInfo(
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
    val accent: Color
)

private fun sectionInfo(section: SectionType): SectionInfo = when (section) {
    SectionType.Vehicle -> SectionInfo(
        R.string.section_vehicle,
        R.string.card_vehicle,
        Icons.Filled.DirectionsCar,
        BrandColors.Primary
    )
    SectionType.IncomeCerts -> SectionInfo(
        R.string.section_income_certs,
        R.string.card_total_income,
        Icons.Filled.AccountBalance,
        BrandColors.Income
    )
    SectionType.Education -> SectionInfo(
        R.string.section_education,
        R.string.card_education,
        Icons.Filled.School,
        BrandColors.Success
    )
    SectionType.House -> SectionInfo(
        R.string.section_house,
        R.string.card_house_medical,
        Icons.Filled.Home,
        BrandColors.AccentDark
    )
    SectionType.Personal -> SectionInfo(
        R.string.section_personal,
        R.string.card_personal,
        Icons.Filled.Person,
        BrandColors.Info
    )
    SectionType.Reports -> SectionInfo(
        R.string.section_reports,
        R.string.action_export,
        Icons.Filled.Article,
        BrandColors.PrimaryDark
    )
    SectionType.CarIncome -> SectionInfo(
        R.string.section_car_income,
        R.string.card_total_income,
        Icons.Filled.LocalTaxi,
        BrandColors.Accent
    )
    SectionType.Associations -> SectionInfo(
        R.string.section_associations,
        R.string.card_associations,
        Icons.Filled.PeopleAlt,
        BrandColors.Warning
    )
    SectionType.CurrentAccount -> SectionInfo(
        R.string.section_current_account,
        R.string.field_current_account_net,
        Icons.Filled.AccountBalance,
        BrandColors.PrimaryDark
    )
    SectionType.Work -> SectionInfo(
        R.string.section_work,
        R.string.card_work,
        Icons.Filled.Work,
        BrandColors.AccentDark
    )
    else -> SectionInfo(
        R.string.section_house,
        R.string.card_house_medical,
        Icons.Filled.Home,
        BrandColors.Primary
    )
}

private fun routeForSection(section: SectionType): String = when (section) {
    SectionType.House -> Routes.HOUSE
    SectionType.Work -> Routes.WORK
    SectionType.Personal -> Routes.PERSONAL
    SectionType.Vehicle -> Routes.VEHICLE
    SectionType.Education -> Routes.EDUCATION
    SectionType.IncomeCerts -> Routes.INCOME_CERTS
    SectionType.CarIncome -> Routes.CAR_INCOME
    SectionType.Reports -> Routes.REPORTS
    SectionType.Associations -> Routes.ASSOCIATIONS
    SectionType.CurrentAccount -> Routes.CURRENT_ACCOUNT
    else -> Routes.HOUSE
}
