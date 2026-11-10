package com.citadel.budget.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.citadel.budget.AppContainer
import com.citadel.budget.data.UserSettings
import com.citadel.budget.ui.activity.TransactionsScreen
import com.citadel.budget.ui.add.AddTransactionScreen
import com.citadel.budget.ui.fortify.FortifyScreen
import com.citadel.budget.ui.home.HomeScreen
import com.citadel.budget.ui.lock.LockScreen
import com.citadel.budget.ui.onboarding.OnboardingScreen
import com.citadel.budget.ui.plan.PlanScreen
import com.citadel.budget.ui.reports.ReportsScreen
import com.citadel.budget.ui.settings.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val LOCK = "lock"
    const val MAIN = "main"
    const val HOME = "home"
    const val PLAN = "plan"
    const val ACTIVITY = "activity"
    const val FORTIFY = "fortify"
    const val ADD = "add?txId={txId}"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
}

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomItem(Routes.PLAN, "Plan", Icons.Filled.PieChart),
    BottomItem(Routes.ACTIVITY, "Activity", Icons.Filled.ReceiptLong),
    BottomItem(Routes.FORTIFY, "Fortify", Icons.Filled.Shield)
)

@Composable
fun CitadelNavGraph(container: AppContainer, settings: UserSettings) {
    val nav = rememberNavController()
    val start = when {
        settings.pinEnabled -> Routes.LOCK
        !settings.onboarded -> Routes.ONBOARDING
        else -> Routes.MAIN
    }

    NavHost(navController = nav, startDestination = start) {

        composable(Routes.ONBOARDING) {
            OnboardingScreen(container) {
                nav.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
            }
        }

        composable(Routes.LOCK) {
            LockScreen(
                container = container,
                onUnlock = { nav.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } } },
                onReset = { nav.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } } }
            )
        }

        composable(Routes.MAIN) {
            MainScaffold(
                container = container,
                onOpenAdd = { id -> nav.navigate("add?txId=$id") },
                onOpenReports = { nav.navigate(Routes.REPORTS) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.ADD,
            arguments = listOf(
                navArgument("txId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { entry ->
            AddTransactionScreen(
                container = container,
                txId = entry.arguments?.getLong("txId") ?: -1L,
                onDone = { nav.popBackStack() }
            )
        }

        composable(Routes.REPORTS) {
            ReportsScreen(container) { nav.popBackStack() }
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                onWiped = { nav.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } } },
                onBack = { nav.popBackStack() }
            )
        }
    }
}

@Composable
fun MainScaffold(
    container: AppContainer,
    onOpenAdd: (Long) -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val inner = rememberNavController()
    val backStack by inner.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = { inner.navigateToTab(item.route) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenAdd(-1L) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add transaction")
            }
        }
    ) { padding ->
        NavHost(
            navController = inner,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    container = container,
                    onOpenAdd = onOpenAdd,
                    onOpenReports = onOpenReports,
                    onOpenSettings = onOpenSettings,
                    onOpenActivity = { inner.navigateToTab(Routes.ACTIVITY) },
                    onOpenFortify = { inner.navigateToTab(Routes.FORTIFY) }
                )
            }
            composable(Routes.PLAN) { PlanScreen(container) }
            composable(Routes.ACTIVITY) {
                TransactionsScreen(container, onEdit = { onOpenAdd(it) })
            }
            composable(Routes.FORTIFY) { FortifyScreen(container) }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
