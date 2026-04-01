package com.yego.sabongbettingsystem.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yego.sabongbettingsystem.data.api.RetrofitClient
import com.yego.sabongbettingsystem.data.model.CreateFightRequest
import com.yego.sabongbettingsystem.data.model.DeclareWinnerRequest
import com.yego.sabongbettingsystem.data.model.Fight
import com.yego.sabongbettingsystem.data.model.UpdateStatusRequest
import com.yego.sabongbettingsystem.data.store.UserStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {

    private val _currentFight = MutableStateFlow<Fight?>(null)
    val currentFight: StateFlow<Fight?> = _currentFight

    private val _fightHistory = MutableStateFlow<List<Fight>>(emptyList())
    val fightHistory: StateFlow<List<Fight>> = _fightHistory

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult

    // ── Helpers ───────────────────────────────────────────

    private suspend fun bearerToken(context: Context): String {
        val token = UserStore(context).token.first() ?: ""
        return "Bearer $token"
    }

    fun clearResult() { _actionResult.value = null }
    fun clearError()  { _error.value = null }

    // ── Load current fight ────────────────────────────────

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

    // ── Load fight history ────────────────────────────────

    fun loadFightHistory(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val response = RetrofitClient.api.getFightHistory(bearerToken(context))
                if (response.isSuccessful) {
                    _fightHistory.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Failed to load history."
                }
            } catch (e: Exception) {
                _error.value = "Cannot connect to server."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Create fight ──────────────────────────────────────

    fun createFight(context: Context, fightNumber: String) {
        if (fightNumber.isBlank()) {
            _error.value = "Fight number is required."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val response = RetrofitClient.api.createFight(
                    bearerToken(context),
                    CreateFightRequest(fightNumber)
                )
                if (response.isSuccessful) {
                    _currentFight.value = response.body()
                    _actionResult.value = "fight_created"
                } else {
                    // show exact server error
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

    // ── Update fight status ───────────────────────────────

    fun updateStatus(context: Context, fightId: Int, status: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val response = RetrofitClient.api.updateFightStatus(
                    bearerToken(context),
                    fightId,
                    UpdateStatusRequest(status)
                )
                if (response.isSuccessful) {
                    loadCurrentFight(context)
                } else {
                    _error.value = "Failed to update fight status."
                }
            } catch (e: Exception) {
                _error.value = "Cannot connect to server."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Declare winner ────────────────────────────────────

    fun declareWinner(context: Context, fightId: Int, winner: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val response = RetrofitClient.api.declareWinner(
                    bearerToken(context),
                    fightId,
                    DeclareWinnerRequest(winner)
                )
                if (response.isSuccessful) {
                    _actionResult.value = "winner_declared"
                } else {
                    _error.value = "Failed to declare winner."
                }
            } catch (e: Exception) {
                _error.value = "Cannot connect to server."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Logout ────────────────────────────────────────────

    fun logout(context: Context, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                RetrofitClient.api.logout(bearerToken(context))
            } catch (_: Exception) { }
            UserStore(context).clear()
            onDone()
        }
    }
}