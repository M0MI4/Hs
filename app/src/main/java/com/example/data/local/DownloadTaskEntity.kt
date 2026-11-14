package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
    val appId: String,
    val appTitle: String,
    val packageName: String,
    val version: String,
    val versionCode: Int,
    val iconUrl: String,
    val localFilePath: String?,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val progress: Float,
    val status: String,
    val startedAt: Long,
    val completedAt: Long?,
    val sha256: String,
    val downloadUrl: String,
    val expectedSignatureSha256: String = ""
) {
    fun toDomain(overrideStatus: DownloadStatus? = null): DownloadTask {
        val parsedStatus = overrideStatus ?: try {
            DownloadStatus.valueOf(status)
        } catch (_: Exception) {
            DownloadStatus.FAILED
        }
        return DownloadTask(
            id = id,
            appId = appId,
            appName = appTitle,
            version = version,
            versionCode = versionCode,
            iconUrl = iconUrl,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            progress = progress,
            speedBytesPerSec = 0L,
            status = parsedStatus,
            filePath = localFilePath,
            startedAt = startedAt,
            completedAt = completedAt,
            sha256 = sha256,
            downloadUrl = downloadUrl,
            packageName = packageName,
            expectedSignatureSha256 = expectedSignatureSha256
        )
    }

    companion object {
        fun fromDomain(task: DownloadTask): DownloadTaskEntity {
            return DownloadTaskEntity(
                id = task.id,
                appId = task.appId,
                appTitle = task.appName,
                packageName = task.packageName,
                version = task.version,
                versionCode = task.versionCode,
                iconUrl = task.iconUrl,
                localFilePath = task.filePath,
                totalBytes = task.totalBytes,
                downloadedBytes = task.downloadedBytes,
                progress = task.progress,
                status = task.status.name,
                startedAt = task.startedAt,
                completedAt = task.completedAt,
                sha256 = task.sha256,
                downloadUrl = task.downloadUrl,
                expectedSignatureSha256 = task.expectedSignatureSha256
            )
        }
    }
}
