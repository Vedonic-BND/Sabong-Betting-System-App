package com.yego.sabongbettingsystem.ui.teller.cashout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.yego.sabongbettingsystem.data.printer.BluetoothPermissionHelper
import com.yego.sabongbettingsystem.data.printer.BluetoothPrinterService
import kotlinx.coroutines.flow.first

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

    var reference by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    var isPrinting   by remember { mutableStateOf(false) }
    var printError   by remember { mutableStateOf<String?>(null) }
    var printSuccess by remember { mutableStateOf(false) }

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

    LaunchedEffect(isPrinting) {
        if (isPrinting && payout != null) {
            val ctx      = context
            val p        = payout
            val name     = UserStore(ctx).name.first() ?: "Teller"
            val error    = kotlinx.coroutines.withContext(
                kotlinx.coroutines.Dispatchers.IO
            ) {
                BluetoothPrinterService.printPayoutReceipt(
                    context    = ctx,
                    reference  = p!!.reference,
                    fight      = p.fight,
                    side       = p.side.uppercase(),
                    betAmount  = p.bet_amount,
                    netPayout  = p.net_payout,
                    teller     = name
                )
            }
            isPrinting   = false
            printSuccess = error == null
            printError   = error
        }
    }

    // reset on confirmed
    LaunchedEffect(confirmed) {
        if (confirmed) {
            reference = ""
            viewModel.clearAll()
        }
    }

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

            // ── Success message ───────────────────────────
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

            // ── Reference input ───────────────────────────
            Text(
                text       = "Enter Reference Number",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

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

            Button(
                onClick  = { viewModel.lookupPayout(context, reference) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                enabled  = reference.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Look Up", fontWeight = FontWeight.SemiBold)
                }
            }

            // ── Payout result ─────────────────────────────
            if (payout != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text       = "Bet Details",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        PayoutRow("Reference",  payout!!.reference)
                        PayoutRow("Fight",       payout!!.fight)
                        PayoutRow(
                            label = "Side",
                            value = payout!!.side.uppercase(),
                            valueColor = if (payout!!.side == "meron")
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                        PayoutRow("Bet Amount",  "₱${payout!!.bet_amount}")
                        PayoutRow(
                            label = "Result",
                            value = if (payout!!.won) "WON ✅" else "LOST ❌",
                            valueColor = if (payout!!.won)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )

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
                                    Text(
                                        text       = "Pay ₱${payout!!.net_payout}",
                                        fontSize   = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {

                                // ── Already paid ──────────────────────────
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text     = "Already paid.",
                                        modifier = Modifier.padding(12.dp),
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // ── Print payout receipt ──────────────────
                                if (printSuccess) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text       = "✅ Receipt printed!",
                                            color      = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier   = Modifier.padding(12.dp),
                                            style      = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (printError != null) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text     = printError!!,
                                            color    = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(12.dp),
                                            style    = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (BluetoothPermissionHelper.hasPermissions(context)) {
                                            isPrinting = true
                                        } else {
                                            launcher.launch(
                                                BluetoothPermissionHelper.requiredPermissions()
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape    = RoundedCornerShape(12.dp),
                                    colors   = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    enabled  = !isPrinting
                                ) {
                                    if (isPrinting) {
                                        CircularProgressIndicator(
                                            modifier    = Modifier.size(20.dp),
                                            color       = MaterialTheme.colorScheme.onSecondary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Printing...")
                                    } else {
                                        Icon(
                                            imageVector        = Icons.Default.Print,
                                            contentDescription = null,
                                            modifier           = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Print Payout Receipt", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text     = "This bet lost. No payout.",
                                    modifier = Modifier.padding(12.dp),
                                    color    = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PayoutRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color      = valueColor
        )
    }
}