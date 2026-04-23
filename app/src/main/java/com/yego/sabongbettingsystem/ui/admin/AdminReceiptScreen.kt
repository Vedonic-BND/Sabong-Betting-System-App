package com.yego.sabongbettingsystem.ui.admin

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.yego.sabongbettingsystem.data.printer.BluetoothPermissionHelper
import com.yego.sabongbettingsystem.data.printer.BluetoothPrinterService
import com.yego.sabongbettingsystem.data.store.PrinterStore
import com.yego.sabongbettingsystem.ui.teller.cashin.ReceiptRow
import com.yego.sabongbettingsystem.viewmodel.CashInViewModel
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReceiptScreen(
    navController   : NavController,
    reference       : String,
    cashInViewModel : CashInViewModel
) {
    val context      = LocalContext.current
    val betResult    by cashInViewModel.betResult.collectAsState()
    val printerStore = remember { PrinterStore(context) }
    val printerAddress by printerStore.printerAddress.collectAsState(initial = null)

    var isPrinting   by remember { mutableStateOf(false) }
    var printSuccess by remember { mutableStateOf(false) }
    var printError   by remember { mutableStateOf<String?>(null) }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) isPrinting = true
        else printError = "Bluetooth permission denied."
    }

    LaunchedEffect(isPrinting) {
        if (isPrinting && betResult != null) {
            val ctx    = context
            val addr   = printerAddress
            val r      = betResult!!.receipt
            val error  = kotlinx.coroutines.withContext(
                kotlinx.coroutines.Dispatchers.IO
            ) {
                BluetoothPrinterService.printReceipt(
                    context        = ctx,
                    fightNumber    = r.fight_number,
                    side           = r.side,
                    amount         = r.amount,
                    reference      = r.reference,
                    teller         = r.teller,
                    date           = r.date,
                    time           = r.time,
                    qrData         = r.reference,
                    printerAddress = addr
                )
            }
            isPrinting   = false
            printSuccess = error == null
            printError   = error
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receipt", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        cashInViewModel.clearResult()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (betResult == null) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "No receipt data.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val receipt   = betResult!!.receipt
            val qrDataUrl = betResult!!.qr

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier            = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🐓 SABONG", fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(text = "BETTING SYSTEM", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text  = "Official Bet Receipt",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        ReceiptRow(label = "Fight #",   value = receipt.fight_number)
                        ReceiptRow(
                            label      = "Side",
                            value      = receipt.side,
                            valueColor = if (receipt.side == "MERON")
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                        ReceiptRow(label = "Amount",    value = "₱${receipt.amount}")
                        ReceiptRow(label = "Reference", value = receipt.reference)
                        ReceiptRow(label = "Teller",    value = receipt.teller)
                        ReceiptRow(label = "Date",      value = receipt.date)
                        ReceiptRow(label = "Time",      value = receipt.time)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text  = "Scan to verify",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = false
                                    setBackgroundColor(0xFFFFFFFF.toInt())
                                    loadDataWithBaseURL(
                                        null,
                                        """
                                        <html>
                                        <body style="margin:0;padding:0;background:white;
                                              display:flex;justify-content:center;align-items:center;
                                              width:150px;height:150px;overflow:hidden;">
                                        <img src="$qrDataUrl" style="width:150px;height:150px;object-fit:contain;"/>
                                        </body></html>
                                        """.trimIndent(),
                                        "text/html", "UTF-8", null
                                    )
                                }
                            },
                            modifier = Modifier.size(150.dp).padding(4.dp)
                        )

                        Text(
                            text          = receipt.reference,
                            fontSize      = 16.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text      = "Keep this receipt.\nPresent upon claiming payout.",
                            style     = MaterialTheme.typography.labelSmall,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // print feedback
                if (printSuccess) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text       = "✅ Receipt printed successfully!",
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

                // print button
                Button(
                    onClick = {
                        if (BluetoothPermissionHelper.hasPermissions(context)) {
                            isPrinting = true
                        } else {
                            launcher.launch(BluetoothPermissionHelper.requiredPermissions())
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
                            Icons.Default.Print,
                            contentDescription = null,
                            modifier           = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Print Receipt", fontWeight = FontWeight.SemiBold)
                    }
                }

                // place another bet button
                OutlinedButton(
                    onClick  = {
                        cashInViewModel.clearResult()
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text("Place Another Bet", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}