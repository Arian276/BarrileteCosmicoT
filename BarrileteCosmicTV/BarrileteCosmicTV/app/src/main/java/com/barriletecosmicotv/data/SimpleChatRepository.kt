package com.barriletecosmicotv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.barriletecosmicotv.data.api.ApiService
import com.barriletecosmicotv.data.api.ChatMessage
import com.barriletecosmicotv.data.api.SendChatRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

// DataStore setup para username solamente
val Context.simpleDataStore: DataStore<Preferences> by preferencesDataStore(name = "simple_chat")

// MENSAJE DE CHAT SIMPLE - Alias para ChatMessage del API
typealias SimpleChatMessage = ChatMessage

@Singleton
class SimpleChatRepository @Inject constructor(
    private val context: Context,
    private val apiService: ApiService
) {
    // CLAVE PARA USERNAME LOCAL
    private val USERNAME_KEY = stringPreferencesKey("simple_username")
    
    // ⚡ OBTENER MENSAJES CON POLLING SÚPER RÁPIDO - ACTUALIZACIONES INSTANTÁNEAS
    fun getChatMessages(streamId: String): Flow<List<SimpleChatMessage>> = flow {
        while (true) {
            try {
                val response = apiService.getChatMessages(streamId, limit = 100, offset = 0)
                if (response.isSuccessful) {
                    response.body()?.let { chatResponse ->
                        emit(chatResponse.messages)
                    } ?: emit(emptyList())
                } else {
                    emit(emptyList())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emit(emptyList())
            }
            // ⚡ POLLING INSTANTÁNEO - Solo 500ms de delay para actualizaciones ultra rápidas
            delay(500)
        }
    }
    
    // OBTENER USERNAME local
    val username: Flow<String> = context.simpleDataStore.data.map { preferences ->
        preferences[USERNAME_KEY] ?: ""
    }
    
    // GUARDAR USERNAME localmente
    suspend fun saveUsername(username: String) {
        context.simpleDataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username.trim()
        }
    }
    
    // AGREGAR MENSAJE via API
    suspend fun addMessage(streamId: String, username: String, message: String): Boolean {
        if (username.isBlank() || message.isBlank()) return false
        
        return try {
            val response = apiService.sendChatMessage(
                streamId,
                SendChatRequest(
                    username = username.trim(),
                    message = message.trim()
                )
            )
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}