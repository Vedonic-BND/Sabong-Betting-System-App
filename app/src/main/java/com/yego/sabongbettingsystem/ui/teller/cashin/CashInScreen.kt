package com.yego.sabongbettingsystem.ui.teller.cashin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.AdfScanner
import androidx.compose.material.icons.filled.History
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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

    var amount       by remember { mutableStateOf("") }
    var selectedSide by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cashInViewModel.loadCurrentFight(context)
        reverbViewModel.connect()
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