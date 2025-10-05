package com.barriletecosmicotv

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.google.android.gms.cast.framework.CastContext
import com.barriletecosmicotv.data.ConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BarrileteCosmicTVApplication : Application() {
    
    @Inject
    lateinit var configManager: ConfigManager
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar Cast de manera simple
        initCastSimple()
        
        // NUEVO: Refresh UI config from backend on startup
        initConfigManager()
    }
    
    private fun initConfigManager() {
        applicationScope.launch {
            try {
                configManager.refreshConfigFromBackend()?.let {
                    android.util.Log.d("ConfigManager", "✅ UI config loaded from backend")
                } ?: run {
                    android.util.Log.d("ConfigManager", "📂 Using fallback config from assets")
                }
            } catch (e: Exception) {
                android.util.Log.e("ConfigManager", "❌ Config refresh failed, using assets: ${e.message}")
            }
        }
    }
    
    private fun initCastSimple() {
        try {
            // Inicialización SIMPLE sin callbacks complejos
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    CastContext.getSharedInstance(this@BarrileteCosmicTVApplication)
                    android.util.Log.d("Cast", "Cast inicializado correctamente")
                } catch (e: Exception) {
                    android.util.Log.e("Cast", "Cast no disponible: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Cast", "Error inicializando Cast simple: ${e.message}")
        }
    }
}