package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.NewsPost
import com.example.data.model.NewsType
import com.google.firebase.firestore.FirebaseFirestore
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.InputStreamReader

enum class NewsSource(val label: String) {
    FIRESTORE_LIVE("Real-Time Firestore (/news)"),
    PASTEBIN_REMOTE("Legacy Cloud Feed"),
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

class NewsRepository(
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
    private val newsCollection = firestore.collection("news")
    private val cacheFile = File(context.filesDir, "cached_firestore_news.json")

    private val _news = MutableStateFlow<List<NewsPost>>(emptyList())
    val news: StateFlow<List<NewsPost>> = _news.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _statusMessage = MutableStateFlow("Connecting to live Firestore news feed...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _currentSource = MutableStateFlow(NewsSource.LOCAL_ASSETS)
    val currentSource: StateFlow<NewsSource> = _currentSource.asStateFlow()

    private val _activeNewsUrl = MutableStateFlow("Firestore: /news (Real-Time Cloud)")
    val activeNewsUrl: StateFlow<String> = _activeNewsUrl.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _notificationAlert = MutableStateFlow<String?>(null)
    val notificationAlert: StateFlow<String?> = _notificationAlert.asStateFlow()

    init {
        // 1. Initial immediate fallback from bundled assets
        loadFromCacheOrAssets()

        // 2. Start real-time Firestore listener
        startRealtimeNewsSync()
    }

    fun getNewsStream(): Flow<List<NewsPost>> = callbackFlow {
        val listenerRegistration = newsCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(NewsPost::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listenerRegistration.remove() }
    }

    private val repoPrefs = context.getSharedPreferences("hsstore_news_prefs", Context.MODE_PRIVATE)

    private fun startRealtimeNewsSync() {
        scope.launch {
            _isRefreshing.value = true
            newsCollection.addSnapshotListener { snapshot, error ->
                _isRefreshing.value = false
                if (error != null) {
                    Log.w("NewsRepository", "Firestore /news listener error: ${error.message}")
                    _statusMessage.value = "Offline mode: loaded local stories"
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(NewsPost::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                if (posts.isNotEmpty()) {
                    repoPrefs.edit().putBoolean("news_has_seeded", true).apply()
                    val orderedPosts = applyCustomOrder(posts)
                    _news.value = orderedPosts
                    _currentSource.value = NewsSource.FIRESTORE_LIVE
                    _statusMessage.value = "Live news: ${orderedPosts.size} stories published on Cloud Firestore"
                    _lastSyncTime.value = System.currentTimeMillis()
                } else {
                    val alreadySeeded = repoPrefs.getBoolean("news_has_seeded", false)
                    if (alreadySeeded) {
                        // Admin intentionally deleted or cleared news; do NOT re-seed
                        _news.value = emptyList()
                        _statusMessage.value = "No stories published in Firestore (0 news)"
                    } else {
                        Log.i("NewsRepository", "First startup: Seeding default news to Firestore...")
                        repoPrefs.edit().putBoolean("news_has_seeded", true).apply()
                        scope.launch {
                            autoSeedFirestoreFromBundledNews()
                        }
                    }
                }
            }
        }
    }

    private suspend fun autoSeedFirestoreFromBundledNews() {
        try {
            val bundled = loadBundledNews()
            if (bundled.isNotEmpty()) {
                _news.value = bundled
                _currentSource.value = NewsSource.LOCAL_ASSETS
                _statusMessage.value = "Seeding ${bundled.size} default stories to Firestore..."

                for (post in bundled) {
                    val docRef = if (post.id.isNotBlank()) newsCollection.document(post.id) else newsCollection.document()
                    docRef.set(post.copy(id = docRef.id), SetOptions.merge()).await()
                }
                _currentSource.value = NewsSource.FIRESTORE_LIVE
                _statusMessage.value = "Seeded ${bundled.size} stories to Firestore collection /news"
            }
        } catch (e: Exception) {
            Log.w("NewsRepository", "Auto-seed news skipped or completed: ${e.message}")
        }
    }

    private fun loadFromCacheOrAssets() {
        val bundled = loadBundledNews()
        if (bundled.isNotEmpty()) {
            _news.value = bundled
            _currentSource.value = NewsSource.LOCAL_ASSETS
            _statusMessage.value = "Loaded ${bundled.size} default stories"
        }
    }

    private fun loadBundledNews(): List<NewsPost> {
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, NewsPost::class.java)
            val adapter = moshi.adapter<List<NewsPost>>(listType)
            context.assets.open("news_announcements.json").use { stream ->
                adapter.fromJson(InputStreamReader(stream).readText()) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "Error parsing bundled news: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveNews(post: NewsPost): Result<Unit> = runCatching {
        val docRef = if (post.id.isNotBlank()) newsCollection.document(post.id) else newsCollection.document()
        val dataToSave = post.copy(id = docRef.id)
        docRef.set(dataToSave, SetOptions.merge()).await()
        _news.value = listOf(dataToSave) + _news.value.filter { it.id != dataToSave.id }
    }

    suspend fun deleteNews(newsId: String): Result<Unit> = runCatching {
        newsCollection.document(newsId).delete().await()
        _news.value = _news.value.filter { it.id != newsId }
    }

    suspend fun syncRemoteNews(customUrl: String? = null): NewsSyncResult {
        _isRefreshing.value = true
        return try {
            val snapshot = newsCollection.orderBy("date", Query.Direction.DESCENDING).get().await()
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(NewsPost::class.java)?.copy(id = doc.id)
            }
            _news.value = posts
            _currentSource.value = NewsSource.FIRESTORE_LIVE
            _statusMessage.value = "Live news: ${posts.size} stories published on Cloud Firestore"
            _lastSyncTime.value = System.currentTimeMillis()
            NewsSyncResult(
                success = true,
                source = _currentSource.value,
                count = posts.size,
                message = "Firestore /news synchronized (${posts.size} active stories)"
            )
        } catch (e: Exception) {
            NewsSyncResult(
                success = false,
                source = _currentSource.value,
                count = _news.value.size,
                message = "Sync failed: ${e.localizedMessage}"
            )
        } finally {
            _isRefreshing.value = false
        }
    }

    fun dismissNotification() {
        _notificationAlert.value = null
    }

    fun resetToDefaultPastebin() {
        _activeNewsUrl.value = "Firestore: /news (Cloud Real-Time)"
    }

    fun applyCustomOrder(newsList: List<NewsPost>): List<NewsPost> {
        val savedOrderStr = repoPrefs.getString("custom_news_order_ids", null)
        if (savedOrderStr.isNullOrBlank()) {
            return newsList.sortedBy { it.sortOrder }
        }
        val orderedIds = savedOrderStr.split(",").filter { it.isNotBlank() }
        if (orderedIds.isEmpty()) return newsList.sortedBy { it.sortOrder }

        val idToIndex = orderedIds.withIndex().associate { it.value to it.index }
        return newsList.sortedBy { idToIndex[it.id] ?: (10000 + it.sortOrder) }
    }

    fun reorderNews(fromIndex: Int, toIndex: Int) {
        val currentList = _news.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _news.value = currentList
            saveCustomOrder(currentList)
        }
    }

    fun saveCustomOrder(newsList: List<NewsPost>) {
        _news.value = newsList
        val ids = newsList.joinToString(",") { it.id }
        repoPrefs.edit().putString("custom_news_order_ids", ids).apply()
        scope.launch {
            try {
                newsList.forEachIndexed { index, post ->
                    if (post.id.isNotBlank()) {
                        newsCollection.document(post.id).update("sortOrder", index)
                    }
                }
            } catch (e: Exception) {
                Log.w("NewsRepo", "Failed to sync sortOrder to Firestore: ${e.message}")
            }
        }
    }
}
