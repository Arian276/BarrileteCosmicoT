package com.barriletecosmicotv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriletecosmicotv.data.api.ApiService
import com.barriletecosmicotv.data.api.ChangePasswordRequest
import com.barriletecosmicotv.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val api: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _currentPassword = MutableStateFlow("")
    val currentPassword: StateFlow<String> = _currentPassword.asStateFlow()

    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        // 🔄 REFRESH AUTOMÁTICO CADA 30s MIENTRAS HAYA SESIÓN
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // Si no hay token, no intenta refrescar
                    val token = sessionManager.getAuthToken().firstOrNull().orEmpty()
                    if (token.isNotBlank()) {
                        refreshSubscription()
                    }
                } catch (_: Exception) {
                    // Evitar crash; si falla una iteración se reintenta en el próximo ciclo
                }
                delay(30_000L)
            }
        }
    }

    fun onCurrentPasswordChange(value: String) { _currentPassword.value = value }
    fun onNewPasswordChange(value: String) { _newPassword.value = value }
    fun onConfirmPasswordChange(value: String) { _confirmPassword.value = value }

    private fun getUsernameFromToken(token: String): String? {
        val re = Regex("^fake-token-for-(.+?)-\\d+$")
        return re.find(token.trim())?.groupValues?.getOrNull(1)
    }

    fun changePassword() {
        if (_newPassword.value.isBlank() || _currentPassword.value.isBlank()) {
            _statusMessage.value = "Completá todos los campos"
            return
        }
        if (_newPassword.value != _confirmPassword.value) {
            _statusMessage.value = "Las contraseñas no coinciden"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val token = sessionManager.getAuthToken().firstOrNull()
            if (token.isNullOrBlank()) {
                _statusMessage.value = "No se encontró sesión. Por favor, iniciá sesión de nuevo."
                return@launch
            }
            val username = getUsernameFromToken(token)
            if (username == null) {
                _statusMessage.value = "Token inválido. Por favor, iniciá sesión de nuevo."
                return@launch
            }

            try {
                val req = ChangePasswordRequest(
                    username = username,
                    currentPassword = _currentPassword.value,
                    newPassword = _newPassword.value
                )
                val resp = api.changePassword(req)

                if (resp.isSuccessful) {
                    val body = resp.body()
                    val msg = body?.message ?: "Contraseña cambiada correctamente"
                    _statusMessage.value = msg
                    onCurrentPasswordChange("")
                    onNewPasswordChange("")
                    onConfirmPasswordChange("")
                } else {
                    _statusMessage.value = parseError(resp.errorBody())
                }
            } catch (e: HttpException) {
                _statusMessage.value = "Error HTTP ${e.code()}"
            } catch (e: IOException) {
                _statusMessage.value = "Error de red. Verificá conexión"
            } catch (e: Exception) {
                _statusMessage.value = "Error inesperado: ${e.message}"
            }
        }
    }

    fun clearStatus() { _statusMessage.value = null }

    private fun parseError(err: ResponseBody?): String {
        return try {
            val s = err?.string().orEmpty()
            if (s.isBlank()) return "Error al cambiar la contraseña"
            val json = JSONObject(s)
            (json.optString("message").takeIf { it.isNotBlank() }
                ?: json.optString("error").takeIf { it.isNotBlank() }
                ?: "Error al cambiar la contraseña")
        } catch (_: Exception) {
            "Error al cambiar la contraseña"
        }
    }

    /**
     * Consulta /me/subscription con el Bearer actual y, si trae expiresAt,
     * lo guarda en DataStore. Devuelve true si actualizó.
     */
    suspend fun refreshSubscription(): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = sessionManager.getAuthToken().firstOrNull().orEmpty()
            if (token.isBlank()) return@withContext false

            val resp = api.getSubscriptionInfo(authorization = "Bearer $token")
            if (!resp.isSuccessful) return@withContext false

            val body = resp.body() ?: return@withContext false
            val expires = body.expiresAt
            if (!expires.isNullOrBlank()) {
                sessionManager.saveSubscriptionDates(registeredAt = null, expiresAt = expires)
                return@withContext true
            }
            false
        } catch (_: Exception) {
            false
        }
    }
}