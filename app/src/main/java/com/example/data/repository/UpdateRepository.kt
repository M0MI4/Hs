package com.example.data.repository

import android.content.Context
import com.example.data.model.AppUpdateConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class UpdateRepository(
    private val context: Context,
    val remoteStoreService: RemoteStoreService = RemoteStoreService(context)
) {
    val currentAppVersionCode = 1
    val currentAppVersionName = "1.0.0"

    val updateConfig: StateFlow<AppUpdateConfig?> = remoteStoreService.remoteUpdate

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _hasUpdate = MutableStateFlow(false)
    val hasUpdate: StateFlow<Boolean> = _hasUpdate.asStateFlow()

    private val _lastCheckedMessage = MutableStateFlow<String>("Monitoring update feed.")
    val lastCheckedMessage: StateFlow<String> = _lastCheckedMessage.asStateFlow()

    init {
        checkUpdateAvailability()
    }

    fun checkUpdateAvailability() {
        val cfg = remoteStoreService.remoteUpdate.value
        if (cfg != null) {
            val hasNewVersion = cfg.storeLatestVersionCode > currentAppVersionCode
            val shouldShow = hasNewVersion && cfg.showDialog
            _hasUpdate.value = shouldShow
            _lastCheckedMessage.value = if (hasNewVersion) {
                if (cfg.showDialog) "Update v${cfg.storeLatestVersion} available!"
                else "Update v${cfg.storeLatestVersion} available (prompt disabled in JSON)."
            } else {
                "HsStore is up to date (v$currentAppVersionName)."
            }
        }
    }

    suspend fun checkForUpdatesRemote(customUrl: String? = null): Result<AppUpdateConfig> = withContext(Dispatchers.IO) {
        _isChecking.value = true
        _lastCheckedMessage.value = "Checking remote JSON feed..."
        try {
            val syncResult = remoteStoreService.syncRemoteCatalog(customUrl)
            _isChecking.value = false
            val cfg = remoteStoreService.remoteUpdate.value ?: AppUpdateConfig()
            val hasNewVersion = cfg.storeLatestVersionCode > currentAppVersionCode
            _hasUpdate.value = hasNewVersion && cfg.showDialog
            _lastCheckedMessage.value = syncResult.message
            Result.success(cfg)
        } catch (e: Exception) {
            _isChecking.value = false
            _lastCheckedMessage.value = "Remote check failed: ${e.localizedMessage}. Using offline manifest."
            checkUpdateAvailability()
            val cfg = remoteStoreService.remoteUpdate.value ?: AppUpdateConfig()
            Result.success(cfg)
        }
    }

    fun dismissUpdate() {
        _hasUpdate.value = false
    }
}
