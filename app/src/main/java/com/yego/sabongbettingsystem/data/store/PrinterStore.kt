package com.yego.sabongbettingsystem.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PrinterStore(private val context: Context) {

    companion object {
        val PRINTER_ADDRESS = stringPreferencesKey("printer_address")
        val PRINTER_NAME    = stringPreferencesKey("printer_name")
    }

    val printerAddress: Flow<String?> = context.dataStore.data
        .map { it[PRINTER_ADDRESS] }

    val printerName: Flow<String?> = context.dataStore.data
        .map { it[PRINTER_NAME] }

    suspend fun save(address: String, name: String) {
        context.dataStore.edit {
            it[PRINTER_ADDRESS] = address
            it[PRINTER_NAME]    = name
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(PRINTER_ADDRESS)
            it.remove(PRINTER_NAME)
        }
    }
}