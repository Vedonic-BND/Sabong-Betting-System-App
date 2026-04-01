package com.yego.sabongbettingsystem.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.yego.sabongbettingsystem.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFightDetailScreen(
    navController: NavController,
    fightId: Int
) {
    val context      = LocalContext.current
    val viewModel    = viewModel<AdminViewModel>()
    val fight        by viewModel.currentFight.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val error        by viewModel.error.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingWinner     by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentFight(context)
    }

    LaunchedEffect(actionResult) {
        if (actionResult == "winner_declared") {
            viewModel.clearResult()
            navController.popBackStack()
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title   = { Text("Confirm Winner") },
            text    = {
                Text("Declare ${pendingWinner.uppercase()} as the winner? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.declareWinner(context, fightId, pendingWinner)
                    }
                ) { Text("Confirm") }
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
                    Text(
                        text = fight?.let { "Fight #${it.fight_number}" } ?: "Fight Detail",
                        fontWeight = FontWeight.Bold
                    )
                },
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

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (fight != null) {

                // totals
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

                Text(
                    text  = "Declare Winner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // winner buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            pendingWinner     = "meron"
                            showConfirmDialog = true
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = !isLoading
                    ) {
                        Text("MERON WINS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            pendingWinner     = "wala"
                            showConfirmDialog = true
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !isLoading
                    ) {
                        Text("WALA WINS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // draw / cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            pendingWinner     = "draw"
                            showConfirmDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        enabled  = !isLoading
                    ) { Text("Draw") }

                    OutlinedButton(
                        onClick = {
                            pendingWinner     = "cancelled"
                            showConfirmDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        enabled  = !isLoading
                    ) { Text("Cancel Fight") }
                }
            }
        }
    }
}