package com.barriletecosmicotv.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.barriletecosmicotv.R

class MediaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var notificationManager: PlayerNotificationManager? = null

    private val binder = MediaPlaybackBinder()

    inner class MediaPlaybackBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
        fun getPlayer(): ExoPlayer? = player
        fun getSessionToken(): androidx.media3.session.SessionToken? = mediaSession?.token
    }

    override fun onCreate() {
        super.onCreate()

        // Crear notification channel para Android O+
        createNotificationChannel()

        player = ExoPlayer.Builder(this).build()

        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(
                    this, 0, sessionIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        val builder = MediaSession.Builder(this, player!!)
        sessionActivityPendingIntent?.let { builder.setSessionActivity(it) }
        mediaSession = builder.build()

        // Configurar notificación para foreground service
        setupNotificationManager()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproducción de Media",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción de BarrileteCosmico TV"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupNotificationManager() {
        notificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID
        )
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return "BarrileteCosmico TV"
                }

                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    return packageManager?.getLaunchIntentForPackage(packageName)?.let { intent ->
                        PendingIntent.getActivity(
                            this@MediaPlaybackService, 0, intent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    }
                }

                override fun getCurrentContentText(player: Player): CharSequence? {
                    return "Transmisión en vivo"
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): android.graphics.Bitmap? {
                    return null
                }
            })
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopForeground(true)
                }

                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing) {
                        startForeground(notificationId, notification)
                    }
                }
            })
            .build()

        // Configurar icono de notificación
        notificationManager?.setSmallIcon(R.drawable.ic_logo)
        notificationManager?.setPlayer(player)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return if (intent?.action == "BIND_PLAYER") {
            binder
        } else {
            super.onBind(intent)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        notificationManager?.setPlayer(null)
        mediaSession?.run {
            player?.release()
            release()
        }
        player = null
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "media_playback_channel"
    }
}