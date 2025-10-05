package com.barriletecosmicotv.data

import com.barriletecosmicotv.data.api.ApiService
import com.barriletecosmicotv.data.api.JoinStreamRequest
import com.barriletecosmicotv.data.api.LeaveStreamRequest
import com.barriletecosmicotv.data.api.PingRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class ViewerTracker(
    private val apiService: ApiService
) {
    private var currentViewerId: String? = null
    private var currentStreamId: String? = null
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    suspend fun joinStream(streamId: String): String? {
        return try {
            // Salir del stream anterior si existe
            leaveCurrentStream()
            
            val viewerId = UUID.randomUUID().toString()
            val response = apiService.joinStream(streamId, JoinStreamRequest(viewerId))
            
            if (response.isSuccessful) {
                currentViewerId = viewerId
                currentStreamId = streamId
                startHeartbeat(streamId, viewerId)
                viewerId
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun leaveCurrentStream() {
        currentViewerId?.let { viewerId ->
            currentStreamId?.let { streamId ->
                try {
                    apiService.leaveStream(streamId, LeaveStreamRequest(viewerId))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        stopHeartbeat()
        currentViewerId = null
        currentStreamId = null
    }
    
    suspend fun getCurrentViewerCount(streamId: String): Int {
        return try {
            val response = apiService.getViewerCount(streamId)
            if (response.isSuccessful) {
                response.body()?.viewerCount ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
    
    private fun startHeartbeat(streamId: String, viewerId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(30000) // Ping cada 30 segundos
                try {
                    apiService.pingStream(streamId, PingRequest(viewerId))
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }
    
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
    
    fun getCurrentViewerId(): String? = currentViewerId
    fun getCurrentStreamId(): String? = currentStreamId
}