package com.yego.sabongbettingsystem.ui.teller.cashin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashInScreen(
    navController: NavController,
    cashInViewModel: CashInViewModel,
    onLogout: () -> Unit
) {
    val context   = LocalContext.current
    val userStore = remember { UserStore(context) }
    val name      by userStore.name.collectAsState(initial = "Teller")
    val viewModel = cashInViewModel
    val fight     by viewModel.currentFight.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error     by viewModel.error.collectAsState()
    val betResult by viewModel.betResult.collectAsState()

    var amount       by remember { mutableStateOf("") }
    var selectedSide by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentFight(context)
    }

    // navigate to receipt on success
    LaunchedEffect(betResult) {
        if (betResult != null) {
            navController.navigate("receipt/${betResult!!.reference}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cash In", fontWeight = FontWeight.Bold)
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text       = "Fight #${fight!!.fight_number}",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            SuggestionChip(
                                onClick = {},
                                label   = {
                                    Text(
                                        text     = fight!!.status.uppercase(),
                                        fontSize = 11.sp
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    labelColor     = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // meron / wala totals
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                                )
                            ) {
                                Column(
                                    modifier            = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text       = "MERON",
                                        fontSize   = 11.sp,
                                        color      = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text       = "₱${fight!!.meron_total}",
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                )
                            ) {
                                Column(
                                    modifier            = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text       = "WALA",
                                        fontSize   = 11.sp,
                                        color      = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text       = "₱${fight!!.wala_total}",
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Place Bet (only if fight is open) ─────────
            if (fight != null && fight!!.status == "open") {

                Text(
                    text       = "Place Bet",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // side selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick  = { selectedSide = "meron" },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSide == "meron")
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            contentColor = if (selectedSide == "meron")
                                MaterialTheme.colorScheme.onError
                            else
                                MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("MERON", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick  = { selectedSide = "wala" },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSide == "wala")
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            contentColor = if (selectedSide == "wala")
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("WALA", fontWeight = FontWeight.Bold)
                    }
                }

                // amount input
                OutlinedTextField(
                    value         = amount,
                    onValueChange = { amount = it },
                    label         = { Text("Bet Amount") },
                    prefix        = { Text("₱") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                // submit button
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull()
                        if (amt != null && selectedSide.isNotEmpty()) {
                            viewModel.placeBet(context, selectedSide, amt)
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
            } else if (fight != null && fight!!.status != "open") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text     = "Betting is currently ${fight!!.status}. Waiting for fight to open.",
                        modifier = Modifier.padding(16.dp),
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        style    = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}