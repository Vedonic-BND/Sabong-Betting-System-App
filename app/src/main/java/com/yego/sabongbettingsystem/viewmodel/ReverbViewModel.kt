package com.yego.sabongbettingsystem.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.yego.sabongbettingsystem.data.realtime.ReverbManager
import kotlinx.coroutines.Dispatchers

data class ReverbFightState(
    val fightNumber : String  = "",
    val status      : String  = "",
    val winner      : String? = null,
    val meronTotal  : Double  = 0.0,
    val walaTotal   : Double  = 0.0,
)

class ReverbViewModel : ViewModel() {

    private val _connected   = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private val _fightState  = MutableStateFlow<ReverbFightState?>(null)
    val fightState: StateFlow<ReverbFightState?> = _fightState

    private val _lastBet     = MutableStateFlow<JSONObject?>(null)
    val lastBet: StateFlow<JSONObject?> = _lastBet

    fun connect() {
        // 1. set callbacks FIRST
        ReverbManager.onConnected = {
            viewModelScope.launch(Dispatchers.Main) { _connected.value = true }
        }
        ReverbManager.onDisconnected = {
            viewModelScope.launch(Dispatchers.Main) { _connected.value = false }
        }
        ReverbManager.onFightUpdated = { data ->
            viewModelScope.launch(Dispatchers.Main) {
                _fightState.value = ReverbFightState(
                    fightNumber = data.optString("fight_number"),
                    status      = data.optString("status"),
                    winner      = data.optString("winner").takeIf { it.isNotEmpty() },
                    meronTotal  = data.optDouble("meron_total", 0.0),
                    walaTotal   = data.optDouble("wala_total", 0.0),
                )
            }
        }
        ReverbManager.onBetPlaced = { data ->
            viewModelScope.launch(Dispatchers.Main) {
                val current = _fightState.value
                _fightState.value = ReverbFightState(
                    fightNumber = current?.fightNumber ?: "",
                    status      = current?.status ?: "open",
                    winner      = current?.winner,
                    meronTotal  = data.optDouble("meron_total", current?.meronTotal ?: 0.0),
                    walaTotal   = data.optDouble("wala_total",  current?.walaTotal  ?: 0.0),
                )
            }
        }
        ReverbManager.onWinnerDeclared = { data ->
            viewModelScope.launch(Dispatchers.Main) {
                val current = _fightState.value
                _fightState.value = current?.copy(
                    status = "done",
                    winner = data.optString("winner"),
                )
            }
        }

        // 2. THEN connect
        ReverbManager.connect()
    }

    fun disconnect() {
        ReverbManager.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}