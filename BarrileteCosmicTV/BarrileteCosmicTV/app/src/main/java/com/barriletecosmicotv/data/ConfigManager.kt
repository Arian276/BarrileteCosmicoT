package com.barriletecosmicotv.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.barriletecosmicotv.data.api.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var config: AppConfig? = null
    
    fun getConfig(): AppConfig {
        if (config == null) {
            config = loadConfigFromAssets()
        }
        return config!!
    }
    
    // NUEVO: Refresh config from backend - disabled to avoid circular dependency
    suspend fun refreshConfigFromBackend(): AppConfig? {
        // Temporarily disabled - return assets config
        return getConfig()
    }
    
    private fun parseBackendConfig(backendData: Map<String, Any>): AppConfig {
        // Convert backend format to Android AppConfig
        val apiData = backendData["api"] as? Map<String, Any>
        val appData = backendData["app"] as? Map<String, Any>
        val uiData = backendData["ui"] as? Map<String, Any>
        val channelsData = backendData["channels"] as? Map<String, Any>
        
        return AppConfig(
            api = ApiConfig(
                baseUrl = (apiData?.get("baseUrl") as? String) ?: "https://barriletecosmico-backend.replit.dev/api/",
                timeout = ((apiData?.get("timeout") as? Double)?.toLong()) ?: 10000L
            ),
            app = AppInfo(
                appName = (appData?.get("name") as? String) ?: "BarrileteCosmico TV",
                version = (appData?.get("version") as? String) ?: "1.0.0",
                environment = "production"
            ),
            ui = UiConfig(
                refreshInterval = ((uiData?.get("refreshInterval") as? Double)?.toLong()) ?: 30000L,
                autoRefreshEnabled = (uiData?.get("autoRefreshEnabled") as? Boolean) ?: true,
                pullToRefreshEnabled = (uiData?.get("pullToRefreshEnabled") as? Boolean) ?: true
            ),
            channels = ChannelsConfig(
                defaultCategory = (channelsData?.get("defaultCategory") as? String) ?: "sports",
                featuredThreshold = ((channelsData?.get("featuredThreshold") as? Double)?.toInt()) ?: 1500,
                gridColumns = ((channelsData?.get("gridColumns") as? Double)?.toInt()) ?: 2,
                animationDelay = ((channelsData?.get("animationDelay") as? Double)?.toInt()) ?: 100
            )
        )
    }
    
    private fun loadConfigFromAssets(): AppConfig {
        return try {
            val inputStream = context.assets.open("config.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            Gson().fromJson(json, AppConfig::class.java)
        } catch (e: Exception) {
            // Fallback configuration if config.json is not found
            AppConfig(
                api = ApiConfig(
                    baseUrl = "https://barriletecosmico-backend.replit.dev/api/",
                    timeout = 10000
                ),
                app = AppInfo(
                    appName = "BarrileteCosmico TV",
                    version = "1.0.0",
                    environment = "production"
                ),
                ui = UiConfig(
                    refreshInterval = 30000,
                    autoRefreshEnabled = true,
                    pullToRefreshEnabled = true
                ),
                channels = ChannelsConfig(
                    defaultCategory = "sports",
                    featuredThreshold = 1500,
                    gridColumns = 2,
                    animationDelay = 100
                )
            )
        }
    }
}

data class AppConfig(
    @SerializedName("api") val api: ApiConfig,
    @SerializedName("app") val app: AppInfo,
    @SerializedName("ui") val ui: UiConfig,
    @SerializedName("channels") val channels: ChannelsConfig
)

data class ApiConfig(
    @SerializedName("baseUrl") val baseUrl: String,
    @SerializedName("timeout") val timeout: Long
)

data class AppInfo(
    @SerializedName("appName") val appName: String,
    @SerializedName("version") val version: String,
    @SerializedName("environment") val environment: String
)

data class UiConfig(
    @SerializedName("refreshInterval") val refreshInterval: Long,
    @SerializedName("autoRefreshEnabled") val autoRefreshEnabled: Boolean,
    @SerializedName("pullToRefreshEnabled") val pullToRefreshEnabled: Boolean
)

data class ChannelsConfig(
    @SerializedName("defaultCategory") val defaultCategory: String,
    @SerializedName("featuredThreshold") val featuredThreshold: Int,
    @SerializedName("gridColumns") val gridColumns: Int,
    @SerializedName("animationDelay") val animationDelay: Int
)