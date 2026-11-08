package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.NewsPost
import com.example.data.model.NewsType
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.RemoteNewsDto
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

enum class NewsSource(val label: String) {
    PASTEBIN_REMOTE("Pastebin Remote (CTpQXLcc)"),
    CUSTOM_REMOTE("Custom Remote URL"),
    LOCAL_CACHE("Local Disk Cache"),
    LOCAL_ASSETS("Preloaded Editorial News"),
    PLACEHOLDER_FALLBACK("Remote Placeholder ('News')")
}

data class NewsSyncResult(
    val success: Boolean,
    val source: NewsSource,
    val count: Int,
    val message: String
)

class NewsRepository(private val context: Context) {

    private val apiService = RetrofitClient.apiService
    private val moshi = RetrofitClient.moshi

    private val cacheFile = File(context.filesDir, "cached_remote_news.json")
    private val prefs = context.getSharedPreferences("hsstore_news_prefs", Context.MODE_PRIVATE)

    private val _news = MutableStateFlow<List<NewsPost>>(emptyList())
    val news: StateFlow<List<NewsPost>> = _news.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _statusMessage = MutableStateFlow("Initializing news feed...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _currentSource = MutableStateFlow(NewsSource.LOCAL_ASSETS)
    val currentSource: StateFlow<NewsSource> = _currentSource.asStateFlow()

    private val _activeNewsUrl = MutableStateFlow(
        prefs.getString("custom_news_url", RetrofitClient.DEFAULT_NEWS_PASTEBIN_URL)
            ?: RetrofitClient.DEFAULT_NEWS_PASTEBIN_URL
    )
    val activeNewsUrl: StateFlow<String> = _activeNewsUrl.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(prefs.getLong("last_news_sync", 0L))
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _notificationAlert = MutableStateFlow<String?>(null)
    val notificationAlert: StateFlow<String?> = _notificationAlert.asStateFlow()

    init {
        // 1. Initial immediate load from disk cache or bundled assets
        loadFromCacheOrAssets()
    }

    /**
     * Initial local load to guarantee instantaneous UI render
     */
    private fun loadFromCacheOrAssets() {
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val cachedJson = cacheFile.readText()
                val parsed = parseNewsJson(cachedJson)
                if (parsed.isNotEmpty()) {
                    _news.value = parsed
                    _currentSource.value = NewsSource.LOCAL_CACHE
                    _statusMessage.value = "Loaded ${parsed.size} cached stories"
                    return
                }
            } catch (e: Exception) {
                Log.w("NewsRepository", "Failed reading disk cache: ${e.message}")
            }
        }
        loadFromAssetsFallback()
    }

    private fun loadFromAssetsFallback() {
        try {
            val jsonString = context.assets.open("news_announcements.json").use { stream ->
                InputStreamReader(stream).readText()
            }
            val parsed = parseNewsJson(jsonString)
            _news.value = parsed
            _currentSource.value = NewsSource.LOCAL_ASSETS
            _statusMessage.value = "Displaying ${parsed.size} editorial stories"
        } catch (e: Exception) {
            Log.e("NewsRepository", "Failed loading assets news: ${e.message}", e)
            _news.value = emptyList()
        }
    }

    /**
     * Sync news from remote Pastebin URL via Retrofit
     */
    suspend fun syncRemoteNews(customUrl: String? = null): NewsSyncResult = withContext(Dispatchers.IO) {
        _isRefreshing.value = true
        val targetUrl = customUrl?.trim()?.ifBlank { null } ?: _activeNewsUrl.value
        _statusMessage.value = "Fetching news from ${targetUrl.take(45)}..."

        try {
            val response = apiService.fetchRawJson(targetUrl)
            if (!response.isSuccessful || response.body() == null) {
                val err = "HTTP ${response.code()}: ${response.message()}"
                _statusMessage.value = "News sync failed ($err). Using cached."
                _isRefreshing.value = false
                return@withContext NewsSyncResult(false, _currentSource.value, _news.value.size, err)
            }

            val rawBody = response.body()!!.string().trim()

            // Handle edge case where Pastebin currently has placeholder text like "News"
            if (rawBody.equals("News", ignoreCase = true) || (!rawBody.startsWith("[") && !rawBody.startsWith("{"))) {
                _statusMessage.value = "Remote host active ('News' placeholder detected). Showing curated stories."
                _currentSource.value = NewsSource.PLACEHOLDER_FALLBACK
                _isRefreshing.value = false
                return@withContext NewsSyncResult(
                    success = true,
                    source = NewsSource.PLACEHOLDER_FALLBACK,
                    count = _news.value.size,
                    message = "Remote host active (placeholder 'News' detected). Showing ${_news.value.size} stories."
                )
            }

            // Parse valid JSON
            val parsedList = parseNewsJson(rawBody)
            if (parsedList.isNotEmpty()) {
                // Save to cache
                try {
                    cacheFile.writeText(rawBody)
                } catch (e: Exception) {
                    Log.w("NewsRepository", "Could not cache news: ${e.message}")
                }

                _news.value = parsedList
                val isPastebin = targetUrl.contains("pastebin.com/raw/CTpQXLcc")
                val source = if (isPastebin) NewsSource.PASTEBIN_REMOTE else NewsSource.CUSTOM_REMOTE
                _currentSource.value = source
                _statusMessage.value = "Synced ${parsedList.size} stories from remote"

                val now = System.currentTimeMillis()
                _lastSyncTime.value = now
                prefs.edit().putLong("last_news_sync", now).apply()

                _isRefreshing.value = false
                return@withContext NewsSyncResult(true, source, parsedList.size, "Successfully synced ${parsedList.size} stories!")
            } else {
                _statusMessage.value = "Remote news returned empty array. Retaining current."
                _isRefreshing.value = false
                return@withContext NewsSyncResult(false, _currentSource.value, _news.value.size, "No valid news entries in JSON")
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "News sync exception: ${e.message}", e)
            _statusMessage.value = "Connection error: ${e.localizedMessage ?: "Unknown error"}. Cached active."
            _isRefreshing.value = false
            return@withContext NewsSyncResult(false, _currentSource.value, _news.value.size, e.localizedMessage ?: "Sync error")
        }
    }

    /**
     * Flexible parser supporting:
     * 1. Top level array: [ { ... } ]
     * 2. Object with "news": [ ... ]
     * 3. Object with "announcements": [ ... ]
     */
    private fun parseNewsJson(jsonString: String): List<NewsPost> {
        val trimmed = jsonString.trim()
        val results = mutableListOf<NewsPost>()

        try {
            // First attempt with Moshi if JSON array
            if (trimmed.startsWith("[")) {
                val listType = Types.newParameterizedType(List::class.java, RemoteNewsDto::class.java)
                val adapter = moshi.adapter<List<RemoteNewsDto>>(listType)
                val dtos = adapter.fromJson(trimmed)
                if (dtos != null && dtos.isNotEmpty()) {
                    return dtos.map { it.toDomain() }
                }
            }

            // If JSON object
            if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                val array = when {
                    obj.has("news") -> obj.optJSONArray("news")
                    obj.has("announcements") -> obj.optJSONArray("announcements")
                    obj.has("items") -> obj.optJSONArray("items")
                    else -> null
                }
                if (array != null) {
                    for (i in 0 until array.length()) {
                        val itemObj = array.optJSONObject(i) ?: continue
                        results.add(parseJsonObjectToNews(itemObj, i))
                    }
                    if (results.isNotEmpty()) return results
                }
            }

            // Fallback manual JSONArray parsing
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val itemObj = arr.optJSONObject(i) ?: continue
                    results.add(parseJsonObjectToNews(itemObj, i))
                }
            }
        } catch (e: Exception) {
            Log.w("NewsRepository", "JSON parse error: ${e.message}")
        }

        return results
    }

    private fun parseJsonObjectToNews(obj: JSONObject, index: Int): NewsPost {
        val bullets = mutableListOf<String>()
        val bulletsArray = obj.optJSONArray("bulletPoints") ?: obj.optJSONArray("bullets") ?: obj.optJSONArray("changelog")
        if (bulletsArray != null) {
            for (j in 0 until bulletsArray.length()) {
                val b = bulletsArray.optString(j, "").trim()
                if (b.isNotBlank()) bullets.add(b)
            }
        }

        val typeStr = obj.optString("type", "").uppercase()
        val bannerUrl = obj.optString("bannerUrl", "")
        val type = when {
            typeStr == "BANNER" || (bannerUrl.isNotBlank() && typeStr != "TEXT_ONLY") -> NewsType.BANNER
            typeStr == "PATCH_NOTES" || obj.has("changelog") -> NewsType.PATCH_NOTES
            typeStr == "SPOTLIGHT" -> NewsType.SPOTLIGHT
            else -> NewsType.TEXT_ONLY
        }

        return NewsPost(
            id = obj.optString("id", "news_$index"),
            title = obj.optString("title", "Announcement"),
            subtitle = obj.optString("subtitle", ""),
            type = type,
            bannerUrl = bannerUrl,
            tag = obj.optString("tag", if (type == NewsType.BANNER) "FEATURED" else "UPDATE"),
            date = obj.optString("date", "Recent"),
            readTime = obj.optString("readTime", "2 min read"),
            author = obj.optString("author", "HsStore Editorial"),
            authorRole = obj.optString("authorRole", "Core Team"),
            content = obj.optString("content", ""),
            bulletPoints = bullets,
            linkedAppId = obj.optString("linkedAppId", "").ifBlank { null },
            linkedAppName = obj.optString("linkedAppName", "").ifBlank { null },
            isPinned = obj.optBoolean("isPinned", false),
            isBreaking = obj.optBoolean("isBreaking", false)
        )
    }

    private fun RemoteNewsDto.toDomain(): NewsPost {
        val banner = this.bannerUrl ?: ""
        val typeStr = (this.type ?: "").uppercase()
        val resolvedType = when {
            typeStr == "BANNER" || (banner.isNotBlank() && typeStr != "TEXT_ONLY") -> NewsType.BANNER
            typeStr == "PATCH_NOTES" || !(this.bulletPoints.isNullOrEmpty()) && typeStr.contains("PATCH") -> NewsType.PATCH_NOTES
            typeStr == "SPOTLIGHT" -> NewsType.SPOTLIGHT
            else -> NewsType.TEXT_ONLY
        }

        return NewsPost(
            id = this.id ?: "news_${System.currentTimeMillis()}",
            title = this.title ?: "HsStore Story",
            subtitle = this.subtitle ?: "",
            type = resolvedType,
            bannerUrl = banner,
            tag = this.tag ?: if (resolvedType == NewsType.BANNER) "FEATURED" else "UPDATE",
            date = this.date ?: "Recent",
            readTime = this.readTime ?: "2 min read",
            author = this.author ?: "HsStore Editorial",
            authorRole = this.authorRole ?: "Core Team",
            content = this.content ?: "",
            bulletPoints = this.bulletPoints ?: emptyList(),
            linkedAppId = this.linkedAppId,
            linkedAppName = this.linkedAppName,
            isPinned = this.isPinned ?: false,
            isBreaking = this.isBreaking ?: false
        )
    }

    fun updateNewsUrl(newUrl: String) {
        val cleaned = newUrl.trim()
        _activeNewsUrl.value = cleaned
        prefs.edit().putString("custom_news_url", cleaned).apply()
    }

    fun resetToDefaultPastebin() {
        _activeNewsUrl.value = RetrofitClient.DEFAULT_NEWS_PASTEBIN_URL
        prefs.edit().remove("custom_news_url").apply()
    }

    fun simulateFeatureRolloutPush(title: String, message: String) {
        _notificationAlert.value = "🔔 [HsStore Push] $title: $message"
    }

    fun clearNotificationAlert() {
        _notificationAlert.value = null
    }
}
