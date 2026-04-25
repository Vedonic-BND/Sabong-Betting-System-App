package com.yego.sabongbettingsystem.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yego.sabongbettingsystem.data.api.RetrofitClient
import com.yego.sabongbettingsystem.data.model.LoginRequest
import com.yego.sabongbettingsystem.data.store.UserStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle    : LoginState()
    object Loading : LoginState()
    data class SuccessAdmin(val name: String)  : LoginState()
    data class SuccessTeller(val name: String) : LoginState()
    data class SuccessRunner(val name: String) : LoginState()
    data class Error(val message: String)      : LoginState()
}

class LoginViewModel : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    fun login(context: Context, username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = LoginState.Error("Please fill in all fields.")
            return
        }

        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val response = RetrofitClient.api.login(
                    LoginRequest(username, password)
                )

                if (response.isSuccessful) {
                    val body = response.body()!!
                    val store = UserStore(context)

                    when (body.role) {
                        "admin" -> {
                            store.saveAdmin(
                                token = body.token!!,
                                name  = body.user.name
                            )
                            _state.value = LoginState.SuccessAdmin(body.user.name)
                        }
                        "teller" -> {
                            val token = body.token ?: body.cashin_token ?: body.cashout_token
                            if (token != null) {
                                store.saveTeller(
                                    cashInToken  = token,
                                    cashOutToken = token,
                                    name         = body.user.name
                                )
                                _state.value = LoginState.SuccessTeller(body.user.name)
                            } else {
                                _state.value = LoginState.Error("No token received.")
                            }
                        }
                        "runner" -> {
                            store.saveRunner(
                                token = body.token!!,
                                name  = body.user.name
                            )
                            _state.value = LoginState.SuccessRunner(body.user.name)
                        }
                        else -> {
                            _state.value = LoginState.Error("Access denied.")
                        }
                    }
                } else {
                    _state.value = LoginState.Error("Invalid credentials.")
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error("Cannot connect to server.")
            }
        }
    }

    fun resetState() {
        _state.value = LoginState.Idle
    }
}
