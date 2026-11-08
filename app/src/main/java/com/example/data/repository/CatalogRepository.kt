package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.ModAppEntity
import com.example.data.model.ModApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    private val scope = CoroutineScope(Dispatchers.IO)
    private val database = AppDatabase.getInstance(context)
    private val modAppDao = database.modAppDao()

    // Offline-First Room Flow: maps database entities directly to ModApp domain models
    val apps: StateFlow<List<ModApp>> = modAppDao.getAllApps()
        .map { entities ->
            if (entities.isEmpty()) {
                // Preload from remoteStoreService's memory or bundled assets if Room is fresh
                remoteStoreService.remoteApps.value
            } else {
                entities.map { it.toDomain() }
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = remoteStoreService.remoteApps.value
        )

    val isLoading: StateFlow<Boolean> = remoteStoreService.isSyncing
    val statusMessage: StateFlow<String> = remoteStoreService.lastSyncLog
    val currentSource: StateFlow<CatalogSource> = remoteStoreService.currentSource
    val activeRemoteUrl: StateFlow<String> = remoteStoreService.activeRemoteUrl
    val preferLocalOverrides: StateFlow<Boolean> = remoteStoreService.preferLocalOverrides

    init {
        // Populate Room with bundled/cached assets on first launch if empty
        scope.launch {
            try {
                val count = modAppDao.getCount()
                if (count == 0) {
                    val initialApps = remoteStoreService.remoteApps.value
                    if (initialApps.isNotEmpty()) {
                        modAppDao.insertApps(initialApps.map { ModAppEntity.fromDomain(it) })
                    }
                }
            } catch (e: Exception) {
                Log.w("CatalogRepo", "Initial Room cache check failed: ${e.message}")
            }
        }
    }

    suspend fun syncWithRetrofit(customUrl: String? = null): RemoteSyncResult {
        val result = remoteStoreService.syncRemoteCatalog(customUrl)
        if (result.success && remoteStoreService.remoteApps.value.isNotEmpty()) {
            scope.launch {
                try {
                    val entities = remoteStoreService.remoteApps.value.map { ModAppEntity.fromDomain(it) }
                    modAppDao.insertApps(entities)
                    Log.i("CatalogRepo", "Persisted ${entities.size} apps into local Room cache.")
                } catch (e: Exception) {
                    Log.e("CatalogRepo", "Error persisting to Room: ${e.message}", e)
                }
            }
        }
        return result
    }

    fun setPreferLocalOverrides(prefer: Boolean) {
        remoteStoreService.setPreferLocalOverrides(prefer)
    }

    fun resetToDefaultPastebin() {
        remoteStoreService.resetToDefaultPastebin()
    }

    /**
     * Filter and Sort using exact ModApp properties (name, author, category, modFeatures, etc.)
     */
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
