package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppUpdateConfig
import com.example.data.model.ModApp
import com.example.data.model.NewsPost
import com.example.data.repository.CatalogRepository
import com.example.data.repository.CatalogSource
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.NewsRepository
import com.example.data.repository.NewsSource
import com.example.data.repository.RemoteSyncResult
import com.example.data.repository.UpdateRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    val catalogRepo = CatalogRepository(application.applicationContext)
    val updateRepo = UpdateRepository(application.applicationContext)
    val newsRepo = NewsRepository(application.applicationContext)
    val firestoreRepo = FirestoreSyncRepository(application.applicationContext)

    // Catalog States
    val apps: StateFlow<List<ModApp>> = catalogRepo.apps
    val isCatalogLoading: StateFlow<Boolean> = catalogRepo.isLoading
    val catalogStatusMessage: StateFlow<String> = catalogRepo.statusMessage
    val currentSource: StateFlow<CatalogSource> = catalogRepo.currentSource
    val activeRemoteUrl: StateFlow<String> = catalogRepo.activeRemoteUrl
    val preferLocalOverrides: StateFlow<Boolean> = catalogRepo.preferLocalOverrides

    // News States
    val newsList: StateFlow<List<NewsPost>> = newsRepo.news
    val isNewsRefreshing: StateFlow<Boolean> = newsRepo.isRefreshing
    val newsStatusMessage: StateFlow<String> = newsRepo.statusMessage
    val currentNewsSource: StateFlow<NewsSource> = newsRepo.currentSource
    val activeNewsUrl: StateFlow<String> = newsRepo.activeNewsUrl

    // Update States
    val updateConfig: StateFlow<AppUpdateConfig?> = updateRepo.updateConfig
    val hasUpdate: StateFlow<Boolean> = updateRepo.hasUpdate

    // UI Dialog & Selection States
    private val _selectedAppForDetail = MutableStateFlow<ModApp?>(null)
    val selectedAppForDetail: StateFlow<ModApp?> = _selectedAppForDetail.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _bookmarkedAppIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedAppIds: StateFlow<Set<String>> = _bookmarkedAppIds.asStateFlow()

    init {
        // Preload installed applications into memory cache to eliminate scroll lag
        com.example.util.InstallStatusHelper.preloadInstalledPackages(application.applicationContext)
        // Startup auto sync for catalog, news, and updates
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            catalogRepo.syncWithRetrofit()
            newsRepo.syncRemoteNews()
            updateRepo.checkUpdateAvailability()
        }
    }

    suspend fun refreshCatalog(): RemoteSyncResult {
        val result = catalogRepo.syncWithRetrofit()
        updateRepo.checkUpdateAvailability()
        return result
    }

    suspend fun refreshNews() = newsRepo.syncRemoteNews()

    fun selectAppForDetail(app: ModApp?) {
        _selectedAppForDetail.value = app
    }

    fun setShowUpdateDialog(show: Boolean) {
        _showUpdateDialog.value = show
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
        updateRepo.dismissUpdate()
    }

    fun syncBookmarksFromFirestore(user: FirebaseUser?) {
        if (user == null) return
        viewModelScope.launch {
            try {
                firestoreRepo.observeBookmarks().collect { list ->
                    _bookmarkedAppIds.value = list.map { it.appId }.toSet()
                }
            } catch (e: Exception) {
                Log.w("StoreViewModel", "Bookmarks sync error: ${e.message}")
            }
        }
    }

    fun toggleBookmark(app: ModApp, user: FirebaseUser?, onResult: (String) -> Unit) {
        if (user == null) {
            val current = _bookmarkedAppIds.value
            _bookmarkedAppIds.value = if (current.contains(app.id)) current - app.id else current + app.id
            onResult("Bookmark updated locally (sign in to sync across devices)")
        } else {
            viewModelScope.launch {
                try {
                    val added = firestoreRepo.toggleBookmark(app.id, app.name)
                    _bookmarkedAppIds.value = if (added) _bookmarkedAppIds.value + app.id else _bookmarkedAppIds.value - app.id
                    onResult(if (added) "Bookmarked ${app.name}" else "Removed bookmark")
                } catch (e: Exception) {
                    onResult("Cloud sync failed: ${e.message}")
                }
            }
        }
    }
}
