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
import androidx.compose.ui.graphics.Color
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
    val userId    by userStore.userId.collectAsState(initial = "")
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
        cashInViewModel.loadSavedNotifications(context)
        cashOutViewModel.loadBetHistory(context)
        // Connect with user ID for filtering notifications
        val userIdLong = userId?.toLongOrNull() ?: -1
        android.util.Log.d("CashInScreen", "Connecting ReverbVM with userId='$userId' → parsed as: $userIdLong")
        reverbViewModel.connect(context, userIdLong)
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

    // Show notification when owner assigns a runner
    LaunchedEffect(notifications) {
        val runnerAssignedNotif = notifications.find { 
            it.title == "Runner Assigned" && !it.isRead 
        }
        if (runnerAssignedNotif != null) {
            acceptedRunnerName = runnerAssignedNotif.message
            showRunnerNotification = true
            // Pass context to sync read status to database
            cashInViewModel.markNotificationAsRead(runnerAssignedNotif.id, context)
            
            // Auto-dismiss after 3 seconds
            delay(3000)
            showRunnerNotification = false
        }
    }

    // Show notification when a runner accepts the request
    LaunchedEffect(runnerAccepted) {
        if (runnerAccepted != null) {
            android.util.Log.d("CashInScreen", "🎉 RunnerAccepted popup triggered!")
            val runnerName = runnerAccepted!!.optString("runner_name", "A runner")
            acceptedRunnerName = runnerName
            showRunnerNotification = true
            
            // Add to notification history
            cashInViewModel.addNotification(
                title = "Runner Accepted",
                message = "$runnerName is on the way to assist you.",
                data = runnerAccepted,
                context = context
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
                onMarkAsRead = { cashInViewModel.markNotificationAsRead(it, context) }
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

            HorizontalDivider()

            // ── Place Bet ─────────────────────────────────
            if (statusDisplay == "open" && (fight != null || reverbFight != null)) {
                
                if (onHandCash < 100000) {

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
                            modifier = Modifier.weight(1f).height(80.dp),
                            shape    = RoundedCornerShape(12.dp),
                            enabled  = meronOpen,
                            colors = ButtonDefaults.buttonColors(
                                containerColor         = if (selectedSide == "meron")
                                    Color(0xFFDC2626)
                                else
                                    Color(0xFFDC2626).copy(alpha = 0.2f),
                                contentColor           = if (selectedSide == "meron")
                                    Color.White
                                else
                                    Color(0xFFDC2626),
                                disabledContainerColor = Color(0xFFDC2626).copy(alpha = 0.06f),
                                disabledContentColor   = Color(0xFFDC2626).copy(alpha = 0.3f)
                            )
                        ) {
                            // ... existing content
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("MERON", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                if (!meronOpen) {
                                    Text(text = "Closed", fontSize = 14.sp)
                                }
                            }
                        }

                        Button(
                            onClick  = { if (walaOpen) selectedSide = "wala" },
                            modifier = Modifier.weight(1f).height(80.dp),
                            shape    = RoundedCornerShape(12.dp),
                            enabled  = walaOpen,
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = if (selectedSide == "wala")
                                    Color(0xFF16A34A)
                                else
                                    Color(0xFF16A34A).copy(alpha = 0.2f),
                                contentColor           = if (selectedSide == "wala")
                                    Color.White
                                else
                                    Color(0xFF16A34A),
                                disabledContainerColor = Color(0xFF16A34A).copy(alpha = 0.06f),
                                disabledContentColor   = Color(0xFF16A34A).copy(alpha = 0.3f)
                            )
                        ) {
                            // ... existing content
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("WALA", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                if (!walaOpen) {
                                    Text(text = "Closed", fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    // amount input
                    OutlinedTextField(
                        value           = amount,
                        onValueChange   = { input ->
                            val numeric = input.filter { it.isDigit() }
                            val value = numeric.toDoubleOrNull() ?: 0.0
                            if (value <= 10000) {
                                amount = numeric
                            } else {
                                amount = "10000"
                            }
                        },
                        label           = { Text("Bet Amount (Max ₱10,000)") },
                        prefix          = { Text("₱", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                        textStyle       = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        modifier        = Modifier.fillMaxWidth(),
                        singleLine      = true,
                        shape           = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )

                    // quick amount buttons
                    val quickAmounts = listOf(50, 100, 200, 500, 1000, 5000, 10000)

                    @Composable
                    fun AmountButton(value: Int, modifier: Modifier) {
                        val backgroundColor = when (value) {
                            50 -> Color(0xFFEF5350)    // Light Red
                            100 -> Color(0xFF9C27B0)   // Purple
                            200 -> Color(0xFF4CAF50)   // Green
                            500 -> Color(0xFFFFC107)   // Gold
                            1000 -> Color(0xFF03A9F4)  // Light Blue
                            5000 -> Color(0xFFFFEB3B)  // Yellow
                            10000 -> Color(0xFF3F51B5) // Dark Blue
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val contentColor = when (value) {
                            500, 5000 -> Color.Black   // Black text for lighter backgrounds
                            else -> Color.White
                        }

                        Button(
                            onClick = {
                                val current = amount.toDoubleOrNull() ?: 0.0
                                val next = (current + value).toInt()
                                amount = if (next > 10000) "10000" else next.toString()
                            },
                            modifier = modifier.height(64.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = backgroundColor,
                                contentColor = contentColor
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            val label = if (value == 10000) "$value" else "+$value"
                            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Row 1: 50, 100, 200
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickAmounts.slice(0..2).forEach { value ->
                                AmountButton(value, Modifier.weight(1f))
                            }
                        }

                        // Row 2: 500, 1000, 5000
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickAmounts.slice(3..5).forEach { value ->
                                AmountButton(value, Modifier.weight(1f))
                            }
                        }

                        // Row 3: 10000, Clear (occupies 2nd and 3rd col)
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 10000
                            AmountButton(10000, Modifier.weight(1f))

                            // Clear (occupies 2 columns)
                            Button(
                                onClick  = { amount = "" },
                                modifier = Modifier.weight(2f).height(64.dp),
                                shape    = RoundedCornerShape(12.dp),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD32F2F), // Red
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Clear", fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                } else {
                    // On-hand cash limiter message
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cash on-hand limit exceeded (₱${String.format(Locale.US, "%,.2f", onHandCash)}). Please remit cash to a runner before taking more bets (Max: ₱100,000).",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
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
