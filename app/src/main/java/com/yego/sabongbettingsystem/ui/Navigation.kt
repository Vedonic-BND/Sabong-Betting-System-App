package com.yego.sabongbettingsystem.ui

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.yego.sabongbettingsystem.data.store.UserStore
import com.yego.sabongbettingsystem.ui.admin.*
import com.yego.sabongbettingsystem.ui.login.LoginScreen
import com.yego.sabongbettingsystem.ui.runner.RunnerScreen
import com.yego.sabongbettingsystem.ui.settings.PrinterSettingsScreen
import com.yego.sabongbettingsystem.ui.teller.TellerModeScreen
import com.yego.sabongbettingsystem.ui.teller.cashin.CashInScreen
import com.yego.sabongbettingsystem.ui.teller.cashin.ReceiptScreen
import com.yego.sabongbettingsystem.ui.teller.cashin.TellerTransactionHistoryScreen
import com.yego.sabongbettingsystem.ui.teller.cashin.RequestRunnerScreen
import com.yego.sabongbettingsystem.ui.teller.cashout.CashOutScreen
import com.yego.sabongbettingsystem.viewmodel.CashInViewModel
import com.yego.sabongbettingsystem.viewmodel.ReverbViewModel
import com.yego.sabongbettingsystem.ui.admin.AdminMainScreen
import com.yego.sabongbettingsystem.ui.admin.AdminReceiptScreen

@Composable
fun AppNavigation() {
    val context   = LocalContext.current
    val userStore = remember { UserStore(context) }

    val token by userStore.token.collectAsState(initial = null)
    val role  by userStore.role.collectAsState(initial = null)
    val app   by userStore.app.collectAsState(initial = null)

    val navController = rememberNavController()

    // determine start destination
    val startDestination = when {
        token.isNullOrEmpty() && role.isNullOrEmpty() -> "login"
        role == "admin"                               -> "admin_home"
        role == "runner"                              -> "runner_home"
        role == "teller" && app == "cashin"           -> "cashin"
        role == "teller" && app == "cashout"          -> "cashout"
        role == "teller"                              -> "teller_mode"
        else                                          -> "login"
    }

    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {

        // ── Login ─────────────────────────────────────────
        composable("login") {
            LoginScreen(
                onAdminLogin  = {
                    navController.navigate("admin_home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onTellerLogin = {
                    navController.navigate("teller_mode") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onRunnerLogin = {
                    navController.navigate("runner_home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // ── Runner ────────────────────────────────────────
        composable("runner_home") {
            RunnerScreen(
                navController = navController,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Teller mode selector ──────────────────────────
        composable("teller_mode") {
            TellerModeScreen(
                onCashIn  = {
                    navController.navigate("cashin") {
                        popUpTo("teller_mode") { inclusive = true }
                    }
                },
                onCashOut = {
                    navController.navigate("cashout") {
                        popUpTo("teller_mode") { inclusive = true }
                    }
                },
                onLogout  = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Admin ─────────────────────────────────────────
        composable("admin_home") {
            AdminMainScreen(
                navController = navController,
                onLogout      = {
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
            route     = "admin_fight/{fightId}",
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

        composable(
            route     = "admin_receipt/{reference}",
            arguments = listOf(navArgument("reference") { type = NavType.StringType })
        ) { backStackEntry ->
            val adminEntry = remember(backStackEntry) {
                navController.getBackStackEntry("admin_home")
            }
            val cashInViewModel = viewModel<CashInViewModel>(adminEntry)
            AdminReceiptScreen(
                navController   = navController,
                reference       = backStackEntry.arguments?.getString("reference") ?: "",
                cashInViewModel = cashInViewModel
            )
        }

        // ── Teller Cash In ────────────────────────────────
        composable("cashin") {
            val cashInViewModel = viewModel<CashInViewModel>(it)
            CashInScreen(
                navController   = navController,
                cashInViewModel = cashInViewModel,
                reverbViewModel = viewModel<ReverbViewModel>(),
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("teller_history") { backStackEntry ->
            val cashInEntry = remember(backStackEntry) {
                navController.getBackStackEntry("cashin")
            }
            val cashInViewModel = viewModel<CashInViewModel>(cashInEntry)
            TellerTransactionHistoryScreen(
                navController = navController,
                viewModel = cashInViewModel
            )
        }

        composable("call_runner") { backStackEntry ->
            val cashInEntry = remember(backStackEntry) {
                navController.getBackStackEntry("cashin")
            }
            val cashInViewModel = viewModel<CashInViewModel>(cashInEntry)
            RequestRunnerScreen(
                navController = navController,
                viewModel = cashInViewModel
            )
        }

        composable(
            route     = "receipt/{reference}",
            arguments = listOf(navArgument("reference") { type = NavType.StringType })
        ) { backStackEntry ->
            val cashInEntry     = remember(backStackEntry) {
                navController.getBackStackEntry("cashin")
            }
            val cashInViewModel = viewModel<CashInViewModel>(cashInEntry)
            ReceiptScreen(
                navController   = navController,
                reference       = backStackEntry.arguments?.getString("reference") ?: "",
                cashInViewModel = cashInViewModel
            )
        }

        // ── Teller Cash Out ───────────────────────────────
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

        // ── Settings ──────────────────────────────────────
        composable("printer_settings") {
            PrinterSettingsScreen(navController = navController)
        }
    }
}