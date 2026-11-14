package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.data.model.ModApp
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalizedStrings
import com.example.ui.theme.VerifiedBadgeGreen
import com.example.util.AppInstallState
import com.example.util.InstalledAppDetector

// Top-level static shapes to eliminate per-frame allocations during scrolling
private val ModCardShape = RoundedCornerShape(16.dp)
private val ModIconSquircleShape = RoundedCornerShape(14.dp)

@Composable
fun ModAppCard(
    app: ModApp,
    isBookmarked: Boolean,
    onAppClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    modifier: Modifier = Modifier,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    downloadTask: DownloadTask? = null,
    onInstallDownloadedClick: (DownloadTask) -> Unit = {},
    onReplaceUninstallClick: () -> Unit = {},
    onPauseDownloadClick: (String) -> Unit = {},
    onResumeDownloadClick: (String) -> Unit = {},
    onCancelDownloadClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val packageStateVersion by InstalledAppDetector.packageStateVersion.collectAsState()
    val installState = remember(
        app.packageName,
        app.versionCode,
        app.expectedSignatureSha256,
        packageStateVersion
    ) {
        InstalledAppDetector.detectAppState(
            context = context,
            packageName = app.packageName,
            storeVersionCode = app.versionCode,
            expectedSignatureSha256 = app.expectedSignatureSha256
        )
    }

    val isDownloading = downloadTask?.status == DownloadStatus.DOWNLOADING ||
        downloadTask?.status == DownloadStatus.QUEUED
    val isPaused = downloadTask?.status == DownloadStatus.PAUSED
    val isApkDownloaded = downloadTask?.status == DownloadStatus.COMPLETED &&
        !downloadTask.filePath.isNullOrBlank()

    val bylineText = remember(app.author, app.category) {
        "${app.author} • ${app.category}"
    }

    val downloadsUnit = LocalizedStrings.get("card_downloads_unit", currentLanguage)
    val metaStatsText = remember(app.version, app.downloadCount, app.sizeDisplay, downloadsUnit) {
        "v${app.version} • ${app.downloadCount} $downloadsUnit • ${app.sizeDisplay}"
    }

    val actionButtonText = when {
        isDownloading || isPaused -> "${(downloadTask!!.progress * 100).toInt()}%"
        installState == AppInstallState.INSTALLED_LATEST -> LocalizedStrings.get("action_open", currentLanguage)
        installState == AppInstallState.SIGNATURE_MISMATCH -> LocalizedStrings.get("action_replace", currentLanguage)
        isApkDownloaded && (installState == AppInstallState.NOT_INSTALLED || installState == AppInstallState.UPDATE_AVAILABLE) ->
            LocalizedStrings.get("action_install", currentLanguage)
        installState == AppInstallState.UPDATE_AVAILABLE -> LocalizedStrings.get("action_update", currentLanguage)
        else -> LocalizedStrings.get("action_get", currentLanguage)
    }

    val modFeaturesText = remember(app.modFeatures) {
        if (app.modFeatures.isNotEmpty()) "⚡ " + app.modFeatures.joinToString(" • ") else ""
    }

    val imageRequest = remember(app.iconUrl, app.packageName, app.versionCode) {
        if (app.iconUrl.isNotBlank()) {
            ImageRequest.Builder(context)
                .data(app.iconUrl)
                .size(186, 186)
                .memoryCacheKey("mod_icon_${app.packageName}_${app.versionCode}")
                .diskCacheKey("mod_icon_${app.packageName}_${app.versionCode}")
                .allowHardware(true)
                .crossfade(false)
                .build()
        } else null
    }

    Surface(
        shape = ModCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(ModCardShape)
            .clickable(onClick = onAppClick)
            .testTag("mod_card_${app.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // iOS App Icon (Squircle shape)
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(ModIconSquircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageRequest != null) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = "${app.name} icon",
                            modifier = Modifier.size(58.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // App Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                lineHeight = 19.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (app.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Verified Mod",
                                tint = VerifiedBadgeGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = bylineText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = metaStatsText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Dynamic Action Capsule Button (GET, %, INSTALL, OPEN, UPDATE, REPLACE)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CupertinoGetButton(
                        text = actionButtonText,
                        horizontalPadding = 12.dp,
                        isHighlighted = isDownloading || isApkDownloaded,
                        containerColor = when {
                            isDownloading || isPaused -> MaterialTheme.colorScheme.primary
                            installState == AppInstallState.SIGNATURE_MISMATCH -> Color(0xFFFF9500)
                            isApkDownloaded && installState != AppInstallState.INSTALLED_LATEST -> VerifiedBadgeGreen
                            else -> null
                        },
                        onClick = {
                            when {
                                isDownloading && downloadTask != null -> {
                                    onPauseDownloadClick(downloadTask.id)
                                }
                                isPaused && downloadTask != null -> {
                                    onResumeDownloadClick(downloadTask.id)
                                }
                                installState == AppInstallState.INSTALLED_LATEST -> {
                                    val launched = InstalledAppDetector.launchInstalledApp(context, app.packageName)
                                    if (!launched) {
                                        if (isApkDownloaded && downloadTask != null) {
                                            onInstallDownloadedClick(downloadTask)
                                        } else {
                                            onDownloadClick()
                                        }
                                    }
                                }
                                installState == AppInstallState.SIGNATURE_MISMATCH -> {
                                    if (isApkDownloaded) {
                                        onReplaceUninstallClick()
                                    } else {
                                        // Download First, Then Replace
                                        onDownloadClick()
                                    }
                                }
                                isApkDownloaded && downloadTask != null -> {
                                    onInstallDownloadedClick(downloadTask)
                                }
                                else -> {
                                    onDownloadClick()
                                }
                            }
                        }
                    )

                    if ((isDownloading || isPaused) && downloadTask != null) {
                        Spacer(modifier = Modifier.width(2.dp))
                        IconButton(
                            onClick = { onCancelDownloadClick(downloadTask.id) },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("cancel_dl_btn_${app.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = LocalizedStrings.get("action_cancel", currentLanguage),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Bookmark Icon
                IconButton(
                    onClick = onBookmarkToggle,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("bookmark_btn_${app.id}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) Color(0xFFFF2D55) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (modFeaturesText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 68.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = modFeaturesText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 68.dp)
                )
            }
        }
    }
}
