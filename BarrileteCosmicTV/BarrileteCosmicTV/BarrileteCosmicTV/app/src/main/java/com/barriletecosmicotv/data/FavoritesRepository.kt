package com.barriletecosmicotv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "barrilete_prefs")

class FavoritesRepository(private val context: Context) {

    private val KEY_FAVORITES = stringSetPreferencesKey("favorite_stream_ids")

    /** Flujo con los IDs (únicos) de streams favoritos */
    val favorites: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        // Siempre devolvemos un set de IDs
        prefs[KEY_FAVORITES] ?: emptySet()
    }

    /** Alterna favorito por ID (no por nombre) */
    suspend fun toggleFavorite(streamId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(streamId)) {
                current.remove(streamId)
            } else {
                current.add(streamId)
            }
            prefs[KEY_FAVORITES] = current
        }
    }

    /** True si el ID está en favoritos */
    suspend fun isFavorite(streamId: String): Boolean {
        val set = favorites.first()
        return set.contains(streamId)
    }

    /** Elimina favoritos que ya no existen en el backend (o que no parecen IDs válidos) */
    suspend fun pruneFavorites(validIds: List<String>) {
        val validSet = validIds.toSet()
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES]?.toMutableSet() ?: mutableSetOf()
            // Mantener solo IDs presentes en backend (evita “por nombre”)
            current.retainAll(validSet)
            prefs[KEY_FAVORITES] = current
        }
    }
}