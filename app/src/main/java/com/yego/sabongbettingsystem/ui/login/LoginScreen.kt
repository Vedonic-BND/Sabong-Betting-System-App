package com.yego.sabongbettingsystem.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yego.sabongbettingsystem.viewmodel.LoginState
import com.yego.sabongbettingsystem.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: (role: String, app: String) -> Unit
) {
    val context   = LocalContext.current
    val viewModel = viewModel<LoginViewModel>()
    val state     by viewModel.state.collectAsState()

    var username        by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var showPassword    by remember { mutableStateOf(false) }
    var selectedApp     by remember { mutableStateOf("admin") }

    val apps = listOf(
        "admin"   to "Admin",
        "cashin"  to "Cash In",
        "cashout" to "Cash Out",
    )

    // handle state
    LaunchedEffect(state) {
        if (state is LoginState.Success) {
            val s = state as LoginState.Success
            onLoginSuccess(s.role, s.app)
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── Logo ──────────────────────────────────
                Text(text = "🐓", fontSize = 48.sp)
                Text(
                    text = "Sabong Betting",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sign in to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── App selector ─────────────────────────
                Text(
                    text = "Login as",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    apps.forEach { (value, label) ->
                        FilterChip(
                            selected = selectedApp == value,
                            onClick  = { selectedApp = value },
                            label    = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Username ──────────────────────────────
                OutlinedTextField(
                    value         = username,
                    onValueChange = { username = it },
                    label         = { Text("Username") },
                    leadingIcon   = { Icon(Icons.Default.Person, null) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp)
                )

                // ── Password ──────────────────────────────
                OutlinedTextField(
                    value         = password,
                    onValueChange = { password = it },
                    label         = { Text("Password") },
                    leadingIcon   = { Icon(Icons.Default.Lock, null) },
                    trailingIcon  = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (showPassword)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    modifier   = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape      = RoundedCornerShape(12.dp)
                )

                // ── Error ─────────────────────────────────
                if (state is LoginState.Error) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text  = (state as LoginState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // ── Login button ──────────────────────────
                Button(
                    onClick = {
                        viewModel.login(context, username, password, selectedApp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape   = RoundedCornerShape(12.dp),
                    enabled = state !is LoginState.Loading
                ) {
                    if (state is LoginState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color    = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text     = "Login",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

            }
        }
    }
}