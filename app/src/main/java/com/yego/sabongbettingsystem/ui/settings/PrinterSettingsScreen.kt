package com.yego.sabongbettingsystem.ui.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yego.sabongbettingsystem.data.printer.BluetoothPrinterService
import com.yego.sabongbettingsystem.data.printer.BluetoothPermissionHelper
import com.yego.sabongbettingsystem.data.store.PrinterStore
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSettingsScreen(navController: NavController) {
    val context         = LocalContext.current
    val printerStore    = remember { PrinterStore(context) }
    val savedAddress    by printerStore.printerAddress.collectAsState(initial = null)
    val savedName       by printerStore.printerName.collectAsState(initial = null)

    val pairedDevices   = remember {
        if (BluetoothPermissionHelper.hasPermissions(context)) {
            BluetoothPrinterService.getPairedDevices(context)
        } else emptyList()
    }

    var testResult      by remember { mutableStateOf<String?>(null) }
    var isTesting       by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Printer Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

            // ── Current printer ───────────────────────────
            if (savedName != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text       = "Selected Printer",
                                style      = MaterialTheme.typography.labelSmall,
                                color      = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text       = savedName!!,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            Icons.Default.BluetoothConnected,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // test print button
                if (testResult != null) {
                    Text(
                        text  = testResult!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (testResult!!.startsWith("✅"))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }

                OutlinedButton(
                    onClick = {
                        isTesting  = true
                        testResult = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    enabled  = !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Testing...")
                    } else {
                        Text("Test Print")
                    }
                }

                LaunchedEffect(isTesting) {
                    if (isTesting) {
                        val ctx   = context
                        val addr  = savedAddress
                        val error = kotlinx.coroutines.withContext(
                            kotlinx.coroutines.Dispatchers.IO
                        ) {
                            BluetoothPrinterService.printReceipt(
                                context        = ctx,
                                fightNumber    = "TEST",
                                side           = "MERON",
                                amount         = "100.00",
                                reference      = "TST-000000",
                                teller         = "Test Teller",
                                date           = "Apr 01, 2026",
                                time           = "10:30 AM",
                                qrData         = "TST-000000",
                                printerAddress = addr
                            )
                        }
                        isTesting  = false
                        testResult = if (error == null) "✅ Test print sent!" else "❌ $error"
                    }
                }
            }

            // ── Paired devices list ───────────────────────
            Text(
                text       = "Paired Bluetooth Devices",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (pairedDevices.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text     = "No paired devices found.\nPair your printer in Bluetooth settings first.",
                        modifier = Modifier.padding(16.dp),
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        style    = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(pairedDevices) { (name, address) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    kotlinx.coroutines.MainScope().launch {
                                        printerStore.save(address, name)
                                    }
                                },
                            shape  = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (address == savedAddress)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text       = name,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text  = address,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (address == savedAddress) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}