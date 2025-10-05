package com.barriletecosmicotv.casting

/**
 * Tipos compartidos para todas las tecnologías de casting REALES
 * Archivo único para evitar redeclaraciones
 */

data class CastDevice(
    val id: String,
    val name: String,
    val technology: CastTechnology,
    val isConnected: Boolean = false,
    val signalStrength: Int = 0, // 0-100
    val capabilities: List<CastCapability> = emptyList()
)

enum class CastTechnology {
    GOOGLE_CAST,
    MIRACAST,
    DLNA,
    AIRPLAY
}

enum class CastConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class CastCapability {
    AUDIO,
    VIDEO,
    SCREEN_MIRROR,
    REMOTE_CONTROL
}