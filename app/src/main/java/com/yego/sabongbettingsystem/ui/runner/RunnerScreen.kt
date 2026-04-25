package com.yego.sabongbettingsystem.ui.runner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.yego.sabongbettingsystem.ui.components.QrScannerView
import com.yego.sabongbettingsystem.data.realtime.ReverbManager

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
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.successMessage.collectAsState()
    val userStore = remember { UserStore(context) }
    val runnerName by userStore.name.collectAsState(initial = "Runner")

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showTransactionDialog by remember { mutableStateOf<Pair<TellerCashStatus, String>?>(null) }
    var amountText by remember { mutableStateOf("") }
    
    var showScanner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadTellers(context)
        viewModel.loadHistory(context)
        viewModel.setupRealtimeListener(context)
        ReverbManager.connect()
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
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("History") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
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

                        if (showScanner) {
                            Card(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                QrScannerView(
                                    onScanned = { scannedValue ->
                                        val teller = tellers.find { it.name.contains(scannedValue, ignoreCase = true) || it.id.toString() == scannedValue }
                                        if (teller != null) {
                                            showTransactionDialog = teller to "collect"
                                            showScanner = false
                                        }
                                    },
                                    onDismiss = { showScanner = false }
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { showScanner = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Scan Teller QR")
                                }
                            }
                        }

                        TellerList(
                            tellers = tellers,
                            onCollect = { showTransactionDialog = it to "collect" },
                            onProvide = { showTransactionDialog = it to "provide" }
                        )
                    }
                }
                1 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // ── History Summary Card ──
                        val totalCollected = history.filter { it.type == "collect" }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                        val totalProvided = history.filter { it.type == "provide" }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                        
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
            AlertDialog(
                onDismissRequest = { showTransactionDialog = null; amountText = "" },
                title = { Text(if (type == "collect") "Collect Cash" else "Provide Cash") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Teller: ${teller.name}")
                        Text("Current On-hand: ₱${teller.on_hand_cash}")
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amountText = it },
                            label = { Text("Amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                viewModel.createTransaction(context, teller.id, amount, type)
                                showTransactionDialog = null
                                amountText = ""
                            }
                        },
                        enabled = amountText.isNotBlank() && !isLoading
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
                        val isCollect = item.type == "collect"
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
