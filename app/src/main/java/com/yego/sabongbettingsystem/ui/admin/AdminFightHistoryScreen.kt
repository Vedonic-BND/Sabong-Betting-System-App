package com.yego.sabongbettingsystem.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yego.sabongbettingsystem.data.model.Fight
import com.yego.sabongbettingsystem.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFightHistoryScreen(navController: NavController) {
    val context   = LocalContext.current
    val viewModel = viewModel<AdminViewModel>()
    val history   by viewModel.fightHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadFightHistory(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fight History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (history.isEmpty()) {
                Text(
                    text     = "No fights yet.",
                    modifier = Modifier.align(Alignment.Center),
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding      = PaddingValues(vertical = 12.dp)
                ) {
                    items(history) { fight ->
                        FightHistoryItem(fight = fight)
                    }
                }
            }
        }
    }
}

@Composable
fun FightHistoryItem(fight: Fight) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp)
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
                    text       = "Fight #${fight.fight_number}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = "Meron ₱${fight.meron_total}  |  Wala ₱${fight.wala_total}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(status = fight.status)
                if (fight.winner != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = "Winner: ${fight.winner.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (fight.winner == "meron")
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}