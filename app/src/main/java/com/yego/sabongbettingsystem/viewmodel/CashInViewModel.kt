package com.yego.sabongbettingsystem.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yego.sabongbettingsystem.data.api.RetrofitClient
import com.yego.sabongbettingsystem.data.model.BetResponse
import com.yego.sabongbettingsystem.data.model.Fight
import com.yego.sabongbettingsystem.data.model.PlaceBetRequest
import com.yego.sabongbettingsystem.data.store.UserStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    private suspend fun bearerToken(context: Context): String {
        val token = UserStore(context).token.first() ?: ""
        return "Bearer $token"
    }

    fun clearResult() { _betResult.value = null }
    fun clearError()  { _error.value = null }

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
                    val errorBody = response.errorBody()?.string()
                    _error.value = "Error ${response.code()}: $errorBody"
                }
            } catch (e: Exception) {
                _error.value = "Cannot connect: ${e.message}"
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
                    val errorBody = response.errorBody()?.string()
                    _error.value = "Error ${response.code()}: $errorBody"
                }
            } catch (e: Exception) {
                _error.value = "Cannot connect: ${e.message}"
            } finally {
                _isLoading.value = false
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
                    val errorBody = response.errorBody()?.string()
                    _error.value = "Error ${response.code()}: $errorBody"
                }
            } catch (e: Exception) {
                _error.value = "Cannot connect: ${e.message}"
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
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = try {
                        org.json.JSONObject(errorBody ?: "").getString("message")
                    } catch (e: Exception) {
                        "Error ${response.code()}: $errorBody"
                    }
                    _error.value = message
                }
            } catch (e: Exception) {
                _error.value = "Cannot connect: ${e.message}"
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
