package com.example.data.repository

import android.content.Context
import com.example.data.model.ModApp
import kotlinx.coroutines.flow.StateFlow

enum class SortOption {
    POPULAR,
    RATING,
    SIZE,
    RECENT
}

class CatalogRepository(
    private val context: Context,
    val remoteStoreService: RemoteStoreService = RemoteStoreService(context)
) {
    val apps: StateFlow<List<ModApp>> = remoteStoreService.remoteApps
    val isLoading: StateFlow<Boolean> = remoteStoreService.isSyncing
    val statusMessage: StateFlow<String> = remoteStoreService.lastSyncLog
    val currentSource: StateFlow<CatalogSource> = remoteStoreService.currentSource
    val activeRemoteUrl: StateFlow<String> = remoteStoreService.activeRemoteUrl
    val preferLocalOverrides: StateFlow<Boolean> = remoteStoreService.preferLocalOverrides

    suspend fun syncWithRetrofit(customUrl: String? = null): RemoteSyncResult {
        return remoteStoreService.syncRemoteCatalog(customUrl)
    }

    fun setPreferLocalOverrides(prefer: Boolean) {
        remoteStoreService.setPreferLocalOverrides(prefer)
    }

    fun resetToDefaultPastebin() {
        remoteStoreService.resetToDefaultPastebin()
    }

    fun filterAndSort(
        query: String,
        category: String,
        sort: SortOption
    ): List<ModApp> {
        var result = apps.value

        if (category.isNotBlank() && category != "All") {
            result = result.filter { it.category.equals(category, ignoreCase = true) }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { app ->
                app.name.lowercase().contains(q) ||
                app.description.lowercase().contains(q) ||
                app.author.lowercase().contains(q) ||
                app.modFeatures.any { it.lowercase().contains(q) } ||
                app.category.lowercase().contains(q)
            }
        }

        return when (sort) {
            SortOption.POPULAR -> result.sortedByDescending { it.downloadsCount }
            SortOption.RATING -> result.sortedByDescending { it.rating }
            SortOption.SIZE -> result.sortedBy { it.sizeBytes }
            SortOption.RECENT -> result.sortedByDescending { it.versionCode }
        }
    }
}
