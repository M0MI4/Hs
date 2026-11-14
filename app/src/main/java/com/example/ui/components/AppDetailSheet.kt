package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil.compose.AsyncImage
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.data.model.ModApp
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalizedStrings
import com.example.ui.theme.VerifiedBadgeGreen
import com.example.util.AppInstallState
import com.example.util.InstalledAppDetector

private val DetailIconShape = RoundedCornerShape(16.dp)
private val ScreenshotShape = RoundedCornerShape(12.dp)
private val UninstallWarningColor = Color(0xFFFF9500)

private fun parseGlowColor(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return null
    return try {
        val parsed = clean.toLong(16)
        if (clean.length == 6) {
            Color((0xFF000000L or parsed).toInt())
        } else {
            Color(parsed.toInt())
        }
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailSheet(
    app: ModApp,
    onDismiss: () -> Unit,
    onStartDownload: () -> Unit,
    modifier: Modifier = Modifier,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    downloadTask: DownloadTask? = null,
    onInstallDownloadedClick: (DownloadTask) -> Unit = {},
    onReplaceUninstallClick: () -> Unit = {},
    onPauseDownload: (String) -> Unit = {},
    onResumeDownload: (String) -> Unit = {},
    onCancelDownload: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Only show sticky bottom download button after top header button scrolls out of view
    val isHeaderScrolledOut by remember {
        derivedStateOf { scrollState.value > 180 }
    }

    val glowColor = remember(app.glowColorHex) {
        parseGlowColor(app.glowColorHex)
    }

    val iconImageRequest = remember(app.iconUrl) {
        coil.request.ImageRequest.Builder(context)
            .data(app.iconUrl)
            .size(216, 216)
            .allowHardware(true)
            .crossfade(false)
            .memoryCacheKey(app.iconUrl)
            .diskCacheKey(app.iconUrl)
            .build()
    }

    // Dynamic App Install State (NOT_INSTALLED, INSTALLED_LATEST, UPDATE_AVAILABLE, SIGNATURE_MISMATCH)
    val packageStateVersion by InstalledAppDetector.packageStateVersion.collectAsState()
    LifecycleResumeEffect(app.packageName) {
        InstalledAppDetector.onPackageBroadcast(context, app.packageName)
        onPauseOrDispose {}
    }

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

    val headerActionText = when {
        isDownloading || isPaused -> "${(downloadTask!!.progress * 100).toInt()}%"
        installState == AppInstallState.INSTALLED_LATEST -> LocalizedStrings.get("action_open", currentLanguage)
        installState == AppInstallState.SIGNATURE_MISMATCH -> LocalizedStrings.get("action_replace", currentLanguage)
        isApkDownloaded && (installState == AppInstallState.NOT_INSTALLED || installState == AppInstallState.UPDATE_AVAILABLE) ->
            LocalizedStrings.get("action_install", currentLanguage)
        installState == AppInstallState.UPDATE_AVAILABLE -> LocalizedStrings.get("action_update", currentLanguage)
        else -> LocalizedStrings.get("action_get", currentLanguage)
    }

    // Lightbox State
    var lightboxInitialIndex by remember { mutableStateOf<Int?>(null) }

    // Curated high quality app preview screenshots or custom uploaded screenshots
    val screenshots = remember(app.id, app.category, app.screenshots) {
        if (app.screenshots.isNotEmpty()) app.screenshots else getSampleScreenshotsForApp(app)
    }

    val releaseNotesHeader = remember(app.version, currentLanguage) {
        val template = LocalizedStrings.get("detail_release_notes", currentLanguage)
        try { String.format(template, app.version) } catch (_: Exception) { "v${app.version}" }
    }

    val updatedText = remember(app.updatedDate, currentLanguage) {
        val template = LocalizedStrings.get("detail_updated", currentLanguage)
        try { String.format(template, app.updatedDate) } catch (_: Exception) { app.updatedDate }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        },
        modifier = modifier
            .statusBarsPadding()
            .testTag("app_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
                .padding(top = 4.dp, bottom = 36.dp)
        ) {
            // Header: Squircle Icon with Optional Ambient Glow, Title, Author & Dynamic Action Capsule
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (glowColor != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            glowColor.copy(alpha = 0.55f),
                                            glowColor.copy(alpha = 0.18f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(DetailIconShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (app.iconUrl.isNotBlank()) {
                            AsyncImage(
                                model = iconImageRequest,
                                contentDescription = app.name,
                                modifier = Modifier.size(72.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.author,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "v${app.version} • ${app.sizeDisplay}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Apple Dynamic Capsule Button (GET, %, INSTALL, OPEN, UPDATE, REPLACE)
                CupertinoGetButton(
                    text = headerActionText,
                    isHighlighted = true,
                    containerColor = when {
                        isDownloading || isPaused -> MaterialTheme.colorScheme.primary
                        installState == AppInstallState.SIGNATURE_MISMATCH -> UninstallWarningColor
                        isApkDownloaded && installState != AppInstallState.INSTALLED_LATEST -> VerifiedBadgeGreen
                        else -> null
                    },
                    onClick = {
                        when {
                            isDownloading && downloadTask != null -> {
                                onPauseDownload(downloadTask.id)
                            }
                            isPaused && downloadTask != null -> {
                                onResumeDownload(downloadTask.id)
                            }
                            installState == AppInstallState.INSTALLED_LATEST -> {
                                val launched = InstalledAppDetector.launchInstalledApp(context, app.packageName)
                                if (!launched) {
                                    if (isApkDownloaded && downloadTask != null) {
                                        onInstallDownloadedClick(downloadTask)
                                    } else {
                                        onStartDownload()
                                    }
                                }
                                onDismiss()
                            }
                            installState == AppInstallState.SIGNATURE_MISMATCH -> {
                                if (isApkDownloaded) {
                                    onReplaceUninstallClick()
                                } else {
                                    onStartDownload()
                                }
                            }
                            isApkDownloaded && downloadTask != null -> {
                                onInstallDownloadedClick(downloadTask)
                            }
                            else -> {
                                onStartDownload()
                            }
                        }
                    }
                )
            }

            // Live Download Progress Bar with Pause / Resume / Cancel controls
            AnimatedVisibility(visible = (isDownloading || isPaused) && downloadTask != null) {
                if (downloadTask != null) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${LocalizedStrings.get("downloads_active_section", currentLanguage)}: ${(downloadTask.progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (isDownloading) onPauseDownload(downloadTask.id)
                                                else onResumeDownload(downloadTask.id)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isDownloading) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = LocalizedStrings.get(if (isDownloading) "action_pause" else "action_resume", currentLanguage),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onCancelDownload(downloadTask.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = LocalizedStrings.get("action_cancel", currentLanguage),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { downloadTask.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                }
            }

            // Smart Signature Mismatch Warning Pill (shown ONLY when an existing package has a different SHA-256 signature)
            AnimatedVisibility(visible = installState == AppInstallState.SIGNATURE_MISMATCH) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = UninstallWarningColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, UninstallWarningColor.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signature_mismatch_warning")
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = UninstallWarningColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isApkDownloaded) {
                                        LocalizedStrings.get("detail_signature_mismatch_ready_note", currentLanguage)
                                    } else {
                                        LocalizedStrings.get("detail_signature_mismatch_note", currentLanguage)
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            if (!isDownloading && !isPaused) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val warningBtnText = if (isApkDownloaded) {
                                    LocalizedStrings.get("detail_uninstall_existing_btn", currentLanguage)
                                } else {
                                    val tpl = LocalizedStrings.get("detail_download_first_replace_btn", currentLanguage)
                                    try { String.format(tpl, app.sizeDisplay) } catch (_: Exception) { "Download Mod First (${app.sizeDisplay})" }
                                }
                                Button(
                                    onClick = {
                                        if (isApkDownloaded) {
                                            onReplaceUninstallClick()
                                        } else {
                                            onStartDownload()
                                        }
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = UninstallWarningColor,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .testTag("uninstall_existing_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isApkDownloaded) Icons.Default.DeleteOutline else Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = warningBtnText,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real App Stats Bar: Downloads | Version | Size • Category
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = LocalizedStrings.get("detail_downloads", currentLanguage),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = app.downloadCount.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        maxLines = 1
                    )
                }

                Divider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier
                        .height(28.dp)
                        .width(0.5.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = LocalizedStrings.get("detail_version", currentLanguage),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "v${app.version}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Divider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier
                        .height(28.dp)
                        .width(0.5.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1.25f)
                ) {
                    Text(
                        text = LocalizedStrings.get("detail_size_category", currentLanguage),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${app.sizeDisplay} • ${app.category}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // iOS Screenshots Carousel
            Text(
                text = LocalizedStrings.get("detail_preview_screenshots", currentLanguage),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = if (currentLanguage.isRtl) 0.sp else 0.6.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 12.dp)
            ) {
                itemsIndexed(
                    items = screenshots,
                    key = { _, url -> url },
                    contentType = { _, _ -> "screenshot_thumbnail" }
                ) { index, url ->
                    val screenshotRequest = remember(url) {
                        coil.request.ImageRequest.Builder(context)
                            .data(url)
                            .allowHardware(true)
                            .crossfade(false)
                            .memoryCacheKey(url)
                            .diskCacheKey(url)
                            .build()
                    }

                    Surface(
                        shape = ScreenshotShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .width(130.dp)
                            .height(220.dp)
                            .clip(ScreenshotShape)
                            .clickable { lightboxInitialIndex = index }
                    ) {
                        AsyncImage(
                            model = screenshotRequest,
                            contentDescription = "Screenshot ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // iOS Segmented Detail Tabs (Only 2 clean tabs: Mod Features & Changelog)
            CupertinoSegmentedControl(
                items = listOf(
                    LocalizedStrings.get("detail_tab_features", currentLanguage),
                    LocalizedStrings.get("detail_tab_changelog", currentLanguage)
                ),
                selectedIndex = selectedTab.coerceIn(0, 1),
                onItemSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = app.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 21.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = LocalizedStrings.get("detail_whats_modded", currentLanguage),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        app.modFeatures.forEach { feat ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = feat,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                                )
                            }
                        }
                    }
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = releaseNotesHeader,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = app.changelog,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        )
                        if (app.updatedDate.isNotBlank()) {
                            Text(
                                text = updatedText,
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }

            // Secondary bottom button only visible once top header GET button scrolls off screen
            AnimatedVisibility(
                visible = isHeaderScrolledOut,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    val primaryBtnText = remember(installState, isDownloading, isPaused, isApkDownloaded, downloadTask?.progress, app.sizeDisplay, currentLanguage) {
                        when {
                            isDownloading || isPaused -> {
                                val pct = ((downloadTask?.progress ?: 0f) * 100).toInt()
                                "${LocalizedStrings.get("downloads_active_section", currentLanguage)} ($pct%)"
                            }
                            installState == AppInstallState.INSTALLED_LATEST -> LocalizedStrings.get("detail_action_open", currentLanguage)
                            installState == AppInstallState.SIGNATURE_MISMATCH -> {
                                if (isApkDownloaded) {
                                    LocalizedStrings.get("detail_action_replace", currentLanguage)
                                } else {
                                    val tpl = LocalizedStrings.get("detail_action_download_replace", currentLanguage)
                                    try { String.format(tpl, app.sizeDisplay) } catch (_: Exception) { "Download & Replace (${app.sizeDisplay})" }
                                }
                            }
                            isApkDownloaded -> LocalizedStrings.get("detail_action_install", currentLanguage)
                            installState == AppInstallState.UPDATE_AVAILABLE -> {
                                val tpl = LocalizedStrings.get("detail_action_update", currentLanguage)
                                try { String.format(tpl, app.sizeDisplay) } catch (_: Exception) { "Update (${app.sizeDisplay})" }
                            }
                            else -> {
                                val tpl = LocalizedStrings.get("detail_action_download", currentLanguage)
                                try { String.format(tpl, app.sizeDisplay) } catch (_: Exception) { "Download (${app.sizeDisplay})" }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            when {
                                isDownloading && downloadTask != null -> {
                                    onPauseDownload(downloadTask.id)
                                }
                                isPaused && downloadTask != null -> {
                                    onResumeDownload(downloadTask.id)
                                }
                                installState == AppInstallState.INSTALLED_LATEST -> {
                                    val launched = InstalledAppDetector.launchInstalledApp(context, app.packageName)
                                    if (!launched) {
                                        if (isApkDownloaded && downloadTask != null) {
                                            onInstallDownloadedClick(downloadTask)
                                        } else {
                                            onStartDownload()
                                        }
                                    }
                                    onDismiss()
                                }
                                installState == AppInstallState.SIGNATURE_MISMATCH -> {
                                    if (isApkDownloaded) {
                                        onReplaceUninstallClick()
                                    } else {
                                        onStartDownload()
                                    }
                                }
                                isApkDownloaded && downloadTask != null -> {
                                    onInstallDownloadedClick(downloadTask)
                                }
                                else -> {
                                    onStartDownload()
                                }
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isDownloading || isPaused -> MaterialTheme.colorScheme.primary
                                installState == AppInstallState.INSTALLED_LATEST -> VerifiedBadgeGreen
                                installState == AppInstallState.SIGNATURE_MISMATCH -> UninstallWarningColor
                                isApkDownloaded -> VerifiedBadgeGreen
                                else -> MaterialTheme.colorScheme.primary
                            },
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("detail_download_action")
                    ) {
                        Icon(
                            imageVector = when {
                                isDownloading -> Icons.Default.Pause
                                isPaused -> Icons.Default.PlayArrow
                                installState == AppInstallState.INSTALLED_LATEST -> Icons.Default.OpenInNew
                                installState == AppInstallState.SIGNATURE_MISMATCH ->
                                    if (isApkDownloaded) Icons.Default.DeleteOutline else Icons.Default.Download
                                installState == AppInstallState.UPDATE_AVAILABLE && !isApkDownloaded -> Icons.Default.Update
                                else -> Icons.Default.Download
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = primaryBtnText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    // Fullscreen Zoomable HorizontalPager Lightbox
    if (lightboxInitialIndex != null) {
        ScreenshotLightboxDialog(
            screenshots = screenshots,
            initialIndex = lightboxInitialIndex!!,
            currentLanguage = currentLanguage,
            onDismiss = { lightboxInitialIndex = null }
        )
    }
}

/**
 * Fullscreen Zoomable HorizontalPager Screenshot Lightbox Viewer
 */
@Composable
fun ScreenshotLightboxDialog(
    screenshots: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (screenshots.size - 1).coerceAtLeast(0)),
        pageCount = { screenshots.size }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
        ) {
            // Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 3.5f)
                                if (scale > 1f) {
                                    val maxOffset = 500f * (scale - 1f)
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                                        y = (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = screenshots[page],
                        contentDescription = "Zoomable screenshot ${page + 1}",
                        modifier = Modifier
                            .fillMaxSize(0.9f)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Top Header: Counter & Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val counterText = remember(pagerState.currentPage, screenshots.size, currentLanguage) {
                    val tpl = LocalizedStrings.get("detail_lightbox_counter", currentLanguage)
                    try { String.format(tpl, pagerState.currentPage + 1, screenshots.size) } catch (_: Exception) {
                        "${pagerState.currentPage + 1} / ${screenshots.size}"
                    }
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape
                ) {
                    Text(
                        text = counterText,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Lightbox",
                        tint = Color.White
                    )
                }
            }

            // Bottom hint
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = LocalizedStrings.get("detail_lightbox_hint", currentLanguage),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                )
            }
        }
    }
}

private fun getSampleScreenshotsForApp(app: ModApp): List<String> {
    return when (app.category.lowercase()) {
        "games" -> listOf(
            "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800&auto=format&fit=crop&q=80"
        )
        "tools" -> listOf(
            "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80"
        )
        "media" -> listOf(
            "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800&auto=format&fit=crop&q=80"
        )
        else -> listOf(
            "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&auto=format&fit=crop&q=80"
        )
    }
}
