package com.yego.sabongbettingsystem.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yego.sabongbettingsystem.viewmodel.CashInViewModel
import com.yego.sabongbettingsystem.viewmodel.ReverbViewModel

@Composable
fun AdminMainScreen(
    navController : NavController,
    onLogout      : () -> Unit
) {
    val reverbViewModel  = viewModel<ReverbViewModel>()
    val cashInViewModel = viewModel<CashInViewModel>()
    var selectedTab      by rememberSaveable { mutableIntStateOf(0) }

    // navigate to receipt — stays in same composable context
    LaunchedEffect(cashInViewModel.betResult.collectAsState().value) {
        val result = cashInViewModel.betResult.value
        if (result != null) {
            navController.navigate("admin_receipt/${result.reference}")
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    icon     = { Icon(Icons.Default.Home, contentDescription = null) },
                    label    = { Text("Fight", fontWeight = FontWeight.Medium) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    icon     = {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null)
                    },
                    label    = { Text("Cash In", fontWeight = FontWeight.Medium) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())   // respects bottom nav height in both orientations
        ) {
            when (selectedTab) {
                0 -> AdminHomeScreen(
                    navController   = navController,
                    reverbViewModel = reverbViewModel,
                    onLogout        = onLogout
                )
                1 -> {
                    val cashInViewModel = viewModel<CashInViewModel>()
                    AdminCashInScreen(
                        navController   = navController,
                        cashInViewModel = cashInViewModel,
                        reverbViewModel = reverbViewModel,
                        onLogout        = onLogout
                    )
                }
            }
        }
    }
}