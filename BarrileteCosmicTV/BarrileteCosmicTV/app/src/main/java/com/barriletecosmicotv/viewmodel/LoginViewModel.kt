package com.barriletecosmicotv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriletecosmicotv.data.SessionManager
import com.barriletecosmicotv.data.api.ApiService
import com.barriletecosmicotv.data.api.LoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.Instant
import java.time.temporal.ChronoUnit

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    /**
     * Evita reintentos si ya está procesando o si ya hubo éxito.
     */
    fun login(username: String, password: String) {
        val current = _loginState.value
        if (current is LoginState.Loading || current is LoginState.Success) return

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = apiService.login(LoginRequest(username, password))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val token = body.token
                    sessionManager.saveAuthToken(token)

                    // ¿Usuario de prueba?
                    val isTestUser = body.user?.name?.equals("Prueba", ignoreCase = true) == true

                    if (isTestUser) {
                        // Para "Prueba" no persistimos expiración
                        val registeredAt = body.user?.registeredAt ?: Instant.now().toString()
                        sessionManager.saveSubscriptionDates(registeredAt = registeredAt, expiresAt = null)
                    } else {
                        // Usar expiresAt que viene en /login; si no viene, fallback +30 días
                        val registeredAt = body.user?.registeredAt ?: Instant.now().toString()
                        val chosenExpiresAt = body.expiresAt
                            ?: Instant.now().plus(30, ChronoUnit.DAYS).toString()
                        sessionManager.saveSubscriptionDates(
                            registeredAt = registeredAt,
                            expiresAt = chosenExpiresAt
                        )
                    }

                    _loginState.value = LoginState.Success(token)
                } else {
                    _loginState.value = LoginState.Error("Usuario o contraseña incorrectos")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Error de conexión. Inténtalo de nuevo.")
            }
        }
    }

    /** Útil después de navegar a HOME para evitar re-disparos por Success previo. */
    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    /** Limpia el error para permitir un nuevo intento. */
    fun clearError() {
        if (_loginState.value is LoginState.Error) {
            _loginState.value = LoginState.Idle
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val token: String) : LoginState()
    data class Error(val message: String) : LoginState()
}