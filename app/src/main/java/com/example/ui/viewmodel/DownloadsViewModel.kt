package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.data.model.ModApp
import com.example.data.repository.DownloadRepository
import com.example.data.repository.FirestoreSyncRepository
import com.example.util.InstalledAppDetector
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    val downloadRepo = DownloadRepository(application.applicationContext)
    val firestoreRepo = FirestoreSyncRepository(application.applicationContext)

    val tasks: StateFlow<List<DownloadTask>> = downloadRepo.tasks

    val activeDownloads: StateFlow<List<DownloadTask>> = tasks
        .map { list ->
            list.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _pendingReplaceAppId = MutableStateFlow<String?>(null)
    val pendingReplaceAppId: StateFlow<String?> = _pendingReplaceAppId.asStateFlow()
    private var pendingReplaceRequestedAtMs: Long = 0L

    fun startDownload(app: ModApp, user: FirebaseUser? = null): String {
        val taskId = downloadRepo.startDownload(app)
        viewModelScope.launch {
            firestoreRepo.incrementAppDownloadCount(app.id)
            if (user != null) {
                try {
                    firestoreRepo.recordDownload(app.id, app.name, app.version)
                } catch (_: Exception) {}
            }
        }
        return taskId
    }

    fun pauseDownload(taskId: String) = downloadRepo.pauseDownload(taskId)

    fun resumeDownload(taskId: String) = downloadRepo.resumeDownload(taskId)

    fun cancelDownload(taskId: String) = downloadRepo.cancelDownload(taskId)

    fun installApk(context: Context, task: DownloadTask): Result<Unit> =
        downloadRepo.installApk(context, task)

    fun initiateReplaceUninstall(context: Context, app: ModApp): Boolean {
        if (app.packageName.isBlank()) return false
        _pendingReplaceAppId.value = app.id
        pendingReplaceRequestedAtMs = System.currentTimeMillis()
        return InstalledAppDetector.uninstallPackage(context, app.packageName)
    }

    fun initiateReplaceUninstallByTask(context: Context, task: DownloadTask): Boolean {
        if (task.packageName.isBlank()) return false
        _pendingReplaceAppId.value = task.appId
        pendingReplaceRequestedAtMs = System.currentTimeMillis()
        return InstalledAppDetector.uninstallPackage(context, task.packageName)
    }

    fun clearPendingReplace() {
        _pendingReplaceAppId.value = null
        pendingReplaceRequestedAtMs = 0L
    }

    /**
     * Checks if an app awaiting replacement uninstall has been removed from the device.
     * Once confirmed NOT_INSTALLED and the downloaded APK exists, launches the system APK installer immediately.
     */
    fun checkAndAutoInstallPendingReplace(
        context: Context,
        apps: List<ModApp>,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val pendingId = _pendingReplaceAppId.value ?: return false
        val completedTask = tasks.value.find {
            it.appId == pendingId &&
            it.status == DownloadStatus.COMPLETED &&
            !it.filePath.isNullOrBlank()
        } ?: return false

        val pkgName = apps.find { it.id == pendingId }?.packageName?.ifBlank { null }
            ?: completedTask.packageName
        if (pkgName.isBlank()) return false

        val freshInfo = InstalledAppDetector.inspectInstalledApp(context, pkgName)
        if (!freshInfo.isInstalled) {
            clearPendingReplace()
            InstalledAppDetector.onPackageBroadcast(context, pkgName)
            installApk(context, completedTask)
            return true
        }

        // Expire stale pending replace requests older than 2 minutes if user aborted uninstall
        if (pendingReplaceRequestedAtMs > 0L && (nowMs - pendingReplaceRequestedAtMs) > 120_000L) {
            clearPendingReplace()
        }
        return false
    }
}
