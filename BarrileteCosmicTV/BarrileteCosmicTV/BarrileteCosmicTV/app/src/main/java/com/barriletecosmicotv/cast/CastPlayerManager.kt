package com.barriletecosmicotv.cast

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CastPlayerManager(private val context: Context) {
    
    private var castContext: CastContext? = null
    private var localPlayer: Player? = null
    private var remoteMediaClient: RemoteMediaClient? = null
    
    private val _isCastConnected = MutableStateFlow(false)
    val isCastConnected: StateFlow<Boolean> = _isCastConnected.asStateFlow()
    
    private var currentStreamUrl: String? = null
    
    // Simplificar SessionManagerListener para resolver errores de compilación
    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            switchToCastPlayer(session)
        }
        
        override fun onSessionEnded(session: CastSession, error: Int) {
            switchToLocalPlayer()
        }
        
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            switchToCastPlayer(session)
        }
        
        override fun onSessionSuspended(session: CastSession, reason: Int) {
            // Mantener Cast player activo
        }
        
        override fun onSessionEnding(session: CastSession) {
            // Session is ending
        }
        
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            _isCastConnected.value = false
        }
        
        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            _isCastConnected.value = false
        }
        
        override fun onSessionResuming(session: CastSession, sessionId: String) {
            // Session is resuming
        }
        
        override fun onSessionStarting(session: CastSession) {
            // Session is starting
        }
    }
    
    fun initialize() {
        try {
            // Inicialización SIMPLE de Cast
            castContext = CastContext.getSharedInstance(context)
            castContext?.sessionManager?.addSessionManagerListener(
                sessionManagerListener, 
                CastSession::class.java
            )
            android.util.Log.d("Cast", "CastPlayerManager inicializado correctamente")
        } catch (e: Exception) {
            // Cast framework no disponible - continuar sin Cast
            android.util.Log.e("Cast", "Cast no disponible: ${e.message}")
            _isCastConnected.value = false
        }
    }
    
    // Método simple para toggle Cast
    fun toggleCast() {
        try {
            castContext?.let { ctx ->
                if (_isCastConnected.value) {
                    // Desconectar Cast
                    ctx.sessionManager.endCurrentSession(true)
                } else {
                    // Intentar conectar Cast
                    val session = ctx.sessionManager.currentCastSession
                    if (session != null && session.isConnected) {
                        _isCastConnected.value = true
                        remoteMediaClient = session.remoteMediaClient
                    } else {
                        android.util.Log.d("Cast", "No hay dispositivos Cast disponibles")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Cast", "Error en toggleCast: ${e.message}")
        }
    }
    
    // Método específico para desconectar Cast
    fun disconnect() {
        try {
            castContext?.sessionManager?.endCurrentSession(true)
            android.util.Log.d("Cast", "Sesión Cast desconectada")
        } catch (e: Exception) {
            android.util.Log.e("Cast", "Error desconectando Cast: ${e.message}")
        }
    }
    
    fun setLocalPlayer(player: Player?) {
        localPlayer = player
    }
    
    fun playStream(streamUrl: String) {
        try {
            currentStreamUrl = streamUrl
            
            // Determinar dónde reproducir según estado Cast
            if (_isCastConnected.value && remoteMediaClient != null) {
                // Cast conectado - reproducir en dispositivo Cast
                try {
                    loadMediaToCast(streamUrl)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Error en Cast - fallback a local
                    playStreamOnLocal(streamUrl)
                }
            } else {
                // No Cast conectado - reproducir local
                playStreamOnLocal(streamUrl)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback final a local
            try {
                playStreamOnLocal(streamUrl)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }
    
    private fun loadMediaToCast(streamUrl: String) {
        try {
            val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                putString(MediaMetadata.KEY_TITLE, "BarrileteCosmico TV")
                putString(MediaMetadata.KEY_SUBTITLE, "Transmisión en vivo")
            }
            
            val contentType = if (streamUrl.contains(".m3u8")) {
                "application/x-mpegURL" // HLS
            } else {
                "video/mp2t" // TS stream
            }
            
            val mediaInfo = MediaInfo.Builder(streamUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType(contentType)
                .setMetadata(mediaMetadata)
                .build()
            
            // Cast loading usando método NO DEPRECADO
            val mediaLoadRequestData = com.google.android.gms.cast.MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .setCurrentTime(0)
                .build()
                
            remoteMediaClient?.load(mediaLoadRequestData)?.setResultCallback { mediaChannelResult ->
                if (mediaChannelResult.status.isSuccess) {
                    // Stream cargado exitosamente en dispositivo Cast
                    localPlayer?.pause() // Pausar player local
                    android.util.Log.d("Cast", "Stream cargado en Cast exitosamente")
                } else {
                    // Error cargando en Cast, volver a local
                    android.util.Log.e("Cast", "Error cargando stream en Cast: ${mediaChannelResult.status}")
                    playStreamOnLocal(streamUrl)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            playStreamOnLocal(streamUrl)
        }
    }
    
    private fun playStreamOnLocal(streamUrl: String) {
        localPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(streamUrl)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }
    
    private fun switchToCastPlayer(session: CastSession) {
        try {
            // REAL Cast connection - obtener cliente remoto
            remoteMediaClient = session.remoteMediaClient
            _isCastConnected.value = true
            
            // Pausar player local inmediatamente
            localPlayer?.pause()
            
            // Transferir stream actual al dispositivo Cast
            currentStreamUrl?.let { url ->
                loadMediaToCast(url)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isCastConnected.value = false
        }
    }
    
    private fun switchToLocalPlayer() {
        _isCastConnected.value = false
        remoteMediaClient = null
        
        currentStreamUrl?.let { url ->
            playStreamOnLocal(url)
        }
    }
    
    fun play() {
        if (_isCastConnected.value && remoteMediaClient != null) {
            remoteMediaClient?.play()
        } else {
            localPlayer?.play()
        }
    }
    
    fun pause() {
        if (_isCastConnected.value && remoteMediaClient != null) {
            remoteMediaClient?.pause()
        } else {
            localPlayer?.pause()
        }
    }
    
    fun release() {
        try {
            castContext?.sessionManager?.removeSessionManagerListener(
                sessionManagerListener, 
                CastSession::class.java
            )
        } catch (e: Exception) {
            // Ignore
        }
        remoteMediaClient = null
        localPlayer = null
    }
}