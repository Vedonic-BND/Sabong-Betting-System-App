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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

    private val _betHistory = MutableStateFlow<List<BetResponse>>(emptyList())
    val betHistory: StateFlow<List<BetResponse>> = _betHistory

    private val _runnerHistory = MutableStateFlow<List<RunnerTransactionResponse>>(emptyList())
    val runnerHistory: StateFlow<List<RunnerTransactionResponse>> = _runnerHistory

    private val _requestSuccess = MutableStateFlow(false)
    val requestSuccess: StateFlow<Boolean> = _requestSuccess

    private val _notifications = MutableStateFlow<List<TellerNotification>>(emptyList())
    val notifications: StateFlow<List<TellerNotification>> = _notifications

    private val _tellerCashStatus = MutableStateFlow<com.yego.sabongbettingsystem.data.model.TellerCashStatusResponse?>(null)
    val tellerCashStatus: StateFlow<com.yego.sabongbettingsystem.data.model.TellerCashStatusResponse?> = _tellerCashStatus

    private suspend fun bearerToken(context: Context): String {
        val token = UserStore(context).token.first() ?: ""
        return "Bearer $token"
    }

    fun clearResult() { _betResult.value = null }
    fun clearError()  { _error.value = null }
    fun clearRequestSuccess() { _requestSuccess.value = false }

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

    fun markNotificationAsRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    fun loadSavedNotifications(context: Context) {
        viewModelScope.launch {
            try {
                val token = bearerToken(context)
                val response = RetrofitClient.api.getNotifications(token)
                if (response.isSuccessful) {
                    val savedNotifications = response.body()?.map { notif ->
                        TellerNotification(
                            id = notif.id.toString(),
                            title = notif.title,
                            message = notif.message,
                            timestamp = notif.timestamp,
                            data = try { org.json.JSONObject(notif.data ?: "{}") } catch (e: Exception) { null },
                            isRead = notif.is_read
                        )
                    } ?: emptyList()
                    _notifications.value = savedNotifications
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
            try {
                val response = RetrofitClient.api.getCurrentFight(bearerToken(context))
                if (response.isSuccessful) {
                    _currentFight.value = response.body()
                } else if (response.code() == 404) {
                    _currentFight.value = null
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
                kotlinx.coroutines.delay(5000)
                loadCurrentFight(context)
            }
        }
    }

    fun stopAutoRefresh() {
        refreshJob?.cancel()
    }
}
