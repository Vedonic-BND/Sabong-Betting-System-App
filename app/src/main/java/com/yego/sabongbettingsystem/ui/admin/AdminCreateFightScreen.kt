package com.yego.sabongbettingsystem.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    var fightNumber by remember { mutableStateOf("") }

    // go back on success
    LaunchedEffect(actionResult) {
        if (actionResult == "fight_created") {
            viewModel.clearResult()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Fight", fontWeight = FontWeight.Bold) },
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

            OutlinedTextField(
                value         = fightNumber,
                onValueChange = { fightNumber = it },
                label         = { Text("Fight Number") },
                placeholder   = { Text("e.g. Fight 1") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    viewModel.createFight(context, fightNumber.trim())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape   = RoundedCornerShape(12.dp),
                enabled = fightNumber.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Fight", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}