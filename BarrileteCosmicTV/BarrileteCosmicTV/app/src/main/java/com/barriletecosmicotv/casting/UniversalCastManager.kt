package com.barriletecosmicotv.casting

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaRouter
import android.util.Log
// import androidx.media3.cast.CastPlayer // Comentado por ahora
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.gms.cast.*
import com.google.android.gms.cast.framework.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UniversalCastManager @Inject constructor(
    private val context: Context
) {
    
    // Estados de conexión
    private val _castConnectionState = MutableStateFlow(CastConnectionState.DISCONNECTED)
    val castConnectionState: StateFlow<CastConnectionState> = _castConnectionState.asStateFlow()
    
    private val _availableDevices = MutableStateFlow<List<CastDevice>>(emptyList())
    val availableDevices: StateFlow<List<CastDevice>> = _availableDevices.asStateFlow()
    
    // TODAS las tecnologías 100% REALES
    private var googleCastManager: SimplifiedGoogleCastManager? = null
    private var miracastManager: RealMiracastManager? = null
    private var dlnaManager: RealDLNAManager? = null
    private var airPlayManager: RealAirPlayManager? = null
    
    private var localPlayer: ExoPlayer? = null
    
    init {
        initializeAllCastingTechnologies()
    }
    
    private fun initializeAllCastingTechnologies() {
        try {
            // Google Cast/Chromecast (versión simplificada)
            googleCastManager = SimplifiedGoogleCastManager(context) { devices, connectionState ->
                updateDeviceList(devices, CastTechnology.GOOGLE_CAST)
                _castConnectionState.value = connectionState
            }
            
            // Miracast REAL - MediaRouter API nativo
            miracastManager = RealMiracastManager(context) { devices, connectionState ->
                updateDeviceList(devices, CastTechnology.MIRACAST)
                if (connectionState != CastConnectionState.DISCONNECTED) {
                    _castConnectionState.value = connectionState
                }
            }
            
            // DLNA REAL - NSD discovery
            dlnaManager = RealDLNAManager(context) { devices, connectionState ->
                updateDeviceList(devices, CastTechnology.DLNA)
                if (connectionState != CastConnectionState.DISCONNECTED) {
                    _castConnectionState.value = connectionState
                }
            }
            
            // AirPlay REAL - mDNS/Bonjour discovery 
            airPlayManager = RealAirPlayManager(context) { devices, connectionState ->
                updateDeviceList(devices, CastTechnology.AIRPLAY)
                if (connectionState != CastConnectionState.DISCONNECTED) {
                    _castConnectionState.value = connectionState
                }
            }
            
            // DLNA y AirPlay REMOVIDOS - eran simulaciones falsas
            
            Log.d("UniversalCast", "✅ Todas las tecnologías de casting inicializadas")
            
        } catch (e: Exception) {
            Log.e("UniversalCast", "❌ Error inicializando tecnologías de cast: ${e.message}")
        }
    }
    
    private fun updateDeviceList(newDevices: List<CastDevice>, technology: CastTechnology) {
        val currentDevices = _availableDevices.value.toMutableList()
        
        // Remover dispositivos de esta tecnología
        currentDevices.removeAll { it.technology == technology }
        
        // Agregar nuevos dispositivos
        currentDevices.addAll(newDevices)
        
        _availableDevices.value = currentDevices
        
        Log.d("UniversalCast", "📱 Dispositivos ${technology.name}: ${newDevices.size} encontrados")
    }
    
    fun setLocalPlayer(player: ExoPlayer) {
        localPlayer = player
        googleCastManager?.setLocalPlayer(player)
        miracastManager?.setLocalPlayer(player)
        dlnaManager?.setLocalPlayer(player)
        airPlayManager?.setLocalPlayer(player)
    }
    
    fun startDeviceDiscovery() {
        Log.d("UniversalCast", "🔍 Iniciando descubrimiento de dispositivos...")
        
        googleCastManager?.startDiscovery()
        miracastManager?.startDiscovery()
        dlnaManager?.startDiscovery()
        airPlayManager?.startDiscovery()
    }
    
    fun stopDeviceDiscovery() {
        googleCastManager?.stopDiscovery()
        miracastManager?.stopDiscovery()
        dlnaManager?.stopDiscovery()
        airPlayManager?.stopDiscovery()
    }
    
    fun connectToDevice(device: CastDevice): Boolean {
        Log.d("UniversalCast", "🔗 Conectando a ${device.name} (${device.technology.name})")
        
        return when (device.technology) {
            CastTechnology.GOOGLE_CAST -> googleCastManager?.connect(device) ?: false
            CastTechnology.MIRACAST -> miracastManager?.connect(device) ?: false
            CastTechnology.DLNA -> dlnaManager?.connect(device) ?: false
            CastTechnology.AIRPLAY -> airPlayManager?.connect(device) ?: false
        }
    }
    
    fun disconnect() {
        Log.d("UniversalCast", "🔌 Desconectando todos los dispositivos")
        
        googleCastManager?.disconnect()
        miracastManager?.disconnect()
        dlnaManager?.disconnect()
        airPlayManager?.disconnect()
        
        _castConnectionState.value = CastConnectionState.DISCONNECTED
    }
    
    fun castCurrentStream(streamUrl: String, streamTitle: String): Boolean {
        Log.d("UniversalCast", "📺 Casting stream: $streamTitle")
        
        val connectedDevice = _availableDevices.value.firstOrNull { it.isConnected }
        
        return when (connectedDevice?.technology) {
            CastTechnology.GOOGLE_CAST -> {
                googleCastManager?.loadMedia(streamUrl, streamTitle) ?: false
            }
            CastTechnology.MIRACAST -> {
                // Miracast refleja la pantalla completa, no streams específicos
                miracastManager?.startScreenMirroring() ?: false
            }
            CastTechnology.DLNA -> {
                dlnaManager?.loadMedia(streamUrl, streamTitle) ?: false
            }
            CastTechnology.AIRPLAY -> {
                airPlayManager?.loadMedia(streamUrl, streamTitle) ?: false
            }
            else -> {
                Log.w("UniversalCast", "⚠️ No hay dispositivos conectados")
                false
            }
        }
    }
    
    private fun isAirPlaySupported(): Boolean {
        // AirPlay normalmente requiere certificación Apple
        // En Android se puede usar bibliotecas de terceros como AllCast
        return try {
            // Verificar si hay bibliotecas AirPlay disponibles
            Class.forName("com.apple.airplay.AirPlaySender")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    fun release() {
        Log.d("UniversalCast", "🧹 Liberando recursos de casting")
        
        stopDeviceDiscovery()
        disconnect()
        
        googleCastManager?.release()
        miracastManager?.release()
        dlnaManager?.release()
        airPlayManager?.release()
        
        googleCastManager = null
        miracastManager = null
        dlnaManager = null
        airPlayManager = null
    }
}

// Tipos movidos a CastingTypes.kt para evitar redeclaraciones