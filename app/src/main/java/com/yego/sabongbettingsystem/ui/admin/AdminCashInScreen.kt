package com.yego.sabongbettingsystem.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yego.sabongbettingsystem.data.model.Fight
import com.yego.sabongbettingsystem.data.store.UserStore
import com.yego.sabongbettingsystem.ui.components.WsStatusBadge
import com.yego.sabongbettingsystem.viewmodel.CashInViewModel
import com.yego.sabongbettingsystem.viewmodel.ReverbFightState
import com.yego.sabongbettingsystem.viewmodel.ReverbViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCashInScreen(
    navController: NavController,
    reverbViewModel: ReverbViewModel,
    cashInViewModel: CashInViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val userStore = remember { UserStore(context) }
    val name by userStore.name.collectAsState(initial = "Admin")

    val connected by reverbViewModel.connected.collectAsState()
    val reverbFight by reverbViewModel.fightState.collectAsState()

    val fight by cashInViewModel.currentFight.collectAsState()
    val isLoading by cashInViewModel.isLoading.collectAsState()
    val error by cashInViewModel.error.collectAsState()
    val betResult by cashInViewModel.betResult.collectAsState()

    var amount by remember { mutableStateOf("") }
    var selectedSide by remember { mutableStateOf("") }

    // Derive statuses safely from both Reverb and API
    val meronOpen = reverbFight?.meronStatus?.let { it == "open" } ?: (fight?.meron_status == "open")
    val walaOpen = reverbFight?.walaStatus?.let { it == "open" } ?: (fight?.wala_status == "open")
    val statusDisplay = reverbFight?.status?.ifEmpty { null } ?: fight?.status ?: "pending"

    LaunchedEffect(Unit) {
        cashInViewModel.loadCurrentFight(context)
        reverbViewModel.connect()
    }

    // Refresh API data when websocket detects a state change
    LaunchedEffect(reverbFight?.status, reverbFight?.fightNumber) {
        if (reverbFight != null) {
            cashInViewModel.loadCurrentFight(context)
        }
    }

    // Reset form when fight changes
    LaunchedEffect(fight?.fight_number) {
        amount = ""
        selectedSide = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cash In", fontWeight = FontWeight.Bold)
                        Text("Admin: $name", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    WsStatusBadge(connected = connected)
                    IconButton(onClick = { navController.navigate("printer_settings") }) {
                        Icon(Icons.Default.Settings, null)
                    }
                    IconButton(onClick = { cashInViewModel.logout(context, onLogout) }) {
                        Icon(Icons.Default.Logout, null)
                    }
                }
            )
        }
    ) { padding ->
        val isTablet = LocalConfiguration.current.screenWidthDp >= 600

        Box(modifier = Modifier.padding(padding)) {
            if (isTablet) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ErrorMessage(error)
                        FightStatusCard(fight, reverbFight, statusDisplay, isLoading)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (fight != null && statusDisplay == "open") {
                            BetForm(
                                amount = amount,
                                onAmountChange = { amount = it },
                                selectedSide = selectedSide,
                                onSideSelected = { selectedSide = it },
                                meronOpen = meronOpen,
                                walaOpen = walaOpen,
                                isLoading = isLoading,
                                onPlaceBet = { amt ->
                                    cashInViewModel.placeBet(context, selectedSide, amt)
                                }
                            )
                        } else if (fight != null && statusDisplay != "open") {
                            BettingClosedMessage(statusDisplay)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ErrorMessage(error)
                    FightStatusCard(fight, reverbFight, statusDisplay, isLoading)
                    if (fight != null && statusDisplay == "open") {
                        BetForm(
                            amount = amount,
                            onAmountChange = { amount = it },
                            selectedSide = selectedSide,
                            onSideSelected = { selectedSide = it },
                            meronOpen = meronOpen,
                            walaOpen = walaOpen,
                            isLoading = isLoading,
                            onPlaceBet = { amt ->
                                cashInViewModel.placeBet(context, selectedSide, amt)
                            }
                        )
                    } else if (fight != null && statusDisplay != "open") {
                        BettingClosedMessage(statusDisplay)
                    }
                }
            }
        }
    }
}

@Composable
fun FightStatusCard(
    fight: Fight?,
    reverbFight: ReverbFightState?,
    statusDisplay: String,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Current Fight",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (fight == null) {
                Text(
                    text = "No open fight available.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fight #${fight.fight_number}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    StatusChip(status = statusDisplay)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SideCard(
                        label = "MERON",
                        amount = reverbFight?.meronTotal?.toString() ?: fight.meron_total,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    SideCard(
                        label = "WALA",
                        amount = reverbFight?.walaTotal?.toString() ?: fight.wala_total,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun BetForm(
    amount: String,
    onAmountChange: (String) -> Unit,
    selectedSide: String,
    onSideSelected: (String) -> Unit,
    meronOpen: Boolean,
    walaOpen: Boolean,
    isLoading: Boolean,
    onPlaceBet: (Double) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Place Bet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // side selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val meronColor = MaterialTheme.colorScheme.error
            Button(
                onClick = { onSideSelected("meron") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = meronOpen,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSide == "meron") meronColor else meronColor.copy(alpha = 0.2f),
                    contentColor = if (selectedSide == "meron") MaterialTheme.colorScheme.onError else meronColor,
                    disabledContainerColor = meronColor.copy(alpha = 0.08f),
                    disabledContentColor = meronColor.copy(alpha = 0.35f)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MERON", fontWeight = FontWeight.Bold)
                    if (!meronOpen) Text(text = "Closed", fontSize = 10.sp)
                }
            }

            val walaColor = MaterialTheme.colorScheme.primary
            Button(
                onClick = { onSideSelected("wala") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = walaOpen,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSide == "wala") walaColor else walaColor.copy(alpha = 0.2f),
                    contentColor = if (selectedSide == "wala") MaterialTheme.colorScheme.onPrimary else walaColor,
                    disabledContainerColor = walaColor.copy(alpha = 0.08f),
                    disabledContentColor = walaColor.copy(alpha = 0.35f)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WALA", fontWeight = FontWeight.Bold)
                    if (!walaOpen) Text(text = "Closed", fontSize = 10.sp)
                }
            }
        }

        // amount input
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = { Text("Bet Amount") },
            prefix = { Text("₱") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        // quick amount buttons
        val quickAmounts = listOf(50, 100, 200, 500, 1000, 5000, 10000)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAmountRow(quickAmounts.take(4), amount, onAmountChange)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickAmounts.drop(4).forEach { value ->
                    QuickAmountButton(value, amount, onAmountChange, Modifier.weight(1f))
                }
                OutlinedButton(
                    onClick = { onAmountChange("") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(text = "Clear", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // place bet button
        Button(
            onClick = {
                amount.toDoubleOrNull()?.let { onPlaceBet(it) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = selectedSide.isNotEmpty() && amount.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = "Place Bet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun QuickAmountRow(values: List<Int>, currentAmount: String, onAmountChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        values.forEach { value ->
            QuickAmountButton(value, currentAmount, onAmountChange, Modifier.weight(1f))
        }
    }
}

@Composable
fun QuickAmountButton(value: Int, currentAmount: String, onAmountChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = {
            val current = currentAmount.toDoubleOrNull() ?: 0.0
            onAmountChange((current + value).toInt().toString())
        },
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Text(text = "+$value", fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BettingClosedMessage(status: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "Betting is currently $status.",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
