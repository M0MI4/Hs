package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.data.model.ModApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DownloadRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val activeJobs = mutableMapOf<String, Job>()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val downloadsDir = File(context.filesDir, "downloads").apply {
        if (!exists()) mkdirs()
    }

    init {
        // Pre-populate with one completed sample mod so the download list is immediately populated
        val initialSample = DownloadTask(
            id = "dl_cyber_vpn",
            appId = "app_cyber_vpn",
            appName = "CyberGhost Pro Mod",
            version = "8.4.2",
            iconUrl = "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=150&auto=format&fit=crop&q=80",
            totalBytes = 34812720L,
            downloadedBytes = 34812720L,
            progress = 1.0f,
            speedBytesPerSec = 0L,
            status = DownloadStatus.COMPLETED,
            filePath = File(downloadsDir, "CyberGhost_Pro_Mod_v8.4.2.apk").absolutePath,
            completedAt = System.currentTimeMillis() - 3600000L,
            sha256 = "4b5d2a93c780962809e2051db57e53f1917f6920fbbad9e9b068df65c82a7201"
        )
        // Ensure dummy file exists for the sample
        val file = File(downloadsDir, "CyberGhost_Pro_Mod_v8.4.2.apk")
        if (!file.exists()) {
            try {
                file.writeText("HsStore Secure APK Payload Mock")
            } catch (e: Exception) {
                Log.w("DownloadRepo", "Could not write sample mock: ${e.message}")
            }
        }
        _tasks.value = listOf(initialSample)
    }

    fun startDownload(app: ModApp): String {
        // Check if already downloading or queued
        val existing = _tasks.value.find { it.appId == app.id }
        if (existing != null && (existing.status == DownloadStatus.DOWNLOADING || existing.status == DownloadStatus.QUEUED)) {
            return existing.id
        }

        val taskId = "dl_" + UUID.randomUUID().toString().take(8)
        val newTask = DownloadTask(
            id = taskId,
            appId = app.id,
            appName = app.name,
            version = app.version,
            iconUrl = app.iconUrl,
            totalBytes = if (app.sizeBytes > 0) app.sizeBytes else 15728640L,
            downloadedBytes = 0L,
            progress = 0f,
            speedBytesPerSec = 0L,
            status = DownloadStatus.DOWNLOADING,
            filePath = null,
            startedAt = System.currentTimeMillis(),
            sha256 = app.sha256
        )

        _tasks.value = listOf(newTask) + _tasks.value.filter { it.id != taskId }
        launchDownloadLoop(newTask)
        return taskId
    }

    fun pauseDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        updateTask(taskId) { it.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L) }
    }

    fun resumeDownload(taskId: String) {
        val task = _tasks.value.find { it.id == taskId } ?: return
        if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.FAILED) {
            updateTask(taskId) { it.copy(status = DownloadStatus.DOWNLOADING) }
            launchDownloadLoop(task)
        }
    }

    fun cancelDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        val task = _tasks.value.find { it.id == taskId }
        task?.filePath?.let { path ->
            try {
                File(path).delete()
            } catch (e: Exception) {
                // ignore
            }
        }
        _tasks.value = _tasks.value.filter { it.id != taskId }
    }

    fun installApk(context: Context, task: DownloadTask): Result<Unit> {
        val path = task.filePath ?: return Result.failure(Exception("Downloaded APK file not found"))
        val apkFile = File(path)
        if (!apkFile.exists()) {
            return Result.failure(Exception("File does not exist at $path"))
        }

        return try {
            val authority = "${context.packageName}.provider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DownloadRepo", "Installation failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun launchDownloadLoop(task: DownloadTask) {
        val job = scope.launch {
            val sanitizedName = task.appName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val targetFile = File(downloadsDir, "${sanitizedName}_v${task.version}.apk")

            var downloaded = task.downloadedBytes
            val total = task.totalBytes
            val chunkSize = (total / 20).coerceAtLeast(65536L)

            // Open or append to file
            val outputStream = FileOutputStream(targetFile, true)

            try {
                val buffer = ByteArray(4096) { 0x5A }

                while (isActive && downloaded < total) {
                    delay(300) // 300ms step for smooth UI updates
                    val nextChunk = (chunkSize + (Math.random() * 50000).toLong()).coerceAtMost(total - downloaded)
                    outputStream.write(buffer, 0, (nextChunk % 4096).toInt().coerceAtLeast(128))
                    downloaded += nextChunk

                    val currentProgress = (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    val speed = (nextChunk * 3.3).toLong() // calculated bytes per sec

                    updateTask(task.id) {
                        it.copy(
                            downloadedBytes = downloaded,
                            progress = currentProgress,
                            speedBytesPerSec = speed,
                            status = DownloadStatus.DOWNLOADING,
                            filePath = targetFile.absolutePath
                        )
                    }
                }

                outputStream.flush()
                outputStream.close()

                if (downloaded >= total) {
                    updateTask(task.id) {
                        it.copy(
                            downloadedBytes = total,
                            progress = 1.0f,
                            speedBytesPerSec = 0L,
                            status = DownloadStatus.COMPLETED,
                            completedAt = System.currentTimeMillis(),
                            filePath = targetFile.absolutePath
                        )
                    }
                }
            } catch (e: Exception) {
                try { outputStream.close() } catch (ignored: Exception) {}
                if (isActive) {
                    updateTask(task.id) {
                        it.copy(status = DownloadStatus.FAILED, speedBytesPerSec = 0L)
                    }
                }
            } finally {
                activeJobs.remove(task.id)
            }
        }
        activeJobs[task.id] = job
    }

    private fun updateTask(taskId: String, block: (DownloadTask) -> DownloadTask) {
        _tasks.value = _tasks.value.map {
            if (it.id == taskId) block(it) else it
        }
    }
}
