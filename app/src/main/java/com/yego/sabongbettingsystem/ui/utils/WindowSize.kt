package com.yego.sabongbettingsystem.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

enum class ScreenSize { COMPACT, MEDIUM, EXPANDED }

@Composable
fun rememberScreenSize(): ScreenSize {
    val configuration = LocalConfiguration.current
    return when {
        configuration.screenWidthDp >= 840 -> ScreenSize.EXPANDED  // large tablet
        configuration.screenWidthDp >= 600 -> ScreenSize.MEDIUM    // small tablet / landscape phone
        else                               -> ScreenSize.COMPACT   // phone
    }
}

@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= 600
}