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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yego.sabongbettingsystem.data.store.UserStore
import com.yego.sabongbettingsystem.ui.components.WsStatusBadge
import com.yego.sabongbettingsystem.viewmodel.AdminViewModel
import com.yego.sabongbettingsystem.viewmodel.CashInViewModel
import com.yego.sabongbettingsystem.viewmodel.ReverbViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCashInScreen(
    navController   : NavController,
    reverbViewModel : ReverbViewModel,
    cashInViewModel : CashInViewModel,
    onLogout        : () -> Unit
) {
    val context      = LocalContext.current
    val userStore    = remember { UserStore(context) }
    val name         by userStore.name.collectAsState(initial = "Admin")
    val connected    by reverbViewModel.connected.collectAsState()
    val reverbFight  by reverbViewModel.fightState.collectAsState()
    val fight        by cashInViewModel.currentFight.collectAsState()
    val isLoading    by cashInViewModel.isLoading.collectAsState()
    val error        by cashInViewModel.error.collectAsState()
    val betResult    by cashInViewModel.betResult.collectAsState()

    var amount        by remember { mutableStateOf("") }
    var selectedSide  by remember { mutableStateOf("") }

    val meronOpen    = (reverbFight?.meronStatus ?: fight?.meron_status) == "open"
    val walaOpen     = (reverbFight?.walaStatus  ?: fight?.wala_status)  == "open"
    val statusDisplay = reverbFight?.status ?: fight?.status ?: "pending"

    // tablet detection
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    LaunchedEffect(Unit) {
        cashInViewModel.loadCurrentFight(context)
    }

    LaunchedEffect(reverbFight) {
        if (reverbFight != null) {
            cashInViewModel.loadCurrentFight(context)
        }
    }

    // reset selected side if closed
    LaunchedEffect(meronOpen, walaOpen) {
        if (selectedSide == "meron" && !meronOpen) selectedSide = ""
        if (selectedSide == "wala"  && !walaOpen)  selectedSide = ""
    }

    // navigate to receipt on success
    LaunchedEffect(betResult) {
        if (betResult != null) {
            navController.navigate("admin_receipt/${betResult!!.reference}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cash In", fontWeight = FontWeight.Bold)
                        Text(
                            text  = "Admin: $name",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    WsStatusBadge(connected = connected)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { navController.navigate("printer_settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { cashInViewModel.logout(context, onLogout) }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->

        val isTablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600

        if (isTablet) {
            // ── Tablet: two column layout ─────────────────
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Left column — current fight card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
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

                    // ── Current Fight Card ────────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text       = "Current Fight",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            } else if (fight == null) {
                                Text(
                                    text  = "No open fight available.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text       = "Fight #${fight!!.fight_number}",
                                        fontSize   = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    StatusChip(status = statusDisplay)
                                }

                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SideCard(
                                        label    = "MERON",
                                        amount   = reverbFight?.meronTotal?.toString()
                                            ?: fight!!.meron_total,
                                        color    = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(1f)
                                    )
                                    SideCard(
                                        label    = "WALA",
                                        amount   = reverbFight?.walaTotal?.toString()
                                            ?: fight!!.wala_total,
                                        color    = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Right column — place bet form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // ── Place Bet ─────────────────────────────────
                    if (fight != null && statusDisplay == "open") {

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
                                onClick  = { selectedSide = "meron" },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape    = RoundedCornerShape(12.dp),
                                enabled  = meronOpen,
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor         = if (selectedSide == "meron")
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                    contentColor           = if (selectedSide == "meron")
                                        MaterialTheme.colorScheme.onError
                                    else
                                        MaterialTheme.colorScheme.error,
                                    disabledContainerColor = MaterialTheme.colorScheme.error
                                        .copy(alpha = 0.08f),
                                    disabledContentColor   = MaterialTheme.colorScheme.error
                                        .copy(alpha = 0.35f)
                                )
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("MERON", fontWeight = FontWeight.Bold)
                                    if (!meronOpen) {
                                        Text(text = "Closed", fontSize = 10.sp)
                                    }
                                }
                            }

                            Button(
                                onClick  = { selectedSide = "wala" },
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
                                        .copy(alpha = 0.08f),
                                    disabledContentColor   = MaterialTheme.colorScheme.primary
                                        .copy(alpha = 0.35f)
                                )
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
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
                                            amount = (current + value).toInt().toString()
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
                                            amount = (current + value).toInt().toString()
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
                                        cashInViewModel.placeBet(context, selectedSide, amt)
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

                    } else if (fight != null && statusDisplay != "open") {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text     = "Betting is currently $statusDisplay.",
                                modifier = Modifier.padding(16.dp),
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                style    = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

        } else {
            // ── Phone: original layout (unchanged) ────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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

                // ── Current Fight Card ────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text       = "Current Fight",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else if (fight == null) {
                            Text(
                                text  = "No open fight available.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(
                                    text       = "Fight #${fight!!.fight_number}",
                                    fontSize   = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusChip(status = statusDisplay)
                            }

                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SideCard(
                                    label    = "MERON",
                                    amount   = reverbFight?.meronTotal?.toString()
                                        ?: fight!!.meron_total,
                                    color    = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                SideCard(
                                    label    = "WALA",
                                    amount   = reverbFight?.walaTotal?.toString()
                                        ?: fight!!.wala_total,
                                    color    = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // ── Place Bet ─────────────────────────────────
                if (fight != null && statusDisplay == "open") {

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
                            onClick  = { selectedSide = "meron" },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape    = RoundedCornerShape(12.dp),
                            enabled  = meronOpen,
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = if (selectedSide == "meron")
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                contentColor           = if (selectedSide == "meron")
                                    MaterialTheme.colorScheme.onError
                                else
                                    MaterialTheme.colorScheme.error,
                                disabledContainerColor = MaterialTheme.colorScheme.error
                                    .copy(alpha = 0.08f),
                                disabledContentColor   = MaterialTheme.colorScheme.error
                                    .copy(alpha = 0.35f)
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("MERON", fontWeight = FontWeight.Bold)
                                if (!meronOpen) {
                                    Text(text = "Closed", fontSize = 10.sp)
                                }
                            }
                        }

                        Button(
                            onClick  = { selectedSide = "wala" },
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
                                    .copy(alpha = 0.08f),
                                disabledContentColor   = MaterialTheme.colorScheme.primary
                                    .copy(alpha = 0.35f)
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
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
                                        amount = (current + value).toInt().toString()
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
                                        amount = (current + value).toInt().toString()
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
                                    cashInViewModel.placeBet(context, selectedSide, amt)
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

                } else if (fight != null && statusDisplay != "open") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text     = "Betting is currently $statusDisplay.",
                            modifier = Modifier.padding(16.dp),
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            style    = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}