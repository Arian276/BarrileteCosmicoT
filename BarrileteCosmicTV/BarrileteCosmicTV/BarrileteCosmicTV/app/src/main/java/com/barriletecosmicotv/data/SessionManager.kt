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
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Singleton
class SessionManager @Inject constructor(@ApplicationContext appContext: Context) {

    private val dataStore = appContext.dataStore

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
    }

    suspend fun saveAuthToken(token: String) {
        dataStore.edit {
            it[TOKEN_KEY] = token
        }
    }

    fun getAuthToken(): Flow<String?> {
        return dataStore.data.map {
            it[TOKEN_KEY]
        }
    }

    /**
     * Método extra: permite saber si hay token guardado.
     * Usado por NavGraph al decidir el startDestination.
     */
    suspend fun hasValidSession(): Boolean {
        val token = getAuthToken().first()
        return !token.isNullOrEmpty()
    }

    /**
     * Nuevo: Flow booleano para observar si la sesión está activa en tiempo real.
     * Esto lo usa NavGraph para detectar "sesión caducó".
     */
    val isLoggedIn: Flow<Boolean> = dataStore.data.map {
        !it[TOKEN_KEY].isNullOrEmpty()
    }

    /**
     * Si alguna vez querés cerrar sesión manualmente o al expirar.
     */
    suspend fun clearSession() {
        dataStore.edit {
            it.remove(TOKEN_KEY)
        }
    }
}