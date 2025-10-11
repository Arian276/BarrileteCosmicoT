package com.barriletecosmicotv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.barriletecosmicotv.data.api.ApiService
import com.barriletecosmicotv.data.api.ToggleLikeRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// DataStore extension property for user ID
val Context.likesDataStore: DataStore<Preferences> by preferencesDataStore(name = "likes_settings")

@Singleton
class LikesRepository @Inject constructor(
    private val context: Context,
    private val apiService: ApiService
) {
    
    companion object {
        private val USER_ID_KEY = stringPreferencesKey("unique_user_id")
    }
    
    // Generar o recuperar userId único  
    private suspend fun getUserId(): String {
        return context.likesDataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }.first().let { savedUserId ->
            if (savedUserId.isNullOrBlank()) {
                val newUserId = UUID.randomUUID().toString()
                context.likesDataStore.edit { preferences ->
                    preferences[USER_ID_KEY] = newUserId
                }
                newUserId
            } else {
                savedUserId
            }
        }
    }
    
    // Obtener el número de likes de un stream
    fun getLikes(streamId: String): Flow<Int> = flow {
        try {
            val userId = getUserId()
            val response = apiService.getLikes(streamId, userId)
            if (response.isSuccessful) {
                response.body()?.let { likeResponse ->
                    emit(likeResponse.likes)
                } ?: emit(0)
            } else {
                emit(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(0)
        }
    }
    
    // Verificar si el usuario ya le dio like a un stream
    fun hasUserLiked(streamId: String): Flow<Boolean> = flow {
        try {
            val userId = getUserId()
            val response = apiService.getLikes(streamId, userId)
            if (response.isSuccessful) {
                response.body()?.let { likeResponse ->
                    emit(likeResponse.liked)
                } ?: emit(false)
            } else {
                emit(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(false)
        }
    }
    
    // Dar like a un stream
    suspend fun toggleLike(streamId: String): Boolean {
        return try {
            val userId = getUserId()
            val response = apiService.toggleLike(streamId, ToggleLikeRequest(userId))
            if (response.isSuccessful) {
                response.body()?.liked ?: false
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}