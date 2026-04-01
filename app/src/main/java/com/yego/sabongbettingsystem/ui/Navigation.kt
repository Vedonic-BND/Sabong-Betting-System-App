package com.yego.sabongbettingsystem.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.yego.sabongbettingsystem.data.store.UserStore
import com.yego.sabongbettingsystem.ui.admin.AdminCreateFightScreen
import com.yego.sabongbettingsystem.ui.admin.AdminFightDetailScreen
import com.yego.sabongbettingsystem.ui.admin.AdminFightHistoryScreen
import com.yego.sabongbettingsystem.ui.admin.AdminHomeScreen
import com.yego.sabongbettingsystem.ui.login.LoginScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yego.sabongbettingsystem.ui.teller.cashin.CashInScreen
import com.yego.sabongbettingsystem.ui.teller.cashin.ReceiptScreen
import com.yego.sabongbettingsystem.ui.teller.cashout.CashOutScreen
import com.yego.sabongbettingsystem.viewmodel.CashInViewModel
import com.yego.sabongbettingsystem.ui.teller.cashin.PrintReceiptScreen

@Composable
fun AppNavigation() {
    val context   = LocalContext.current
    val userStore = remember { UserStore(context) }

    val token by userStore.token.collectAsState(initial = null)
    val role  by userStore.role.collectAsState(initial = null)
    val app   by userStore.app.collectAsState(initial = null)

    val navController = rememberNavController()

    val startDestination = when {
        token.isNullOrEmpty() -> "login"
        role == "admin"       -> "admin_home"
        app  == "cashin"      -> "cashin"
        app  == "cashout"     -> "cashout"
        else                  -> "login"
    }

    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {

        // ── Login ─────────────────────────────────────────
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role, app ->
                    when {
                        role == "admin"  -> navController.navigate("admin_home") {
                            popUpTo("login") { inclusive = true }
                        }
                        app == "cashin"  -> navController.navigate("cashin") {
                            popUpTo("login") { inclusive = true }
                        }
                        app == "cashout" -> navController.navigate("cashout") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        // ── Admin ─────────────────────────────────────────
        composable("admin_home") {
            AdminHomeScreen(
                navController = navController,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("admin_create_fight") {
            AdminCreateFightScreen(navController = navController)
        }

        composable(
            route = "admin_fight/{fightId}",
            arguments = listOf(navArgument("fightId") { type = NavType.IntType })
        ) { backStackEntry ->
            AdminFightDetailScreen(
                navController = navController,
                fightId       = backStackEntry.arguments?.getInt("fightId") ?: 0
            )
        }

        composable("admin_history") {
            AdminFightHistoryScreen(navController = navController)
        }

        composable("cashin") {
            val cashInEntry = remember(it) {
                navController.getBackStackEntry("cashin")
            }
            val cashInViewModel = viewModel<CashInViewModel>(cashInEntry)
            CashInScreen(
                navController   = navController,
                cashInViewModel = cashInViewModel,
                onLogout        = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route     = "receipt/{reference}",
            arguments = listOf(navArgument("reference") { type = NavType.StringType })
        ) { backStackEntry ->
            val cashInEntry = remember(backStackEntry) {
                navController.getBackStackEntry("cashin")
            }
            val cashInViewModel = viewModel<CashInViewModel>(cashInEntry)
            ReceiptScreen(
                navController   = navController,
                reference       = backStackEntry.arguments?.getString("reference") ?: "",
                cashInViewModel = cashInViewModel
            )
        }

        composable(
            route     = "print/{reference}",
            arguments = listOf(navArgument("reference") { type = NavType.StringType })
        ) { backStackEntry ->
            PrintReceiptScreen(
                navController = navController,
                reference     = backStackEntry.arguments?.getString("reference") ?: ""
            )
        }

        composable("cashout") {
            CashOutScreen(
                navController = navController,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}