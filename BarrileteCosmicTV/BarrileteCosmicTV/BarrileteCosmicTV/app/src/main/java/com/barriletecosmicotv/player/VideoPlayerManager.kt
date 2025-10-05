package com.barriletecosmicotv.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.barriletecosmicotv.service.MediaPlaybackService
import com.barriletecosmicotv.cast.CastPlayerManager
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VideoPlayerManager(private val context: Context) {

    private var mediaPlaybackService: MediaPlaybackService? = null
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _player = MutableStateFlow<Player?>(null)
    val player: StateFlow<Player?> = _player.asStateFlow()

    // Cast Player Manager
    private val castPlayerManager = CastPlayerManager(context)
    val isCastConnected = castPlayerManager.isCastConnected

    private var pendingStreamUrl: String? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaPlaybackService.MediaPlaybackBinder
            mediaPlaybackService = binder.getService()

            // Obtener token de sesión y crear MediaController
            binder.getSessionToken()?.let { token ->
                controllerFuture = MediaController.Builder(context, token).buildAsync()
                controllerFuture?.addListener(Runnable {
                    try {
                        mediaController = controllerFuture?.get()
                        _player.value = mediaController
                        setupPlayerListener()

                        // Configurar Cast Player Manager
                        castPlayerManager.setLocalPlayer(mediaController)

                        // Si hay una URL pendiente, reproducirla ahora
                        pendingStreamUrl?.let { url ->
                            playStreamInternal(url)
                            pendingStreamUrl = null
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, context.mainExecutor)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaPlaybackService = null
            releaseController()
        }
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }
        })
    }

    fun bindToService() {
        // Inicializar Cast
        castPlayerManager.initialize()

        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = "BIND_PLAYER"
        }
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        context.startService(Intent(context, MediaPlaybackService::class.java))
    }

    fun unbindFromService() {
        try {
            context.unbindService(serviceConnection)
        } catch (e: Exception) {
            // Service already unbound
        }
        releaseController()
    }

    private fun releaseController() {
        controllerFuture?.cancel(true)
        mediaController?.release()
        mediaController = null
        controllerFuture = null
    }

    fun playStream(streamUrl: String) {
        if (mediaController != null) {
            // Usar Cast Player Manager que maneja local vs cast automáticamente
            castPlayerManager.playStream(streamUrl)
        } else {
            // Guardar para reproducir cuando el controlador esté listo
            pendingStreamUrl = streamUrl
        }
    }

    private fun playStreamInternal(streamUrl: String) {
        // Usar Cast Player Manager que maneja local vs cast automáticamente
        castPlayerManager.playStream(streamUrl)
    }

    fun play() {
        castPlayerManager.play()
    }

    fun pause() {
        castPlayerManager.pause()
    }

    fun stop() {
        mediaController?.stop()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun getCurrentPosition(): Long {
        return mediaController?.currentPosition ?: 0L
    }

    fun getDuration(): Long {
        return mediaController?.duration ?: 0L
    }

    fun release() {
        castPlayerManager.release()
        unbindFromService()
    }
}