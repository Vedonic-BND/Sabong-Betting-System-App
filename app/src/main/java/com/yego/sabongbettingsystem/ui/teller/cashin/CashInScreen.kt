package com.yego.sabongbettingsystem.ui.teller.cashin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.AdfScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Close
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
import com.yego.sabongbettingsystem.ui.components.WsStatusBadge
import com.yego.sabongbettingsystem.viewmodel.ReverbViewModel
import com.yego.sabongbettingsystem.viewmodel.CashOutViewModel
import com.yego.sabongbettingsystem.ui.utils.parseCurrency
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yego.sabongbettingsystem.viewmodel.TellerNotification
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashInScreen(
    navController   : NavController,
    cashInViewModel : CashInViewModel,
    reverbViewModel : ReverbViewModel,
    onLogout        : () -> Unit
) {
    val context   = LocalContext.current
    val userStore = remember { UserStore(context) }
    val name      by userStore.name.collectAsState(initial = "Teller")
    val fight     by cashInViewModel.currentFight.collectAsState()
    val isLoading by cashInViewModel.isLoading.collectAsState()
    val error     by cashInViewModel.error.collectAsState()
    val betResult by cashInViewModel.betResult.collectAsState()
    val connected     by reverbViewModel.connected.collectAsState()
    val reverbFight   by reverbViewModel.fightState.collectAsState()
    val betHistory    by cashInViewModel.betHistory.collectAsState()
    val runnerHistory by cashInViewModel.runnerHistory.collectAsState()
    val lastBet       by reverbViewModel.lastBet.collectAsState()
    val cashUpdated   by reverbViewModel.cashUpdated.collectAsState()
    val runnerAccepted by reverbViewModel.runnerAccepted.collectAsState()
    val notifications by cashInViewModel.notifications.collectAsState()
    val tellerCashStatus by cashInViewModel.tellerCashStatus.collectAsState()
    
    val cashOutViewModel = viewModel<CashOutViewModel>()
    val cashOutHistory by cashOutViewModel.betHistory.collectAsState()

    var amount       by remember { mutableStateOf("") }
    var selectedSide by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showRunnerNotification by remember { mutableStateOf(false) }
    var acceptedRunnerName by remember { mutableStateOf("") }
    var showNotificationSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cashInViewModel.loadCurrentFight(context)
        cashInViewModel.loadBetHistory(context)
        cashInViewModel.loadRunnerHistory(context)
        cashInViewModel.loadTellerCashStatus(context)
        cashOutViewModel.loadBetHistory(context)
        reverbViewModel.connect()
    }

    // Refresh data when teller cash is updated via Reverb (runner transaction)
    LaunchedEffect(cashUpdated) {
        if (cashUpdated != null) {
            cashInViewModel.loadRunnerHistory(context)
            cashInViewModel.loadTellerCashStatus(context)
            cashOutViewModel.loadBetHistory(context)
        }
    }

    // Refresh bet history when a new bet is placed via Reverb
    LaunchedEffect(lastBet) {
        if (lastBet != null) {
            cashInViewModel.loadBetHistory(context)
            cashOutViewModel.loadBetHistory(context)
        }
    }

    // Refresh cashout history when winner is declared (payouts become available)
    LaunchedEffect(reverbFight?.winner) {
        if (reverbFight?.winner != null) {
            cashOutViewModel.loadBetHistory(context)
        }
    }

    // Show notification when a runner accepts the request
    LaunchedEffect(runnerAccepted) {
        if (runnerAccepted != null) {
            val runnerName = runnerAccepted!!.optString("runner_name", "A runner")
            acceptedRunnerName = runnerName
            showRunnerNotification = true
            
            // Add to notification history
            cashInViewModel.addNotification(
                title = "Runner Accepted",
                message = "$runnerName is on the way to assist you.",
                data = runnerAccepted
            )
            
            // Clear the state in ReverbViewModel so it doesn't re-trigger on recomposition
            reverbViewModel.clearRunnerAccepted()
            // Reset the request success state in CashInViewModel
            cashInViewModel.clearRequestSuccess()
            
            // Auto-dismiss after 3 seconds
            delay(3000)
            showRunnerNotification = false
        }
    }

    val meronOpen = when {
        reverbFight?.meronStatus?.isNotEmpty() == true -> reverbFight!!.meronStatus == "open"
        fight != null -> fight!!.meron_status == "open"
        else          -> true
    }

    val walaOpen = when {
        reverbFight?.walaStatus?.isNotEmpty() == true -> reverbFight!!.walaStatus == "open"
        fight != null -> fight!!.wala_status == "open"
        else          -> true
    }

    val statusDisplay = when {
        reverbFight?.status?.isNotEmpty() == true -> reverbFight!!.status
        fight != null -> fight!!.status
        else          -> "pending"
    }

    val fightNumber = reverbFight?.fightNumber?.takeIf { it.isNotEmpty() }
        ?: fight?.fight_number
        ?: ""

    // reset selected side when side closes via reverb
    LaunchedEffect(meronOpen) {
        if (!meronOpen && selectedSide == "meron") selectedSide = ""
    }

    LaunchedEffect(walaOpen) {
        if (!walaOpen && selectedSide == "wala") selectedSide = ""
    }

    // navigate to receipt on success
    LaunchedEffect(betResult) {
        if (betResult != null) {
            navController.navigate("receipt/${betResult!!.reference}")
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Bet") },
            text = {
                Column {
                    Text("Are you sure you want to place this bet?")
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text("Side: ", fontWeight = FontWeight.Bold)
                        Text(selectedSide.uppercase(), color = if (selectedSide == "meron") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }
                    Row {
                        Text("Amount: ", fontWeight = FontWeight.Bold)
                        Text("₱$amount", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        val amt = amount.toDoubleOrNull()
                        if (amt != null && selectedSide.isNotEmpty()) {
                            cashInViewModel.placeBet(context, selectedSide, amt)
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNotificationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            NotificationSheetContent(
                notifications = notifications,
                onClear = { cashInViewModel.clearNotifications() },
                onMarkAsRead = { cashInViewModel.markNotificationAsRead(it) }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cash In - Fight #$fightNumber", fontWeight = FontWeight.Bold)
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
                    
                    val unreadCount = notifications.count { !it.isRead }
                    IconButton(onClick = { showNotificationSheet = true }) {
                        BadgedBox(badge = {
                            if (unreadCount > 0) {
                                Badge { Text(unreadCount.toString()) }
                            }
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }

                    IconButton(onClick = { navController.navigate("printer_settings") }) {
                        Icon(Icons.Default.AdfScanner, contentDescription = "Printer Settings")
                    }
                    IconButton(onClick = { cashInViewModel.logout(context, onLogout) }) {
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Runner Accepted Notification ────────────────────
            if (showRunnerNotification) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Runner Accepted!",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$acceptedRunnerName is on the way.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = { showRunnerNotification = false }) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

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

            // ── On-Hand Cash Card ────────────────────────
            // Use authoritative value from backend TellerCash model
            val onHandCash = tellerCashStatus?.on_hand_cash?.toDoubleOrNull() ?: 0.0
            val totalCashInValue = tellerCashStatus?.total_cash_in?.toDoubleOrNull() ?: 0.0
            val totalPaidOutValue = tellerCashStatus?.total_paid_out?.toDoubleOrNull() ?: 0.0
            
            // Calculate breakdown: runner transactions from history
            val betInAmount = betHistory.sumOf { it.receipt.amount.parseCurrency() }
            val payoutAmount = cashOutHistory
                .filter { it.status?.lowercase() == "paid" }
                .sumOf { it.net_payout.parseCurrency() }
            val providedAmount = runnerHistory
                .filter { it.type == "cash_in" }
                .sumOf { it.amount.parseCurrency() }
            val collectedAmount = runnerHistory
                .filter { it.type == "cash_out" }
                .sumOf { it.amount.parseCurrency() }

            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    navController.navigate("call_runner")
                },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header with total
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        Column {
                            Text(
                                text = "TOTAL ON-HAND CASH",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "₱${String.format(Locale.US, "%,.2f", onHandCash)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Breakdown section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Bet in: ₱${String.format(Locale.US, "%,.0f", betInAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Payout: ₱${String.format(Locale.US, "%,.0f", payoutAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Provided: ₱${String.format(Locale.US, "%,.0f", providedAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Collected: ₱${String.format(Locale.US, "%,.0f", collectedAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // ── Bet Summary Card ──────────────────────────
            val meronCount = betHistory.count { it.receipt.side!!.uppercase() == "MERON" }
            val walaCount = betHistory.count { it.receipt.side!!.uppercase() == "WALA" }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MERON BETS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = meronCount.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(40.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "WALA BETS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = walaCount.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ── Place Bet ─────────────────────────────────
            if (statusDisplay == "open" && (fight != null || reverbFight != null)) {

                Text(
                    text       = "Place Bet",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // side selector
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick  = { if (meronOpen) selectedSide = "meron" },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape    = RoundedCornerShape(12.dp),
                        enabled  = meronOpen,
                        colors = ButtonDefaults.buttonColors(
                            containerColor         = if (selectedSide == "meron")
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            contentColor           = if (selectedSide == "meron")
                                MaterialTheme.colorScheme.onError
                            else
                                MaterialTheme.colorScheme.error,
                            disabledContainerColor = MaterialTheme.colorScheme.error
                                .copy(alpha = 0.06f),
                            disabledContentColor   = MaterialTheme.colorScheme.error
                                .copy(alpha = 0.3f)
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MERON", fontWeight = FontWeight.Bold)
                            if (!meronOpen) {
                                Text(text = "Closed", fontSize = 10.sp)
                            }
                        }
                    }

                    Button(
                        onClick  = { if (walaOpen) selectedSide = "wala" },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape    = RoundedCornerShape(12.dp),
                        enabled  = walaOpen,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = if (selectedSide == "wala")
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            contentColor           = if (selectedSide == "wala")
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary
                                .copy(alpha = 0.06f),
                            disabledContentColor   = MaterialTheme.colorScheme.primary
                                .copy(alpha = 0.3f)
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("WALA", fontWeight = FontWeight.Bold)
                            if (!walaOpen) {
                                Text(text = "Closed", fontSize = 10.sp)
                            }
                        }
                    }
                }

                // amount input
                OutlinedTextField(
                    value           = amount,
                    onValueChange   = { amount = it },
                    label           = { Text("Bet Amount") },
                    prefix          = { Text("₱") },
                    modifier        = Modifier.fillMaxWidth(),
                    singleLine      = true,
                    shape           = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                // quick amount buttons
                val quickAmounts = listOf(50, 100, 200, 500, 1000, 5000, 10000)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickAmounts.take(4).forEach { value ->
                            OutlinedButton(
                                onClick        = {
                                    val current = amount.toDoubleOrNull() ?: 0.0
                                    amount      = (current + value).toInt().toString()
                                },
                                modifier       = Modifier.weight(1f),
                                shape          = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(
                                    horizontal = 4.dp, vertical = 8.dp
                                )
                            ) {
                                Text(
                                    text       = "+$value",
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickAmounts.drop(4).forEach { value ->
                            OutlinedButton(
                                onClick        = {
                                    val current = amount.toDoubleOrNull() ?: 0.0
                                    amount      = (current + value).toInt().toString()
                                },
                                modifier       = Modifier.weight(1f),
                                shape          = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(
                                    horizontal = 4.dp, vertical = 8.dp
                                )
                            ) {
                                Text(
                                    text       = "+$value",
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        OutlinedButton(
                            onClick        = { amount = "" },
                            modifier       = Modifier.weight(1f),
                            shape          = RoundedCornerShape(8.dp),
                            colors         = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border         = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            ),
                            contentPadding = PaddingValues(
                                horizontal = 4.dp, vertical = 8.dp
                            )
                        ) {
                            Text(
                                text       = "Clear",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // place bet button
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull()
                        if (amt != null && selectedSide.isNotEmpty()) {
                            val sideStillOpen = if (selectedSide == "meron")
                                meronOpen else walaOpen
                            if (sideStillOpen) {
                                showConfirmDialog = true
                            } else {
                                selectedSide = ""
                            }
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

            } else if (statusDisplay != "open" && (fight != null || reverbFight != null)) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text     = "Betting is currently $statusDisplay. Waiting for fight to open.",
                        modifier = Modifier.padding(16.dp),
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        style    = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // ── Transaction History Button ────────────────
            OutlinedButton(
                onClick = { navController.navigate("teller_history") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("View Transaction History", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun NotificationSheetContent(
    notifications: List<TellerNotification>,
    onClear: () -> Unit,
    onMarkAsRead: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 500.dp)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Alert History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (notifications.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text("Clear All")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No alerts found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { notification ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notification.isRead) 
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        onClick = { onMarkAsRead(notification.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Notifications, 
                                contentDescription = null,
                                tint = if (notification.isRead) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    notification.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    notification.message,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    notification.timestamp,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            if (!notification.isRead) {
                                Box(
                                    modifier = Modifier.size(8.dp).background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
