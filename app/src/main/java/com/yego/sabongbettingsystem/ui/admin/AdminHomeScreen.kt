package com.yego.sabongbettingsystem.ui.admin

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    navController: NavController,
    onLogout: () -> Unit
) {
    val context   = LocalContext.current
    val userStore = remember { UserStore(context) }
    val name      by userStore.name.collectAsState(initial = "Admin")
    val viewModel = viewModel<AdminViewModel>()
    val fight     by viewModel.currentFight.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error     by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCurrentFight(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Panel", fontWeight = FontWeight.Bold)
                        Text(
                            text  = "Welcome, $name",
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            text  = "No active fight",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text       = "Fight #${fight!!.fight_number}",
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            StatusChip(status = fight!!.status)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SideCard(
                                label    = "MERON",
                                amount   = fight!!.meron_total,
                                color    = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            SideCard(
                                label    = "WALA",
                                amount   = fight!!.wala_total,
                                color    = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (fight!!.status != "done" && fight!!.status != "cancelled") {
                            FightActionButtons(
                                fight         = fight!!,
                                viewModel     = viewModel,
                                context       = context,
                                navController = navController
                            )
                        }
                    }
                }
            }

            // ── Quick Actions ─────────────────────────────
            Text(
                text  = "Actions",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick  = { navController.navigate("admin_create_fight") },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("New Fight")
                }

                OutlinedButton(
                    onClick  = { navController.navigate("admin_history") },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("History")
                }
            }
        }
    }
}

@Composable
fun FightActionButtons(
    fight: Fight,
    viewModel: AdminViewModel,
    context: Context,
    navController: NavController
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (fight.status) {
            "pending" -> {
                Button(
                    onClick  = { viewModel.updateStatus(context, fight.id, "open") },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Open Betting")
                }
            }
            "open" -> {
                Button(
                    onClick  = { viewModel.updateStatus(context, fight.id, "closed") },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Close Betting")
                }
            }
            "closed" -> {
                Button(
                    onClick  = { navController.navigate("admin_fight/${fight.id}") },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Declare Winner")
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