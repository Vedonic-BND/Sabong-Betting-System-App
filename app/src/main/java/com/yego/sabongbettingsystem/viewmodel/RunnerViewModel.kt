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
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*

data class RunnerNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: String,
    val data: JSONObject? = null,
    var isRead: Boolean = false
)

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

    private val _incomingRequest = MutableStateFlow<JSONObject?>(null)
    val incomingRequest: StateFlow<JSONObject?> = _incomingRequest

    private val _notifications = MutableStateFlow<List<RunnerNotification>>(emptyList())
    val notifications: StateFlow<List<RunnerNotification>> = _notifications

    private suspend fun bearerToken(context: Context): String {
        val token = UserStore(context).token.first() ?: ""
        return "Bearer $token"
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }

    fun clearIncomingRequest() {
        _incomingRequest.value = null
    }

    fun markNotificationAsRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    fun setupRealtimeListener(context: Context) {
        ReverbManager.onTellerCashUpdated = { data ->
            // When a teller's cash is updated, refresh the tellers list
            loadTellers(context)
        }
        
        ReverbManager.onCashRequested = { data ->
            viewModelScope.launch(Dispatchers.Main) {
                _incomingRequest.value = data
                
                // Add to notification history
                val tellerName = data.optString("teller_name", "A teller")
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val timestamp = sdf.format(Date())
                
                val newNotification = RunnerNotification(
                    title = "Runner Requested",
                    message = "$tellerName needs assistance.",
                    timestamp = timestamp,
                    data = data
                )
                _notifications.value = listOf(newNotification) + _notifications.value
            }
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

    fun acceptRequest(context: Context, requestId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.api.acceptCashRequest(bearerToken(context), requestId)
                if (response.isSuccessful) {
                    _successMessage.value = "Request accepted! Heading to the teller now."
                    _incomingRequest.value = null
                    // Refresh tellers and history
                    loadTellers(context)
                    loadHistory(context)
                } else {
                    _error.value = "Failed to accept request"
                }
            } catch (e: Exception) {
                _error.value = "Connection error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun declineRequest(context: Context, requestId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.api.declineCashRequest(bearerToken(context), requestId)
                if (response.isSuccessful) {
                    _incomingRequest.value = null
                } else {
                    _error.value = "Failed to decline request"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
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
