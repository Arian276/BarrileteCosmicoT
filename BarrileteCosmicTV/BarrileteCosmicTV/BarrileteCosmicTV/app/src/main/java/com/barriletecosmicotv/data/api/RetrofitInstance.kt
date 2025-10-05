package com.barriletecosmicotv.data.api

import com.barriletecosmicotv.data.ConfigManager
import com.barriletecosmicotv.data.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitInstance @Inject constructor(
    private val configManager: ConfigManager,
    private val sessionManager: SessionManager
) {

    // Interceptor que inyecta el token si existe
    private val authInterceptor = Interceptor { chain ->
        val token = runBlocking { sessionManager.getAuthToken().first() }
        val requestBuilder = chain.request().newBuilder()
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    // Interceptor que detecta sesión inválida (usuario eliminado / token inválido)
    private val sessionGuardInterceptor = Interceptor { chain ->
        val response: Response = chain.proceed(chain.request())
        // Códigos típicos cuando el backend elimina/invalida usuario:
        // 401 = Unauthorized, 404 = Not Found (usuario inexistente), 410 = Gone
        if (response.code == 401 || response.code == 404 || response.code == 410) {
            // Limpiar la sesión inmediatamente
            runBlocking {
                try {
                    sessionManager.clearSession()
                } catch (_: Throwable) {
                    // No propagamos errores de limpieza
                }
            }
            // Opcional: agregamos una cabecera para que capas superiores puedan mostrar mensaje
            return@Interceptor response.newBuilder()
                .header("X-Session-Expired", "true")
                .build()
        }
        response
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(sessionGuardInterceptor) // << agregado
        .connectTimeout(configManager.getConfig().api.timeout, TimeUnit.MILLISECONDS)
        .readTimeout(configManager.getConfig().api.timeout, TimeUnit.MILLISECONDS)
        .writeTimeout(configManager.getConfig().api.timeout, TimeUnit.MILLISECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(configManager.getConfig().api.baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}