package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadTask
import com.example.data.model.ModApp
import com.example.ui.components.CupertinoPullToRefreshBox
import com.example.ui.components.CupertinoSearchField
import com.example.ui.components.CupertinoSegmentedControl
import com.example.ui.components.ModAppCard
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalizedStrings
import com.example.util.AppInstallState
import com.example.util.InstalledAppDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ReorderPillShape = RoundedCornerShape(12.dp)
private val ReorderHandleShape = RoundedCornerShape(8.dp)

@Composable
fun AppsScreen(
    apps: List<ModApp>,
    bookmarkedIds: Set<String>,
    downloadTasks: List<DownloadTask> = emptyList(),
    isRefreshing: Boolean = false,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    isOnline: Boolean = true,
    isAdmin: Boolean = false,
    onRefresh: () -> Unit = {},
    onAppSelected: (ModApp) -> Unit,
    onDownloadApp: (ModApp) -> Unit,
    onInstallDownloadedApp: (DownloadTask) -> Unit = {},
    onReplaceUninstallApp: (ModApp) -> Unit = {},
    onPauseDownload: (String) -> Unit = {},
    onResumeDownload: (String) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onBookmarkToggle: (ModApp) -> Unit,
    onReorderApps: (Int, Int) -> Unit = { _, _ -> },
    onIsScrollingChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val packageStateVersion by InstalledAppDetector.packageStateVersion.collectAsState()

    val publicApps = remember(apps) {
        apps.filter { it.isPublished && it.downloadUrl.isNotBlank() }
    }

    // Offload package inspection for UPDATE_AVAILABLE mods to background IO thread
    val updateAvailableApps by produceState(
        initialValue = emptyList<ModApp>(),
        publicApps,
        packageStateVersion
    ) {
        value = withContext(Dispatchers.IO) {
            publicApps.filter { app ->
                app.packageName.isNotBlank() &&
                    InstalledAppDetector.checkState(
                        context = context,
                        packageName = app.packageName,
                        storeVersionCode = app.versionCode,
                        expectedSignatureSha256 = app.expectedSignatureSha256
                    ) == AppInstallState.UPDATE_AVAILABLE
            }
        }
    }

    val rawCategories = remember(updateAvailableApps.isNotEmpty()) {
        if (updateAvailableApps.isNotEmpty()) {
            listOf("All", "Apps", "Games", "Updates")
        } else {
            listOf("All", "Apps", "Games")
        }
    }
    val categoryKeys = remember(updateAvailableApps.isNotEmpty()) {
        if (updateAvailableApps.isNotEmpty()) {
            listOf("cat_all", "cat_apps", "cat_games", "cat_updates")
        } else {
            listOf("cat_all", "cat_apps", "cat_games")
        }
    }
    val localizedCategories = remember(currentLanguage, categoryKeys, updateAvailableApps.size) {
        categoryKeys.map { key ->
            val label = LocalizedStrings.get(key, currentLanguage)
            if (key == "cat_updates" && updateAvailableApps.isNotEmpty()) {
                "$label (${updateAvailableApps.size})"
            } else {
                label
            }
        }
    }

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val safeSelectedCategoryIndex = selectedCategoryIndex.coerceIn(0, (rawCategories.size - 1).coerceAtLeast(0))
    LaunchedEffect(rawCategories.size) {
        if (selectedCategoryIndex >= rawCategories.size) {
            selectedCategoryIndex = 0
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isReorderMode by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    // Smoothness: notify container when scrolling to pause background animations
    LaunchedEffect(lazyListState.isScrollInProgress) {
        onIsScrollingChanged(lazyListState.isScrollInProgress)
    }

    // Offload search and filtering from Main thread to Dispatchers.Default
    val filteredApps by produceState(
        initialValue = publicApps,
        publicApps,
        updateAvailableApps,
        rawCategories,
        safeSelectedCategoryIndex,
        searchQuery
    ) {
        value = withContext(Dispatchers.Default) {
            val selectedRawCat = rawCategories.getOrElse(safeSelectedCategoryIndex) { "All" }
            var list = when (selectedRawCat) {
                "Updates" -> updateAvailableApps
                "Games" -> publicApps.filter {
                    it.category.equals("Games", ignoreCase = true) ||
                        it.category.equals("Emulators", ignoreCase = true)
                }
                "Apps" -> publicApps.filter {
                    !it.category.equals("Games", ignoreCase = true) &&
                        !it.category.equals("Emulators", ignoreCase = true)
                }
                else -> publicApps
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.name.lowercase().contains(q) ||
                        it.description.lowercase().contains(q) ||
                        it.author.lowercase().contains(q)
                }
            }
            list
        }
    }

    val searchPlaceholder = remember(currentLanguage, localizedCategories, safeSelectedCategoryIndex) {
        val prefix = LocalizedStrings.get("apps_search_prefix", currentLanguage)
        val cat = localizedCategories.getOrElse(safeSelectedCategoryIndex) { "Apps" }
        try { String.format(prefix, cat) } catch (e: Exception) { "Search in $cat..." }
    }

    val taskByAppId = remember(downloadTasks) {
        downloadTasks.associateBy { it.appId }
    }

    val categoryHeader = remember(localizedCategories, safeSelectedCategoryIndex, filteredApps.size, currentLanguage) {
        val cat = localizedCategories.getOrElse(safeSelectedCategoryIndex) { "All" }
        val label = if (currentLanguage.isRtl) cat else cat.uppercase()
        "$label (${filteredApps.size})"
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
                .testTag("apps_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
        ) {
            // iOS Large Title Header
            item(key = "header_title", contentType = "header") {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = LocalizedStrings.get("apps_kicker", currentLanguage),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = if (currentLanguage.isRtl) 0.sp else 0.8.sp
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LocalizedStrings.get("apps_title", currentLanguage),
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp
                            )
                        )

                        if (!isOnline) {
                            Spacer(modifier = Modifier.width(8.dp))
                            com.example.ui.components.OfflineHeaderBadge(
                                isOnline = false,
                                currentLanguage = currentLanguage
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Capsule
                    CupertinoSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = searchPlaceholder
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Segmented Control
                    CupertinoSegmentedControl(
                        items = localizedCategories,
                        selectedIndex = safeSelectedCategoryIndex,
                        onItemSelected = { selectedCategoryIndex = it }
                    )
                }
            }

            // Highlighted "Updates Available (N)" Banner & Quick Update Cards
            val selectedRawCategory = rawCategories.getOrElse(safeSelectedCategoryIndex) { "All" }
            if (updateAvailableApps.isNotEmpty() && selectedRawCategory != "Updates" && searchQuery.isBlank()) {
                item(key = "updates_available_banner", contentType = "updates_banner") {
                    val bannerTitle = try {
                        String.format(
                            LocalizedStrings.get("updates_available_section", currentLanguage),
                            updateAvailableApps.size
                        )
                    } catch (_: Exception) {
                        "Updates Available (${updateAvailableApps.size})"
                    }
                    val updatesTabIndex = rawCategories.indexOf("Updates")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .testTag("updates_available_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0A84FF).copy(alpha = 0.18f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdateAlt,
                                            contentDescription = null,
                                            tint = Color(0xFF0A84FF),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = bannerTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = LocalizedStrings.get("updates_available_subtitle", currentLanguage),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                            if (updatesTabIndex >= 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0A84FF),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { selectedCategoryIndex = updatesTabIndex }
                                        .testTag("updates_view_all_pill")
                                ) {
                                    Text(
                                        text = LocalizedStrings.get("updates_view_all", currentLanguage),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                items(
                    items = updateAvailableApps.take(3),
                    key = { app -> "update_avail_${app.id}" },
                    contentType = { "mod_app_card" }
                ) { app ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        ModAppCard(
                            app = app,
                            isBookmarked = bookmarkedIds.contains(app.id),
                            onAppClick = { onAppSelected(app) },
                            onDownloadClick = { onDownloadApp(app) },
                            onBookmarkToggle = { onBookmarkToggle(app) },
                            currentLanguage = currentLanguage,
                            downloadTask = taskByAppId[app.id],
                            onInstallDownloadedClick = onInstallDownloadedApp,
                            onReplaceUninstallClick = { onReplaceUninstallApp(app) },
                            onPauseDownloadClick = onPauseDownload,
                            onResumeDownloadClick = onResumeDownload,
                            onCancelDownloadClick = onCancelDownload
                        )
                    }
                }

                item(key = "updates_divider_spacer", contentType = "spacer") {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Category Count + Edit Reorder Button
            item(key = "header_category_count", contentType = "category_count") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = categoryHeader,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = if (currentLanguage.isRtl) 0.sp else 0.5.sp,
                            fontSize = 12.sp
                        )
                    )

                    if (isAdmin) {
                        // Three Lines Reorder Button Toggle (Admins Only)
                        Surface(
                            shape = ReorderPillShape,
                            color = if (isReorderMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clip(ReorderPillShape)
                                .clickable { isReorderMode = !isReorderMode }
                                .testTag("toggle_reorder_mode")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.DragHandle,
                                    contentDescription = if (isReorderMode) "Done" else "Sort Apps",
                                    tint = if (isReorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isReorderMode) "Done" else "Edit Order",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isReorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (publicApps.isEmpty() && isRefreshing) {
                items(
                    count = 5,
                    key = { "skeleton_$it" },
                    contentType = { "skeleton" }
                ) {
                    Box(modifier = Modifier.padding(vertical = 5.dp)) {
                        com.example.ui.components.CupertinoModCardSkeleton()
                    }
                }
            } else if (filteredApps.isEmpty()) {
                item(key = "empty_search_state", contentType = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = LocalizedStrings.get("search_no_results", currentLanguage),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = filteredApps,
                    key = { _, app -> app.id },
                    contentType = { _, _ -> "mod_app_card" }
                ) { index, app ->
                    var dragAccumulator by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ModAppCard(
                                    app = app,
                                    isBookmarked = bookmarkedIds.contains(app.id),
                                    onAppClick = { onAppSelected(app) },
                                    onDownloadClick = { onDownloadApp(app) },
                                    onBookmarkToggle = { onBookmarkToggle(app) },
                                    currentLanguage = currentLanguage,
                                    downloadTask = taskByAppId[app.id],
                                    onInstallDownloadedClick = onInstallDownloadedApp,
                                    onReplaceUninstallClick = { onReplaceUninstallApp(app) },
                                    onPauseDownloadClick = onPauseDownload,
                                    onResumeDownloadClick = onResumeDownload,
                                    onCancelDownloadClick = onCancelDownload
                                )
                            }

                            // Three Lines Drag Handle & Up/Down Nudge Controls in Reorder Mode (Admins Only)
                            AnimatedVisibility(
                                visible = isAdmin && isReorderMode,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 6.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (index > 0) onReorderApps(index, index - 1)
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Move Up",
                                                tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (index < filteredApps.size - 1) onReorderApps(index, index + 1)
                                            },
                                            enabled = index < filteredApps.size - 1,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Move Down",
                                                tint = if (index < filteredApps.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Three Lines Under Each Other (Drag Handle)
                                    Surface(
                                        shape = ReorderHandleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .padding(start = 2.dp)
                                            .size(width = 36.dp, height = 54.dp)
                                            .pointerInput(app.id, index) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { dragAccumulator = 0f },
                                                    onDragEnd = { dragAccumulator = 0f },
                                                    onDragCancel = { dragAccumulator = 0f },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragAccumulator += dragAmount.y
                                                        val threshold = 90f
                                                        if (dragAccumulator > threshold && index < filteredApps.size - 1) {
                                                            onReorderApps(index, index + 1)
                                                            dragAccumulator = 0f
                                                        } else if (dragAccumulator < -threshold && index > 0) {
                                                            onReorderApps(index, index - 1)
                                                            dragAccumulator = 0f
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = "Drag Reorder Handle",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
