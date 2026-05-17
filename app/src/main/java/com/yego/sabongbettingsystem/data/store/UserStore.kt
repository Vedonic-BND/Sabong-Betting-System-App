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
        val TOKEN         = stringPreferencesKey("token")
        val CASHIN_TOKEN  = stringPreferencesKey("cashin_token")
        val CASHOUT_TOKEN = stringPreferencesKey("cashout_token")
        val NAME          = stringPreferencesKey("name")
        val ROLE          = stringPreferencesKey("role")
        val APP           = stringPreferencesKey("app")
        val USER_ID       = stringPreferencesKey("user_id")
    }

    val token        : Flow<String?> = context.dataStore.data.map { it[TOKEN] }
    val cashInToken  : Flow<String?> = context.dataStore.data.map { it[CASHIN_TOKEN] }
    val cashOutToken : Flow<String?> = context.dataStore.data.map { it[CASHOUT_TOKEN] }
    val name         : Flow<String?> = context.dataStore.data.map { it[NAME] }
    val role         : Flow<String?> = context.dataStore.data.map { it[ROLE] }
    val app          : Flow<String?> = context.dataStore.data.map { it[APP] }
    val userId       : Flow<String?> = context.dataStore.data.map { it[USER_ID] }

    suspend fun saveAdmin(token: String, name: String, userId: Int) {
        context.dataStore.edit {
            it[TOKEN]   = token
            it[NAME]    = name
            it[ROLE]    = "admin"
            it[APP]     = "admin"
            it[USER_ID] = userId.toString()
        }
    }

    suspend fun saveRunner(token: String, name: String, userId: Int) {
        context.dataStore.edit {
            it[TOKEN]   = token
            it[NAME]    = name
            it[ROLE]    = "runner"
            it[APP]     = "runner"
            it[USER_ID] = userId.toString()
        }
    }

    suspend fun saveTeller(
        cashInToken: String,
        cashOutToken: String,
        name: String,
        userId: Int
    ) {
        context.dataStore.edit {
            // For new system: both cashInToken and cashOutToken are the same
            // But also store in TOKEN for compatibility with bearerToken()
            it[TOKEN]         = cashInToken
            it[CASHIN_TOKEN]  = cashInToken
            it[CASHOUT_TOKEN] = cashOutToken
            it[NAME]          = name
            it[ROLE]          = "teller"
            it[APP]           = ""  // not selected yet
            it[USER_ID]       = userId.toString()
        }
    }

    suspend fun selectTellerMode(mode: String) {
        context.dataStore.edit {
            it[APP]   = mode
            it[TOKEN] = if (mode == "cashin")
                (context.dataStore.data.map { p -> p[CASHIN_TOKEN] ?: "" }
                    .let { "" }) // handled below
            else ""
        }
        // set active token based on mode
        val cashIn  = context.dataStore.data.map { it[CASHIN_TOKEN] ?: "" }
        val cashOut = context.dataStore.data.map { it[CASHOUT_TOKEN] ?: "" }
        context.dataStore.edit { prefs ->
            prefs[TOKEN] = if (mode == "cashin")
                prefs[CASHIN_TOKEN] ?: ""
            else
                prefs[CASHOUT_TOKEN] ?: ""
            prefs[APP] = mode
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
