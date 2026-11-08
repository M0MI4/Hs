package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.data.model.ModApp
import com.example.data.remote.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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
        val sampleFileName = "CyberGhost_Pro_Mod_v8.4.2.apk"
        val sampleFile = File(downloadsDir, sampleFileName)
        if (!sampleFile.exists()) {
            try {
                sampleFile.writeText("HsStore Verified APK Archive")
            } catch (e: Exception) {
                Log.w("DownloadRepo", "Sample file creation: ${e.message}")
            }
        }

        val initialSample = DownloadTask(
            id = "dl_cyber_vpn",
            appId = "app_cyber_vpn",
            appName = "CyberGhost Pro Mod",
            version = "8.4.2",
            packageName = "com.cyberghostvpn.mod",
            iconUrl = "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=150&auto=format&fit=crop&q=80",
            totalBytes = 34812720L,
            downloadedBytes = 34812720L,
            progress = 1.0f,
            speedBytesPerSec = 0L,
            status = DownloadStatus.COMPLETED,
            filePath = sampleFile.absolutePath,
            completedAt = System.currentTimeMillis() - 3600000L,
            sha256 = "4b5d2a93c780962809e2051db57e53f1917f6920fbbad9e9b068df65c82a7201",
            downloadUrl = "https://github.com"
        )
        _tasks.value = listOf(initialSample)
    }

    fun startDownload(app: ModApp): String {
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
            packageName = app.packageName,
            iconUrl = app.iconUrl,
            totalBytes = if (app.sizeBytes > 0) app.sizeBytes else 15728640L,
            downloadedBytes = 0L,
            progress = 0f,
            speedBytesPerSec = 0L,
            status = DownloadStatus.DOWNLOADING,
            filePath = null,
            startedAt = System.currentTimeMillis(),
            sha256 = app.sha256,
            downloadUrl = app.downloadUrl
        )

        _tasks.value = listOf(newTask) + _tasks.value.filter { it.id != taskId }
        launchRealStreamingDownload(newTask)
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
            launchRealStreamingDownload(task)
        }
    }

    fun cancelDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        val task = _tasks.value.find { it.id == taskId }
        task?.filePath?.let { path ->
            try {
                File(path).delete()
            } catch (ignored: Exception) {}
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

    /**
     * Real HTTP Streaming Download with HTTP Range Header Resume Support
     * Eliminates fake byte writing loops (0x5A) entirely.
     */
    private fun launchRealStreamingDownload(task: DownloadTask) {
        val job = scope.launch {
            val sanitizedName = task.appName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val targetFile = File(downloadsDir, "${sanitizedName}_v${task.version}.apk")

            val currentDownloaded = if (targetFile.exists()) targetFile.length() else 0L
            val isResume = currentDownloaded > 0L && currentDownloaded < task.totalBytes

            val rangeHeader = if (isResume) "bytes=$currentDownloaded-" else null

            var outputStream: FileOutputStream? = null
            var inputStream: InputStream? = null

            try {
                val urlToDownload = if (task.downloadUrl.isNotBlank() && task.downloadUrl.startsWith("http")) {
                    task.downloadUrl
                } else {
                    // Fallback to Pastebin host or sample archive if app URL was blank
                    "https://github.com/aistudio/sample.apk"
                }

                val response: Response<ResponseBody> = try {
                    RetrofitClient.apiService.downloadFileStream(urlToDownload, rangeHeader)
                } catch (netEx: Exception) {
                    Log.w("DownloadRepo", "Network streaming call error: ${netEx.message}")
                    null
                } ?: run {
                    // If network fails (e.g. offline/mock environment), handle cleanly
                    handleFallbackOrFailure(task, targetFile, currentDownloaded)
                    return@launch
                }

                if (!response.isSuccessful && response.code() != 206) {
                    Log.e("DownloadRepo", "Server returned HTTP ${response.code()}: ${response.message()}")
                    handleFallbackOrFailure(task, targetFile, currentDownloaded)
                    return@launch
                }

                val body = response.body()
                if (body == null) {
                    handleFallbackOrFailure(task, targetFile, currentDownloaded)
                    return@launch
                }

                // If server responded with 200 OK instead of 206 Partial Content, Range was ignored; restart from 0
                val appendMode = isResume && response.code() == 206
                val startingBytes = if (appendMode) currentDownloaded else 0L
                val serverContentLength = body.contentLength()
                val totalBytes = if (serverContentLength > 0L) {
                    if (appendMode) startingBytes + serverContentLength else serverContentLength
                } else {
                    task.totalBytes.coerceAtLeast(10485760L)
                }

                outputStream = FileOutputStream(targetFile, appendMode)
                inputStream = body.byteStream()

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = startingBytes
                var lastUpdateTime = System.currentTimeMillis()
                var bytesSinceLastUpdate = 0L

                while (isActive) {
                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    bytesSinceLastUpdate += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDiff = now - lastUpdateTime
                    if (timeDiff >= 300) { // Update UI state throttled to ~300ms
                        val speed = if (timeDiff > 0) (bytesSinceLastUpdate * 1000L) / timeDiff else 0L
                        val progress = (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)

                        updateTask(task.id) {
                            it.copy(
                                downloadedBytes = totalRead,
                                totalBytes = totalBytes,
                                progress = progress,
                                speedBytesPerSec = speed,
                                status = DownloadStatus.DOWNLOADING,
                                filePath = targetFile.absolutePath
                            )
                        }
                        lastUpdateTime = now
                        bytesSinceLastUpdate = 0L
                    }
                }

                outputStream.flush()

                if (isActive) {
                    val finalProgress = 1.0f
                    updateTask(task.id) {
                        it.copy(
                            downloadedBytes = totalRead,
                            totalBytes = totalRead,
                            progress = finalProgress,
                            speedBytesPerSec = 0L,
                            status = DownloadStatus.COMPLETED,
                            completedAt = System.currentTimeMillis(),
                            filePath = targetFile.absolutePath
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("DownloadRepo", "Streaming error for ${task.appName}: ${e.message}", e)
                if (isActive) {
                    updateTask(task.id) {
                        it.copy(status = DownloadStatus.FAILED, speedBytesPerSec = 0L)
                    }
                }
            } finally {
                try { inputStream?.close() } catch (ignored: Exception) {}
                try { outputStream?.close() } catch (ignored: Exception) {}
                activeJobs.remove(task.id)
            }
        }

        activeJobs[task.id] = job
    }

    private fun handleFallbackOrFailure(task: DownloadTask, targetFile: File, currentDownloaded: Long) {
        // If offline or remote URL is unreachable, mark FAILED cleanly
        updateTask(task.id) {
            it.copy(
                status = DownloadStatus.FAILED,
                speedBytesPerSec = 0L,
                filePath = if (targetFile.exists()) targetFile.absolutePath else null
            )
        }
    }

    private fun updateTask(taskId: String, block: (DownloadTask) -> DownloadTask) {
        _tasks.value = _tasks.value.map {
            if (it.id == taskId) block(it) else it
        }
    }
}
