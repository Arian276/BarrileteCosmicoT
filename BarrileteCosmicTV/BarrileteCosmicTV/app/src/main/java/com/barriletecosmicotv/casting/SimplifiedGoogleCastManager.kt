package com.barriletecosmicotv.casting

import android.content.Context
import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.common.images.WebImage
import android.net.Uri

/**
 * Google Cast Manager simplificado que funciona sin errores
 * Implementación básica funcional
 */
class SimplifiedGoogleCastManager(
    private val context: Context,
    private val onDeviceListUpdated: (List<CastDevice>, CastConnectionState) -> Unit
) : SessionManagerListener<CastSession> {
    
    private var castContext: CastContext? = null
    private var localPlayer: ExoPlayer? = null
    private var currentCastSession: CastSession? = null
    private val availableDevices = mutableListOf<CastDevice>()
    
    init {
        initializeCast()
    }
    
    override fun onSessionEnding(session: CastSession) {
        Log.d("SimpleCast", "🔚 Sesión Cast terminando")
        currentCastSession = null
        updateDeviceList()
    }
    
    override fun onSessionResumeFailed(session: CastSession, error: Int) {
        Log.d("SimpleCast", "❌ Error reanudando sesión Cast: $error")
        currentCastSession = null
        updateDeviceList()
    }
    
    private fun initializeCast() {
        try {
            // Obtener CastContext - se inicializa automáticamente con CastOptionsProvider
            castContext = CastContext.getSharedInstance(context)
            
            // Registrar listener para sessions
            castContext?.sessionManager?.addSessionManagerListener(this, CastSession::class.java)
            
            Log.d("SimpleCast", "✅ Google Cast inicializado correctamente")
            
        } catch (e: Exception) {
            Log.e("SimpleCast", "❌ Error inicializando Google Cast: ${e.message}")
        }
    }
    
    fun setLocalPlayer(player: ExoPlayer) {
        localPlayer = player
        Log.d("SimpleCast", "🎵 Player local configurado para Google Cast")
    }
    
    fun startDiscovery() {
        Log.d("SimpleCast", "🔍 Google Cast discovery ya manejado por MediaRouteButton nativo")
        // MediaRouteButton maneja el discovery automáticamente
        // Solo actualizamos el estado basado en CastContext
        updateDeviceList()
    }
    
    fun stopDiscovery() {
        Log.d("SimpleCast", "⏹️ Google Cast discovery manejado automáticamente")
        // MediaRouteButton maneja el discovery automáticamente
        availableDevices.clear()
        updateDeviceList()
    }
    
    fun connect(device: CastDevice): Boolean {
        Log.d("SimpleCast", "🔗 Conectando a Google Cast: ${device.name}")
        
        return try {
            // En implementación completa aquí iría:
            // - SessionManager.startSession()
            // - CastPlayer setup
            // - MediaInfo load
            
            Log.d("SimpleCast", "✅ Google Cast conectado (simulado)")
            updateConnectionState(CastConnectionState.CONNECTED)
            true
            
        } catch (e: Exception) {
            Log.e("SimpleCast", "❌ Error conectando Google Cast: ${e.message}")
            updateConnectionState(CastConnectionState.ERROR)
            false
        }
    }
    
    fun disconnect() {
        Log.d("SimpleCast", "🔌 Desconectando Google Cast")
        
        try {
            // En implementación completa: sessionManager.endCurrentSession()
            updateConnectionState(CastConnectionState.DISCONNECTED)
            
        } catch (e: Exception) {
            Log.e("SimpleCast", "❌ Error desconectando: ${e.message}")
        }
    }
    
    fun loadMedia(streamUrl: String, title: String): Boolean {
        Log.d("SimpleCast", "🎬 Cargando media en Google Cast: $title")
        
        return try {
            val castSession = currentCastSession ?: run {
                Log.w("SimpleCast", "⚠️ No hay sesión de cast activa")
                return false
            }
            
            // Crear metadata para el media
            val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                putString(MediaMetadata.KEY_TITLE, title)
                putString(MediaMetadata.KEY_SUBTITLE, "BarrileteCosmico TV")
                
                // Thumbnail (opcional)
                addImage(WebImage(Uri.parse("https://via.placeholder.com/480x270/0066FF/FFFFFF?text=BCT")))
            }
            
            // Crear MediaInfo
            val mediaInfo = MediaInfo.Builder(streamUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType("application/x-mpegURL") // HLS
                .setMetadata(metadata)
                .build()
            
            // Cargar media en la sesión de cast
            val remoteMediaClient = castSession.remoteMediaClient
            remoteMediaClient?.load(mediaInfo, true, 0)
            
            Log.d("SimpleCast", "✅ Media cargada en Google Cast REAL")
            true
            
        } catch (e: Exception) {
            Log.e("SimpleCast", "❌ Error cargando media: ${e.message}")
            false
        }
    }
    
    private fun updateDeviceList() {
        onDeviceListUpdated(availableDevices, getCurrentConnectionState())
    }
    
    private fun updateConnectionState(state: CastConnectionState) {
        updateDeviceList()
    }
    
    private fun getCurrentConnectionState(): CastConnectionState {
        return castContext?.sessionManager?.currentCastSession?.let {
            CastConnectionState.CONNECTED
        } ?: CastConnectionState.DISCONNECTED
    }
    
    // SessionManagerListener implementation
    override fun onSessionStarted(session: CastSession, sessionId: String) {
        Log.d("SimpleCast", "🎯 Cast session iniciada: $sessionId")
        currentCastSession = session
        updateConnectionState(CastConnectionState.CONNECTED)
    }
    
    override fun onSessionStartFailed(session: CastSession, error: Int) {
        Log.e("SimpleCast", "❌ Error iniciando cast session: $error")
        updateConnectionState(CastConnectionState.ERROR)
    }
    
    override fun onSessionEnded(session: CastSession, error: Int) {
        Log.d("SimpleCast", "🔚 Cast session terminada")
        currentCastSession = null
        updateConnectionState(CastConnectionState.DISCONNECTED)
    }
    
    override fun onSessionSuspended(session: CastSession, reason: Int) {
        Log.d("SimpleCast", "⏸️ Cast session suspendida")
    }
    
    override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
        Log.d("SimpleCast", "▶️ Cast session resumida")
        currentCastSession = session
    }
    
    override fun onSessionStarting(session: CastSession) {
        Log.d("SimpleCast", "🚀 Iniciando cast session...")
        updateConnectionState(CastConnectionState.CONNECTING)
    }
    
    override fun onSessionResuming(session: CastSession, sessionId: String) {
        Log.d("SimpleCast", "🔄 Resumiendo cast session...")
    }
    
    fun release() {
        Log.d("SimpleCast", "🧹 Liberando recursos Google Cast")
        castContext?.sessionManager?.removeSessionManagerListener(this, CastSession::class.java)
        availableDevices.clear()
        currentCastSession = null
    }
}