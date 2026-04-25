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

    private val _cashUpdated = MutableStateFlow<JSONObject?>(null)
    val cashUpdated: StateFlow<JSONObject?> = _cashUpdated

    fun resetState() {
        _fightState.value = null
        _lastBet.value = null
    }

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

        ReverbManager.onWinnerDeclared = { data ->
            viewModelScope.launch(Dispatchers.Main) {
                val current = _fightState.value
                _fightState.value = current?.copy(
                    status = "done",
                    winner = data.optString("winner"),
                )
            }
        }

        ReverbManager.onTellerCashUpdated = { data ->
            viewModelScope.launch(Dispatchers.Main) {
                _cashUpdated.value = data
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