package com.barriletecosmicotv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Singleton
class SessionManager @Inject constructor(@ApplicationContext appContext: Context) {

    private val dataStore = appContext.dataStore

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val REGISTERED_AT_KEY = stringPreferencesKey("registered_at")
        private val EXPIRES_AT_KEY = stringPreferencesKey("expires_at")

        private const val DAYS_30_MS = 30L * 24 * 60 * 60 * 1000
        private val TOKEN_TS_RE = Pattern.compile("^fake-token-for-(.+?)-(\\d+)$")
    }

    suspend fun saveAuthToken(token: String) {
        dataStore.edit {
            it[TOKEN_KEY] = token
            // limpiar expiración previa para forzar fetch fresco
            it.remove(EXPIRES_AT_KEY)
        }
    }

    suspend fun saveSubscriptionDates(registeredAt: String?, expiresAt: String?) {
        dataStore.edit {
            if (registeredAt != null) it[REGISTERED_AT_KEY] = registeredAt
            if (expiresAt != null) it[EXPIRES_AT_KEY] = expiresAt
        }
    }

    fun getAuthToken(): Flow<String?> {
        return dataStore.data.map { it[TOKEN_KEY] }
    }

    fun getToken(): String? = runBlocking { getAuthToken().first() }

    fun getExpiresAt(): Flow<String?> {
        return dataStore.data.map { it[EXPIRES_AT_KEY] }
    }

    /**
     * Devuelve los días restantes (redondeo hacia arriba). Si exp <= now -> 0.
     */
    suspend fun getDaysRemaining(): Int {
        val now = System.currentTimeMillis()
        val expiresStored = getExpiresAt().first() ?: return 0
        return try {
            val exp = java.time.Instant.parse(expiresStored).toEpochMilli()
            if (exp <= now) return 0
            val msPerDay = 1000 * 60 * 60 * 24.0
            ceil((exp - now) / msPerDay).toInt().coerceAtLeast(0)
        } catch (_: Exception) {
            0
        }
    }

    suspend fun hasValidSession(): Boolean {
        val token = getAuthToken().first()
        if (token.isNullOrEmpty()) return false

        val expires = getExpiresAt().first()
        if (expires.isNullOrEmpty()) return true

        return try {
            val now = System.currentTimeMillis()
            val exp = java.time.Instant.parse(expires).toEpochMilli()
            now < exp
        } catch (_: Exception) {
            true
        }
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { !it[TOKEN_KEY].isNullOrEmpty() }

    suspend fun clearSession() {
        dataStore.edit {
            it.remove(TOKEN_KEY)
            it.remove(REGISTERED_AT_KEY)
            it.remove(EXPIRES_AT_KEY)
        }
    }
}