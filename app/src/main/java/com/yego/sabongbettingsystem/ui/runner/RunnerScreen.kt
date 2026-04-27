package com.yego.sabongbettingsystem.ui.runner

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yego.sabongbettingsystem.data.model.RunnerTransactionResponse
import com.yego.sabongbettingsystem.data.model.TellerCashStatus
import com.yego.sabongbettingsystem.data.store.UserStore
import com.yego.sabongbettingsystem.viewmodel.RunnerViewModel
import com.yego.sabongbettingsystem.data.realtime.ReverbManager
import com.yego.sabongbettingsystem.viewmodel.RunnerNotification
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunnerScreen(
    navController: NavController,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = viewModel<RunnerViewModel>()
    val tellers by viewModel.tellers.collectAsState()
    val history by viewModel.history.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.successMessage.collectAsState()
    val incomingRequest by viewModel.incomingRequest.collectAsState()
    val userStore = remember { UserStore(context) }
    val runnerName by userStore.name.collectAsState(initial = "Runner")

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showTransactionDialog by remember { mutableStateOf<Pair<TellerCashStatus, String>?>(null) }
    var amountText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        android.util.Log.d("RunnerScreen", "LaunchedEffect starting - loading initial data")
        viewModel.loadTellers(context)
        viewModel.loadHistory(context)
        viewModel.loadSavedNotifications(context)
        viewModel.setupRealtimeListener(context)
        ReverbManager.connect()
        
        // Periodically refresh saved notifications every 2 seconds (reduced from 3)
        while (true) {
            delay(2000)
            android.util.Log.d("RunnerScreen", "Polling notifications...")
            viewModel.loadSavedNotifications(context)
        }
    }

    // Auto-clear messages after 3 seconds
    LaunchedEffect(error) {
        if (error != null) {
            delay(3000)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(success) {
        if (success != null) {
            delay(3000)
            viewModel.clearMessages()
        }
    }

    // ── Sound and Vibration Effect ──
    LaunchedEffect(incomingRequest) {
        if (incomingRequest != null) {
            // Play notification sound
            try {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(context, notification)
                r.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Vibrate
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Runner Panel", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Runner: $runnerName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadTellers(context); viewModel.loadHistory(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { viewModel.logout(context, onLogout) }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("Tellers") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { 
                        BadgedBox(badge = {
                            if (notifications.any { !it.isRead }) {
                                Badge { Text(notifications.count { !it.isRead }.toString()) }
                            }
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                    },
                    label = { Text("Alerts") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("History") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // ── Real-time Notification ──
            if (incomingRequest != null) {
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "RUNNER REQUESTED!",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val tellerName = incomingRequest!!.optString("teller_name", "A teller")
                                val requestType = incomingRequest!!.optString("request_type", "assistance")
                                val customMessage = incomingRequest!!.optString("custom_message", "")
                                
                                val displayMessage = when (requestType) {
                                    "assistance" -> "Assistance needed at counter"
                                    "need_cash" -> "Runner needed - Need cash"
                                    "collect_cash" -> "Runner needed - Collect excess cash"
                                    "other" -> "Custom request: $customMessage"
                                    else -> "Assistance needed at counter"
                                }
                                
                                Text(
                                    text = "$tellerName - $displayMessage",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        // Accept and Decline buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { 
                                    val tellerId = incomingRequest!!.optInt("teller_id", -1)
                                    if (tellerId > 0) {
                                        viewModel.acceptRequest(context, tellerId)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("ACCEPT", fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedButton(
                                onClick = { 
                                    // Just clear locally as requested
                                    viewModel.clearIncomingRequest()
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading
                            ) {
                                Text("DECLINE", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (error != null) {
                ErrorMessage(error!!, onDismiss = { viewModel.clearMessages() })
            }
            if (success != null) {
                SuccessMessage(success!!, onDismiss = { viewModel.clearMessages() })
            }

            when (selectedTabIndex) {
                0 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // ── Summary Card ──
                        val totalCash = tellers.sumOf { it.on_hand_cash.toDoubleOrNull() ?: 0.0 }
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TOTAL ACCUMULATED CASH",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "₱${String.format("%,.2f", totalCash)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Sum of Startup (Runner) + Sales (Cash In)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        TellerList(
                            tellers = tellers,
                            onCollect = { showTransactionDialog = it to "collect" },
                            onProvide = { showTransactionDialog = it to "provide" }
                        )
                    }
                }
                1 -> {
                    NotificationHistoryList(
                        notifications = notifications,
                        onClear = { viewModel.clearNotifications() },
                        onMarkAsRead = { viewModel.markNotificationAsRead(it) }
                    )
                }
                2 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // ── History Summary Card ──
                        val totalCollected = history.filter { it.type == "cash_out" }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                        val totalProvided = history.filter { it.type == "cash_in" }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Collected", style = MaterialTheme.typography.labelSmall)
                                    Text("₱${String.format("%,.0f", totalCollected)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                                VerticalDivider(modifier = Modifier.height(30.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Provided", style = MaterialTheme.typography.labelSmall)
                                    Text("₱${String.format("%,.0f", totalProvided)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        HistoryList(history = history)
                    }
                }
            }
        }

        if (showTransactionDialog != null) {
            val (teller, type) = showTransactionDialog!!
            val tellerOnHandAmount = teller.on_hand_cash.toDoubleOrNull() ?: 0.0
            val inputAmount = amountText.toDoubleOrNull() ?: 0.0
            val isCollect = type == "collect"
            val isAmountValid = inputAmount > 0 && (!isCollect || inputAmount <= tellerOnHandAmount)

            AlertDialog(
                onDismissRequest = { showTransactionDialog = null; amountText = "" },
                title = { Text(if (isCollect) "Collect Cash" else "Provide Cash") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Teller: ${teller.name}")
                        Text("Current On-hand: ₱${teller.on_hand_cash}")
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { input ->
                                // Filter to allow only digits and at most one decimal point
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) {
                                    amountText = filtered
                                }
                            },
                            label = { Text("Amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            isError = isCollect && inputAmount > tellerOnHandAmount && amountText.isNotBlank()
                        )
                        if (isCollect && inputAmount > tellerOnHandAmount && amountText.isNotBlank()) {
                            Text(
                                "Cannot collect more than ₱${teller.on_hand_cash}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Convert amount to Double
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                viewModel.createTransaction(context, teller.id, amount, type)
                                showTransactionDialog = null
                                amountText = ""
                            }
                        },
                        enabled = isAmountValid && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Confirm")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTransactionDialog = null; amountText = "" }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun NotificationHistoryList(
    notifications: List<RunnerNotification>,
    onClear: () -> Unit,
    onMarkAsRead: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel = viewModel<RunnerViewModel>()
    var acceptingId by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Auto-stop refreshing after 2 seconds
    LaunchedEffect(Unit) {
        delay(2000)
        isRefreshing = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Notification Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { 
                    isRefreshing = true
                    viewModel.loadSavedNotifications(context)

                }) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
                if (notifications.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text("Clear All", fontSize = 12.sp)
                    }
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No recent notifications.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("Waiting for runner requests...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notification ->
                    val data = notification.data
                    val requestType = data?.optString("request_type", "") ?: ""
                    val tellerId = data?.optInt("teller_id", -1) ?: -1
                    val isAssistanceRequest = notification.title.contains("Runner Requested", ignoreCase = true)
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notification.isRead) 
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                    
                                    // Show request type badge if available
                                    if (requestType.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        Surface(
                                            modifier = Modifier.wrapContentWidth(),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                requestType.uppercase().replace("_", " "),
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(4.dp, 2.dp),
                                                color = MaterialTheme.colorScheme.onSecondary
                                            )
                                        }
                                    }
                                    
                                    Text(
                                        notification.timestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(top = 4.dp)
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
                            
                            // Accept button for assistance requests
                            if (isAssistanceRequest && tellerId > 0) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        acceptingId = notification.id
                                        viewModel.acceptRequest(context, tellerId)
                                        onMarkAsRead(notification.id)
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    enabled = acceptingId != notification.id
                                ) {
                                    if (acceptingId == notification.id) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("ACCEPT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TellerList(
    tellers: List<TellerCashStatus>,
    onCollect: (TellerCashStatus) -> Unit,
    onProvide: (TellerCashStatus) -> Unit
) {
    if (tellers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tellers found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tellers) { teller ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(teller.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("On-hand: ₱${teller.on_hand_cash}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                        
                        if (teller.last_transaction != null) {
                            Text(
                                "Last: ${teller.last_transaction}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onCollect(teller) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Collect")
                            }
                            Button(
                                onClick = { onProvide(teller) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Provide")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryList(history: List<RunnerTransactionResponse>) {
    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transactions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isCollect = item.type == "cash_out"
                        Icon(
                            if (isCollect) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
                            contentDescription = null,
                            tint = if (isCollect) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCollect) "Collected from ${item.teller_name}" else "Provided to ${item.teller_name}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("${item.date} ${item.time}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            "₱${item.amount}",
                            fontWeight = FontWeight.Bold,
                            color = if (isCollect) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorMessage(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
fun SuccessMessage(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}
