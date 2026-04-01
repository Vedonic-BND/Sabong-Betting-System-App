package com.yego.sabongbettingsystem.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yego.sabongbettingsystem.data.api.RetrofitClient
import com.yego.sabongbettingsystem.data.model.PayoutResponse
import com.yego.sabongbettingsystem.data.store.UserStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CashOutViewModel : ViewModel() {

    private val _payout = MutableStateFlow<PayoutResponse?>(null)
    val payout: StateFlow<PayoutResponse?> = _payout

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _confirmed = MutableStateFlow(false)
    val confirmed: StateFlow<Boolean> = _confirmed

    private suspend fun bearerToken(context: Context): String {
        val token = UserStore(context).token.first() ?: ""
        return "Bearer $token"
    }

    fun clearAll() {
        _payout.value    = null
        _error.value     = null
        _confirmed.value = false
    }

    fun lookupPayout(context: Context, reference: String) {
        if (reference.isBlank()) {
            _error.value = "Please enter a reference number."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            _payout.value    = null
            try {
                val response = RetrofitClient.api.getPayout(
                    bearerToken(context),
                    reference.trim()
                )
                if (response.isSuccessful) {
                    _payout.value = response.body()
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

    fun confirmPayout(context: Context, reference: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val response = RetrofitClient.api.confirmPayout(
                    bearerToken(context),
                    reference
                )
                if (response.isSuccessful) {
                    _confirmed.value = true
                    _payout.value    = null
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