package com.aistudio.smartcostpro.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aistudio.smartcostpro.app.R
import com.aistudio.smartcostpro.app.ui.car_income.CarIncomeScreen
import com.aistudio.smartcostpro.app.ui.current_account.CurrentAccountScreen
import com.aistudio.smartcostpro.app.ui.dashboard.DashboardScreen
import com.aistudio.smartcostpro.app.ui.education.EducationScreen
import com.aistudio.smartcostpro.app.ui.home.HomeScreen
import com.aistudio.smartcostpro.app.ui.income.IncomeScreen
import com.aistudio.smartcostpro.app.ui.login.LoginScreen
import com.aistudio.smartcostpro.app.ui.reports.ReportsScreen
import com.aistudio.smartcostpro.app.ui.sections.AssociationsScreen
import com.aistudio.smartcostpro.app.ui.sections.BackupScreen
import com.aistudio.smartcostpro.app.ui.sections.ExpenseSectionScreens.HouseScreen
import com.aistudio.smartcostpro.app.ui.sections.ExpenseSectionScreens.PersonalScreen
import com.aistudio.smartcostpro.app.ui.sections.ExpenseSectionScreens.WorkScreen
import com.aistudio.smartcostpro.app.ui.sections.RestoreScreen
import com.aistudio.smartcostpro.app.ui.sections.SectionsMenuScreen
import com.aistudio.smartcostpro.app.ui.settings.SettingsScreen
import com.aistudio.smartcostpro.app.ui.vehicle.VehicleScreen

/**
 * Root navigation host — Stage 3.2.
 *
 * **Login flow:** the NavHost starts on the Login screen. Once the user signs
 * in (or as guest), the LoginViewModel flips `signedIn = true` and the
 * LoginScreen calls `onSignedIn` which navigates to Home. From Home, the
 * bottom navigation switches between Home / Sections / Settings.
 *
 * **Bottom nav visibility:** the bottom bar is hidden on Login (because the
 * user hasn't authenticated yet) and on every section detail screen
 * (because they have their own TopAppBar + actions).
 */
@Composable
fun RootNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Routes where the bottom nav should be visible.
    val showBottomNav = currentRoute in listOf(Routes.HOME, Routes.SECTIONS_MENU, Routes.SETTINGS)

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    BottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelResId)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ───────── Login ─────────
            composable(Routes.LOGIN) {
                LoginScreen(
                    onSignedIn = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            // ───────── Home ─────────
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenSections = { navController.navigate(Routes.SECTIONS_MENU) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenNotifications = { /* Stage 4 */ }
                )
            }

            // ───────── Sections menu ─────────
            composable(Routes.SECTIONS_MENU) {
                SectionsMenuScreen(
                    onSectionClick = { section -> navController.navigate(section) },
                    onDashboardClick = { navController.navigate(Routes.HOME) }
                )
            }

            // ───────── Settings ─────────
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onNavigateToBackup = { navController.navigate(Routes.BACKUP) },
                    onNavigateToRestore = { navController.navigate(Routes.RESTORE) }
                )
            }

            // ───────── Legacy Dashboard ─────────
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onSectionClick = { section -> navController.navigate(section) }
                )
            }

            // ───────── Section detail screens ─────────
            composable(Routes.HOUSE) { HouseScreen() }
            composable(Routes.WORK) { WorkScreen() }
            composable(Routes.PERSONAL) { PersonalScreen() }
            composable(Routes.ASSOCIATIONS) { AssociationsScreen() }
            composable(Routes.BACKUP) { BackupScreen() }
            composable(Routes.RESTORE) { RestoreScreen() }
            composable(Routes.VEHICLE) { VehicleScreen() }
            composable(Routes.EDUCATION) { EducationScreen() }
            composable(Routes.INCOME_CERTS) { IncomeScreen() }
            composable(Routes.CAR_INCOME) { CarIncomeScreen() }
            composable(Routes.REPORTS) { ReportsScreen() }
            composable(Routes.CURRENT_ACCOUNT) { CurrentAccountScreen() }
        }
    }
}
