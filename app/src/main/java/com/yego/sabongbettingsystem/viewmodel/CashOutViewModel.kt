package com.yego.sabongbettingsystem.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yego.sabongbettingsystem.data.api.RetrofitClient
import com.yego.sabongbettingsystem.data.model.BetResponse
import com.yego.sabongbettingsystem.data.model.PayoutResponse
import com.yego.sabongbettingsystem.data.store.UserStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class CashOutViewModel : ViewModel() {

    private val _payout = MutableStateFlow<PayoutResponse?>(null)
    val payout: StateFlow<PayoutResponse?> = _payout

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _confirmed = MutableStateFlow(false)
    val confirmed: StateFlow<Boolean> = _confirmed

    private val _betHistory = MutableStateFlow<List<BetResponse>>(emptyList())
    val betHistory: StateFlow<List<BetResponse>> = _betHistory

    private suspend fun bearerToken(context: Context): String {
        val token = UserStore(context).token.first() ?: ""
        return "Bearer $token"
    }

    fun clearAll() {
        _payout.value    = null
        _error.value     = null
        _confirmed.value = false
    }

    fun loadBetHistory(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val token = bearerToken(context)
                val response = RetrofitClient.api.getBetHistory(token)
                if (response.isSuccessful) {
                    val rawHistory = response.body()?.data ?: emptyList()
                    
                    // Enrich history by fetching payout status for each bet
                    val enrichedHistory = rawHistory.map { bet ->
                        async {
                            try {
                                val payoutRes = RetrofitClient.api.getPayout(token, bet.reference)
                                if (payoutRes.isSuccessful) {
                                    val p = payoutRes.body()
                                    bet.copy(
                                        winner = p?.winner,
                                        won = p?.won,
                                        status = p?.status,
                                        net_payout = p?.net_payout
                                    )
                                } else {
                                    bet
                                }
                            } catch (e: Exception) {
                                bet
                            }
                        }
                    }.awaitAll()

                    _betHistory.value = enrichedHistory
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
                val currentTellerName = UserStore(context).name.first() ?: "Unknown"
                val response = RetrofitClient.api.getPayout(
                    bearerToken(context),
                    reference.trim()
                )
                if (response.isSuccessful) {
                    val payout = response.body()
                    // Verify that the payout belongs to the current teller
                    if (payout != null && payout.teller != currentTellerName) {
                        _error.value = "Access denied: This receipt is issued by another teller (${payout.teller})"
                    } else {
                        _payout.value = payout
                    }
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
                    loadBetHistory(context) // refresh history after payout
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
