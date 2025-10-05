package com.barriletecosmicotv.data.api

import com.barriletecosmicotv.model.Stream
import com.barriletecosmicotv.model.Category
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("streams")
    suspend fun getStreams(): Response<List<Stream>>

    @GET("streams/featured")
    suspend fun getFeaturedStreams(): Response<List<Stream>>

    @GET("streams/{id}")
    suspend fun getStreamById(@Path("id") streamId: String): Response<Stream>

    @GET("streams/search")
    suspend fun searchStreams(@Query("query") query: String): Response<List<Stream>>

    @GET("streams/category/{category}")
    suspend fun getStreamsByCategory(@Path("category") category: String): Response<List<Stream>>

    // Viewer tracking endpoints
    @POST("streams/{id}/join")
    suspend fun joinStream(
        @Path("id") streamId: String,
        @Body request: JoinStreamRequest
    ): Response<ViewerResponse>

    @POST("streams/{id}/leave")
    suspend fun leaveStream(
        @Path("id") streamId: String,
        @Body request: LeaveStreamRequest
    ): Response<ViewerResponse>

    @POST("streams/{id}/ping")
    suspend fun pingStream(
        @Path("id") streamId: String,
        @Body request: PingRequest
    ): Response<PingResponse>

    @GET("streams/{id}/viewers")
    suspend fun getViewerCount(@Path("id") streamId: String): Response<ViewerCountResponse>

    @GET("categories")
    suspend fun getCategories(): Response<List<Category>>

    // UI Config endpoints
    @GET("ui-config")
    suspend fun getUIConfig(): Response<UIConfigResponse>

    @PUT("ui-config")
    suspend fun updateUIConfig(@Body config: Map<String, Any>): Response<UIConfigResponse>

    @GET("ui-config/{section}")
    suspend fun getUIConfigSection(@Path("section") section: String): Response<UIConfigSectionResponse>

    @PUT("ui-config/{section}")
    suspend fun updateUIConfigSection(
        @Path("section") section: String,
        @Body data: Map<String, Any>
    ): Response<UIConfigSectionResponse>

    // Likes endpoints
    @POST("streams/{id}/like")
    suspend fun toggleLike(
        @Path("id") streamId: String,
        @Body request: ToggleLikeRequest
    ): Response<LikeResponse>

    @GET("streams/{id}/likes")
    suspend fun getLikes(
        @Path("id") streamId: String,
        @Query("userId") userId: String? = null
    ): Response<LikeResponse>

    @GET("streams/likes/summary")
    suspend fun getLikesSummary(): Response<Map<String, LikeSummary>>

    // Chat endpoints
    @POST("streams/{id}/chat")
    suspend fun sendChatMessage(
        @Path("id") streamId: String,
        @Body request: SendChatRequest
    ): Response<ChatMessageResponse>

    @GET("streams/{id}/chat")
    suspend fun getChatMessages(
        @Path("id") streamId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<ChatMessagesResponse>

    @DELETE("streams/{id}/chat")
    suspend fun clearChat(@Path("id") streamId: String): Response<ChatClearResponse>

    // Configuration endpoints
    // OLD UI Config endpoints REMOVED - using new ui-config endpoints
}

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String, val message: String)

data class JoinStreamRequest(val viewerId: String? = null)
data class LeaveStreamRequest(val viewerId: String)
data class PingRequest(val viewerId: String)

data class ViewerResponse(
    val viewerId: String,
    val viewerCount: Int,
    val streamId: String
)

data class ViewerCountResponse(
    val streamId: String,
    val viewerCount: Int,
    val timestamp: String
)

// UI Config response models
data class UIConfigResponse(
    val success: Boolean,
    val config: Map<String, Any>,
    val timestamp: String
)

data class UIConfigSectionResponse(
    val success: Boolean,
    val section: String,
    val data: Map<String, Any>,
    val timestamp: String
)

data class PingResponse(
    val viewerCount: Int,
    val streamId: String
)

// Likes request/response classes
data class ToggleLikeRequest(val userId: String)

data class LikeResponse(
    val streamId: String,
    val likes: Int,
    val liked: Boolean,
    val timestamp: String
)

data class LikeSummary(
    val likes: Int,
    val streamId: String
)

// Chat request/response classes
data class SendChatRequest(
    val username: String,
    val message: String
)

data class ChatMessage(
    val id: String,
    val username: String,
    val message: String,
    val timestamp: Long,
    val colorHex: String
)

data class ChatMessageResponse(
    val streamId: String,
    val message: ChatMessage,
    val totalMessages: Int,
    val timestamp: String
)

data class ChatMessagesResponse(
    val streamId: String,
    val messages: List<ChatMessage>,
    val totalMessages: Int,
    val offset: Int,
    val limit: Int,
    val timestamp: String
)

data class ChatClearResponse(
    val streamId: String,
    val message: String,
    val timestamp: String
)

// DUPLICATE UIConfigResponse REMOVED - using Map-based version above

data class UIConfiguration(
    val app: AppConfig,
    val theme: ThemeConfig,
    val strings: StringsConfig,
    val features: FeaturesConfig,
    val ui: UIConfig,
    val assets: AssetsConfig
)

data class AppConfig(
    val name: String,
    val version: String,
    val tagline: String,
    val description: String
)

data class ThemeConfig(
    val primary: String,
    val secondary: String,
    val background: String,
    val surface: String,
    val card: String,
    val border: String,
    val textPrimary: String,
    val textSecondary: String,
    val textMuted: String,
    val accent: String,
    val success: String,
    val warning: String,
    val error: String,
    val liveIndicator: String,
    val adminColor: String
)

data class StringsConfig(
    val home: HomeStrings,
    val stream: StreamStrings,
    val general: GeneralStrings
)

data class HomeStrings(
    val welcomeTitle: String,
    val welcomeSubtitle: String,
    val searchPlaceholder: String,
    val noChannelsMessage: String,
    val loadingMessage: String,
    val refreshingMessage: String,
    val noResultsTitle: String,
    val noResultsMessage: String
)

data class StreamStrings(
    val viewersLabel: String,
    val chatTitle: String,
    val chatPlaceholder: String,
    val usernameDialogTitle: String,
    val usernameDialogSubtitle: String,
    val usernameDialogPlaceholder: String,
    val usernameDialogConfirm: String,
    val usernameDialogCancel: String,
    val changeNameTitle: String,
    val changeNameSubtitle: String,
    val fullscreenEnter: String,
    val fullscreenExit: String,
    val liveLabel: String
)

data class GeneralStrings(
    val defaultChannelTitle: String,
    val featuredLabel: String,
    val sportsCategory: String,
    val loadingGeneral: String,
    val errorGeneral: String,
    val retryButton: String
)

data class FeaturesConfig(
    val enableChat: Boolean,
    val enableLikes: Boolean,
    val enableViewerCount: Boolean,
    val enableFullscreen: Boolean,
    val enableCast: Boolean,
    val autoRefresh: Boolean,
    val refreshInterval: Long,
    val pullToRefresh: Boolean,
    val chatMaxMessages: Int,
    val chatRefreshInterval: Long
)

data class UIConfig(
    val gridColumns: Int,
    val animationDelay: Int,
    val featuredThreshold: Int,
    val headerHeight: Int,
    val bottomNavHeight: Int,
    val borderRadius: Int,
    val cardElevation: Int
)

data class AssetsConfig(
    val logoUrl: String,
    val backgroundPattern: String,
    val defaultThumbnail: String,
    val liveIconUrl: String
)

data class UISectionResponse(
    val section: String,
    val config: Map<String, Any>,
    val timestamp: String
)

data class UpdateUIConfigRequest(
    val section: String,
    val data: Map<String, Any>
)

data class UIUpdateResponse(
    val section: String,
    val updatedConfig: Map<String, Any>,
    val message: String,
    val timestamp: String
)