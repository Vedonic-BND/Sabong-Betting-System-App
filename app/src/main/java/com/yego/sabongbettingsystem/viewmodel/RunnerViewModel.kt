package com.yego.sabongbettingsystem.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yego.sabongbettingsystem.data.api.RetrofitClient
import com.yego.sabongbettingsystem.data.model.RunnerTransactionRequest
import com.yego.sabongbettingsystem.data.model.RunnerTransactionResponse
import com.yego.sabongbettingsystem.data.model.TellerCashStatus
import com.yego.sabongbettingsystem.data.store.UserStore
import com.yego.sabongbettingsystem.data.realtime.ReverbManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

class RunnerViewModel : ViewModel() {

    private val _tellers = MutableStateFlow<List<TellerCashStatus>>(emptyList())
    val tellers: StateFlow<List<TellerCashStatus>> = _tellers

    private val _history = MutableStateFlow<List<RunnerTransactionResponse>>(emptyList())
    val history: StateFlow<List<RunnerTransactionResponse>> = _history

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private suspend fun bearerToken(context: Context): String {
        val token = UserStore(context).token.first() ?: ""
        return "Bearer $token"
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }

    fun setupRealtimeListener(context: Context) {
        ReverbManager.onTellerCashUpdated = { data ->
            // When a teller's cash is updated, refresh the tellers list
            loadTellers(context)
        }
    }

    fun loadTellers(context: Context) {
        viewModelScope.launch {
            // Only show loading indicator if list is empty (first load)
            if (_tellers.value.isEmpty()) _isLoading.value = true
            try {
                val response = RetrofitClient.api.getTellersCashStatus(bearerToken(context))
                if (response.isSuccessful) {
                    _tellers.value = response.body() ?: emptyList()
                } else {
                    // _error.value = "Failed to load tellers"
                }
            } catch (e: Exception) {
                // _error.value = "Connection error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadHistory(context: Context) {
        viewModelScope.launch {
            if (_history.value.isEmpty()) _isLoading.value = true
            try {
                val response = RetrofitClient.api.getRunnerHistory(bearerToken(context))
                if (response.isSuccessful) {
                    _history.value = response.body() ?: emptyList()
                } else {
                    // _error.value = "Failed to load history"
                }
            } catch (e: Exception) {
                // _error.value = "Connection error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createTransaction(context: Context, tellerId: Int, amount: Double, type: String) {
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            try {
                val request = RunnerTransactionRequest(tellerId, amount, type)
                val response = RetrofitClient.api.createRunnerTransaction(bearerToken(context), request)
                if (response.isSuccessful) {
                    _successMessage.value = "Transaction successful"
                    loadTellers(context)
                    loadHistory(context)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = try {
                        org.json.JSONObject(errorBody ?: "").getString("message")
                    } catch (e: Exception) {
                        "Transaction failed"
                    }
                    _error.value = message
                }
            } catch (e: Exception) {
                _error.value = "Connection error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Auto Refresh Logic ───────────────────────────────
    private var refreshJob: Job? = null

    fun startAutoRefresh(context: Context) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                loadTellers(context)
                loadHistory(context)
                delay(5000) // refresh every 5 seconds
            }
        }
    }

    fun stopAutoRefresh() {
        refreshJob?.cancel()
    }

    fun logout(context: Context, onDone: () -> Unit) {
        stopAutoRefresh()
        viewModelScope.launch {
            try {
                RetrofitClient.api.logout(bearerToken(context))
            } catch (_: Exception) { }
            UserStore(context).clear()
            onDone()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
