package com.barriletecosmicotv.di

import android.content.Context
import com.barriletecosmicotv.data.api.ApiService
import com.barriletecosmicotv.data.api.RetrofitInstance
import com.barriletecosmicotv.data.repository.StreamRepository
import com.barriletecosmicotv.data.ConfigManager
import com.barriletecosmicotv.data.ViewerTracker
import com.barriletecosmicotv.data.FavoritesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApiService(retrofitInstance: RetrofitInstance): ApiService {
        return retrofitInstance.api
    }

    @Provides
    @Singleton
    fun provideStreamRepository(apiService: ApiService): StreamRepository {
        return StreamRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideViewerTracker(apiService: ApiService): ViewerTracker {
        return ViewerTracker(apiService)
    }

    @Provides
    @Singleton
    fun provideConfigManager(@ApplicationContext context: Context): ConfigManager {
        return ConfigManager(context)
    }

    // ✅ Nuevo: Repositorio de favoritos persistente con DataStore
    @Provides
    @Singleton
    fun provideFavoritesRepository(
        @ApplicationContext context: Context
    ): FavoritesRepository {
        return FavoritesRepository(context)
    }

    // Ya existentes
    @Provides
    @Singleton
    fun provideSimpleChatRepository(
        @ApplicationContext context: Context,
        apiService: ApiService
    ): com.barriletecosmicotv.data.SimpleChatRepository {
        return com.barriletecosmicotv.data.SimpleChatRepository(context, apiService)
    }

    @Provides
    @Singleton
    fun provideLikesRepository(
        @ApplicationContext context: Context,
        apiService: ApiService
    ): com.barriletecosmicotv.data.LikesRepository {
        return com.barriletecosmicotv.data.LikesRepository(context, apiService)
    }

    @Provides
    @Singleton
    fun provideUniversalCastManager(
        @ApplicationContext context: Context
    ): com.barriletecosmicotv.casting.UniversalCastManager {
        return com.barriletecosmicotv.casting.UniversalCastManager(context)
    }
}