package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AppUpdateConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UpdateRepository(
    private val context: Context,
    val firestore: FirebaseFirestore = try {
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    } catch (e: Exception) {
        FirebaseFirestore.getInstance()
    }
) {
    val currentAppVersionCode = 1
    val currentAppVersionName = "1.3.0"

    private val scope = CoroutineScope(Dispatchers.IO)
    private val configDoc = firestore.collection("config").document("store")

    private val _updateConfig = MutableStateFlow<AppUpdateConfig?>(
        AppUpdateConfig(
            storeLatestVersion = "1.3.0",
            storeLatestVersionCode = 1,
            currentVersionCode = 1,
            title = "HsStore System Update",
            announcementMessage = "All systems operational on Cloud Firestore.",
            downloadUrl = "https://github.com/M0MI4/Hs/releases"
        )
    )
    val updateConfig: StateFlow<AppUpdateConfig?> = _updateConfig.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _hasUpdate = MutableStateFlow(false)
    val hasUpdate: StateFlow<Boolean> = _hasUpdate.asStateFlow()

    private val _lastCheckedMessage = MutableStateFlow<String>("Monitoring Firestore /config/store.")
    val lastCheckedMessage: StateFlow<String> = _lastCheckedMessage.asStateFlow()

    init {
        startRealtimeConfigSync()
    }

    private fun startRealtimeConfigSync() {
        scope.launch {
            configDoc.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("UpdateRepository", "Error observing /config/store: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val cfg = snapshot.toObject(AppUpdateConfig::class.java)
                    if (cfg != null) {
                        _updateConfig.value = cfg
                        val hasNew = cfg.storeLatestVersionCode > currentAppVersionCode
                        _hasUpdate.value = hasNew && cfg.showDialog
                        _lastCheckedMessage.value = if (hasNew) {
                            "Update v${cfg.storeLatestVersion} available!"
                        } else {
                            "HsStore is up to date (v$currentAppVersionName)."
                        }
                    }
                } else {
                    // Document doesn't exist yet -> seed initial default store config
                    scope.launch {
                        try {
                            configDoc.set(_updateConfig.value ?: AppUpdateConfig(), SetOptions.merge()).await()
                            Log.i("UpdateRepository", "Initialized /config/store in Firestore")
                        } catch (e: Exception) {
                            Log.w("UpdateRepository", "Config seed error: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    fun checkUpdateAvailability() {
        val cfg = _updateConfig.value
        if (cfg != null) {
            val hasNewVersion = cfg.storeLatestVersionCode > currentAppVersionCode
            _hasUpdate.value = hasNewVersion && cfg.showDialog
        }
    }

    suspend fun saveUpdateConfig(config: AppUpdateConfig): Result<Unit> = runCatching {
        configDoc.set(config, SetOptions.merge()).await()
        _updateConfig.value = config
        checkUpdateAvailability()
    }

    suspend fun checkForUpdatesRemote(customUrl: String? = null): Result<AppUpdateConfig> {
        _isChecking.value = true
        checkUpdateAvailability()
        _isChecking.value = false
        val cfg = _updateConfig.value ?: AppUpdateConfig()
        return Result.success(cfg)
    }

    fun dismissUpdate() {
        _hasUpdate.value = false
    }
}
