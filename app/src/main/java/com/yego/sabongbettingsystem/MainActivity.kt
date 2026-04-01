package com.yego.sabongbettingsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yego.sabongbettingsystem.ui.AppNavigation
import com.yego.sabongbettingsystem.ui.theme.SabongBettingSystemTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SabongBettingSystemTheme {
                AppNavigation()
            }
        }
    }
}