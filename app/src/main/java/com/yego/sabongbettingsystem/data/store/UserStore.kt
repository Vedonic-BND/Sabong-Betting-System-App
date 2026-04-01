package com.yego.sabongbettingsystem.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserStore(private val context: Context) {

    companion object {
        val TOKEN    = stringPreferencesKey("token")
        val NAME     = stringPreferencesKey("name")
        val ROLE     = stringPreferencesKey("role")
        val APP      = stringPreferencesKey("app")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[TOKEN] }
    val name:  Flow<String?> = context.dataStore.data.map { it[NAME] }
    val role:  Flow<String?> = context.dataStore.data.map { it[ROLE] }
    val app:   Flow<String?> = context.dataStore.data.map { it[APP] }

    suspend fun save(token: String, name: String, role: String, app: String) {
        context.dataStore.edit {
            it[TOKEN] = token
            it[NAME]  = name
            it[ROLE]  = role
            it[APP]   = app
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}