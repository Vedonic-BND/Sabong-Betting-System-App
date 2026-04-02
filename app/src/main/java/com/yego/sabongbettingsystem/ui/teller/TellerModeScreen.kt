package com.yego.sabongbettingsystem.ui.teller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yego.sabongbettingsystem.data.store.UserStore
import kotlinx.coroutines.launch

@Composable
fun TellerModeScreen(
    onCashIn  : () -> Unit,
    onCashOut : () -> Unit,
    onLogout  : () -> Unit
) {
    val context = LocalContext.current
    val store   = remember { UserStore(context) }
    val name    by store.name.collectAsState(initial = "Teller")
    val scope   = rememberCoroutineScope()

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Text(text = "🐓", fontSize = 48.sp)

            Text(
                text       = "Welcome, $name!",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )

            Text(
                text      = "Select your mode for this session",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // ── Cash In button ────────────────────────────
            Button(
                onClick = {
                    scope.launch {
                        store.selectTellerMode("cashin")
                        onCashIn()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector        = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    modifier           = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text       = "Cash In",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text     = "Place bets for players",
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            // ── Cash Out button ───────────────────────────
            Button(
                onClick = {
                    scope.launch {
                        store.selectTellerMode("cashout")
                        onCashOut()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector        = Icons.Default.Payments,
                    contentDescription = null,
                    modifier           = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text       = "Cash Out",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text     = "Pay out winners",
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Logout ────────────────────────────────────
            TextButton(
                onClick = {
                    scope.launch {
                        store.clear()
                        onLogout()
                    }
                }
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null,
                    modifier           = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Logout")
            }
        }
    }
}