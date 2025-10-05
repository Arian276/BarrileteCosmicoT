package com.barriletecosmicotv.service

import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import androidx.media3.session.MediaSession
import com.barriletecosmicotv.R

class BackgroundAudioService : MediaSessionService() {
    
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "background_playback"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Crear canal de notificación
        createNotificationChannel()
        
        // Inicializar ExoPlayer
        player = ExoPlayer.Builder(this).build()
        
        // Crear MediaSession
        mediaSession = MediaSession.Builder(this, player).build()
        
        // Configurar como foreground service
        startForegroundService()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproducción en segundo plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controla la reproducción de video en segundo plano"
                setSound(null, null)
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BarrileteCosmico TV")
            .setContentText("Reproduciendo en segundo plano")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
            
        startForeground(NOTIFICATION_ID, notification)
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onDestroy() {
        player.release()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
    
    // Métodos para control externo
    fun playStream(streamUrl: String) {
        val mediaItem = MediaItem.fromUri(streamUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }
    
    fun pauseStream() {
        player.pause()
    }
    
    fun stopStream() {
        player.stop()
    }
    
    fun isPlaying(): Boolean {
        return player.isPlaying
    }
    
    // Binder para comunicación local
    inner class BackgroundAudioBinder : Binder() {
        fun getService(): BackgroundAudioService = this@BackgroundAudioService
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return if (intent?.action == "BIND_BACKGROUND_AUDIO") {
            BackgroundAudioBinder()
        } else {
            super.onBind(intent)
        }
    }
}