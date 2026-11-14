package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadTaskDao
import com.example.data.local.DownloadTaskEntity
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class DownloadRepository(
    private val context: Context,
    private val downloadTaskDao: DownloadTaskDao = AppDatabase.getInstance(context).downloadTaskDao()
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val activeJobs = mutableMapOf<String, Job>()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    // Persistent APK storage directory inside filesDir (never cacheDir)
    private val apksDir = File(context.filesDir, "apks").apply {
        if (!exists()) mkdirs()
    }

    init {
        scope.launch {
            restoreSavedTasks()
        }
    }

    fun apkFileForApp(appId: String): File {
        val safeId = appId.trim().replace("[^a-zA-Z0-9._-]".toRegex(), "_").ifBlank { "app" }
        return File(apksDir, "$safeId.apk")
    }

    suspend fun restoreSavedTasks() {
        try {
            val savedEntities = downloadTaskDao.getAllTasks()
            val restored = mutableListOf<DownloadTask>()

            for (entity in savedEntities) {
                val file = entity.localFilePath?.let { File(it) } ?: apkFileForApp(entity.appId)
                when (entity.status) {
                    DownloadStatus.COMPLETED.name -> {
                        if (file.exists() && file.length() > 0L) {
                            val resolvedSize = if (entity.totalBytes > 0L) entity.totalBytes else file.length()
                            restored.add(
                                entity.copy(
                                    localFilePath = file.absolutePath,
                                    totalBytes = resolvedSize,
                                    downloadedBytes = resolvedSize,
                                    progress = 1.0f
                                ).toDomain(overrideStatus = DownloadStatus.COMPLETED)
                            )
                        } else {
                            // Clean up orphaned record whose APK no longer exists
                            downloadTaskDao.deleteById(entity.id)
                        }
                    }
                    DownloadStatus.DOWNLOADING.name, DownloadStatus.QUEUED.name -> {
                        val partialLen = if (file.exists()) file.length() else 0L
                        val total = entity.totalBytes.coerceAtLeast(1L)
                        val restoredProgress = if (partialLen > 0L) {
                            (partialLen.toFloat() / total.toFloat()).coerceIn(0f, 0.99f)
                        } else {
                            0f
                        }
                        val pausedEntity = entity.copy(
                            status = DownloadStatus.PAUSED.name,
                            localFilePath = if (file.exists()) file.absolutePath else entity.localFilePath,
                            downloadedBytes = partialLen,
                            progress = restoredProgress
                        )
                        downloadTaskDao.upsertTask(pausedEntity)
                        restored.add(pausedEntity.toDomain(overrideStatus = DownloadStatus.PAUSED))
                    }
                    else -> {
                        val partialLen = if (file.exists()) file.length() else entity.downloadedBytes
                        restored.add(
                            entity.copy(
                                localFilePath = if (file.exists()) file.absolutePath else entity.localFilePath,
                                downloadedBytes = partialLen
                            ).toDomain()
                        )
                    }
                }
            }

            // Merge atomically with any in-memory tasks added concurrently during startup
            _tasks.update { current ->
                val currentMap = current.associateBy { it.appId }
                (current + restored.filter { !currentMap.containsKey(it.appId) })
                    .sortedByDescending { it.startedAt }
            }
        } catch (e: Exception) {
            Log.e("DownloadRepo", "Failed restoring saved downloads: ${e.message}", e)
        }
    }

    fun startDownload(app: ModApp): String {
        val existing = _tasks.value.find { it.appId == app.id }
        if (existing != null && (existing.status == DownloadStatus.DOWNLOADING || existing.status == DownloadStatus.QUEUED)) {
            return existing.id
        }

        val targetFile = apkFileForApp(app.id)
        val taskId = "dl_" + UUID.randomUUID().toString().take(8)
        val newTask = DownloadTask(
            id = taskId,
            appId = app.id,
            appName = app.name,
            version = app.version,
            versionCode = app.versionCode,
            packageName = app.packageName,
            iconUrl = app.iconUrl,
            totalBytes = if (app.sizeBytes > 0) app.sizeBytes else 15728640L,
            downloadedBytes = 0L,
            progress = 0f,
            speedBytesPerSec = 0L,
            status = DownloadStatus.DOWNLOADING,
            filePath = targetFile.absolutePath,
            startedAt = System.currentTimeMillis(),
            sha256 = app.sha256,
            downloadUrl = app.downloadUrl,
            expectedSignatureSha256 = app.expectedSignatureSha256
        )

        _tasks.update { current ->
            listOf(newTask) + current.filter { it.appId != app.id && it.id != taskId }
        }
        scope.launch {
            downloadTaskDao.deleteByAppId(app.id)
            downloadTaskDao.upsertTask(DownloadTaskEntity.fromDomain(newTask))
        }
        launchRealStreamingDownload(newTask, targetFile)
        return taskId
    }

    fun pauseDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        updateTask(taskId, persist = true) {
            it.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L)
        }
    }

    fun resumeDownload(taskId: String) {
        val task = _tasks.value.find { it.id == taskId } ?: return
        if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.FAILED) {
            val targetFile = task.filePath?.let { File(it) } ?: apkFileForApp(task.appId)
            val updated = task.copy(
                status = DownloadStatus.DOWNLOADING,
                filePath = targetFile.absolutePath
            )
            updateTask(taskId, persist = true) { updated }
            launchRealStreamingDownload(updated, targetFile)
        }
    }

    fun cancelDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        val task = _tasks.value.find { it.id == taskId }
        task?.filePath?.let { path ->
            try {
                File(path).delete()
            } catch (_: Exception) {}
        }
        _tasks.update { current -> current.filter { it.id != taskId } }
        scope.launch {
            downloadTaskDao.deleteById(taskId)
            if (task != null) {
                downloadTaskDao.deleteByAppId(task.appId)
            }
        }
    }

    fun installApk(context: Context, task: DownloadTask): Result<Unit> {
        val path = task.filePath ?: apkFileForApp(task.appId).absolutePath
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

    suspend fun upsertSavedTask(task: DownloadTask) {
        _tasks.update { current ->
            listOf(task) + current.filter { it.appId != task.appId && it.id != task.id }
        }
        downloadTaskDao.deleteByAppId(task.appId)
        downloadTaskDao.upsertTask(DownloadTaskEntity.fromDomain(task))
    }

    /**
     * Real HTTP Streaming Download with HTTP Range Header Resume Support
     * Writes directly to persistent files/apks/<app.id>.apk.
     */
    private fun launchRealStreamingDownload(task: DownloadTask, targetFile: File) {
        targetFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

        val job = scope.launch {
            val currentDownloaded = if (targetFile.exists()) targetFile.length() else 0L
            val isResume = currentDownloaded > 0L && currentDownloaded < task.totalBytes

            val rangeHeader = if (isResume) "bytes=$currentDownloaded-" else null

            var outputStream: FileOutputStream? = null
            var inputStream: InputStream? = null

            try {
                val urlToDownload = if (task.downloadUrl.isNotBlank() && task.downloadUrl.startsWith("http")) {
                    task.downloadUrl
                } else {
                    "https://github.com/aistudio/sample.apk"
                }

                val response: Response<ResponseBody> = try {
                    RetrofitClient.apiService.downloadFileStream(urlToDownload, rangeHeader)
                } catch (netEx: Exception) {
                    Log.w("DownloadRepo", "Network streaming call error: ${netEx.message}")
                    null
                } ?: run {
                    handleFallbackOrFailure(task, targetFile)
                    return@launch
                }

                if (!response.isSuccessful && response.code() != 206) {
                    Log.e("DownloadRepo", "Server returned HTTP ${response.code()}: ${response.message()}")
                    handleFallbackOrFailure(task, targetFile)
                    return@launch
                }

                val body = response.body()
                if (body == null) {
                    handleFallbackOrFailure(task, targetFile)
                    return@launch
                }

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
                var lastPersistTime = lastUpdateTime
                var bytesSinceLastUpdate = 0L

                while (isActive) {
                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    bytesSinceLastUpdate += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDiff = now - lastUpdateTime
                    if (timeDiff >= 300) {
                        val speed = if (timeDiff > 0) (bytesSinceLastUpdate * 1000L) / timeDiff else 0L
                        val progress = (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        val shouldPersist = (now - lastPersistTime) >= 1500

                        updateTask(task.id, persist = shouldPersist) {
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
                        if (shouldPersist) lastPersistTime = now
                        bytesSinceLastUpdate = 0L
                    }
                }

                outputStream.flush()

                if (isActive) {
                    updateTask(task.id, persist = true) {
                        it.copy(
                            downloadedBytes = totalRead,
                            totalBytes = totalRead,
                            progress = 1.0f,
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
                    updateTask(task.id, persist = true) {
                        it.copy(
                            status = DownloadStatus.FAILED,
                            speedBytesPerSec = 0L,
                            downloadedBytes = if (targetFile.exists()) targetFile.length() else it.downloadedBytes,
                            filePath = targetFile.absolutePath
                        )
                    }
                }
            } finally {
                try { inputStream?.close() } catch (_: Exception) {}
                try { outputStream?.close() } catch (_: Exception) {}
                activeJobs.remove(task.id)
            }
        }

        activeJobs[task.id] = job
    }

    private fun handleFallbackOrFailure(task: DownloadTask, targetFile: File) {
        updateTask(task.id, persist = true) {
            it.copy(
                status = DownloadStatus.FAILED,
                speedBytesPerSec = 0L,
                downloadedBytes = if (targetFile.exists()) targetFile.length() else 0L,
                filePath = if (targetFile.exists()) targetFile.absolutePath else null
            )
        }
    }

    private fun updateTask(
        taskId: String,
        persist: Boolean = false,
        block: (DownloadTask) -> DownloadTask
    ) {
        var updatedTask: DownloadTask? = null
        _tasks.update { current ->
            current.map {
                if (it.id == taskId) {
                    block(it).also { res -> updatedTask = res }
                } else {
                    it
                }
            }
        }
        if (persist) {
            updatedTask?.let { task ->
                scope.launch {
                    downloadTaskDao.upsertTask(DownloadTaskEntity.fromDomain(task))
                }
            }
        }
    }
}
