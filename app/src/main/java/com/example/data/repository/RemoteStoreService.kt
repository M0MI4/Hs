package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.AppUpdateConfig
import com.example.data.model.ModApp
import com.example.data.model.NewsPost
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.RemoteModAppDto
import com.example.data.remote.dto.RemoteNewsDto
import com.example.data.remote.dto.RemoteStorePayload
import com.example.data.remote.dto.RemoteUpdateDto
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader

enum class CatalogSource(val label: String) {
    PASTEBIN_REMOTE("Remote (Pastebin Host)"),
    CUSTOM_REMOTE("Remote (Custom URL)"),
    LOCAL_CACHE("Local Cached Snapshot"),
    LOCAL_ASSETS("Preloaded Local Assets")
}

data class RemoteSyncResult(
    val success: Boolean,
    val source: CatalogSource,
    val appsCount: Int,
    val hasUpdate: Boolean,
    val message: String
)

class RemoteStoreService(private val context: Context) {

    private val apiService = RetrofitClient.apiService
    private val moshi = RetrofitClient.moshi

    private val cacheFile = File(context.filesDir, "cached_remote_store.json")

    // Active Remote URL (defaults to user's Pastebin host)
    private val _activeRemoteUrl = MutableStateFlow(RetrofitClient.DEFAULT_PASTEBIN_URL)
    val activeRemoteUrl: StateFlow<String> = _activeRemoteUrl.asStateFlow()

    // Local Override Toggle (when enabled, user's local edits take precedence over remote)
    private val _preferLocalOverrides = MutableStateFlow(false)
    val preferLocalOverrides: StateFlow<Boolean> = _preferLocalOverrides.asStateFlow()

    private val _currentSource = MutableStateFlow(CatalogSource.LOCAL_ASSETS)
    val currentSource: StateFlow<CatalogSource> = _currentSource.asStateFlow()

    private val _remoteApps = MutableStateFlow<List<ModApp>>(emptyList())
    val remoteApps: StateFlow<List<ModApp>> = _remoteApps.asStateFlow()

    private val _remoteUpdate = MutableStateFlow<AppUpdateConfig?>(null)
    val remoteUpdate: StateFlow<AppUpdateConfig?> = _remoteUpdate.asStateFlow()

    private val _remoteNews = MutableStateFlow<List<NewsPost>>(emptyList())
    val remoteNews: StateFlow<List<NewsPost>> = _remoteNews.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncLog = MutableStateFlow("Initialized with local bundled assets.")
    val lastSyncLog: StateFlow<String> = _lastSyncLog.asStateFlow()

    init {
        // Step 1: Load preloaded assets first so UI is immediately rich
        loadBundledLocalAssets()
        // Step 2: Check if cached snapshot exists
        loadFromDiskCache()
    }

    fun setRemoteUrl(newUrl: String) {
        _activeRemoteUrl.value = newUrl.trim()
    }

    fun setPreferLocalOverrides(preferLocal: Boolean) {
        _preferLocalOverrides.value = preferLocal
    }

    fun resetToDefaultPastebin() {
        _activeRemoteUrl.value = RetrofitClient.DEFAULT_PASTEBIN_URL
    }

    /**
     * Retrofit fetch with local override fallback logic
     */
    suspend fun syncRemoteCatalog(customUrl: String? = null): RemoteSyncResult = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        val targetUrl = customUrl?.trim() ?: _activeRemoteUrl.value
        _activeRemoteUrl.value = targetUrl

        val isDefaultPastebin = targetUrl == RetrofitClient.DEFAULT_PASTEBIN_URL

        _lastSyncLog.value = "Connecting via Retrofit to $targetUrl..."
        Log.d("RemoteStoreService", "Fetching remote JSON from $targetUrl")

        try {
            val response = apiService.fetchRawJson(targetUrl)

            if (!response.isSuccessful) {
                val errCode = response.code()
                val errMsg = "HTTP $errCode error from remote host."
                Log.w("RemoteStoreService", errMsg)
                _isSyncing.value = false
                val fallbackResult = fallbackToLocalOrCache("Remote server returned HTTP $errCode.")
                return@withContext fallbackResult
            }

            val rawBody = response.body()?.string()?.trim() ?: ""
            if (rawBody.isEmpty()) {
                _isSyncing.value = false
                return@withContext fallbackToLocalOrCache("Empty response received from remote server.")
            }

            // Check if response is valid JSON (e.g. user's pastebin currently holds placeholder text "Test")
            if (!rawBody.startsWith("{") && !rawBody.startsWith("[")) {
                val msg = "Host returned non-JSON text ('${rawBody.take(40)}'). Awaiting your JSON update on Pastebin!"
                Log.i("RemoteStoreService", msg)
                _isSyncing.value = false
                return@withContext fallbackToLocalOrCache(msg)
            }

            // Auto-heal common syntax issues like missing comma between adjacent quoted fields: "abc" "def" -> "abc", "def"
            val sanitizedBody = sanitizeMalformedJson(rawBody)

            // Parse using Moshi
            val parsedResult = parseRemoteJsonWithMoshi(sanitizedBody)

            if (parsedResult == null || (parsedResult.first.isEmpty() && parsedResult.second == null)) {
                _isSyncing.value = false
                return@withContext fallbackToLocalOrCache("Could not parse apps or update configuration from remote JSON.")
            }

            val (apps, updateConfig, announcements) = parsedResult

            // Save to disk cache for offline resilience
            try {
                cacheFile.writeText(rawBody)
            } catch (e: Exception) {
                Log.w("RemoteStoreService", "Failed to cache remote JSON: ${e.message}")
            }

            // Apply to state, respecting local override preference if active
            val effectiveApps = if (_preferLocalOverrides.value) {
                // Merge local assets over remote
                val localApps = readLocalAssetApps()
                val localIds = localApps.map { it.id }.toSet()
                localApps + apps.filter { it.id !in localIds }
            } else {
                apps
            }

            _remoteApps.value = effectiveApps
            if (updateConfig != null) {
                _remoteUpdate.value = updateConfig
            }
            if (announcements.isNotEmpty()) {
                _remoteNews.value = announcements
            }

            val source = if (isDefaultPastebin) CatalogSource.PASTEBIN_REMOTE else CatalogSource.CUSTOM_REMOTE
            _currentSource.value = source
            _isSyncing.value = false

            val successMsg = "Successfully parsed ${effectiveApps.size} apps and update config from $source!"
            _lastSyncLog.value = successMsg
            Log.i("RemoteStoreService", successMsg)

            RemoteSyncResult(
                success = true,
                source = source,
                appsCount = effectiveApps.size,
                hasUpdate = (updateConfig?.storeLatestVersionCode ?: 0) > 1,
                message = successMsg
            )
        } catch (e: Exception) {
            _isSyncing.value = false
            val errMsg = "Retrofit network error: ${e.localizedMessage}"
            Log.e("RemoteStoreService", errMsg, e)
            fallbackToLocalOrCache(errMsg)
        }
    }

    private fun fallbackToLocalOrCache(reason: String): RemoteSyncResult {
        // Attempt to read from disk cache first
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val cachedJson = cacheFile.readText()
                val parsed = parseRemoteJsonWithMoshi(cachedJson)
                if (parsed != null && parsed.first.isNotEmpty()) {
                    _remoteApps.value = parsed.first
                    parsed.second?.let { _remoteUpdate.value = it }
                    if (parsed.third.isNotEmpty()) _remoteNews.value = parsed.third
                    _currentSource.value = CatalogSource.LOCAL_CACHE
                    val msg = "$reason Loaded previous snapshot from local cache (${parsed.first.size} apps)."
                    _lastSyncLog.value = msg
                    return RemoteSyncResult(
                        success = false,
                        source = CatalogSource.LOCAL_CACHE,
                        appsCount = parsed.first.size,
                        hasUpdate = (parsed.second?.storeLatestVersionCode ?: 0) > 1,
                        message = msg
                    )
                }
            } catch (e: Exception) {
                Log.w("RemoteStoreService", "Cache fallback error: ${e.message}")
            }
        }

        // Fallback to preloaded local assets
        loadBundledLocalAssets()
        _currentSource.value = CatalogSource.LOCAL_ASSETS
        val msg = "$reason Operating on bundled local catalog (${_remoteApps.value.size} apps)."
        _lastSyncLog.value = msg
        return RemoteSyncResult(
            success = false,
            source = CatalogSource.LOCAL_ASSETS,
            appsCount = _remoteApps.value.size,
            hasUpdate = (_remoteUpdate.value?.storeLatestVersionCode ?: 0) > 1,
            message = msg
        )
    }

    /**
     * Parses JSON supporting both wrapped object and bare array formats
     */
    private fun parseRemoteJsonWithMoshi(json: String): Triple<List<ModApp>, AppUpdateConfig?, List<NewsPost>>? {
        val trimmed = json.trim()

        // 1. Wrapped Object Format: { "storeUpdate": {...}, "apps": [...] }
        if (trimmed.startsWith("{")) {
            val adapter = moshi.adapter(RemoteStorePayload::class.java)
            val payload = adapter.fromJson(trimmed)
            if (payload != null) {
                val apps = payload.apps?.map { it.toDomain() } ?: emptyList()
                val update = payload.storeUpdate?.toDomain()
                val news = payload.announcements?.map { it.toDomain() } ?: emptyList()
                return Triple(apps, update, news)
            }
        }

        // 2. Bare Array Format: [ { "id": "...", "name": "..." }, ... ]
        if (trimmed.startsWith("[")) {
            val listType = Types.newParameterizedType(List::class.java, RemoteModAppDto::class.java)
            val adapter = moshi.adapter<List<RemoteModAppDto>>(listType)
            val appDtos = adapter.fromJson(trimmed)
            if (appDtos != null) {
                val apps = appDtos.map { it.toDomain() }
                return Triple(apps, null, emptyList())
            }
        }

        return null
    }

    /**
     * Auto-heals common hand-written JSON errors (such as missing commas between quoted string properties: "abc" "def" -> "abc", "def")
     */
    private fun sanitizeMalformedJson(raw: String): String {
        return try {
            // Replace pattern: "value" [whitespace or newline] "key" -> "value", "key"
            // where there is no comma, colon, open bracket, or open brace in between
            val missingCommaRegex = Regex("(\"[^\"]*\")\\s+(\"[^\"]*\")\\s*:")
            raw.replace(missingCommaRegex, "$1, $2:")
        } catch (e: Exception) {
            raw
        }
    }

    private fun loadFromDiskCache() {
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val cached = cacheFile.readText()
                val parsed = parseRemoteJsonWithMoshi(cached)
                if (parsed != null && parsed.first.isNotEmpty()) {
                    _remoteApps.value = parsed.first
                    parsed.second?.let { _remoteUpdate.value = it }
                    if (parsed.third.isNotEmpty()) _remoteNews.value = parsed.third
                    _currentSource.value = CatalogSource.LOCAL_CACHE
                    _lastSyncLog.value = "Loaded ${parsed.first.size} apps from local cache snapshot."
                }
            } catch (e: Exception) {
                Log.w("RemoteStoreService", "Error reading disk cache: ${e.message}")
            }
        }
    }

    private fun loadBundledLocalAssets() {
        val localApps = readLocalAssetApps()
        _remoteApps.value = localApps

        try {
            val updateJson = context.assets.open("app_update.json").use { InputStreamReader(it).readText() }
            val adapter = moshi.adapter(RemoteUpdateDto::class.java)
            adapter.fromJson(updateJson)?.toDomain()?.let {
                _remoteUpdate.value = it
            }
        } catch (e: Exception) {
            Log.w("RemoteStoreService", "Error reading bundled update asset: ${e.message}")
        }
    }

    private fun readLocalAssetApps(): List<ModApp> {
        return try {
            val catalogJson = context.assets.open("mod_catalog.json").use { InputStreamReader(it).readText() }
            val listType = Types.newParameterizedType(List::class.java, RemoteModAppDto::class.java)
            val adapter = moshi.adapter<List<RemoteModAppDto>>(listType)
            adapter.fromJson(catalogJson)?.map { it.toDomain() } ?: emptyList()
        } catch (e: Exception) {
            Log.w("RemoteStoreService", "Error reading bundled catalog asset: ${e.message}")
            emptyList()
        }
    }

    // Extension Mappers from DTO to Domain Models
    private fun RemoteModAppDto.toDomain(): ModApp {
        return ModApp(
            id = id ?: "app_${System.currentTimeMillis()}",
            name = name ?: "Untitled Mod",
            packageName = packageName ?: "com.mod.unknown",
            category = category ?: "Tools",
            version = version ?: "1.0.0",
            versionCode = versionCode ?: 1,
            sizeBytes = sizeBytes ?: 10485760L,
            sizeDisplay = sizeDisplay ?: "10 MB",
            iconUrl = iconUrl ?: "",
            downloadUrl = downloadUrl ?: "",
            sha256 = sha256 ?: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            description = description ?: "",
            modFeatures = modFeatures ?: emptyList(),
            changelog = changelog ?: "Initial release",
            rating = rating ?: 4.8f,
            downloadsCount = downloadsCount ?: 1000,
            isVerified = isVerified ?: true,
            author = author ?: "Community Modder",
            updatedDate = updatedDate ?: "Recent",
            permissionsRequired = permissionsRequired ?: emptyList()
        )
    }

    private fun RemoteUpdateDto.toDomain(): AppUpdateConfig {
        return AppUpdateConfig(
            storeLatestVersion = storeLatestVersion ?: "1.0.0",
            storeLatestVersionCode = storeLatestVersionCode ?: 1,
            currentVersionCode = currentVersionCode ?: 1,
            mandatory = mandatory ?: false,
            title = title ?: "HsStore Update",
            announcementMessage = announcementMessage ?: "A new update is available.",
            downloadUrl = downloadUrl ?: "",
            sizeDisplay = sizeDisplay ?: "12 MB",
            sha256 = sha256 ?: "",
            publishedAt = publishedAt ?: "Recent",
            changelog = changelog ?: emptyList(),
            showDialog = showDialog ?: false
        )
    }

    private fun RemoteNewsDto.toDomain(): NewsPost {
        return NewsPost(
            id = id ?: "news_${System.currentTimeMillis()}",
            title = title ?: "Announcement",
            tag = tag ?: "Update",
            date = date ?: "Recent",
            readTime = readTime ?: "2 min",
            author = author ?: "HsStore Team",
            content = content ?: "",
            isPinned = isPinned ?: false
        )
    }
}
