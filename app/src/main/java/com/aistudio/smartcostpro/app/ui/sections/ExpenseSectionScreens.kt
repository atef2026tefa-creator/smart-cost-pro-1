package com.aistudio.smartcostpro.app.ui.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.aistudio.smartcostpro.app.R
import com.aistudio.smartcostpro.app.domain.model.ExpenseType
import com.aistudio.smartcostpro.app.ui.expenses.ExpenseListScreen
import com.aistudio.smartcostpro.app.ui.expenses.ExpenseListViewModel

/**
 * House & Medical section screen.
 *
 * Per the spec, the House section combines:
 *  - House expenses (type = `house`)
 *  - Medical expenses (type = `medical`)
 *
 * Stage 2 keeps the unified list but visually distinguishes medical rows via
 * the medical category icon. The AddExpenseModal lets the user pick either
 * `house` or `medical` from the section dropdown, so a single screen handles
 * both.
 *
 * For Stage 2 simplicity, we show the **house** type filtered list as the
 * primary view. Medical records are added through the same Add dialog
 * (pick "Medical" in the section dropdown) and appear in the dashboard's
 * `homeAndMedicalTotal`.
 */
@Composable
fun HouseScreen(
    viewModel: com.aistudio.smartcostpro.app.ui.expenses.ExpenseListViewModel = hiltViewModel()
) {
    ExpenseListScreen(
        type = ExpenseType.House,
        screenTitle = stringResource(R.string.screen_house_title),
        screenSubtitle = stringResource(R.string.screen_house_subtitle),
        allowMedical = true,
        viewModel = viewModel
    )
}

/**
 * Work section screen — uses the unified [ExpenseListScreen] with the
 * work expense type.
 */
@Composable
fun WorkScreen(
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    ExpenseListScreen(
        type = ExpenseType.Work,
        screenTitle = stringResource(R.string.screen_work_title),
        screenSubtitle = stringResource(R.string.screen_work_subtitle),
        viewModel = viewModel
    )
}

/**
 * Personal section screen.
 *
 * Stage 2 keeps this purely as the personal expense list. The spec mentions
 * the Personal card may show `personalTotal + associationFinancials.installments`,
 * but that's a dashboard concern — not this list.
 */
@Composable
fun PersonalScreen(
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    ExpenseListScreen(
        type = ExpenseType.Personal,
        screenTitle = stringResource(R.string.screen_personal_title),
        screenSubtitle = stringResource(R.string.screen_personal_subtitle),
        viewModel = viewModel
    )
}
