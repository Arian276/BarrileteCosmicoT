package com.barriletecosmicotv.data.repository

import com.barriletecosmicotv.data.api.ApiService
import com.barriletecosmicotv.model.Stream
import com.barriletecosmicotv.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamRepository @Inject constructor(
    private val apiService: ApiService
) {
    
    fun getStreams(): Flow<List<Stream>> = flow {
        try {
            println("🔄 StreamRepository: Iniciando llamada a getStreams()")
            val response = apiService.getStreams()
            println("📡 StreamRepository: Respuesta recibida - Success: ${response.isSuccessful}, Code: ${response.code()}")
            
            if (response.isSuccessful) {
                response.body()?.let { streams ->
                    println("✅ StreamRepository: ${streams.size} canales deserializados correctamente")
                    println("📺 StreamRepository: Primeros 3 canales: ${streams.take(3).map { "${it.title} (${it.category})" }}")
                    emit(streams)
                } ?: run {
                    println("❌ StreamRepository: response.body() es null")
                    emit(emptyList())
                }
            } else {
                println("❌ StreamRepository: Respuesta no exitosa - ${response.code()}: ${response.message()}")
                emit(emptyList())
            }
        } catch (e: Exception) {
            println("💥 StreamRepository: Excepción capturada: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }
    
    fun getFeaturedStreams(): Flow<List<Stream>> = flow {
        try {
            val response = apiService.getFeaturedStreams()
            if (response.isSuccessful) {
                response.body()?.let { streams ->
                    emit(streams)
                } ?: emit(emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    fun getStreamById(streamId: String): Flow<Stream?> = flow {
        try {
            val response = apiService.getStreamById(streamId)
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }
    
    fun searchStreams(query: String): Flow<List<Stream>> = flow {
        try {
            val response = apiService.searchStreams(query)
            if (response.isSuccessful) {
                response.body()?.let { streams ->
                    emit(streams)
                } ?: emit(emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    fun getCategories(): Flow<List<Category>> = flow {
        try {
            val response = apiService.getCategories()
            if (response.isSuccessful) {
                response.body()?.let { categories ->
                    emit(categories)
                } ?: emit(emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}