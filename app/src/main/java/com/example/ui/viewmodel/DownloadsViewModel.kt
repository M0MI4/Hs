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

    private val selfPackageName = application.packageName

    val selfUpdateState = downloadRepo.selfUpdateState

    val tasks: StateFlow<List<DownloadTask>> = downloadRepo.tasks
        .map { list ->
            list.filter {
                !it.isSelfUpdate &&
                it.appId != "app_hsstore_self_update" &&
                it.appId != "app_hsstore_update" &&
                it.packageName != selfPackageName
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeDownloads: StateFlow<List<DownloadTask>> = tasks
        .map { list ->
            list.filter {
                !it.isSelfUpdate &&
                (it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _pendingReplaceAppId = MutableStateFlow<String?>(null)
    val pendingReplaceAppId: StateFlow<String?> = _pendingReplaceAppId.asStateFlow()
    private var pendingReplaceRequestedAtMs: Long = 0L

    fun startSelfUpdateDownload(url: String) = downloadRepo.downloadSelfUpdate(url)

    fun cancelSelfUpdate() = downloadRepo.cancelSelfUpdate()

    fun installSelfUpdateApk(context: Context): Result<Unit> =
        downloadRepo.installSelfUpdateApk(context)

    fun startDownload(app: ModApp, user: FirebaseUser? = null): String {
        if (app.packageName == selfPackageName ||
            app.id == "app_hsstore_self_update" ||
            app.id == "app_hsstore_update"
        ) {
            downloadRepo.downloadSelfUpdate(app.downloadUrl)
            return "self_update"
        }
        val taskId = downloadRepo.startDownload(app)
        if (user != null) {
            viewModelScope.launch {
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
        if (app.packageName.isBlank() || app.packageName == context.packageName) return false
        _pendingReplaceAppId.value = app.id
        pendingReplaceRequestedAtMs = System.currentTimeMillis()
        return InstalledAppDetector.uninstallApp(context, app.packageName)
    }

    fun initiateReplaceUninstallByTask(context: Context, task: DownloadTask): Boolean {
        if (task.packageName.isBlank() || task.packageName == context.packageName) return false
        _pendingReplaceAppId.value = task.appId
        pendingReplaceRequestedAtMs = System.currentTimeMillis()
        return InstalledAppDetector.uninstallApp(context, task.packageName)
    }

    /**
     * Enforces SIGNATURE_MISMATCH check via live PackageManager before launching APK installer.
     * Never calls installApk when a conflicting signature is installed (except self-updates).
     */
    fun installOrReplaceCompletedApk(
        context: Context,
        task: DownloadTask,
        catalogApp: ModApp? = null
    ): Result<Unit> {
        val pkgName = catalogApp?.packageName?.ifBlank { null } ?: task.packageName
        if (pkgName == context.packageName || task.isSelfUpdate) {
            return installApk(context, task)
        }
        val versionCode = catalogApp?.versionCode ?: task.versionCode
        val expectedSig = catalogApp?.expectedSignatureSha256?.ifBlank { null }
            ?: task.expectedSignatureSha256

        if (pkgName.isNotBlank()) {
            val state = InstalledAppDetector.checkState(
                context = context,
                packageName = pkgName,
                storeVersionCode = versionCode,
                expectedSignatureSha256 = expectedSig
            )
            if (state == com.example.util.AppInstallState.SIGNATURE_MISMATCH) {
                _pendingReplaceAppId.value = task.appId
                pendingReplaceRequestedAtMs = System.currentTimeMillis()
                InstalledAppDetector.uninstallApp(context, pkgName)
                return Result.success(Unit)
            }
        }
        return installApk(context, task)
    }

    fun clearPendingReplace() {
        _pendingReplaceAppId.value = null
        pendingReplaceRequestedAtMs = 0L
    }

    /**
     * Checks if an app awaiting replacement uninstall has been removed from the device.
     * Once checkState returns NOT_INSTALLED and the downloaded APK exists, clears pendingReplaceAppId
     * and launches installApk immediately.
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

        val catalogApp = apps.find { it.id == pendingId }
        val pkgName = catalogApp?.packageName?.ifBlank { null } ?: completedTask.packageName
        if (pkgName.isBlank()) return false

        val currentState = InstalledAppDetector.checkState(
            context = context,
            packageName = pkgName,
            storeVersionCode = catalogApp?.versionCode ?: completedTask.versionCode,
            expectedSignatureSha256 = catalogApp?.expectedSignatureSha256?.ifBlank { null }
                ?: completedTask.expectedSignatureSha256
        )
        if (currentState == com.example.util.AppInstallState.NOT_INSTALLED) {
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
