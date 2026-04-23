package com.yego.sabongbettingsystem.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
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
fun AdminCreateFightScreen(navController: NavController) {
    val context      = LocalContext.current
    val viewModel    = viewModel<AdminViewModel>()
    val isLoading    by viewModel.isLoading.collectAsState()
    val error        by viewModel.error.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()
    val history      by viewModel.fightHistory.collectAsState()
    val currentFight by viewModel.currentFight.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }

    // calculate next fight number: get last fight number and add 1
    // if no fights exist (after reset), it will be 0 + 1 = 1
    val nextFightNumber = remember(history, currentFight) {
        val allNumbers = mutableListOf<Int>()
        
        // Get all fight numbers from history
        history.mapNotNullTo(allNumbers) { it.fight_number.toIntOrNull() }
        
        // Also check current fight if it exists
        currentFight?.fight_number?.toIntOrNull()?.let { allNumbers.add(it) }
        
        // Get the max number, default to 0 if no fights exist
        val lastNumber = allNumbers.maxOrNull() ?: 0
        
        (lastNumber + 1).toString()
    }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentFight(context)
        viewModel.loadFightHistory(context)
    }

    // handle side effects from ViewModel actions
    LaunchedEffect(actionResult) {
        when (actionResult) {
            "fight_created" -> {
                viewModel.clearResult()
                navController.popBackStack()
            }
            "fight_reset" -> {
                viewModel.clearResult()
                // After reset, explicitly reload to ensure UI shows #1
                viewModel.loadCurrentFight(context)
                viewModel.loadFightHistory(context)
            }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Fight", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
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
                text = "Next Fight Number",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "#$nextFightNumber",
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    viewModel.createFight(context, nextFightNumber)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape   = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(24.dp),
                        color       = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text("CREATE FIGHT #$nextFightNumber", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.RestartAlt, null)
                Spacer(Modifier.width(6.dp))
                Text("Reset Fight Counter to 1")
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = { navController.popBackStack() },
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    }
}
