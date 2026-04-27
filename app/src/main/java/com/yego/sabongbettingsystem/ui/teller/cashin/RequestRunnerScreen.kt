package com.yego.sabongbettingsystem.ui.teller.cashin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yego.sabongbettingsystem.viewmodel.CashInViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestRunnerScreen(
    navController: NavController,
    viewModel: CashInViewModel
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.requestSuccess.collectAsState()
    
    var selectedRequestType by remember { mutableStateOf<String?>(null) }
    var customMessage by remember { mutableStateOf("") }

    LaunchedEffect(success) {
        if (success) {
            // Auto-dismiss after 3 seconds and return to CashInScreen
            delay(3000)
            viewModel.clearRequestSuccess()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call Runner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        viewModel.clearRequestSuccess()
                        navController.popBackStack() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (success) {
                Spacer(Modifier.height(32.dp))
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Request Sent!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "A notification has been sent to available runners. Please wait for them to arrive.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(48.dp))
                Button(
                    onClick = {
                        viewModel.clearRequestSuccess()
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go Back", fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = "What Do You Need?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    text = "Select the type of assistance you need",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))

                // Request Type Selection
                RequestTypeOption(
                    title = "Assistance",
                    description = "General help at counter",
                    isSelected = selectedRequestType == "assistance",
                    onClick = { selectedRequestType = "assistance" }
                )

                Spacer(Modifier.height(12.dp))

                RequestTypeOption(
                    title = "Need Cash",
                    description = "Request additional cash",
                    isSelected = selectedRequestType == "need_cash",
                    onClick = { selectedRequestType = "need_cash" }
                )

                Spacer(Modifier.height(12.dp))

                RequestTypeOption(
                    title = "Collect Cash",
                    description = "Collection of excess cash",
                    isSelected = selectedRequestType == "collect_cash",
                    onClick = { selectedRequestType = "collect_cash" }
                )

                Spacer(Modifier.height(12.dp))

                RequestTypeOption(
                    title = "Other",
                    description = "Custom request",
                    isSelected = selectedRequestType == "other",
                    onClick = { selectedRequestType = "other" }
                )

                // Custom Message Input for "Other"
                if (selectedRequestType == "other") {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { customMessage = it },
                        label = { Text("Enter your message") },
                        placeholder = { Text("What do you need?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (error != null) {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { 
                        viewModel.requestRunner(
                            context, 
                            selectedRequestType ?: "assistance",
                            customMessage
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading && selectedRequestType != null
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text("CALL RUNNER NOW", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RequestTypeOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (isSelected) {
                RadioButton(
                    selected = true,
                    onClick = { },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
