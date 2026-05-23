package com.yego.sabongbettingsystem.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yego.sabongbettingsystem.data.api.RetrofitClient
import com.yego.sabongbettingsystem.data.model.BetResponse
import com.yego.sabongbettingsystem.data.model.Fight
import com.yego.sabongbettingsystem.data.model.PlaceBetRequest
import com.yego.sabongbettingsystem.data.store.UserStore
import com.yego.sabongbettingsystem.data.model.AssistanceRequest
import com.yego.sabongbettingsystem.data.model.RunnerTransactionResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class TellerNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: String,
    val data: JSONObject? = null,
    var isRead: Boolean = false
)

class CashInViewModel : ViewModel() {

    private val _currentFight = MutableStateFlow<Fight?>(null)
    val currentFight: StateFlow<Fight?> = _currentFight

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _betResult = MutableStateFlow<BetResponse?>(null)
    val betResult: StateFlow<BetResponse?> = _betResult

    private val _betDeleted = MutableStateFlow<Int?>(null)
    val betDeleted: StateFlow<Int?> = _betDeleted

    private val _betHistory = MutableStateFlow<List<BetResponse>>(emptyList())
    val betHistory: StateFlow<List<BetResponse>> = _betHistory

    private val _adminBetHistory = MutableStateFlow<List<BetResponse>>(emptyList())
    val adminBetHistory: StateFlow<List<BetResponse>> = _adminBetHistory

    private val _runnerHistory = MutableStateFlow<List<RunnerTransactionResponse>>(emptyList())
    val runnerHistory: StateFlow<List<RunnerTransactionResponse>> = _runnerHistory

    private val _requestSuccess = MutableStateFlow(false)
    val requestSuccess: StateFlow<Boolean> = _requestSuccess

    private val _notifications = MutableStateFlow<List<TellerNotification>>(emptyList())
    val notifications: StateFlow<List<TellerNotification>> = _notifications

    private val _tellerCashStatus = MutableStateFlow<com.yego.sabongbettingsystem.data.model.TellerCashStatusResponse?>(null)
    val tellerCashStatus: StateFlow<com.yego.sabongbettingsystem.data.model.TellerCashStatusResponse?> = _tellerCashStatus

    // Track last successful sync timestamp to optimize polling
    private var lastNotificationSyncTime: Long = 0L
    
    // Control polling - only use as fallback when WebSocket unavailable
    private var pollingJob: kotlinx.coroutines.Job? = null
    private var isWebSocketConnected = false

    private suspend fun bearerToken(context: Context): String {
        val token = UserStore(context).token.first() ?: ""
        return "Bearer $token"
    }

    fun clearResult() { _betResult.value = null }
    fun clearError()  { _error.value = null }
    fun clearRequestSuccess() { _requestSuccess.value = false }
    fun clearBetDeleted() { _betDeleted.value = null }

    fun addNotification(title: String, message: String, data: JSONObject? = null, context: android.content.Context? = null) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timestamp = sdf.format(Date())
        val newNotification = TellerNotification(
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
                    android.util.Log.e("CashInVM", "Failed to save notification to database", e)
                }
            }
        }
    }

    fun markNotificationAsRead(id: String, context: Context? = null) {
        // Update local state immediately for instant UI feedback
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        
        // Also sync to database asynchronously to prevent polling from re-showing
        if (context != null) {
            viewModelScope.launch {
                try {
                    val token = bearerToken(context)
                    RetrofitClient.api.markNotificationAsRead(token, id.toInt())
                } catch (e: Exception) {
                    android.util.Log.e("CashInVM", "Failed to mark notification as read in database", e)
                }
            }
        }
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    fun loadSavedNotifications(context: Context) {
        if (!isWebSocketConnected) {
            startFallbackPolling(context)
        }
        viewModelScope.launch {
            try {
                val token = bearerToken(context)
                val currentTime = System.currentTimeMillis()
                
                // Check if enough time has passed since last sync (min 30 seconds as fallback)
                if (isWebSocketConnected && (currentTime - lastNotificationSyncTime) < 30000) {
                    android.util.Log.d("CashInVM", "Skipping notification sync - WebSocket connected and data is fresh")
                    return@launch
                }
                
                android.util.Log.d("CashInVM", "Fetching notifications with token: ${token.take(20)}...")
                val response = RetrofitClient.api.getNotifications(token)
                if (response.isSuccessful) {
                    val savedNotifications = response.body()?.map { notif ->
                        android.util.Log.d("CashInVM", "Loaded notification: ${notif.title} - ${notif.message}")
                        TellerNotification(
                            id = notif.id.toString(),
                            title = notif.title,
                            message = notif.message,
                            timestamp = notif.timestamp,
                            data = try { org.json.JSONObject(notif.data ?: "{}") } catch (e: Exception) { null },
                            isRead = notif.is_read
                        )
                    } ?: emptyList()
                    
                    // Simple, efficient deduplication using database IDs only
                    val existingDbIds = _notifications.value
                        .map { it.id }
                        .filter { it.all { c -> c.isDigit() } } // Only numeric IDs from DB
                        .toSet()
                    
                    // Replace entire list with fresh data from DB for consistency
                    _notifications.value = savedNotifications
                    lastNotificationSyncTime = currentTime
                    
                    android.util.Log.d("CashInVM", "Notifications synced: ${savedNotifications.size} total")
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("CashInVM", "Failed to load notifications: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                android.util.Log.e("CashInVM", "Failed to load saved notifications", e)
            }
        }
    }

    fun loadCurrentFight(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            android.util.Log.d("CashInVM", "📥 loadCurrentFight() START")
            try {
                val response = RetrofitClient.api.getCurrentFight(bearerToken(context))
                android.util.Log.d("CashInVM", "API Response Code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    val fight = response.body()
                    if (fight != null) {
                        _currentFight.value = fight
                        android.util.Log.d("CashInVM", "✅ loadCurrentFight() SUCCESS - fight: ${fight.fight_number}, meron: ${fight.meron_total}, wala: ${fight.wala_total}")
                    } else {
                        android.util.Log.d("CashInVM", "❌ loadCurrentFight() - Response body is NULL!")
                        _error.value = "Fight data is null"
                    }
                } else if (response.code() == 404) {
                    _currentFight.value = null
                    android.util.Log.d("CashInVM", "✅ loadCurrentFight() 404 - No active fight")
                } else {
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string() ?: ""
                        if (errorBody.isNotBlank()) {
                            org.json.JSONObject(errorBody).optString("message", errorBody)
                        } else {
                            "Error ${response.code()}"
                        }
                    } catch (e: Exception) {
                        "Error ${response.code()}"
                    }
                    _error.value = errorMessage
                    android.util.Log.d("CashInVM", "❌ loadCurrentFight() FAILED: $errorMessage")
                }
            } catch (e: Exception) {
                _error.value = "Cannot connect: ${e.message ?: "Unable to reach server"}"
                android.util.Log.d("CashInVM", "❌ loadCurrentFight() EXCEPTION: ${e.message}", e)
            } finally {
                _isLoading.value = false
                android.util.Log.d("CashInVM", "📤 loadCurrentFight() END")
            }
        }
    }

    fun loadBetHistory(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val response = RetrofitClient.api.getBetHistory(bearerToken(context))
                if (response.isSuccessful) {
                    _betHistory.value = response.body()?.data ?: emptyList()
                } else {
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string() ?: ""
                        if (errorBody.isNotBlank()) {
                            org.json.JSONObject(errorBody).optString("message", errorBody)
                        } else {
                            "Error ${response.code()}"
                        }
                    } catch (e: Exception) {
                        "Error ${response.code()}"
                    }
                    _error.value = errorMessage
                }
            } catch (e: Exception) {
                // Silently ignore or handle
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadRunnerHistory(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Use teller-specific endpoint to get runner transactions for this teller
                val response = RetrofitClient.api.getTellerRunnerTransactions(bearerToken(context))
                if (response.isSuccessful) {
                    _runnerHistory.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Silently ignore or handle
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadTellerCashStatus(context: Context) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getTellerCashStatus(bearerToken(context))
                if (response.isSuccessful) {
                    _tellerCashStatus.value = response.body()
                }
            } catch (e: Exception) {
                // Silently ignore or handle
            }
        }
    }

    // ── WebSocket and Polling Management ─────────────────

    /**
     * Start fallback polling for notifications when WebSocket is unavailable.
     * Uses 30-second intervals to minimize database load.
     */
    fun startFallbackPolling(context: Context?) {
        if (context == null) return
        if (pollingJob?.isActive == true) {
            android.util.Log.d("CashInVM", "Polling already active, skipping duplicate start")
            return
        }
        
        pollingJob = viewModelScope.launch {
            android.util.Log.d("CashInVM", "Starting fallback notification polling (30s interval)")
            while (isActive && !isWebSocketConnected) {
                delay(30000) // Poll every 30 seconds as fallback only
                if (!isWebSocketConnected) {
                    android.util.Log.d("CashInVM", "Polling: fetching notifications (WebSocket unavailable)")
                    loadSavedNotifications(context)
                } else {
                    android.util.Log.d("CashInVM", "WebSocket reconnected, stopping fallback polling")
                    break
                }
            }
        }
    }

    /**
     * Stop fallback polling when WebSocket is available.
     */
    fun stopPolling() {
        if (pollingJob?.isActive == true) {
            android.util.Log.d("CashInVM", "Stopping fallback notification polling")
            pollingJob?.cancel()
        }
    }

    /**
     * Track WebSocket connection status for polling management.
     */
    fun setWebSocketConnected(connected: Boolean) {
        isWebSocketConnected = connected
        if (connected) {
            stopPolling()
        } else {
            startFallbackPolling(null) // Will get context in next load call
        }
    }

    fun loadBetByReference(context: Context, reference: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val response = RetrofitClient.api.getBetByReference(bearerToken(context), reference)
                if (response.isSuccessful) {
                    _betResult.value = response.body()
                } else {
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string() ?: ""
                        if (errorBody.isNotBlank()) {
                            org.json.JSONObject(errorBody).optString("message", errorBody)
                        } else {
                            "Error ${response.code()}"
                        }
                    } catch (e: Exception) {
                        "Error ${response.code()}"
                    }
                    _error.value = errorMessage
                }
            } catch (e: Exception) {
                _error.value = "Cannot connect: ${e.message ?: "Unable to reach server"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun placeBet(context: Context, side: String, amount: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val userStore = UserStore(context)
                val role = userStore.role.first() ?: ""
                val token = bearerToken(context)

                // Switch based on role
                val response = if (role.lowercase() == "admin") {
                    RetrofitClient.api.placeBetAsAdmin(token, PlaceBetRequest(side, amount))
                } else {
                    RetrofitClient.api.placeBet(token, PlaceBetRequest(side, amount))
                }

                if (response.isSuccessful) {
                    _betResult.value = response.body()
                    loadCurrentFight(context)
                    loadBetHistory(context) // Ensure history is updated after placing a bet
                } else {
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string() ?: ""
                        if (errorBody.isNotBlank()) {
                            org.json.JSONObject(errorBody).optString("message", errorBody)
                        } else {
                            "Error ${response.code()}"
                        }
                    } catch (e: Exception) {
                        "Error ${response.code()}"
                    }
                    _error.value = errorMessage
                }
            } catch (e: Exception) {
                _error.value = "Cannot connect: ${e.message ?: "Unable to reach server"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun requestRunner(context: Context, requestType: String? = null, customMessage: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = bearerToken(context)
                val request = AssistanceRequest(
                    request_type = requestType ?: "assistance",
                    custom_message = customMessage
                )
                val response = RetrofitClient.api.sendAssistanceRequest(token, request)
                if (response.isSuccessful) {
                    _requestSuccess.value = true
                } else if (response.code() == 429) {
                    // Too many requests - teller is in cooldown
                    try {
                        val errorBody = response.errorBody()?.string() ?: ""
                        val errorJson = org.json.JSONObject(errorBody)
                        val retryAfter = errorJson.optInt("retry_after_seconds", 30)
                        _error.value = "Please wait ${retryAfter}s before requesting again."
                    } catch (e: Exception) {
                        _error.value = "Please wait before requesting again. (30s cooldown)"
                    }
                } else {
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string() ?: ""
                        if (errorBody.isNotBlank()) {
                            org.json.JSONObject(errorBody).optString("message", errorBody)
                        } else {
                            "Request failed. Please try again."
                        }
                    } catch (e: Exception) {
                        "Request failed. Please try again."
                    }
                    _error.value = errorMessage
                }
            } catch (e: Exception) {
                _error.value = "Connection error: ${e.message ?: "Unable to reach server"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout(context: Context, onDone: () -> Unit) {
        stopAutoRefresh()
        stopPolling()
        viewModelScope.launch {
            try {
                RetrofitClient.api.logout(bearerToken(context))
            } catch (_: Exception) { }
            UserStore(context).clear()
            onDone()
        }
    }

    //autorefresh
    private var refreshJob: kotlinx.coroutines.Job? = null

    fun startAutoRefresh(context: Context) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                loadCurrentFight(context)
            }
        }
    }

    fun stopAutoRefresh() {
        refreshJob?.cancel()
    }

    // ── Admin Bet Management ──────────────────────────────

    fun loadAdminBetHistory(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            android.util.Log.d("CashInVM", "📥 loadAdminBetHistory() START")
            try {
                val response = RetrofitClient.api.getAdminBetHistory(bearerToken(context))
                if (response.isSuccessful) {
                    val data = response.body()?.data ?: emptyList()
                    _adminBetHistory.value = data
                    android.util.Log.d("CashInVM", "✅ loadAdminBetHistory() SUCCESS - loaded ${data.size} bets")
                } else {
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string() ?: ""
                        if (errorBody.isNotBlank()) {
                            org.json.JSONObject(errorBody).optString("message", errorBody)
                        } else {
                            "Error ${response.code()}"
                        }
                    } catch (e: Exception) {
                        "Error ${response.code()}"
                    }
                    _error.value = errorMessage
                    android.util.Log.d("CashInVM", "❌ loadAdminBetHistory() FAILED: $errorMessage")
                }
            } catch (e: Exception) {
                _error.value = "Cannot connect: ${e.message ?: "Unable to reach server"}"
                android.util.Log.d("CashInVM", "❌ loadAdminBetHistory() EXCEPTION: ${e.message}")
            } finally {
                _isLoading.value = false
                android.util.Log.d("CashInVM", "📤 loadAdminBetHistory() END")
            }
        }
    }

    fun deleteAdminBet(context: Context, betId: Int, reverbViewModel: ReverbViewModel? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val response = RetrofitClient.api.deleteAdminBet(bearerToken(context), betId)
                if (response.isSuccessful) {
                    android.util.Log.d("CashInVM", "✅ Bet deletion successful (API returned success)")
                    
                    // Signal deletion event with the bet ID
                    _betDeleted.value = betId
                    
                    // Log the signal
                    android.util.Log.d("CashInVM", "🎯 Deletion event triggered with betId: $betId")
                } else {
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string() ?: ""
                        if (errorBody.isNotBlank()) {
                            org.json.JSONObject(errorBody).optString("message", errorBody)
                        } else {
                            "Error ${response.code()}"
                        }
                    } catch (e: Exception) {
                        "Error ${response.code()}"
                    }
                    _error.value = errorMessage
                    android.util.Log.d("CashInVM", "❌ Deletion failed: $errorMessage")
                }
            } catch (e: Exception) {
                _error.value = "Cannot connect: ${e.message ?: "Unable to reach server"}"
                android.util.Log.d("CashInVM", "❌ Deletion exception: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
        stopPolling()
    }
}
