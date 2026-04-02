package com.yego.sabongbettingsystem.ui.teller.cashin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AdfScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yego.sabongbettingsystem.data.store.UserStore
import com.yego.sabongbettingsystem.viewmodel.CashInViewModel
import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yego.sabongbettingsystem.ui.components.WsStatusBadge
import com.yego.sabongbettingsystem.viewmodel.ReverbViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashInScreen(
    navController: NavController,
    cashInViewModel: CashInViewModel,
    reverbViewModel : ReverbViewModel,
    onLogout: () -> Unit
) {
    val context   = LocalContext.current
    val userStore = remember { UserStore(context) }
    val name      by userStore.name.collectAsState(initial = "Teller")
    val viewModel = cashInViewModel
    val fight     by viewModel.currentFight.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error     by viewModel.error.collectAsState()
    val betResult by viewModel.betResult.collectAsState()

    var amount       by remember { mutableStateOf("") }
    var selectedSide by remember { mutableStateOf("") }

    val connected       by reverbViewModel.connected.collectAsState()
    val reverbFight     by reverbViewModel.fightState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCurrentFight(context)
//        viewModel.startAutoRefresh(context)
        reverbViewModel.connect()
    }

    // reload fight from API whenever reverb fires a fight update
    LaunchedEffect(reverbFight) {
        if (reverbFight != null) {
            viewModel.loadCurrentFight(context)
        }
    }

//    DisposableEffect(Unit) {
//        onDispose {
////            viewModel.stopAutoRefresh()
//            reverbViewModel.disconnect()
//        }
//    }

    val meronDisplay = reverbFight?.meronTotal?.let {
        "%.2f".format(it)
    } ?: fight?.meron_total ?: "0.00"

    val walaDisplay  = reverbFight?.walaTotal?.let {
        "%.2f".format(it)
    } ?: fight?.wala_total ?: "0.00"

    val statusDisplay = reverbFight?.status ?: fight?.status ?: "pending"

    val fightNumber = reverbFight?.fightNumber?.takeIf { it.isNotEmpty() }
        ?: fight?.fight_number ?: ""




    // navigate to receipt on success
    LaunchedEffect(betResult) {
        if (betResult != null) {
            navController.navigate("receipt/${betResult!!.reference}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cash In", fontWeight = FontWeight.Bold)
                        Text(
                            text  = "Teller: $name",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    WsStatusBadge(connected = connected)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { navController.navigate("printer_settings") }) {
                        Icon(Icons.Default.AdfScanner, contentDescription = "Printer Settings")
                    }
                    IconButton(onClick = {
                        viewModel.logout(context, onLogout)
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }

                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Error ─────────────────────────────────────
            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text     = error!!,
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style    = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // ── Current Fight Card ────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text       = "Current Fight",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else if (fight == null) {
                        Text(
                            text  = "No open fight available.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text       = "Fight #${fightNumber}",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            SuggestionChip(
                                onClick = {},
                                label   = {
                                    Text(
                                        text     = statusDisplay.uppercase(),
                                        fontSize = 11.sp
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    labelColor     = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // meron / wala totals
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                                )
                            ) {
                                Column(
                                    modifier            = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text       = "MERON",
                                        fontSize   = 11.sp,
                                        color      = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text       = "₱${meronDisplay}",
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                )
                            ) {
                                Column(
                                    modifier            = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text       = "WALA",
                                        fontSize   = 11.sp,
                                        color      = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text       = "₱${walaDisplay}",
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Place Bet (only if fight is open) ─────────
            if (fight != null && statusDisplay == "open") {

                Text(
                    text       = "Place Bet",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // side selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick  = { selectedSide = "meron" },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSide == "meron")
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            contentColor = if (selectedSide == "meron")
                                MaterialTheme.colorScheme.onError
                            else
                                MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("MERON", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick  = { selectedSide = "wala" },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSide == "wala")
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            contentColor = if (selectedSide == "wala")
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("WALA", fontWeight = FontWeight.Bold)
                    }
                }

                // amount input
                OutlinedTextField(
                    value         = amount,
                    onValueChange = { amount = it },
                    label         = { Text("Bet Amount") },
                    prefix        = { Text("₱") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

// ── Quick amount buttons ──────────────────────
                val quickAmounts = listOf(50, 100, 200, 500, 1000, 5000, 10000)

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // row 1 — small amounts
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickAmounts.take(4).forEach { value ->
                            OutlinedButton(
                                onClick  = {
                                    val current = amount.toDoubleOrNull() ?: 0.0
                                    amount = (current + value).toInt().toString()
                                },
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text     = "+$value",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // row 2 — large amounts + clear
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickAmounts.drop(4).forEach { value ->
                            OutlinedButton(
                                onClick  = {
                                    val current = amount.toDoubleOrNull() ?: 0.0
                                    amount = (current + value).toInt().toString()
                                },
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text       = "+$value",
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // clear button
                        OutlinedButton(
                            onClick  = { amount = "" },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(8.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text       = "Clear",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // submit button
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull()
                        if (amt != null && selectedSide.isNotEmpty()) {
                            viewModel.placeBet(context, selectedSide, amt)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape   = RoundedCornerShape(12.dp),
                    enabled = selectedSide.isNotEmpty() &&
                            amount.isNotBlank() &&
                            !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            color       = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text       = "Place Bet",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else if (fight != null && statusDisplay != "open") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text     = "Betting is currently ${statusDisplay}. Waiting for fight to open.",
                        modifier = Modifier.padding(16.dp),
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        style    = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}