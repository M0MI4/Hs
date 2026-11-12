package com.example.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppUpdateConfig
import com.example.data.model.ModApp
import com.example.data.model.NewsPost
import com.example.data.model.NewsType
import com.example.ui.components.CupertinoSearchField
import com.example.ui.components.CupertinoSegmentedControl
import com.example.ui.theme.VerifiedBadgeGreen
import com.example.ui.theme.iOSSystemBlue
import com.example.ui.theme.iOSSystemRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    adminViewModel: AdminViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminState by adminViewModel.adminState.collectAsState()
    val apps by adminViewModel.catalogRepository.apps.collectAsState()
    val newsList by adminViewModel.newsRepository.news.collectAsState()
    val updateConfig by adminViewModel.updateRepository.updateConfig.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabLabels = listOf("Apps Catalog", "News Broadcasts", "Store Updates")

    var showAppEditorSheet by remember { mutableStateOf(false) }
    var editingApp by remember { mutableStateOf<ModApp?>(null) }
    var appToDelete by remember { mutableStateOf<ModApp?>(null) }

    var showNewsEditorSheet by remember { mutableStateOf(false) }
    var editingNews by remember { mutableStateOf<NewsPost?>(null) }
    var newsToDelete by remember { mutableStateOf<NewsPost?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("admin_panel_screen")
    ) {
        // Top Cupertino Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Text(
                        text = "HsStore Admin Studio",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = adminState.currentAdminEmail ?: "Cloud Firestore Console",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // Sign out button
            OutlinedButton(
                onClick = {
                    adminViewModel.logoutAdmin()
                    onNavigateBack()
                },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, iOSSystemRed.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = iOSSystemRed),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Sign Out",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sign Out", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Notification Banner
        AnimatedVisibility(visible = adminState.errorMessage != null || adminState.successMessage != null) {
            Surface(
                color = if (adminState.errorMessage != null) iOSSystemRed.copy(alpha = 0.15f) else VerifiedBadgeGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (adminState.errorMessage != null) iOSSystemRed else VerifiedBadgeGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (adminState.errorMessage != null) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (adminState.errorMessage != null) iOSSystemRed else VerifiedBadgeGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = adminState.errorMessage ?: adminState.successMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { adminViewModel.clearMessages() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Segmented Control Tabs
        CupertinoSegmentedControl(
            items = tabLabels,
            selectedIndex = selectedTab,
            onItemSelected = { selectedTab = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Main Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AppsManagementView(
                    apps = apps,
                    onEditApp = {
                        editingApp = it
                        showAppEditorSheet = true
                    },
                    onCreateApp = {
                        editingApp = null
                        showAppEditorSheet = true
                    },
                    onDeleteApp = { appToDelete = it },
                    onReorderApps = { from, to -> adminViewModel.reorderApps(from, to) }
                )
                1 -> NewsManagementView(
                    newsList = newsList,
                    onEditNews = {
                        editingNews = it
                        showNewsEditorSheet = true
                    },
                    onCreateNews = {
                        editingNews = null
                        showNewsEditorSheet = true
                    },
                    onDeleteNews = { newsToDelete = it },
                    onReorderNews = { from, to -> adminViewModel.reorderNews(from, to) }
                )
                2 -> StoreConfigView(
                    currentConfig = updateConfig ?: AppUpdateConfig(),
                    isLoading = adminState.isLoading,
                    onSaveConfig = { cfg -> adminViewModel.saveStoreConfig(cfg) },
                    onBroadcastNotification = { title, msg, link ->
                        adminViewModel.broadcastNotification(title, msg, link)
                    },
                    onClearNotification = {
                        adminViewModel.clearActiveNotification()
                    }
                )
            }
        }
    }

    // App Editor Modal Sheet
    if (showAppEditorSheet) {
        AppEditorBottomSheet(
            app = editingApp,
            adminViewModel = adminViewModel,
            onDismiss = { showAppEditorSheet = false },
            onSaved = { showAppEditorSheet = false }
        )
    }

    // News Editor Modal Sheet
    if (showNewsEditorSheet) {
        NewsEditorBottomSheet(
            news = editingNews,
            adminViewModel = adminViewModel,
            onDismiss = { showNewsEditorSheet = false },
            onSaved = { showNewsEditorSheet = false }
        )
    }

    // App Delete Confirmation Dialog
    appToDelete?.let { app ->
        AlertDialog(
            onDismissRequest = { appToDelete = null },
            title = { Text("Delete App From Store?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete '${app.name}' from Cloud Firestore? This will remove it for all users.") },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteApp(app.id, app.name)
                        appToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSSystemRed)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { appToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // News Delete Confirmation Dialog
    newsToDelete?.let { news ->
        AlertDialog(
            onDismissRequest = { newsToDelete = null },
            title = { Text("Delete Announcement?", fontWeight = FontWeight.Bold) },
            text = { Text("Delete '${news.title}' from Cloud Firestore news feed?") },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteNews(news.id)
                        newsToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSSystemRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { newsToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// 1. APPS MANAGEMENT VIEW
// -------------------------------------------------------------
@Composable
private fun AppsManagementView(
    apps: List<ModApp>,
    onEditApp: (ModApp) -> Unit,
    onCreateApp: () -> Unit,
    onDeleteApp: (ModApp) -> Unit,
    onReorderApps: (Int, Int) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var isOrderEditing by remember { mutableStateOf(false) }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else {
            val q = searchQuery.trim().lowercase()
            apps.filter { it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = true,
        flingBehavior = androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp)
    ) {
        item(key = "apps_header", contentType = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CATALOG PACKAGES (${apps.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { isOrderEditing = !isOrderEditing },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isOrderEditing) VerifiedBadgeGreen else MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, if (isOrderEditing) VerifiedBadgeGreen else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isOrderEditing) Icons.Default.CheckCircle else Icons.Default.SwapVert,
                            contentDescription = "Edit Order",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOrderEditing) "Done" else "Edit Order",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onCreateApp,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New App",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New App", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            CupertinoSearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search ${apps.size} store apps...",
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (filteredApps.isEmpty()) {
            item(key = "empty_apps") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No matching apps found in Firestore catalog.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onCreateApp) {
                            Text("+ Add First Mod Package")
                        }
                    }
                }
            }
        } else {
            itemsIndexed(
                items = filteredApps,
                key = { _, app -> app.id.ifBlank { app.packageName } },
                contentType = { _, _ -> "admin_app_card" }
            ) { index, app ->
                AdminAppItemCard(
                    app = app,
                    index = index,
                    totalCount = filteredApps.size,
                    isOrderEditing = isOrderEditing && searchQuery.isBlank(),
                    onEdit = { onEditApp(app) },
                    onDelete = { onDeleteApp(app) },
                    onMoveUp = { if (index > 0) onReorderApps(index, index - 1) },
                    onMoveDown = { if (index < filteredApps.size - 1) onReorderApps(index, index + 1) },
                    onReorder = onReorderApps
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AdminAppItemCard(
    app: ModApp,
    index: Int,
    totalCount: Int,
    isOrderEditing: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isOrderEditing) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Three-line drag handle and nudge arrows when ordering is active
            if (isOrderEditing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .pointerInput(index, totalCount) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { accumulatedDragY = 0f },
                                    onDragEnd = { accumulatedDragY = 0f },
                                    onDragCancel = { accumulatedDragY = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        accumulatedDragY += dragAmount.y
                                        val threshold = 70f
                                        if (accumulatedDragY > threshold && index < totalCount - 1) {
                                            onReorder(index, index + 1)
                                            accumulatedDragY = 0f
                                        } else if (accumulatedDragY < -threshold && index > 0) {
                                            onReorder(index, index - 1)
                                            accumulatedDragY = 0f
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Hold and drag up or down",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = index > 0,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move Up",
                                tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onMoveDown,
                            enabled = index < totalCount - 1,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move Down",
                                tint = if (index < totalCount - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                AsyncImage(
                    model = app.iconUrl.ifBlank { com.example.R.drawable.ic_launcher_foreground },
                    contentDescription = app.name,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (app.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Verified",
                            tint = VerifiedBadgeGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = "${app.packageName} • v${app.version} (${app.sizeDisplay})",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )

                Text(
                    text = "Category: ${app.category} • Downloads: ${app.downloadsCount}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                )
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit App",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete App",
                        tint = iOSSystemRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. APP EDITOR BOTTOM SHEET WITH LOCAL APK PARSER
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppEditorBottomSheet(
    app: ModApp?,
    adminViewModel: AdminViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditMode = app != null

    var name by remember { mutableStateOf(app?.name ?: "") }
    var packageName by remember { mutableStateOf(app?.packageName ?: "") }
    var category by remember { mutableStateOf(app?.category ?: "Apps") }
    var version by remember { mutableStateOf(app?.version ?: "1.0.0") }
    var versionCode by remember { mutableStateOf(app?.versionCode?.toString() ?: "1") }
    var sizeDisplay by remember { mutableStateOf(app?.sizeDisplay ?: "25.0 MB") }
    var downloadUrl by remember { mutableStateOf(app?.downloadUrl ?: "") }
    var iconUrl by remember { mutableStateOf(app?.iconUrl ?: "") }
    var sha256 by remember { mutableStateOf(app?.sha256 ?: "") }
    var description by remember { mutableStateOf(app?.description ?: "") }
    var modFeaturesText by remember { mutableStateOf(app?.modFeatures?.joinToString("\n") ?: "") }
    var author by remember { mutableStateOf(app?.author ?: "Hama Sade Team") }
    var isVerified by remember { mutableStateOf(app?.isVerified ?: true) }
    var isParsingApk by remember { mutableStateOf(false) }
    var screenshotsList by remember(app) { mutableStateOf(app?.screenshots ?: emptyList()) }
    var screenshotInputUrl by remember { mutableStateOf("") }

    val categories = listOf("Apps", "Games", "Tools", "Emulators", "Media", "Tweaks")

    // Android APK File Picker
    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isParsingApk = true
            scope.launch {
                val parsed = adminViewModel.parseLocalApkFromUri(context, uri)
                isParsingApk = false
                if (parsed != null) {
                    name = parsed.title
                    packageName = parsed.packageName
                    version = parsed.version
                    versionCode = parsed.versionCode.toString()
                    sizeDisplay = parsed.sizeDisplay
                    sha256 = parsed.sha256
                    Toast.makeText(context, "APK metadata parsed successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not parse APK package information.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Screenshot Photo Picker from Device Gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            screenshotsList = screenshotsList + uri.toString()
            Toast.makeText(context, "Screenshot added from gallery!", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isEditMode) "Edit App Package" else "Create New App Package",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // APK Auto-Parser Button
            Button(
                onClick = { apkPickerLauncher.launch("*/*") },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isParsingApk) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extracting APK Metadata & SHA-256...", color = MaterialTheme.colorScheme.onSecondaryContainer)
                } else {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Pick APK",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto-Parse from Local .APK File", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("App Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Package Name (e.g. com.app.mod) *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.take(3).forEach { cat ->
                            FilterChip(
                                selected = category.equals(cat, ignoreCase = true),
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.drop(3).forEach { cat ->
                            FilterChip(
                                selected = category.equals(cat, ignoreCase = true),
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = version,
                            onValueChange = { version = it },
                            label = { Text("Version") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = versionCode,
                            onValueChange = { versionCode = it },
                            label = { Text("Version Code") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sizeDisplay,
                            onValueChange = { sizeDisplay = it },
                            label = { Text("Size (MB)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = downloadUrl,
                        onValueChange = { downloadUrl = it },
                        label = { Text("Direct Download APK URL *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = iconUrl,
                        onValueChange = { iconUrl = it },
                        label = { Text("App Icon URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Screenshots Editor Item
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "App Screenshots (${screenshotsList.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gallery Pick", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = screenshotInputUrl,
                                onValueChange = { screenshotInputUrl = it },
                                label = { Text("Add Screenshot Image URL") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (screenshotInputUrl.isNotBlank()) {
                                        screenshotsList = screenshotsList + screenshotInputUrl.trim()
                                        screenshotInputUrl = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Text("+ Add")
                            }
                        }

                        if (screenshotsList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(screenshotsList) { sIndex, sUrl ->
                                    Box(
                                        modifier = Modifier
                                            .size(width = 84.dp, height = 130.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(sUrl)
                                                .crossfade(false)
                                                .allowHardware(true)
                                                .build(),
                                            contentDescription = "Screenshot ${sIndex + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        IconButton(
                                            onClick = {
                                                screenshotsList = screenshotsList.toMutableList().also { it.removeAt(sIndex) }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.7f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove screenshot",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = sha256,
                        onValueChange = { sha256 = it },
                        label = { Text("SHA-256 Checksum Signature") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("App Description") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = modFeaturesText,
                        onValueChange = { modFeaturesText = it },
                        label = { Text("Mod Features (one bullet per line)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Verified Developer Sandbox", fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isVerified,
                            onCheckedChange = { isVerified = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VerifiedBadgeGreen)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (name.isBlank() || packageName.isBlank()) {
                        Toast.makeText(context, "Please provide title and package name.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val features = modFeaturesText.lines().map { it.trim() }.filter { it.isNotBlank() }
                    val vCode = versionCode.toIntOrNull() ?: 1
                    val newApp = (app ?: ModApp()).copy(
                        name = name.trim(),
                        packageName = packageName.trim(),
                        category = category,
                        version = version.trim(),
                        versionCode = vCode,
                        sizeDisplay = sizeDisplay.trim(),
                        downloadUrl = downloadUrl.trim(),
                        iconUrl = iconUrl.trim(),
                        screenshots = screenshotsList,
                        sha256 = sha256.trim(),
                        description = description.trim(),
                        modFeatures = features,
                        author = author.trim(),
                        isVerified = isVerified,
                        updatedDate = System.currentTimeMillis().toString()
                    )
                    adminViewModel.saveApp(newApp) {
                        onSaved()
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditMode) "Save Changes to Firestore" else "Publish App to Store", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------
// 3. NEWS & BROADCASTS VIEW
// -------------------------------------------------------------
@Composable
private fun NewsManagementView(
    newsList: List<NewsPost>,
    onEditNews: (NewsPost) -> Unit,
    onCreateNews: () -> Unit,
    onDeleteNews: (NewsPost) -> Unit,
    onReorderNews: (Int, Int) -> Unit = { _, _ -> }
) {
    var isOrderEditing by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = true,
        flingBehavior = androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp)
    ) {
        item(key = "news_header", contentType = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ACTIVE STORIES & ANNOUNCEMENTS (${newsList.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )

                    OutlinedButton(
                        onClick = { isOrderEditing = !isOrderEditing },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isOrderEditing) VerifiedBadgeGreen else MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, if (isOrderEditing) VerifiedBadgeGreen else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isOrderEditing) Icons.Default.CheckCircle else Icons.Default.SwapVert,
                            contentDescription = "Edit Order",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOrderEditing) "Done" else "Edit Order",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Prominently placed, centered Blue Broadcast Story Button
                Button(
                    onClick = onCreateNews,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Create New Broadcast Story",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Create New Broadcast Story",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (newsList.isEmpty()) {
            item(key = "empty_news") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No news or announcements currently in Firestore.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onCreateNews) {
                            Text("+ Create First Broadcast")
                        }
                    }
                }
            }
        } else {
            itemsIndexed(
                items = newsList,
                key = { _, post -> post.id.ifBlank { post.title } },
                contentType = { _, _ -> "admin_news_card" }
            ) { index, post ->
                var accumulatedDragY by remember { mutableFloatStateOf(0f) }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (isOrderEditing) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOrderEditing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .pointerInput(index, newsList.size) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { accumulatedDragY = 0f },
                                                onDragEnd = { accumulatedDragY = 0f },
                                                onDragCancel = { accumulatedDragY = 0f },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    accumulatedDragY += dragAmount.y
                                                    val threshold = 70f
                                                    if (accumulatedDragY > threshold && index < newsList.size - 1) {
                                                        onReorderNews(index, index + 1)
                                                        accumulatedDragY = 0f
                                                    } else if (accumulatedDragY < -threshold && index > 0) {
                                                        onReorderNews(index, index - 1)
                                                        accumulatedDragY = 0f
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Hold and drag to reorder",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = { if (index > 0) onReorderNews(index, index - 1) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move Up",
                                            tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { if (index < newsList.size - 1) onReorderNews(index, index + 1) },
                                        enabled = index < newsList.size - 1,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move Down",
                                            tint = if (index < newsList.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = post.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            )
                            if (post.subtitle.isNotBlank()) {
                                Text(
                                    text = post.subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            Text(
                                text = "Type: ${post.type.name} • Tag: ${post.tag} • ${post.author}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                            )
                        }

                        Row {
                            IconButton(onClick = { onEditNews(post) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onDeleteNews(post) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = iOSSystemRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. STORE FORCE UPDATE & CONFIG VIEW
// -------------------------------------------------------------
@Composable
private fun StoreConfigView(
    currentConfig: AppUpdateConfig,
    isLoading: Boolean,
    onSaveConfig: (AppUpdateConfig) -> Unit,
    onBroadcastNotification: (String, String, String) -> Unit = { _, _, _ -> },
    onClearNotification: () -> Unit = {}
) {
    val context = LocalContext.current
    var latestVersion by remember(currentConfig) { mutableStateOf(currentConfig.storeLatestVersion) }
    var latestVersionCode by remember(currentConfig) { mutableStateOf(currentConfig.storeLatestVersionCode.toString()) }
    var mandatory by remember(currentConfig) { mutableStateOf(currentConfig.mandatory) }
    var showDialog by remember(currentConfig) { mutableStateOf(currentConfig.showDialog) }
    var downloadUrl by remember(currentConfig) { mutableStateOf(currentConfig.downloadUrl) }
    var announcementMessage by remember(currentConfig) { mutableStateOf(currentConfig.announcementMessage) }

    var notifTitle by remember { mutableStateOf("") }
    var notifMessage by remember { mutableStateOf("") }
    var notifActionUrl by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = true,
        flingBehavior = androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp)
    ) {
        // Broadcast User Notification Section
        item(key = "broadcast_notification", contentType = "card") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, iOSSystemBlue.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = iOSSystemBlue.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = iOSSystemBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Broadcast User Notification",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Sends push & in-app alerts to all users",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = notifTitle,
                        onValueChange = { notifTitle = it },
                        label = { Text("Notification Title *") },
                        placeholder = { Text("e.g. 🎉 New Mod Released: Subway Surfers v3.2") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notifMessage,
                        onValueChange = { notifMessage = it },
                        label = { Text("Notification Message / Body *") },
                        placeholder = { Text("Detailed release notes or important announcement...") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notifActionUrl,
                        onValueChange = { notifActionUrl = it },
                        label = { Text("Action Link / Package (optional)") },
                        placeholder = { Text("https://... or app package name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (notifTitle.isBlank() || notifMessage.isBlank()) {
                                Toast.makeText(context, "Please enter both notification title and message.", Toast.LENGTH_SHORT).show()
                            } else {
                                onBroadcastNotification(notifTitle, notifMessage, notifActionUrl)
                                Toast.makeText(context, "Broadcast sent to all users!", Toast.LENGTH_SHORT).show()
                                notifTitle = ""
                                notifMessage = ""
                                notifActionUrl = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Broadcast Notification to Users", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            onClearNotification()
                            Toast.makeText(context, "Active notification cleared.", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = iOSSystemRed),
                        border = BorderStroke(1.dp, iOSSystemRed.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear / Dismiss Active Notification", fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Force Update Section
        item(key = "force_update", contentType = "card") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Global Force Update & Version Control",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Published to Firestore document /config/store. Active users will instantly see update alerts.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = latestVersion,
                        onValueChange = { latestVersion = it },
                        label = { Text("Target App Version (e.g. 1.3.1)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = latestVersionCode,
                        onValueChange = { latestVersionCode = it },
                        label = { Text("Target Version Code (e.g. 31)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = downloadUrl,
                        onValueChange = { downloadUrl = it },
                        label = { Text("Update APK Download URL") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = announcementMessage,
                        onValueChange = { announcementMessage = it },
                        label = { Text("Announcement Message in Dialog") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Show Update Popup to Users", fontWeight = FontWeight.Medium)
                        Switch(checked = showDialog, onCheckedChange = { showDialog = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mandatory Force Update", fontWeight = FontWeight.Medium)
                        Switch(checked = mandatory, onCheckedChange = { mandatory = it })
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val vCode = latestVersionCode.toIntOrNull() ?: 1
                            val newCfg = currentConfig.copy(
                                storeLatestVersion = latestVersion.trim(),
                                storeLatestVersionCode = vCode,
                                mandatory = mandatory,
                                showDialog = showDialog,
                                downloadUrl = downloadUrl.trim(),
                                announcementMessage = announcementMessage.trim()
                            )
                            onSaveConfig(newCfg)
                            Toast.makeText(context, "Store configuration updated!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Broadcast Config to All Devices", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. NEWS EDITOR BOTTOM SHEET
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsEditorBottomSheet(
    news: NewsPost?,
    adminViewModel: AdminViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val isEditMode = news != null

    var title by remember { mutableStateOf(news?.title ?: "") }
    var subtitle by remember { mutableStateOf(news?.subtitle ?: "") }
    var tag by remember { mutableStateOf(news?.tag ?: "Update") }
    var selectedType by remember { mutableStateOf(news?.type ?: NewsType.TEXT_ONLY) }
    var bannerUrl by remember { mutableStateOf(news?.bannerUrl ?: "") }
    var author by remember { mutableStateOf(news?.author ?: "HsStore Editorial") }
    var authorRole by remember { mutableStateOf(news?.authorRole ?: "Curator") }
    var readTime by remember { mutableStateOf(news?.readTime ?: "2 min read") }
    var content by remember { mutableStateOf(news?.content ?: "") }
    var bulletPointsText by remember { mutableStateOf(news?.bulletPoints?.joinToString("\n") ?: "") }
    var linkedAppName by remember { mutableStateOf(news?.linkedAppName ?: "") }
    var isPinned by remember { mutableStateOf(news?.isPinned ?: false) }
    var isBreaking by remember { mutableStateOf(news?.isBreaking ?: false) }

    val tags = listOf("Update", "Feature", "Notice", "Event", "Security", "Patch")
    val newsTypes = listOf(NewsType.TEXT_ONLY, NewsType.BANNER, NewsType.PATCH_NOTES, NewsType.SPOTLIGHT)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isEditMode) "Edit Announcement" else "Broadcast New Story",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Announcement Headline *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Subheading / Kicker") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Story Type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        newsTypes.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Text("Category Tag", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.take(3).forEach { t ->
                            FilterChip(
                                selected = tag.equals(t, ignoreCase = true),
                                onClick = { tag = t },
                                label = { Text(t, fontSize = 12.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.drop(3).forEach { t ->
                            FilterChip(
                                selected = tag.equals(t, ignoreCase = true),
                                onClick = { tag = t },
                                label = { Text(t, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                if (selectedType == NewsType.BANNER || selectedType == NewsType.SPOTLIGHT) {
                    item {
                        OutlinedTextField(
                            value = bannerUrl,
                            onValueChange = { bannerUrl = it },
                            label = { Text("Banner Image URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = author,
                            onValueChange = { author = it },
                            label = { Text("Author") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = readTime,
                            onValueChange = { readTime = it },
                            label = { Text("Read Time") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Full Article Content") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = bulletPointsText,
                        onValueChange = { bulletPointsText = it },
                        label = { Text("Highlights / Bullet Points (one per line)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = linkedAppName,
                        onValueChange = { linkedAppName = it },
                        label = { Text("Linked App Package Name (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pin Announcement to Top", fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isPinned,
                            onCheckedChange = { isPinned = it }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mark as Breaking News", fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isBreaking,
                            onCheckedChange = { isBreaking = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = iOSSystemRed)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Please enter an announcement title.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val bullets = bulletPointsText.lines().map { it.trim() }.filter { it.isNotBlank() }
                    val dateFormatted = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(java.util.Date())
                    val newPost = (news ?: NewsPost()).copy(
                        title = title.trim(),
                        subtitle = subtitle.trim(),
                        tag = tag.trim(),
                        type = selectedType,
                        bannerUrl = bannerUrl.trim(),
                        author = author.trim(),
                        authorRole = authorRole.trim(),
                        readTime = readTime.trim(),
                        date = if (news != null && news.date.isNotBlank()) news.date else dateFormatted,
                        content = content.trim(),
                        bulletPoints = bullets,
                        linkedAppName = if (linkedAppName.isNotBlank()) linkedAppName.trim() else null,
                        isPinned = isPinned,
                        isBreaking = isBreaking
                    )
                    adminViewModel.saveNews(newPost) {
                        onSaved()
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditMode) "Save Changes to News" else "Publish Story to Firestore", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
