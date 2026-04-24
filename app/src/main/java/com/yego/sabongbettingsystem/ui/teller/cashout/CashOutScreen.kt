package com.yego.sabongbettingsystem.ui.teller.cashout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yego.sabongbettingsystem.data.store.UserStore
import com.yego.sabongbettingsystem.viewmodel.CashOutViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.yego.sabongbettingsystem.data.printer.BluetoothPermissionHelper
import com.yego.sabongbettingsystem.data.printer.BluetoothPrinterService
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.saveable.rememberSaveable
import com.yego.sabongbettingsystem.data.model.BetResponse
import com.yego.sabongbettingsystem.ui.components.QrScannerView
import com.yego.sabongbettingsystem.ui.components.WsStatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashOutScreen(
    navController: NavController,
    onLogout: () -> Unit
) {
    val context   = LocalContext.current
    val userStore = remember { UserStore(context) }
    val name      by userStore.name.collectAsState(initial = "Teller")
    val viewModel = viewModel<CashOutViewModel>()
    val payout    by viewModel.payout.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error     by viewModel.error.collectAsState()
    val confirmed by viewModel.confirmed.collectAsState()
    val betHistory by viewModel.betHistory.collectAsState()

    var reference by rememberSaveable { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    var isPrinting   by remember { mutableStateOf(false) }
    var printError   by remember { mutableStateOf<String?>(null) }
    var printSuccess by remember { mutableStateOf(false) }

    var showScanner by remember { mutableStateOf(false) }
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            isPrinting = true
            printError = null
        } else {
            printError = "Bluetooth permission denied."
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadBetHistory(context)
    }

    LaunchedEffect(isPrinting) {
        if (isPrinting && payout != null) {
            val ctx      = context
            val p        = payout!!
            val tellerName = UserStore(ctx).name.first() ?: "Teller"
            val resultError = kotlinx.coroutines.withContext(
                kotlinx.coroutines.Dispatchers.IO
            ) {
                BluetoothPrinterService.printPayoutReceipt(
                    context    = ctx,
                    reference  = p.reference,
                    fight      = p.fight,
                    side       = p.side.uppercase(),
                    betAmount  = p.bet_amount,
                    netPayout  = p.net_payout,
                    status     = p.status.uppercase(),
                    payoutDate = p.payout_date,
                    payoutTime = p.payout_time,
                    teller     = tellerName
                )
            }
            isPrinting   = false
            printSuccess = resultError == null
            printError   = resultError
        }
    }

    // reset on confirmed
    LaunchedEffect(confirmed) {
        if (confirmed) {
            // After payout, keep the reference to allow printing immediately
            // reference = ""
            // viewModel.clearAll()
            viewModel.loadBetHistory(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cash Out", fontWeight = FontWeight.Bold)
                        Text(
                            text  = "Teller: $name",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    WsStatusBadge(connected = true)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        viewModel.logout(context, onLogout)
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                    IconButton(onClick = { navController.navigate("printer_settings") }) {
                        Icon(Icons.Default.AdfScanner, contentDescription = "Printer Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Lookup") }
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
            if (showConfirmDialog && payout != null) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title   = { Text("Confirm Payout") },
                    text    = {
                        Text(
                            "Pay ₱${payout!!.net_payout} for reference ${payout!!.reference}?"
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            showConfirmDialog = false
                            viewModel.confirmPayout(context, payout!!.reference)
                        }) { Text("Confirm") }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            when (selectedTabIndex) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (confirmed) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text     = "✅ Payout confirmed successfully!",
                                    color    = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(12.dp),
                                    style    = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

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

                        Text(
                            text       = "Enter or Scan Reference Number",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (showScanner) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(16.dp)
                            ) {
                                QrScannerView(
                                    onScanned = { scannedValue ->
                                        reference   = scannedValue
                                        showScanner = false
                                        viewModel.lookupPayout(context, scannedValue)
                                    },
                                    onDismiss = { showScanner = false }
                                )
                            }
                        }

                        OutlinedTextField(
                            value         = reference,
                            onValueChange = {
                                reference = it.uppercase()
                                viewModel.clearAll()
                            },
                            label         = { Text("Reference #") },
                            placeholder   = { Text("e.g. XKP-123456") },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true,
                            shape         = RoundedCornerShape(12.dp),
                            trailingIcon  = {
                                IconButton(
                                    onClick  = { viewModel.lookupPayout(context, reference) },
                                    enabled  = reference.isNotBlank() && !isLoading
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            }
                        )

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick  = {
                                    showScanner  = !showScanner
                                    viewModel.clearAll()
                                },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape    = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (showScanner) "Close Scanner" else "Scan QR")
                            }

                            Button(
                                onClick  = { viewModel.lookupPayout(context, reference) },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape    = RoundedCornerShape(12.dp),
                                enabled  = reference.isNotBlank() && !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                } else {
                                    Text("Look Up", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        if (payout != null) {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Bet Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    PayoutRow("Reference",  payout!!.reference)
                                    PayoutRow("Fight",       payout!!.fight)
                                    PayoutRow("Side", payout!!.side.uppercase(), valueColor = if (payout!!.side == "meron") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                    PayoutRow("Bet Amount",  "₱${payout!!.bet_amount}")
                                    PayoutRow("Result", if (payout!!.won) "WON ✅" else "LOST ❌", valueColor = if (payout!!.won) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                    HorizontalDivider()
                                    PayoutRow("Status", payout!!.status.uppercase())

                                    if (payout!!.won) {
                                        PayoutRow("Net Payout", "₱${payout!!.net_payout}")
                                        if (payout!!.status == "pending") {
                                            Button(
                                                onClick  = { showConfirmDialog = true },
                                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                                shape    = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Pay ₱${payout!!.net_payout}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(8.dp)) {
                                                Text("Already paid.", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (payout!!.payout_date != null) {
                                                PayoutRow("Paid On", "${payout!!.payout_date} ${payout!!.payout_time}")
                                            }

                                            if (printSuccess) {
                                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(8.dp)) {
                                                    Text("✅ Receipt printed!", color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            if (printError != null) {
                                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(8.dp)) {
                                                    Text(printError!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                            Button(
                                                onClick = { if (BluetoothPermissionHelper.hasPermissions(context)) isPrinting = true else launcher.launch(BluetoothPermissionHelper.requiredPermissions()) },
                                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                                shape    = RoundedCornerShape(12.dp),
                                                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                enabled  = !isPrinting
                                            ) {
                                                if (isPrinting) {
                                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Printing...")
                                                } else {
                                                    Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Print Payout Receipt", fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    } else {
                                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(8.dp)) {
                                            Text("This bet lost. No payout.", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    TransactionHistoryTabs(betHistory) { bet ->
                        reference = bet.reference
                        selectedTabIndex = 0
                        viewModel.lookupPayout(context, bet.reference)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionHistoryTabs(history: List<BetResponse>, onItemClick: (BetResponse) -> Unit) {
    var subTabIndex by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Unpaid", "Paid")

    val unpaidWinners = history.filter { bet ->
        val won = bet.won == true
        val isPaid = bet.status?.lowercase() == "paid"
        won && !isPaid
    }
    
    val paidWinners = history.filter { bet ->
        bet.status?.lowercase() == "paid"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = subTabIndex, containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant) {
            subTabs.forEachIndexed { index, title ->
                val count = if(index == 0) unpaidWinners.size else paidWinners.size
                Tab(
                    selected = subTabIndex == index,
                    onClick = { subTabIndex = index },
                    text = { Text(text = "$title ($count)", fontSize = 12.sp) }
                )
            }
        }

        val displayList = if (subTabIndex == 0) unpaidWinners else paidWinners

        if (displayList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if(subTabIndex == 0) "No unpaid winners found." else "No paid transactions found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(displayList) { bet -> 
                    TransactionItem(bet = bet, onClick = { onItemClick(bet) }) 
                }
            }
        }
    }
}

@Composable
fun TransactionItem(bet: BetResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }, 
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Ref: ${bet.reference}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(text = "₱${bet.receipt.amount}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Fight #${bet.receipt.fight_number} - ${bet.receipt.side}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "${bet.receipt.date} ${bet.receipt.time}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun PayoutRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
