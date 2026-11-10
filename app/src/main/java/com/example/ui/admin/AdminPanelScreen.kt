package com.example.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
                    onDeleteApp = { appToDelete = it }
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
                    onDeleteNews = { newsToDelete = it }
                )
                2 -> StoreConfigView(
                    currentConfig = updateConfig ?: AppUpdateConfig(),
                    isLoading = adminState.isLoading,
                    onSaveConfig = { cfg -> adminViewModel.saveStoreConfig(cfg) }
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
    onDeleteApp: (ModApp) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else {
            val q = searchQuery.trim().lowercase()
            apps.filter { it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp)
    ) {
        item(key = "apps_header") {
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
            items(
                items = filteredApps,
                key = { it.id.ifBlank { it.packageName } }
            ) { app ->
                AdminAppItemCard(
                    app = app,
                    onEdit = { onEditApp(app) },
                    onDelete = { onDeleteApp(app) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AdminAppItemCard(
    app: ModApp,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
    onDeleteNews: (NewsPost) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp)
    ) {
        item(key = "news_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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

                Button(
                    onClick = onCreateNews,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "New Broadcast",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Broadcast", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        items(
            items = newsList,
            key = { it.id.ifBlank { it.title } }
        ) { post ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        )
                        Text(
                            text = post.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "Type: ${post.type.name} • Tag: ${post.tag}",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                        )
                    }

                    Row {
                        IconButton(onClick = { onDeleteNews(post) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = iOSSystemRed, modifier = Modifier.size(18.dp))
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
    onSaveConfig: (AppUpdateConfig) -> Unit
) {
    val context = LocalContext.current
    var latestVersion by remember(currentConfig) { mutableStateOf(currentConfig.storeLatestVersion) }
    var latestVersionCode by remember(currentConfig) { mutableStateOf(currentConfig.storeLatestVersionCode.toString()) }
    var mandatory by remember(currentConfig) { mutableStateOf(currentConfig.mandatory) }
    var showDialog by remember(currentConfig) { mutableStateOf(currentConfig.showDialog) }
    var downloadUrl by remember(currentConfig) { mutableStateOf(currentConfig.downloadUrl) }
    var announcementMessage by remember(currentConfig) { mutableStateOf(currentConfig.announcementMessage) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
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
