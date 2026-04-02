package com.yego.sabongbettingsystem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WsStatusBadge(connected: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (connected)
                    Color(0xFF166534).copy(alpha = 0.15f)
                else
                    Color(0xFF991B1B).copy(alpha = 0.15f)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (connected) Color(0xFF16A34A)
                    else           Color(0xFFDC2626)
                )
        )
        Text(
            text     = if (connected) "Live" else "Connecting...",
            fontSize = 11.sp,
            color    = if (connected) Color(0xFF16A34A) else Color(0xFFDC2626)
        )
    }
}