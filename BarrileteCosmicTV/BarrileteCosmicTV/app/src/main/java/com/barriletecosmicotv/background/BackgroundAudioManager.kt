package com.barriletecosmicotv.background

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.barriletecosmicotv.service.BackgroundAudioService

class BackgroundAudioManager(private val context: Context) {
    
    private var backgroundService: BackgroundAudioService? = null
    private var isBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BackgroundAudioService.BackgroundAudioBinder
            backgroundService = binder.getService()
            isBound = true
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            backgroundService = null
            isBound = false
        }
    }
    
    fun startBackgroundPlayback() {
        val intent = Intent(context, BackgroundAudioService::class.java)
        context.startForegroundService(intent)
        
        val bindIntent = Intent(context, BackgroundAudioService::class.java).apply {
            action = "BIND_BACKGROUND_AUDIO"
        }
        context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    fun stopBackgroundPlayback() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
        
        val intent = Intent(context, BackgroundAudioService::class.java)
        context.stopService(intent)
        backgroundService = null
    }
    
    fun playStream(streamUrl: String) {
        if (!isBound) {
            startBackgroundPlayback()
            // Esperar un poco para que se conecte
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                backgroundService?.playStream(streamUrl)
            }, 1000)
        } else {
            backgroundService?.playStream(streamUrl)
        }
    }
    
    fun pause() {
        backgroundService?.pauseStream()
    }
    
    fun stop() {
        backgroundService?.stopStream()
    }
    
    fun isPlaying(): Boolean {
        return backgroundService?.isPlaying() ?: false
    }
}