package com.yego.sabongbettingsystem.ui.admin

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yego.sabongbettingsystem.data.model.Fight
import com.yego.sabongbettingsystem.data.store.UserStore
import com.yego.sabongbettingsystem.viewmodel.AdminViewModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.yego.sabongbettingsystem.ui.components.WsStatusBadge
import com.yego.sabongbettingsystem.viewmodel.ReverbViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    navController: NavController,
    reverbViewModel : ReverbViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val userStore = remember { UserStore(context) }
    val name by userStore.name.collectAsState(initial = "Admin")
    val viewModel = viewModel<AdminViewModel>()
    val fight by viewModel.currentFight.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val connected by reverbViewModel.connected.collectAsState()
    val reverbFight by reverbViewModel.fightState.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentFight(context)
        reverbViewModel.connect()
    }

    // reload fight from API whenever status or fight number changes via websocket
    LaunchedEffect(reverbFight?.status, reverbFight?.fightNumber) {
        if (reverbFight != null) {
            viewModel.loadCurrentFight(context)
        }
    }

    val actionResult by viewModel.actionResult.collectAsState()

    LaunchedEffect(actionResult) {
        if (actionResult == "bet_finalized") {
            fight?.id?.let { id ->
                navController.navigate("admin_fight/$id")
            }
            viewModel.clearResult()
        }
        if (actionResult == "fight_reset") {
            viewModel.clearResult()
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Fight Counter") },
            text = { Text("Are you sure you want to reset the fight counter to 1? This is usually done at the start of the day.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetFightNumber(context)
                        showResetDialog = false
                    }
                ) { Text("Reset", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    // use reverb data if available, fallback to loaded fight
    // merge reverb real-time data into the main fight object
    val displayFight = reverbFight?.let { r ->
        fight?.copy(
            status = if (r.status.isNotEmpty()) r.status else fight?.status ?: "pending",
            meron_status = r.meronStatus,
            wala_status = r.walaStatus,
            meron_total = r.meronTotal.toString(),
            wala_total = r.walaTotal.toString(),
        )
    } ?: fight

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Panel", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Welcome, $name",
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
                    IconButton(onClick = { viewModel.logout(context, onLogout) }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        val isTablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600

        Box(modifier = Modifier.padding(padding)) {
            if (isTablet) {
                TabletLayout(
                    displayFight = displayFight,
                    isLoading = isLoading,
                    error = error,
                    viewModel = viewModel,
                    navController = navController,
                    onReset = { showResetDialog = true }
                )
            } else {
                PhoneLayout(
                    displayFight = displayFight,
                    isLoading = isLoading,
                    error = error,
                    viewModel = viewModel,
                    navController = navController,
                    onReset = { showResetDialog = true }
                )
            }
        }
    }
}

@Composable
fun TabletLayout(
    displayFight: Fight?,
    isLoading: Boolean,
    error: String?,
    viewModel: AdminViewModel,
    navController: NavController,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ErrorMessage(error)
            FightStatusCard(displayFight, isLoading)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (displayFight != null && displayFight.status !in listOf("done", "cancelled")) {
                FightControlCard(displayFight, viewModel, navController)
            }
            QuickActions(navController, displayFight, onReset)
        }
    }
}

@Composable
fun PhoneLayout(
    displayFight: Fight?,
    isLoading: Boolean,
    error: String?,
    viewModel: AdminViewModel,
    navController: NavController,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ErrorMessage(error)
        FightStatusCard(displayFight, isLoading)
        if (displayFight != null && displayFight.status !in listOf("done", "cancelled")) {
            FightControlCard(displayFight, viewModel, navController)
        }
        QuickActions(navController, displayFight, onReset)
    }
}

@Composable
fun FightStatusCard(fight: Fight?, isLoading: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Current Fight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (fight == null) {
                Text("No active fight", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fight #${fight.fight_number}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    StatusChip(status = fight.status)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SideCard("MERON", fight.meron_total, MaterialTheme.colorScheme.error, Modifier.weight(1f))
                    SideCard("WALA", fight.wala_total, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun FightControlCard(fight: Fight, viewModel: AdminViewModel, navController: NavController) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Fight Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FightActionButtons(fight, viewModel, LocalContext.current, navController)
        }
    }
}

@Composable
fun QuickActions(navController: NavController, displayFight: Fight?, onReset: () -> Unit) {
    val hasActiveFight = displayFight != null && displayFight.status != "done" && displayFight.status != "cancelled"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Actions", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { 
                    // No need to pass fight number anymore as it's sequential on the server
                    navController.navigate("admin_create_fight") 
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = !hasActiveFight
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("New Fight")
            }
            OutlinedButton(onClick = { navController.navigate("admin_history") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.History, null)
                Spacer(Modifier.width(6.dp))
                Text("History")
            }
        }
        
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.RestartAlt, null)
            Spacer(Modifier.width(6.dp))
            Text("Reset Fight Counter to 1")
        }
    }
}

@Composable
fun ErrorMessage(error: String?) {
    if (error != null) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(8.dp)) {
            Text(error, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun FightActionButtons(
    fight         : Fight,
    viewModel     : AdminViewModel,
    context       : android.content.Context,
    navController : NavController
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val bothClosed = fight.meron_status == "closed" && fight.wala_status == "closed"
        
        when (fight.status) {

            // ── Pending → open betting ────────────────────
            "pending" -> {
                Button(
                    onClick  = { 
                        // Automatically open sides when opening betting
                        viewModel.updateAllSideStatus(context, fight.id, "open", "open")
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Open Betting", fontWeight = FontWeight.Bold)
                }
            }

            // ── Open → side controls + close/finalize ─────
            "open" -> {

                // meron control row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text     = "Meron",
                        modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically),
                        fontWeight = FontWeight.Bold,
                        color    = MaterialTheme.colorScheme.error,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.weight(1f))
                    if (fight.meron_status == "open") {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateSideStatus(
                                    context, fight.id, "meron", "closed"
                                )
                            },
                            modifier = Modifier.height(52.dp),
                            shape  = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Close Meron", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    } else {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateSideStatus(
                                    context, fight.id, "meron", "open"
                                )
                            },
                            modifier = Modifier.height(52.dp),
                            shape  = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) { Text("Reopen Meron", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                // wala control row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text     = "Wala",
                        modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically),
                        fontWeight = FontWeight.Bold,
                        color    = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.weight(1f))
                    if (fight.wala_status == "open") {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateSideStatus(
                                    context, fight.id, "wala", "closed"
                                )
                            },
                            modifier = Modifier.height(52.dp),
                            shape  = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Close Wala", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    } else {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateSideStatus(
                                    context, fight.id, "wala", "open"
                                )
                            },
                            modifier = Modifier.height(52.dp),
                            shape  = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) { Text("Reopen Wala", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                HorizontalDivider()

                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Close All / Open All toggle ───────────────
                    if (!bothClosed) {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateAllSideStatus(context, fight.id, "closed", "closed")
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape  = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { 
                            Icon(Icons.Default.Close, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Close All Sides", fontSize = 18.sp, fontWeight = FontWeight.Bold) 
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateAllSideStatus(context, fight.id, "open", "open")
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape  = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) { 
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open All Sides", fontSize = 18.sp, fontWeight = FontWeight.Bold) 
                        }
                    }

                    // ── Finalize Bets ─────────────────────────────
                    Button(
                        onClick  = {
                            // Automatically close all sides and the fight if not already done, then finalize
                            if (!bothClosed || fight.status != "closed") {
                                viewModel.updateAllSideStatus(context, fight.id, "closed", "closed")
                            }
                            viewModel.finalizeBet(context, fight.id)
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Lock, null)
                        Spacer(Modifier.width(8.dp))
                        Text("FINALIZE BETS", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // ── Closed → reopen or declare winner ─────────
            "closed" -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick  = {
                            viewModel.updateAllSideStatus(context, fight.id, "open", "open")
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(10.dp)
                    ) { Text("Reopen Betting", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

                    Button(
                        onClick  = {
                            viewModel.finalizeBet(context, fight.id)
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape    = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier           = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("DECLARE WINNER", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (label, color) = when (status) {
        "open"      -> "Open"      to MaterialTheme.colorScheme.primary
        "closed"    -> "Closed"    to MaterialTheme.colorScheme.error
        "pending"   -> "Pending"   to MaterialTheme.colorScheme.onSurfaceVariant
        "done"      -> "Done"      to MaterialTheme.colorScheme.tertiary
        "cancelled" -> "Cancelled" to MaterialTheme.colorScheme.error
        else        -> status      to MaterialTheme.colorScheme.onSurfaceVariant
    }
    SuggestionChip(
        onClick = {},
        label   = { Text(label, fontSize = 12.sp) },
        colors  = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.15f),
            labelColor     = color
        )
    )
}

@Composable
fun SideCard(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text       = label,
                fontSize   = 12.sp,
                color      = color,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = "₱$amount",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = color
            )
        }
    }
}