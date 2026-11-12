package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.ModAppEntity
import com.example.data.model.ModApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.InputStreamReader

enum class CatalogSource(val label: String) {
    FIRESTORE_LIVE("Real-Time Firestore Cloud (/apps)"),
    LOCAL_CACHE("Local Room Database"),
    LOCAL_ASSETS("Preloaded Bundled Apps")
}

data class RemoteSyncResult(
    val success: Boolean,
    val source: CatalogSource,
    val count: Int,
    val message: String
)

enum class SortOption {
    POPULAR,
    RATING,
    SIZE,
    RECENT
}

class CatalogRepository(
    private val context: Context,
    val firestore: FirebaseFirestore = try {
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    } catch (e: Exception) {
        FirebaseFirestore.getInstance()
    }
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val database = AppDatabase.getInstance(context)
    private val modAppDao = database.modAppDao()
    private val appsCollection = firestore.collection("apps")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow("Connecting to live Firestore catalog...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _currentSource = MutableStateFlow(CatalogSource.LOCAL_CACHE)
    val currentSource: StateFlow<CatalogSource> = _currentSource.asStateFlow()

    private val _activeRemoteUrl = MutableStateFlow("Firestore: /apps (Real-Time Cloud)")
    val activeRemoteUrl: StateFlow<String> = _activeRemoteUrl.asStateFlow()

    private val _preferLocalOverrides = MutableStateFlow(false)
    val preferLocalOverrides: StateFlow<Boolean> = _preferLocalOverrides.asStateFlow()

    private val _firestoreApps = MutableStateFlow<List<ModApp>>(emptyList())

    /**
     * Real-time stream of all approved store apps.
     * Automatically updates the UI whenever an admin adds, edits, or removes an app.
     */
    fun getAppsStream(): Flow<List<ModApp>> = callbackFlow {
        val listenerRegistration = appsCollection
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.e("CatalogRepo", "Firestore snapshot listener error: ${error.code} - ${error.message}", error)
                    _statusMessage.value = "Firestore Error: ${error.code} (${error.localizedMessage})"
                    close(error)
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ModApp::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(apps)
            }
        awaitClose { listenerRegistration.remove() }
    }

    // Offline-First Room + Firestore combined Flow
    val apps: StateFlow<List<ModApp>> = _firestoreApps.asStateFlow()

    init {
        // 1. Initial Room load for instant rendering
        scope.launch {
            try {
                modAppDao.getAllApps().collect { entities ->
                    if (_firestoreApps.value.isEmpty() && entities.isNotEmpty()) {
                        _firestoreApps.value = entities.map { it.toDomain() }
                        _currentSource.value = CatalogSource.LOCAL_CACHE
                        _statusMessage.value = "Loaded ${entities.size} apps from Room cache"
                    }
                }
            } catch (e: Exception) {
                Log.w("CatalogRepo", "Room initial load failed: ${e.message}")
            }
        }

        // 2. Start observing Firestore in real-time
        startRealtimeFirestoreSync()
    }

    private val repoPrefs = context.getSharedPreferences("hsstore_catalog_prefs", Context.MODE_PRIVATE)

    private fun startRealtimeFirestoreSync() {
        scope.launch {
            _isLoading.value = true
            appsCollection.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    Log.e("CatalogRepo", "Firestore /apps listener error: ${error.code} - ${error.message}", error)
                    _statusMessage.value = "Firestore Error: ${error.code} (${error.localizedMessage})"
                    return@addSnapshotListener
                }

                val remoteApps = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ModApp::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                if (remoteApps.isNotEmpty()) {
                    repoPrefs.edit().putBoolean("catalog_has_seeded", true).apply()
                    val orderedApps = applyCustomOrder(remoteApps)
                    _firestoreApps.value = orderedApps
                    _currentSource.value = CatalogSource.FIRESTORE_LIVE
                    _statusMessage.value = "Live sync: ${orderedApps.size} apps from Cloud Firestore"

                    // Cache to Room in background
                    scope.launch {
                        try {
                            val entities = orderedApps.map { ModAppEntity.fromDomain(it) }
                            modAppDao.insertApps(entities)
                        } catch (e: Exception) {
                            Log.w("CatalogRepo", "Room persistence warning: ${e.message}")
                        }
                    }
                } else {
                    // Do NOT wipe local cache or Room if remote snapshot is temporarily empty
                    // (e.g. during auth state changes, reconnection, or offline startup).
                    val alreadySeeded = repoPrefs.getBoolean("catalog_has_seeded", false)
                    if (!alreadySeeded && _firestoreApps.value.isEmpty()) {
                        scope.launch {
                            try {
                                val existingCount = appsCollection.limit(1).get().await().size()
                                if (existingCount == 0) {
                                    repoPrefs.edit().putBoolean("catalog_has_seeded", true).apply()
                                    autoSeedFirestoreFromBundledCatalog()
                                }
                            } catch (e: Exception) {
                                Log.w("CatalogRepo", "Auto-seed check skipped: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun autoSeedFirestoreFromBundledCatalog() {
        try {
            // Check if Firestore already has apps before seeding
            val existing = appsCollection.limit(1).get().await()
            if (!existing.isEmpty) {
                Log.i("CatalogRepo", "Firestore /apps already contains apps, skipping auto-seed.")
                return
            }
            val bundled = loadBundledCatalog()
            if (bundled.isNotEmpty()) {
                _firestoreApps.value = bundled
                _currentSource.value = CatalogSource.LOCAL_ASSETS
                _statusMessage.value = "Seeding ${bundled.size} bundled apps to Firestore..."

                // Persist locally to Room
                modAppDao.insertApps(bundled.map { ModAppEntity.fromDomain(it) })

                // Seed to Firestore in background
                for (app in bundled) {
                    val docRef = if (app.id.isNotBlank()) appsCollection.document(app.id) else appsCollection.document()
                    docRef.set(app.copy(id = docRef.id), SetOptions.merge()).await()
                }
                _currentSource.value = CatalogSource.FIRESTORE_LIVE
                _statusMessage.value = "Successfully seeded ${bundled.size} apps into Firestore."
                Log.i("CatalogRepo", "Seeded ${bundled.size} apps to Firestore collection /apps")
            }
        } catch (e: Exception) {
            Log.w("CatalogRepo", "Auto-seed skipped or completed: ${e.message}")
        }
    }

    private fun loadBundledCatalog(): List<ModApp> {
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, ModApp::class.java)
            val adapter = moshi.adapter<List<ModApp>>(listType)
            context.assets.open("mod_catalog.json").use { stream ->
                adapter.fromJson(InputStreamReader(stream).readText()) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("CatalogRepo", "Failed to parse bundled catalog: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Saves an app directly to Firestore /apps/{appId} and updates local Room cache.
     */
    suspend fun saveApp(app: ModApp): Result<Unit> = runCatching {
        val docRef = if (app.id.isNotBlank()) {
            appsCollection.document(app.id)
        } else {
            appsCollection.document()
        }
        val dataToSave = app.copy(
            id = docRef.id,
            updatedDate = System.currentTimeMillis().toString()
        )
        docRef.set(dataToSave, SetOptions.merge()).await()
        modAppDao.insertApp(ModAppEntity.fromDomain(dataToSave))
    }

    /**
     * Deletes an app from Firestore /apps/{appId} and removes it from Room cache.
     */
    suspend fun deleteApp(appId: String): Result<Unit> = runCatching {
        appsCollection.document(appId).delete().await()
        modAppDao.deleteAppById(appId)
        _firestoreApps.value = _firestoreApps.value.filter { it.id != appId }
    }

    suspend fun syncWithRetrofit(customUrl: String? = null): RemoteSyncResult {
        _isLoading.value = true
        return try {
            val snapshot = appsCollection.get().await()
            val remoteApps = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ModApp::class.java)?.copy(id = doc.id)
            }
            if (remoteApps.isNotEmpty()) {
                val ordered = applyCustomOrder(remoteApps)
                _firestoreApps.value = ordered
                scope.launch {
                    try {
                        modAppDao.insertApps(ordered.map { ModAppEntity.fromDomain(it) })
                    } catch (_: Exception) {}
                }
            }
            _currentSource.value = CatalogSource.FIRESTORE_LIVE
            _statusMessage.value = "Live sync: ${_firestoreApps.value.size} apps from Cloud Firestore"
            RemoteSyncResult(
                success = true,
                source = _currentSource.value,
                count = _firestoreApps.value.size,
                message = "Firestore synchronized (${_firestoreApps.value.size} apps active)"
            )
        } catch (e: Exception) {
            RemoteSyncResult(
                success = false,
                source = _currentSource.value,
                count = _firestoreApps.value.size,
                message = "Sync failed: ${e.localizedMessage}"
            )
        } finally {
            _isLoading.value = false
        }
    }

    fun setPreferLocalOverrides(prefer: Boolean) {
        _preferLocalOverrides.value = prefer
    }

    fun resetToDefaultPastebin() {
        // No-op kept for backward compatibility with settings UI
        _activeRemoteUrl.value = "Firestore: /apps (Cloud Real-Time)"
    }

    /**
     * Filter and Sort using exact ModApp properties
     */
    fun filterAndSort(
        query: String,
        selectedCategory: String = "All",
        sortOption: SortOption = SortOption.POPULAR
    ): List<ModApp> {
        var result = _firestoreApps.value

        // Category filter
        when (selectedCategory) {
            "Games" -> result = result.filter { it.category.equals("Games", ignoreCase = true) || it.category.equals("Emulators", ignoreCase = true) }
            "Apps" -> result = result.filter { !it.category.equals("Games", ignoreCase = true) && !it.category.equals("Emulators", ignoreCase = true) }
            "All" -> { /* Include everything */ }
            else -> result = result.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        // Query search
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.name.lowercase().contains(q) ||
                it.packageName.lowercase().contains(q) ||
                it.author.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.modFeatures.any { f -> f.lowercase().contains(q) }
            }
        }

        // Sorting
        return when (sortOption) {
            SortOption.POPULAR -> result.sortedByDescending { it.downloadsCount }
            SortOption.RATING -> result.sortedByDescending { it.rating }
            SortOption.SIZE -> result.sortedBy { it.sizeBytes }
            SortOption.RECENT -> result.sortedByDescending { it.versionCode }
        }
    }

    fun applyCustomOrder(appsList: List<ModApp>): List<ModApp> {
        val savedOrderStr = repoPrefs.getString("custom_app_order_ids", null)
        if (savedOrderStr.isNullOrBlank()) {
            return appsList.sortedBy { it.sortOrder }
        }
        val orderedIds = savedOrderStr.split(",").filter { it.isNotBlank() }
        if (orderedIds.isEmpty()) return appsList.sortedBy { it.sortOrder }

        val idToIndex = orderedIds.withIndex().associate { it.value to it.index }
        return appsList.sortedBy { idToIndex[it.id] ?: (10000 + it.sortOrder) }
    }

    fun reorderApps(fromIndex: Int, toIndex: Int) {
        val currentList = _firestoreApps.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _firestoreApps.value = currentList
            saveCustomOrder(currentList)
        }
    }

    fun saveCustomOrder(appsList: List<ModApp>) {
        _firestoreApps.value = appsList
        val ids = appsList.joinToString(",") { it.id }
        repoPrefs.edit().putString("custom_app_order_ids", ids).apply()
        scope.launch {
            try {
                appsList.forEachIndexed { index, app ->
                    if (app.id.isNotBlank()) {
                        appsCollection.document(app.id).update("sortOrder", index)
                    }
                }
            } catch (e: Exception) {
                Log.w("CatalogRepo", "Failed to sync sortOrder to Firestore: ${e.message}")
            }
        }
    }
}
