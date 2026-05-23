package com.yego.sabongbettingsystem.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.yego.sabongbettingsystem.data.realtime.ReverbManager
import com.yego.sabongbettingsystem.data.store.UserStore
import kotlinx.coroutines.Dispatchers

data class ReverbFightState(
    val fightNumber  : String  = "",
    val status       : String  = "",
    val meronStatus  : String  = "open",
    val walaStatus   : String  = "open",
    val winner       : String? = null,
    val meronTotal   : Double  = 0.0,
    val walaTotal    : Double  = 0.0,
)

class ReverbViewModel : ViewModel() {

    private val _connected   = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private val _fightState  = MutableStateFlow<ReverbFightState?>(null)
    val fightState: StateFlow<ReverbFightState?> = _fightState

    private val _lastBet     = MutableStateFlow<JSONObject?>(null)
    val lastBet: StateFlow<JSONObject?> = _lastBet

    private val _betDeleted  = MutableStateFlow<JSONObject?>(null)
    val betDeleted: StateFlow<JSONObject?> = _betDeleted

    private val _cashUpdated = MutableStateFlow<JSONObject?>(null)
    val cashUpdated: StateFlow<JSONObject?> = _cashUpdated

    private val _runnerAccepted = MutableStateFlow<JSONObject?>(null)
    val runnerAccepted: StateFlow<JSONObject?> = _runnerAccepted

    private val _runnerDeclined = MutableStateFlow<JSONObject?>(null)
    val runnerDeclined: StateFlow<JSONObject?> = _runnerDeclined
    
    // Store current user ID for filtering notifications
    private var currentUserId: Long = -1

    fun resetState() {
        _fightState.value = null
        _lastBet.value = null
        _betDeleted.value = null
        _runnerAccepted.value = null
        _runnerDeclined.value = null
    }

    fun clearRunnerAccepted() {
        _runnerAccepted.value = null
    }

    fun clearRunnerDeclined() {
        _runnerDeclined.value = null
    }

    fun setBetDeleted(data: JSONObject?) {
        _betDeleted.value = data
    }

    fun clearBetDeleted() {
        _betDeleted.value = null
    }

    fun connect(context: Any? = null, userId: Long = -1) {
        // Store the current user ID for filtering
        currentUserId = userId
        
        // If userId wasn't provided or is -1, try to fetch from UserStore
        if (currentUserId == -1L && context is Context) {
            viewModelScope.launch {
                try {
                    val userStore = UserStore(context)
                    val storedUserId = userStore.userId.first()?.toLongOrNull()
                    if (storedUserId != null && storedUserId != -1L) {
                        currentUserId = storedUserId
                        android.util.Log.d("ReverbVM", "✅ Fetched userId from UserStore: $currentUserId")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ReverbVM", "Failed to fetch userId from UserStore: ${e.message}")
                }
            }
        } else if (currentUserId != -1L) {
            android.util.Log.d("ReverbVM", "Using provided userId: $currentUserId")
        }
        
        // 1. set callbacks FIRST (only for events this screen cares about)
        // NOTE: Don't override onConnected/onDisconnected - let RunnerViewModel or other screens manage those
        // BUT we still track connection status for UI purposes in ReverbViewModel
        
        // Track connection status for UI badge (WsStatusBadge)
        ReverbManager.onConnected = {
            viewModelScope.launch(Dispatchers.Main) {
                _connected.value = true
                android.util.Log.d("ReverbVM", "✅ WebSocket connected - ReverbVM UI updated")
            }
        }
        
        ReverbManager.onDisconnected = {
            viewModelScope.launch(Dispatchers.Main) {
                _connected.value = false
                android.util.Log.d("ReverbVM", "❌ WebSocket disconnected - ReverbVM UI updated")
            }
        }
        
        ReverbManager.onFightUpdated = { data ->
            viewModelScope.launch(Dispatchers.Main) {
                val current = _fightState.value
                _fightState.value = ReverbFightState(
                    fightNumber = data.optString("fight_number", current?.fightNumber ?: ""),
                    status      = data.optString("status", current?.status ?: ""),
                    meronStatus = data.optString("meron_status", current?.meronStatus ?: "open"),
                    walaStatus  = data.optString("wala_status", current?.walaStatus ?: "open"),
                    winner      = data.optString("winner", current?.winner),
                    meronTotal  = data.optDouble("meron_total", current?.meronTotal ?: 0.0),
                    walaTotal   = data.optDouble("wala_total", current?.walaTotal ?: 0.0),
                )
            }
        }
        
        ReverbManager.onBetPlaced = { data ->
            viewModelScope.launch(Dispatchers.Main) {
                val current = _fightState.value
                // When a bet is placed, we usually get updated totals and possibly status
                _fightState.value = ReverbFightState(
                    fightNumber = data.optString("fight_number", current?.fightNumber ?: ""),
                    status      = data.optString("status", current?.status ?: ""),
                    meronStatus = data.optString("meron_status", current?.meronStatus ?: "open"),
                    walaStatus  = data.optString("wala_status", current?.walaStatus ?: "open"),
                    winner      = data.optString("winner", current?.winner),
                    meronTotal  = data.optDouble("meron_total", current?.meronTotal ?: 0.0),
                    walaTotal   = data.optDouble("wala_total", current?.walaTotal ?: 0.0),
                )
                _lastBet.value = data
            }
        }

        ReverbManager.onBetDeleted = { data ->
            viewModelScope.launch(Dispatchers.Main) {
                android.util.Log.d("ReverbVM", "🎯 onBetDeleted received: $data")
                val current = _fightState.value
                // When a bet is deleted, update the fight totals
                _fightState.value = ReverbFightState(
                    fightNumber = data.optString("fight_number", current?.fightNumber ?: ""),
                    status      = data.optString("status", current?.status ?: ""),
                    meronStatus = data.optString("meron_status", current?.meronStatus ?: "open"),
                    walaStatus  = data.optString("wala_status", current?.walaStatus ?: "open"),
                    winner      = data.optString("winner", current?.winner),
                    meronTotal  = data.optDouble("meron_total", current?.meronTotal ?: 0.0),
                    walaTotal   = data.optDouble("wala_total", current?.walaTotal ?: 0.0),
                )
                // Also trigger the betDeleted event so UI can refresh
                _betDeleted.value = data
                android.util.Log.d("ReverbVM", "✅ betDeleted event triggered and state updated")
            }
        }

        ReverbManager.onWinnerDeclared = { data ->
            viewModelScope.launch(Dispatchers.Main) {
                try {
                    val current = _fightState.value
                    val fightNumber = data.optString("fight_number", "") ?: ""
                    val currentFightNumber = current?.fightNumber ?: ""
                    
                    if (fightNumber == currentFightNumber && fightNumber.isNotEmpty()) {
                        _fightState.value = current?.copy(
                            status = "done",
                            winner = data.optString("winner") ?: "",
                        )
                        android.util.Log.d("ReverbVM", "✅ Current fight #$fightNumber winner declared")
                    } else {
                        android.util.Log.d("ReverbVM", "📢 Reannouncement detected: fight #$fightNumber != current #$currentFightNumber")
                        _fightState.value = current?.copy(
                            winner = data.optString("winner") ?: "",
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ReverbVM", "Error in onWinnerDeclared: ${e.message}")
                }
            }
        }

        ReverbManager.onTellerCashUpdated = { data ->
            viewModelScope.launch(Dispatchers.Main) {
                _cashUpdated.value = data
            }
        }

        ReverbManager.onRunnerAccepted = { data ->
            viewModelScope.launch(Dispatchers.Main) {
                // For RunnerAccepted events, we should show them to:
                // 1. The teller who made the request (teller_id matches current user)
                // 2. Always show if we couldn't get user ID (currentUserId = -1)
                // This ensures tellers always see when their requests are accepted
                val tellerIdInNotif = data.optLong("teller_id", -1L)
                
                android.util.Log.d("ReverbVM", "RunnerAccepted Event: teller_id=$tellerIdInNotif, currentUserId=$currentUserId")
                
                // Show if: (1) it's for this teller, OR (2) we don't have a user ID yet
                if (currentUserId == -1L || tellerIdInNotif == currentUserId) {
                    android.util.Log.d("ReverbVM", "✅ Setting runnerAccepted - MATCH! (showing to teller)")
                    _runnerAccepted.value = data
                } else {
                    android.util.Log.d("ReverbVM", "❌ Filtering out runnerAccepted - not for current teller (teller_id=$tellerIdInNotif, currentUserId=$currentUserId)")
                }
            }
        }

        ReverbManager.onRunnerDeclined = { data ->
            viewModelScope.launch(Dispatchers.Main) {
                _runnerDeclined.value = data
            }
        }

        // 2. THEN connect
        ReverbManager.connect()
    }

    fun disconnect() {
        // ⚠️ IMPORTANT: Don't disconnect the global ReverbManager singleton!
        // The WebSocket should remain connected for the entire app lifetime.
        // Multiple screens may need real-time events, so we cannot disconnect
        // when one screen closes.
        android.util.Log.d("ReverbVM", "⚠️  Skipping disconnect() - ReverbManager is global singleton")
    }

    override fun onCleared() {
        super.onCleared()
        // Don't call disconnect() - see comment above
        android.util.Log.d("ReverbVM", "ReverbVM cleared (no disconnect)")
    }
}
