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
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val role: String, val app: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    fun login(
        context: Context,
        username: String,
        password: String,
        app: String
    ) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = LoginState.Error("Please fill in all fields.")
            return
        }

        viewModelScope.launch {
            _state.value = LoginState.Loading

            try {
                val response = RetrofitClient.api.login(
                    LoginRequest(username, password, app)
                )

                if (response.isSuccessful) {
                    val body = response.body()!!
                    UserStore(context).save(
                        token = body.token,
                        name  = body.user.name,
                        role  = body.user.role,
                        app   = body.user.app
                    )
                    _state.value = LoginState.Success(
                        role = body.user.role,
                        app  = body.user.app
                    )
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