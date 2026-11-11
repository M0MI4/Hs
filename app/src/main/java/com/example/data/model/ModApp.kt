package com.example.data.model

import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Immutable
data class ModApp(
    val id: String = "",
    val name: String = "",
    val packageName: String = "",
    val category: String = "Tools",
    val version: String = "1.0.0",
    val versionCode: Int = 1,
    val sizeBytes: Long = 0L,
    val sizeDisplay: String = "0 MB",
    val iconUrl: String = "",
    val downloadUrl: String = "",
    val sha256: String = "",
    val description: String = "",
    val modFeatures: List<String> = emptyList(),
    val changelog: String = "",
    val rating: Float = 5.0f,
    val downloadsCount: Int = 0,
    val isVerified: Boolean = true,
    val author: String = "Community Dev",
    val updatedDate: String = "",
    val permissionsRequired: List<String> = emptyList(),
    val sortOrder: Int = 0
)

@Immutable
data class UserNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val actionUrl: String = "",
    val isActive: Boolean = true
)

@Immutable
data class AppUpdateConfig(
    val storeLatestVersion: String = "1.0.0",
    val storeLatestVersionCode: Int = 1,
    val currentVersionCode: Int = 1,
    val mandatory: Boolean = false,
    val title: String = "HsStore Update",
    val announcementMessage: String = "",
    val downloadUrl: String = "",
    val sizeDisplay: String = "12 MB",
    val sha256: String = "",
    val publishedAt: String = "",
    val changelog: List<String> = emptyList(),
    val showDialog: Boolean = false
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

@Immutable
data class DownloadTask(
    val id: String = "",
    val appId: String = "",
    val appName: String = "",
    val version: String = "",
    val iconUrl: String = "",
    val totalBytes: Long = 10485760L,
    val downloadedBytes: Long = 0L,
    val progress: Float = 0.0f,
    val speedBytesPerSec: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val filePath: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val sha256: String = "",
    val downloadUrl: String = "",
    val packageName: String = ""
)

enum class NewsType {
    BANNER,
    TEXT_ONLY,
    PATCH_NOTES,
    SPOTLIGHT
}

@Immutable
data class NewsPost(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val type: NewsType = NewsType.TEXT_ONLY,
    val bannerUrl: String = "",
    val tag: String = "Update",
    val date: String = "",
    val readTime: String = "2 min read",
    val author: String = "HsStore Team",
    val authorRole: String = "Editorial",
    val content: String = "",
    val bulletPoints: List<String> = emptyList(),
    val linkedAppId: String? = null,
    val linkedAppName: String? = null,
    val isPinned: Boolean = false,
    val isBreaking: Boolean = false,
    val sortOrder: Int = 0
)

@Immutable
@IgnoreExtraProperties
data class DeviceConfigItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val installedApps: List<String> = emptyList(),
    val customConfigJson: String = "{}",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@IgnoreExtraProperties
data class UserDownloadRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val appId: String = "",
    val appName: String = "",
    val version: String = "",
    val status: String = "COMPLETED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@IgnoreExtraProperties
data class UserBookmarkItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val appId: String = "",
    val appName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Immutable
@IgnoreExtraProperties
data class FeedbackReportItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "SUGGESTION",
    val subject: String = "",
    val details: String = "",
    val deviceInfo: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
