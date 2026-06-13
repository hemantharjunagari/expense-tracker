package com.spendless.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spendless.app.core.data.database.entities.LendBorrowType
import com.spendless.app.lend.ui.*
import com.spendless.app.ui.screens.analytics.AnalyticsScreen
import com.spendless.app.ui.screens.budget.BudgetScreen
import com.spendless.app.ui.screens.dashboard.DashboardScreen
import com.spendless.app.ui.screens.onboarding.OnboardingScreen
import com.spendless.app.ui.screens.settings.SettingsScreen
import com.spendless.app.ui.screens.settings.CategoryManagementScreen
import com.spendless.app.ui.screens.transactions.TransactionsScreen

sealed class Screen(val route: String) {
    data object Onboarding  : Screen("onboarding")
    data object Dashboard   : Screen("dashboard")
    data object Transactions: Screen("transactions?tab={tab}") {
        fun route(tab: String) = "transactions?tab=$tab"
    }
    data object Analytics   : Screen("analytics")
    data object Budget      : Screen("budget")
    data object Settings    : Screen("settings")
    data object ManageCategories : Screen("settings/categories")

    // Lend & Borrow module
    data object LendDashboard : Screen("lend/dashboard")
    data object LendAdd       : Screen("lend/add/{type}") {
        fun route(type: LendBorrowType) = "lend/add/${type.name}"
    }
    data object LendDetail    : Screen("lend/detail/{recordId}") {
        fun route(id: Long) = "lend/detail/$id"
    }
    data object LendContact   : Screen("lend/contact/{phone}") {
        fun route(phone: String) = "lend/contact/${java.net.URLEncoder.encode(phone, "UTF-8")}"
    }
    data object LendAnalytics : Screen("lend/analytics")
}

private val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 4 }
}
private val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 4 }
}
private val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(250)) + slideInHorizontally(tween(250)) { -it / 4 }
}
private val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 4 }
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Onboarding.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition
    ) {
        // ── Core screens ──────────────────────────────────────────────────────

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToTransactions = { tab ->
                    val route = if (tab != null) Screen.Transactions.route(tab) else Screen.Transactions.route
                    navController.navigate(route)
                },
                onNavigateToAnalytics    = { navController.navigate(Screen.Analytics.route) },
                onNavigateToBudget       = { navController.navigate(Screen.Budget.route) },
                onNavigateToSettings     = { navController.navigate(Screen.Settings.route) },
                onNavigateToLendBorrow   = { navController.navigate(Screen.LendDashboard.route) },
                onNavigateToReview       = { navController.navigate(Screen.Transactions.route("PENDING")) }
            )
        }

        composable(
            route = Screen.Transactions.route,
            arguments = listOf(navArgument("tab") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            TransactionsScreen(onNavigateBack = { navController.popBackStack() })
        }



        composable(Screen.Analytics.route) {
            AnalyticsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Budget.route) {
            BudgetScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCategories = { navController.navigate(Screen.ManageCategories.route) },
                onNavigateToBudget = { navController.navigate(Screen.Budget.route) }
            )
        }

        composable(Screen.ManageCategories.route) {
            CategoryManagementScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Lend & Borrow module ──────────────────────────────────────────────

        composable(Screen.LendDashboard.route) {
            LendBorrowDashboardScreen(
                onNavigateBack       = { navController.popBackStack() },
                onNavigateToAdd      = { type -> navController.navigate(Screen.LendAdd.route(type)) },
                onNavigateToDetail   = { id -> navController.navigate(Screen.LendDetail.route(id)) },
                onNavigateToContact  = { phone -> navController.navigate(Screen.LendContact.route(phone)) },
                onNavigateToAnalytics = { navController.navigate(Screen.LendAnalytics.route) }
            )
        }

        composable(
            route = Screen.LendAdd.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) {
            AddLendBorrowScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.LendDetail.route,
            arguments = listOf(navArgument("recordId") { type = NavType.LongType })
        ) {
            LendBorrowDetailScreen(
                onNavigateBack       = { navController.popBackStack() },
                onNavigateToContact  = { phone -> navController.navigate(Screen.LendContact.route(phone)) }
            )
        }

        composable(
            route = Screen.LendContact.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) {
            ContactProfileScreenContent(
                onNavigateBack       = { navController.popBackStack() },
                onNavigateToDetail   = { id -> navController.navigate(Screen.LendDetail.route(id)) }
            )
        }

        composable(Screen.LendAnalytics.route) {
            LendBorrowAnalyticsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
