package com.yego.sabongbettingsystem.ui.utils

/**
 * Safely parses a currency string (e.g., "1,234.56", "₱ 500.00") into a Double.
 */
fun String?.parseCurrency(): Double {
    if (this == null) return 0.0
    return try {
        // Remove currency symbols, commas, and spaces
        this.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0
    } catch (e: Exception) {
        0.0
    }
}
