package com.example

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.data.model.DownloadStatus
import com.example.data.model.ModApp
import com.example.data.repository.CatalogRepository
import com.example.data.repository.DownloadRepository
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.NewsRepository
import com.example.data.repository.RemoteConfigRepository
import com.example.data.repository.RemoteStoreService
import com.example.data.repository.UpdateRepository
import com.example.ui.components.AppDetailSheet
import com.example.ui.components.ParticleCanvas
import com.example.ui.components.ParticleDensity
import com.example.ui.components.UpdateDialog
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.TodayNewsScreen
import com.example.ui.theme.HsStoreTheme
import com.example.ui.theme.ThemePreset
import com.example.ui.theme.iOSDarkSeparator
import com.example.ui.theme.iOSLightSeparator
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalizedStrings

/**
 * iOS App Store 4-tab Navigation Enum
 * TODAY: Apple News & Editorial Announcements (Controlled via Pastebin CTpQXLcc)
 * APPS: Modded Applications Directory with Search & Categories
 * DOWNLOADS: Installations & Active Updates Queue
 * SETTINGS: Appearance, Particles FX, Remote Host Controllers & Support
 */
enum class MainTab(val title: String) {
    TODAY("Today"),
    APPS("Apps"),
    DOWNLOADS("Downloads"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {

    private lateinit var catalogRepo: CatalogRepository
    private lateinit var updateRepo: UpdateRepository
    private lateinit var downloadRepo: DownloadRepository
    private lateinit var remoteRepo: RemoteConfigRepository
    private lateinit var newsRepo: NewsRepository
    private lateinit var firestoreRepo: FirestoreSyncRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val remoteStoreService = RemoteStoreService(applicationContext)
        catalogRepo = CatalogRepository(applicationContext, remoteStoreService)
        updateRepo = UpdateRepository(applicationContext, remoteStoreService)
        downloadRepo = DownloadRepository(applicationContext)
        remoteRepo = RemoteConfigRepository(applicationContext)
        newsRepo = NewsRepository(applicationContext)
        firestoreRepo = FirestoreSyncRepository(applicationContext)
        val networkMonitor = com.example.util.NetworkMonitor(applicationContext)

        setContent {
            val prefs = getSharedPreferences("hsstore_prefs", Context.MODE_PRIVATE)
            var currentLanguage by remember {
                val savedCode = prefs.getString("selected_lang", AppLanguage.ENGLISH.code)
                mutableStateOf(if (savedCode == AppLanguage.KURDISH_SORANI.code) AppLanguage.KURDISH_SORANI else AppLanguage.ENGLISH)
            }

            val isOnline by networkMonitor.isOnline.collectAsState(initial = true)

            var themePreset by remember { mutableStateOf(ThemePreset.APPLE_BLUE) }
            var isDarkTheme by remember { mutableStateOf(true) }
            var particlesEnabled by remember { mutableStateOf(true) }
            var particleDensity by remember { mutableStateOf(ParticleDensity.MEDIUM) }

            HsStoreTheme(preset = themePreset, darkTheme = isDarkTheme) {
                MainAppContent(
                    catalogRepo = catalogRepo,
                    updateRepo = updateRepo,
                    downloadRepo = downloadRepo,
                    newsRepo = newsRepo,
                    firestoreRepo = firestoreRepo,
                    themePreset = themePreset,
                    isDarkTheme = isDarkTheme,
                    particlesEnabled = particlesEnabled,
                    particleDensity = particleDensity,
                    currentLanguage = currentLanguage,
                    isOnline = isOnline,
                    onThemePresetChanged = { themePreset = it },
                    onDarkThemeToggled = { isDarkTheme = it },
                    onParticlesEnabledToggled = { particlesEnabled = it },
                    onParticleDensityChanged = { particleDensity = it },
                    onLanguageChanged = { lang ->
                        currentLanguage = lang
                        prefs.edit().putString("selected_lang", lang.code).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    catalogRepo: CatalogRepository,
    updateRepo: UpdateRepository,
    downloadRepo: DownloadRepository,
    newsRepo: NewsRepository,
    firestoreRepo: FirestoreSyncRepository,
    themePreset: ThemePreset,
    isDarkTheme: Boolean,
    particlesEnabled: Boolean,
    particleDensity: ParticleDensity,
    currentLanguage: AppLanguage,
    isOnline: Boolean,
    onThemePresetChanged: (ThemePreset) -> Unit,
    onDarkThemeToggled: (Boolean) -> Unit,
    onParticlesEnabledToggled: (Boolean) -> Unit,
    onParticleDensityChanged: (ParticleDensity) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var currentTab by remember { mutableStateOf(MainTab.TODAY) }
    var selectedAppForDetail by remember { mutableStateOf<ModApp?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isAuthLoading by remember { mutableStateOf(false) }

    // User auth state
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Reactive states from repositories
    val apps by catalogRepo.apps.collectAsState()
    val isCatalogLoading by catalogRepo.isLoading.collectAsState()
    val catalogStatusMessage by catalogRepo.statusMessage.collectAsState()

    // News states (Pastebin CTpQXLcc)
    val newsList by newsRepo.news.collectAsState()
    val isNewsRefreshing by newsRepo.isRefreshing.collectAsState()
    val newsStatusMessage by newsRepo.statusMessage.collectAsState()
    val currentNewsSource by newsRepo.currentSource.collectAsState()
    val activeNewsUrl by newsRepo.activeNewsUrl.collectAsState()

    val updateConfig by updateRepo.updateConfig.collectAsState()
    val hasUpdate by updateRepo.hasUpdate.collectAsState()

    val downloadTasks by downloadRepo.tasks.collectAsState()

    val currentSource by catalogRepo.currentSource.collectAsState()
    val activeRemoteUrl by catalogRepo.activeRemoteUrl.collectAsState()
    val preferLocalOverrides by catalogRepo.preferLocalOverrides.collectAsState()

    // Local bookmarks cache
    var bookmarkedAppIds by remember { mutableStateOf(setOf<String>()) }

    // On app startup, query both Pastebin endpoints via Retrofit and check store updates
    LaunchedEffect(Unit) {
        catalogRepo.syncWithRetrofit()
        newsRepo.syncRemoteNews()
        updateRepo.checkUpdateAvailability()
    }

    // On user sign-in or start, observe remote bookmarks if signed in
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                firestoreRepo.observeBookmarks().collect { list ->
                    bookmarkedAppIds = list.map { it.appId }.toSet()
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "Failed to sync bookmarks: ${e.message}")
            }
        }
    }

    // Auto-prompt update dialog on start if update is available
    LaunchedEffect(hasUpdate) {
        if (hasUpdate && updateConfig != null) {
            showUpdateDialog = true
        }
    }

    // Custom back navigation handling: return to Today tab
    if (currentTab != MainTab.TODAY) {
        BackHandler {
            currentTab = MainTab.TODAY
        }
    }

    val activeDownloads = downloadTasks.filter {
        it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
    }

    // Kurdish Sorani Layout Mirroring (True Right-to-Left vs Left-to-Right)
    CompositionLocalProvider(
        LocalLayoutDirection provides if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        // Root container with solid background prevents light mode showing black window backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Particle Backdrop with Master Enable/Disable Switch
            ParticleCanvas(
                enabled = particlesEnabled,
                density = particleDensity,
                accentColor = themePreset.primaryColor
            )

            Scaffold(
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        // Quick Active Download Mini-Banner (if downloading and not currently on downloads screen)
                        if (activeDownloads.isNotEmpty() && currentTab != MainTab.DOWNLOADS) {
                            val active = activeDownloads.first()
                            Surface(
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentTab = MainTab.DOWNLOADS }
                                    .padding(horizontal = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${LocalizedStrings.get("downloads_active_section", currentLanguage)}: ${active.appName} (${(active.progress * 100).toInt()}%)",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        Text(
                                            text = LocalizedStrings.get("downloads_action_view", currentLanguage),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { active.progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surface
                                    )
                                }
                            }
                        }

                        // iOS Divider above Tab Bar
                        Divider(
                            color = if (isDarkTheme) iOSDarkSeparator else iOSLightSeparator,
                            thickness = 0.5.dp
                        )

                        // iOS Cupertino Translucent Bottom Tab Bar (Localized tabs & authentic Apple layout)
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("bottom_nav_bar")
                        ) {
                            // 1. TODAY Tab (News, Banners & Editorial Stories)
                            NavigationBarItem(
                                selected = currentTab == MainTab.TODAY,
                                onClick = { currentTab = MainTab.TODAY },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == MainTab.TODAY) Icons.Filled.Today else Icons.Outlined.Today,
                                        contentDescription = LocalizedStrings.get("tab_today", currentLanguage),
                                        modifier = Modifier.size(23.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = LocalizedStrings.get("tab_today", currentLanguage),
                                        fontSize = 10.sp,
                                        fontWeight = if (currentTab == MainTab.TODAY) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = Color(0xFF8E8E93),
                                    unselectedTextColor = Color(0xFF8E8E93),
                                    indicatorColor = Color.Transparent
                                )
                            )

                            // 2. APPS Tab (Modded App Catalog & Categories)
                            NavigationBarItem(
                                selected = currentTab == MainTab.APPS,
                                onClick = { currentTab = MainTab.APPS },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == MainTab.APPS) Icons.Filled.Apps else Icons.Outlined.Apps,
                                        contentDescription = LocalizedStrings.get("tab_apps", currentLanguage),
                                        modifier = Modifier.size(23.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = LocalizedStrings.get("tab_apps", currentLanguage),
                                        fontSize = 10.sp,
                                        fontWeight = if (currentTab == MainTab.APPS) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = Color(0xFF8E8E93),
                                    unselectedTextColor = Color(0xFF8E8E93),
                                    indicatorColor = Color.Transparent
                                )
                            )

                            // 3. DOWNLOADS Tab (Active progress & Completed packages)
                            NavigationBarItem(
                                selected = currentTab == MainTab.DOWNLOADS,
                                onClick = { currentTab = MainTab.DOWNLOADS },
                                icon = {
                                    if (activeDownloads.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = Color.White
                                                ) {
                                                    Text("${activeDownloads.size}", fontSize = 9.sp)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (currentTab == MainTab.DOWNLOADS) Icons.Filled.Download else Icons.Outlined.Download,
                                                contentDescription = LocalizedStrings.get("tab_downloads", currentLanguage),
                                                modifier = Modifier.size(23.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (currentTab == MainTab.DOWNLOADS) Icons.Filled.Download else Icons.Outlined.Download,
                                            contentDescription = LocalizedStrings.get("tab_downloads", currentLanguage),
                                            modifier = Modifier.size(23.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = LocalizedStrings.get("tab_downloads", currentLanguage),
                                        fontSize = 10.sp,
                                        fontWeight = if (currentTab == MainTab.DOWNLOADS) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = Color(0xFF8E8E93),
                                    unselectedTextColor = Color(0xFF8E8E93),
                                    indicatorColor = Color.Transparent
                                )
                            )

                            // 4. SETTINGS Tab (Appearance, Language, FX & Support)
                            NavigationBarItem(
                                selected = currentTab == MainTab.SETTINGS,
                                onClick = { currentTab = MainTab.SETTINGS },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == MainTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = LocalizedStrings.get("tab_settings", currentLanguage),
                                        modifier = Modifier.size(23.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = LocalizedStrings.get("tab_settings", currentLanguage),
                                        fontSize = 10.sp,
                                        fontWeight = if (currentTab == MainTab.SETTINGS) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = Color(0xFF8E8E93),
                                    unselectedTextColor = Color(0xFF8E8E93),
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .statusBarsPadding()
                ) {
                    when (currentTab) {
                        // Today Tab: Dedicated to News, Banners, Text Bulletins & Patch Notes with Pull-To-Refresh
                        MainTab.TODAY -> TodayNewsScreen(
                            newsList = newsList,
                            isRefreshing = isNewsRefreshing,
                            statusMessage = newsStatusMessage,
                            currentSource = currentNewsSource,
                            activeNewsUrl = activeNewsUrl,
                            particlesEnabled = particlesEnabled,
                            appsCatalog = apps,
                            currentLanguage = currentLanguage,
                            isOnline = isOnline,
                            onRefreshNews = {
                                coroutineScope.launch {
                                    val res = newsRepo.syncRemoteNews()
                                    Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onToggleParticles = {
                                onParticlesEnabledToggled(!particlesEnabled)
                            },
                            onDownloadLinkedApp = { app ->
                                downloadRepo.startDownload(app)
                                Toast.makeText(context, "Added ${app.name} to downloads", Toast.LENGTH_SHORT).show()
                                if (currentUser != null) {
                                    coroutineScope.launch {
                                        try {
                                            firestoreRepo.recordDownload(app.id, app.name, app.version)
                                        } catch (ignored: Exception) {}
                                    }
                                }
                            },
                            onNavigateToApps = {
                                currentTab = MainTab.APPS
                            }
                        )

                        // Apps Tab: Directory with Categories, Search, Squircle Cards & Pull-To-Refresh
                        MainTab.APPS -> AppsScreen(
                            apps = apps,
                            bookmarkedIds = bookmarkedAppIds,
                            isRefreshing = isCatalogLoading,
                            currentLanguage = currentLanguage,
                            isOnline = isOnline,
                            onRefresh = {
                                coroutineScope.launch {
                                    val res = catalogRepo.syncWithRetrofit()
                                    updateRepo.checkUpdateAvailability()
                                    Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onAppSelected = { selectedAppForDetail = it },
                            onDownloadApp = { app ->
                                downloadRepo.startDownload(app)
                                Toast.makeText(context, "Added ${app.name} to downloads", Toast.LENGTH_SHORT).show()
                                if (currentUser != null) {
                                    coroutineScope.launch {
                                        try {
                                            firestoreRepo.recordDownload(app.id, app.name, app.version)
                                        } catch (e: Exception) {
                                            Log.w("MainActivity", "Failed to record download in Firestore: ${e.message}")
                                        }
                                    }
                                }
                            },
                            onBookmarkToggle = { app ->
                                if (currentUser == null) {
                                    bookmarkedAppIds = if (bookmarkedAppIds.contains(app.id)) {
                                        bookmarkedAppIds - app.id
                                    } else {
                                        bookmarkedAppIds + app.id
                                    }
                                    Toast.makeText(context, "Bookmark updated (Sign in to sync)", Toast.LENGTH_SHORT).show()
                                } else {
                                    coroutineScope.launch {
                                        try {
                                            val added = firestoreRepo.toggleBookmark(app.id, app.name)
                                            bookmarkedAppIds = if (added) bookmarkedAppIds + app.id else bookmarkedAppIds - app.id
                                            Toast.makeText(context, if (added) "Bookmarked ${app.name}" else "Removed bookmark", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cloud sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )

                        // Downloads Tab: Active Progress, Completed Packages & Pull-To-Refresh
                        MainTab.DOWNLOADS -> DownloadsScreen(
                            tasks = downloadTasks,
                            isRefreshing = false,
                            currentLanguage = currentLanguage,
                            onRefresh = {
                                // Instant refresh
                            },
                            onPause = { downloadRepo.pauseDownload(it) },
                            onResume = { downloadRepo.resumeDownload(it) },
                            onCancel = { downloadRepo.cancelDownload(it) },
                            onInstall = { task ->
                                val res = downloadRepo.installApk(context, task)
                                if (res.isFailure) {
                                    Toast.makeText(context, "Could not launch package installer: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        )

                        // Settings Tab: Appearance, Language Switcher, Particles & Feedback
                        MainTab.SETTINGS -> AboutScreen(
                            currentPreset = themePreset,
                            isDarkTheme = isDarkTheme,
                            particlesEnabled = particlesEnabled,
                            particleDensity = particleDensity,
                            userEmail = currentUser?.email,
                            isAuthLoading = isAuthLoading,
                            currentLanguage = currentLanguage,
                            onPresetSelected = onThemePresetChanged,
                            onDarkThemeToggled = onDarkThemeToggled,
                            onParticlesEnabledToggled = onParticlesEnabledToggled,
                            onParticleDensityChanged = onParticleDensityChanged,
                            onLanguageChanged = onLanguageChanged,
                            onGoogleSignInClick = {
                                isAuthLoading = true
                                signInWithGoogle(
                                    context = context,
                                    credentialManager = credentialManager,
                                    scope = coroutineScope,
                                    onAuthSuccess = {
                                        isAuthLoading = false
                                        currentUser = Firebase.auth.currentUser
                                        Toast.makeText(context, "Signed in as ${currentUser?.email ?: "Google User"}", Toast.LENGTH_SHORT).show()
                                    },
                                    onAuthError = { err ->
                                        isAuthLoading = false
                                        Toast.makeText(context, "Sign in failed: $err", Toast.LENGTH_SHORT).show()
                                    },
                                    onAuthCancelled = {
                                        isAuthLoading = false
                                    }
                                )
                            },
                            onSignOutClick = {
                                isAuthLoading = true
                                signOutGoogle(credentialManager, coroutineScope) {
                                    isAuthLoading = false
                                    currentUser = null
                                    bookmarkedAppIds = emptySet()
                                    Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onSubmitFeedback = { type, subject, details, deviceInfo ->
                                try {
                                    val docId = firestoreRepo.submitFeedback(type, subject, details, deviceInfo)
                                    Result.success(docId)
                                } catch (e: Exception) {
                                    Result.failure(e)
                                }
                            }
                        )
                    }
                }
            }

        // App Detail iOS Modal Bottom Sheet
        if (selectedAppForDetail != null) {
            AppDetailSheet(
                app = selectedAppForDetail!!,
                onDismiss = { selectedAppForDetail = null },
                onStartDownload = {
                    val app = selectedAppForDetail!!
                    downloadRepo.startDownload(app)
                    Toast.makeText(context, "Downloading ${app.name}...", Toast.LENGTH_SHORT).show()
                    if (currentUser != null) {
                        coroutineScope.launch {
                            try {
                                firestoreRepo.recordDownload(app.id, app.name, app.version)
                            } catch (ignored: Exception) {}
                        }
                    }
                }
            )
        }

        // Remote JSON Store Update Dialog
        if (showUpdateDialog && updateConfig != null) {
            UpdateDialog(
                updateConfig = updateConfig!!,
                onDismiss = {
                    showUpdateDialog = false
                    updateRepo.dismissUpdate()
                },
                onDownloadUpdate = {
                    showUpdateDialog = false
                    updateRepo.dismissUpdate()
                    val updateApp = ModApp(
                        id = "app_hsstore_update",
                        name = "HsStore Cyber Upgrade",
                        version = updateConfig!!.storeLatestVersion,
                        sizeBytes = 14889779L,
                        sizeDisplay = updateConfig!!.sizeDisplay,
                        downloadUrl = updateConfig!!.downloadUrl,
                        sha256 = updateConfig!!.sha256
                    )
                    downloadRepo.startDownload(updateApp)
                    currentTab = MainTab.DOWNLOADS
                    Toast.makeText(context, "Downloading HsStore v${updateConfig!!.storeLatestVersion} update...", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
    }
}

private fun signInWithGoogle(
    context: android.content.Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onAuthCancelled: () -> Unit
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Web Client ID missing: ${e.message}")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed: ${e.message}", e)
            onAuthError(e.localizedMessage ?: "Sign-in failed")
        }
    }
}

private fun signOutGoogle(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}
