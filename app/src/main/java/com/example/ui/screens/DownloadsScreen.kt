package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.data.model.ModApp
import com.example.ui.components.CupertinoGetButton
import com.example.ui.components.CupertinoPullToRefreshBox
import com.example.ui.components.CupertinoGroupedCard
import com.example.ui.theme.VerifiedBadgeGreen
import com.example.util.AppInstallState
import com.example.util.InstalledAppDetector

import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalizedStrings
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.ui.platform.LocalContext

private val TaskIconShape = RoundedCornerShape(11.dp)

@Composable
fun DownloadsScreen(
    tasks: List<DownloadTask>,
    appsCatalog: List<ModApp> = emptyList(),
    isRefreshing: Boolean = false,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onRefresh: () -> Unit = {},
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onInstall: (DownloadTask) -> Unit,
    onReplaceUninstall: (DownloadTask) -> Unit = {},
    onIsScrollingChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeTasks = androidx.compose.runtime.remember(tasks) {
        tasks.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PAUSED || it.status == DownloadStatus.QUEUED }
    }
    val completedTasks = androidx.compose.runtime.remember(tasks) {
        tasks.filter { it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.FAILED }
    }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Smoothness: notify container when scrolling to pause background animations
    androidx.compose.runtime.LaunchedEffect(lazyListState.isScrollInProgress) {
        onIsScrollingChanged(lazyListState.isScrollInProgress)
    }

    CupertinoPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            userScrollEnabled = true,
            flingBehavior = ScrollableDefaults.flingBehavior(),
            modifier = Modifier
                .fillMaxSize()
                .testTag("downloads_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
        ) {
        // iOS Large Title Header
        item(key = "downloads_header", contentType = "header") {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = LocalizedStrings.get("downloads_kicker", currentLanguage),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (currentLanguage.isRtl) 0.sp else 0.8.sp
                    )
                )
                Text(
                    text = LocalizedStrings.get("downloads_title", currentLanguage),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp
                    )
                )
            }
        }

        // Active Downloads Group
        if (activeTasks.isNotEmpty()) {
            item(key = "active_downloads_group", contentType = "active_group") {
                Text(
                    text = "${LocalizedStrings.get("downloads_active_queue", currentLanguage)} (${activeTasks.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = if (currentLanguage.isRtl) 0.sp else 0.5.sp
                    ),
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                CupertinoGroupedCard {
                    activeTasks.forEachIndexed { index, task ->
                        ActiveDownloadRow(
                            task = task,
                            onPause = { onPause(task.id) },
                            onResume = { onResume(task.id) },
                            onCancel = { onCancel(task.id) },
                            showDivider = index < activeTasks.size - 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Downloaded Packages Group
        item(key = "completed_downloads_group", contentType = "completed_group") {
            Text(
                text = "${LocalizedStrings.get("downloads_completed", currentLanguage)} (${completedTasks.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = if (currentLanguage.isRtl) 0.sp else 0.5.sp
                ),
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
            )

            if (completedTasks.isEmpty()) {
                CupertinoGroupedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = LocalizedStrings.get("downloads_empty", currentLanguage),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = LocalizedStrings.get("downloads_empty_sub", currentLanguage),
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            } else {
                CupertinoGroupedCard {
                    completedTasks.forEachIndexed { index, task ->
                        val matchingApp = appsCatalog.find {
                            it.id == task.appId || (task.packageName.isNotBlank() && it.packageName == task.packageName)
                        }
                        CompletedDownloadRow(
                            task = task,
                            matchingApp = matchingApp,
                            currentLanguage = currentLanguage,
                            onInstall = { onInstall(task) },
                            onReplaceUninstall = { onReplaceUninstall(task) },
                            onDelete = { onCancel(task.id) },
                            showDivider = index < completedTasks.size - 1
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun ActiveDownloadRow(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    showDivider: Boolean
) {
    val context = LocalContext.current
    val iconRequest = remember(task.iconUrl) {
        coil.request.ImageRequest.Builder(context)
            .data(task.iconUrl)
            .size(144, 144)
            .allowHardware(true)
            .crossfade(false)
            .memoryCacheKey(task.iconUrl)
            .diskCacheKey(task.iconUrl)
            .build()
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(TaskIconShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (task.iconUrl.isNotBlank()) {
                    AsyncImage(
                        model = iconRequest,
                        contentDescription = task.appName,
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.appName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                )
                Text(
                    text = "v${task.version} • ${(task.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                )
            }

            Row {
                if (task.status == DownloadStatus.DOWNLOADING) {
                    IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                } else {
                    IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }

                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { task.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    if (showDivider) {
        Divider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = 76.dp)
        )
    }
}

@Composable
private fun CompletedDownloadRow(
    task: DownloadTask,
    matchingApp: ModApp?,
    currentLanguage: AppLanguage,
    onInstall: () -> Unit,
    onReplaceUninstall: () -> Unit,
    onDelete: () -> Unit,
    showDivider: Boolean
) {
    val context = LocalContext.current
    val packageStateVersion by InstalledAppDetector.packageStateVersion.collectAsState()
    val installState = remember(
        task.packageName,
        matchingApp?.versionCode,
        matchingApp?.expectedSignatureSha256,
        packageStateVersion
    ) {
        InstalledAppDetector.detectAppState(
            context = context,
            packageName = task.packageName,
            storeVersionCode = matchingApp?.versionCode ?: 0,
            expectedSignatureSha256 = matchingApp?.expectedSignatureSha256 ?: ""
        )
    }

    val buttonLabel = when (installState) {
        AppInstallState.SIGNATURE_MISMATCH -> LocalizedStrings.get("action_replace", currentLanguage)
        AppInstallState.INSTALLED_LATEST -> LocalizedStrings.get("action_open", currentLanguage)
        else -> LocalizedStrings.get("action_install", currentLanguage)
    }

    val iconRequest = remember(task.iconUrl) {
        coil.request.ImageRequest.Builder(context)
            .data(task.iconUrl)
            .size(144, 144)
            .allowHardware(true)
            .crossfade(false)
            .memoryCacheKey(task.iconUrl)
            .diskCacheKey(task.iconUrl)
            .build()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(TaskIconShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (task.iconUrl.isNotBlank()) {
                AsyncImage(
                    model = iconRequest,
                    contentDescription = task.appName,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = VerifiedBadgeGreen)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.appName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
            )
            Text(
                text = "v${task.version} • ${formatBytes(task.totalBytes)}",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            )
        }

        CupertinoGetButton(
            text = buttonLabel,
            isHighlighted = true,
            containerColor = if (installState == AppInstallState.SIGNATURE_MISMATCH) Color(0xFFFF9500) else null,
            onClick = {
                when (installState) {
                    AppInstallState.SIGNATURE_MISMATCH -> onReplaceUninstall()
                    AppInstallState.INSTALLED_LATEST -> {
                        if (!InstalledAppDetector.launchInstalledApp(context, task.packageName)) {
                            onInstall()
                        }
                    }
                    else -> onInstall()
                }
            }
        )

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
    }

    if (showDivider) {
        Divider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = 76.dp)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return "%.1f MB".format(mb)
}
