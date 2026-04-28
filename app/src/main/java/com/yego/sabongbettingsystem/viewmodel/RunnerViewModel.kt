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
    val activeRequest: StateFlow<JSONObject?> = _incomingRequest

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

    fun addNotification(title: String, message: String, data: org.json.JSONObject? = null, context: android.content.Context? = null) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timestamp = sdf.format(Date())
        val newNotification = RunnerNotification(
            title = title,
            message = message,
            timestamp = timestamp,
            data = data
        )
        _notifications.value = listOf(newNotification) + _notifications.value

        // Save to database
        if (context != null) {
            viewModelScope.launch {
                try {
                    val notificationRequest = com.yego.sabongbettingsystem.data.model.NotificationRequest(
                        title = title,
                        message = message,
                        data = data?.toString()
                    )
                    val token = bearerToken(context)
                    RetrofitClient.api.saveNotification(token, notificationRequest)
                } catch (e: Exception) {
                    android.util.Log.e("RunnerVM", "Failed to save notification to database", e)
                }
            }
        }
    }

    fun loadSavedNotifications(context: Context) {
        viewModelScope.launch {
            try {
                val token = bearerToken(context)
                android.util.Log.d("RunnerVM", "Fetching notifications with token: ${token.take(20)}...")
                val response = RetrofitClient.api.getNotifications(token)
                android.util.Log.d("RunnerVM", "Notifications response code: ${response.code()}")
                if (response.isSuccessful) {
                    val savedNotifications = response.body()?.map { notif ->
                        android.util.Log.d("RunnerVM", "Loaded notification: ${notif.title} - ${notif.message}")
                        RunnerNotification(
                            id = notif.id.toString(),
                            title = notif.title,
                            message = notif.message,
                            timestamp = notif.timestamp,
                            data = try { org.json.JSONObject(notif.data ?: "{}") } catch (e: Exception) { null },
                            isRead = notif.is_read
                        )
                    } ?: emptyList()
                    android.util.Log.d("RunnerVM", "Total notifications loaded: ${savedNotifications.size}")
                    _notifications.value = savedNotifications
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("RunnerVM", "Failed to load notifications: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                android.util.Log.e("RunnerVM", "Failed to load saved notifications", e)
            }
        }
    }

    fun setupRealtimeListener(context: Context) {
        ReverbManager.onTellerCashUpdated = { data ->
            // When a teller's cash is updated, refresh the tellers list
            viewModelScope.launch(Dispatchers.Main) {
                loadTellers(context)
            }
        }
        
        ReverbManager.onCashRequested = { data ->
            viewModelScope.launch(Dispatchers.Main) {
                _incomingRequest.value = data
                
                // Add to notification history (will be saved to database)
                val tellerName = data.optString("teller_name", "A teller")
                val requestType = data.optString("request_type", "assistance")
                val customMessage = data.optString("custom_message", "")
                
                val displayMessage = when (requestType) {
                    "assistance" -> "Assistance needed at counter"
                    "need_cash" -> "Runner needed - Need cash"
                    "collect_cash" -> "Runner needed - Collect excess cash"
                    "other" -> "Custom request: $customMessage"
                    else -> "Assistance needed at counter"
                }
                
                addNotification(
                    title = "Runner Requested",
                    message = "$tellerName - $displayMessage",
                    data = data,
                    context = context
                )
                
                // Trigger sound and vibration immediately on real-time notification
                triggerSoundAndVibration(context)
            }
        }

        ReverbManager.onRunnerAccepted = { data ->
            viewModelScope.launch(Dispatchers.Main) {
                val assignedRunnerName = data.optString("runner_name", "A runner")
                val tellerName = data.optString("teller_name", "A teller")
                
                addNotification(
                    title = "Request Assigned",
                    message = "$assignedRunnerName has been assigned to $tellerName.",
                    data = data,
                    context = context
                )
            }
        }
    }

    fun loadTellers(context: Context) {
        viewModelScope.launch {
            if (_tellers.value.isEmpty()) _isLoading.value = true
            try {
                val token = bearerToken(context)
                if (token.length < 10) {
                    _error.value = "Authentication error. Please login again."
                    return@launch
                }
                val response = RetrofitClient.api.getTellersCashStatus(token)
                if (response.isSuccessful) {
                    // API returns a list directly or wrapped in { "data": [...] }
                    // Handle the response body based on updated model
                    val responseBody = response.body()
                    _tellers.value = responseBody ?: emptyList()
                    android.util.Log.d("RunnerVM", "Tellers loaded: ${_tellers.value.size}")
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val errorMsg = try {
                        if (errorBody.isNotBlank()) {
                            org.json.JSONObject(errorBody).optString("message", "Failed to load tellers")
                        } else {
                            "Failed to load tellers: ${response.code()}"
                        }
                    } catch (e: Exception) {
                        "Failed to load tellers: ${response.code()}"
                    }
                    _error.value = errorMsg
                    android.util.Log.e("RunnerVM", "Error loading tellers: $errorBody")
                }
            } catch (e: Exception) {
                _error.value = "Connection error: Unable to reach server"
                android.util.Log.e("RunnerVM", "Exception loading tellers", e)
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
                _error.value = "Connection error"
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
                // Convert collect/provide to cash_in/cash_out for API
                val apiType = if (type == "collect") "cash_out" else "cash_in"
                val request = RunnerTransactionRequest(tellerId, amount, apiType)
                val response = RetrofitClient.api.createRunnerTransaction(bearerToken(context), request)
                if (response.isSuccessful) {
                    _successMessage.value = "Transaction successful"
                    loadTellers(context)
                    loadHistory(context)
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val message = try {
                        if (errorBody.isNotBlank()) {
                            org.json.JSONObject(errorBody).optString("message", "Transaction failed")
                        } else {
                            "Transaction failed: Error ${response.code()}"
                        }
                    } catch (e: Exception) {
                        "Transaction failed"
                    }
                    _error.value = message
                    android.util.Log.e("RunnerVM", "Transaction error: $errorBody")
                }
            } catch (e: Exception) {
                _error.value = "Connection error: Unable to reach server"
                android.util.Log.e("RunnerVM", "Exception creating transaction", e)
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
                delay(10000) // refresh every 10 seconds
            }
        }
    }

    fun stopAutoRefresh() {
        refreshJob?.cancel()
    }

    fun acceptRequest(context: Context, tellerId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.api.acceptAssistance(bearerToken(context), tellerId)
                if (response.isSuccessful) {
                    _successMessage.value = "Request accepted! You have 15 seconds."
                    _incomingRequest.value = null
                    // Refresh tellers and history
                    loadTellers(context)
                    loadHistory(context)
                    loadSavedNotifications(context)
                } else {
                    val errorBody = response.errorBody()?.string()
                    _error.value = "Failed to accept: $errorBody"
                }
            } catch (e: Exception) {
                _error.value = "Connection error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun triggerSoundAndVibration(context: Context) {
        try {
            // Play notification sound
            val notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(context, notification)
            ringtone.play()
        } catch (e: Exception) {
            android.util.Log.e("RunnerVM", "Failed to play sound: ${e.message}")
        }

        try {
            // Vibrate device
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        } catch (e: Exception) {
            android.util.Log.e("RunnerVM", "Failed to vibrate: ${e.message}")
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
