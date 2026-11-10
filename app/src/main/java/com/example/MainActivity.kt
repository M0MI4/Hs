package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ModApp
import com.example.ui.admin.AdminPanelScreen
import com.example.ui.admin.AdminViewModel
import com.example.ui.components.AppDetailSheet
import com.example.ui.components.CupertinoFloatingTabBar
import com.example.ui.components.ParticleCanvas
import com.example.ui.components.UpdateDialog
import com.example.ui.i18n.LocalizedStrings
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.TodayNewsScreen
import com.example.ui.theme.HsStoreTheme
import com.example.ui.theme.VerifiedBadgeGreen
import com.example.ui.viewmodel.DownloadsViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.NetworkMonitor
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

/**
 * iOS App Store 4-tab Navigation Enum
 */
enum class MainTab(val title: String) {
    TODAY("Today"),
    APPS("Apps"),
    DOWNLOADS("Downloads"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {

    private val storeViewModel: StoreViewModel by viewModels()
    private val downloadsViewModel: DownloadsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val adminViewModel: AdminViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val networkMonitor = NetworkMonitor(applicationContext)

        setContent {
            val isOnline by networkMonitor.isOnline.collectAsState(initial = true)
            val currentLanguage by settingsViewModel.currentLanguage.collectAsState()
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()
            val themePreset by settingsViewModel.themePreset.collectAsState()
            val particlesEnabled by settingsViewModel.particlesEnabled.collectAsState()
            val particleDensity by settingsViewModel.particleDensity.collectAsState()

            HsStoreTheme(preset = themePreset, darkTheme = isDarkTheme) {
                MainAppContent(
                    storeViewModel = storeViewModel,
                    downloadsViewModel = downloadsViewModel,
                    settingsViewModel = settingsViewModel,
                    adminViewModel = adminViewModel,
                    isOnline = isOnline
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    storeViewModel: StoreViewModel,
    downloadsViewModel: DownloadsViewModel,
    settingsViewModel: SettingsViewModel,
    adminViewModel: AdminViewModel,
    isOnline: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val adminState by adminViewModel.adminState.collectAsState()
    var showAdminPanel by remember { mutableStateOf(false) }

    // Preferences & Theme States from SettingsViewModel
    val currentTab by settingsViewModel.currentTab.collectAsState()
    val currentLanguage by settingsViewModel.currentLanguage.collectAsState()
    val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()
    val themePreset by settingsViewModel.themePreset.collectAsState()
    val particlesEnabled by settingsViewModel.particlesEnabled.collectAsState()
    val particleDensity by settingsViewModel.particleDensity.collectAsState()

    // Store & Catalog States from StoreViewModel
    val apps by storeViewModel.apps.collectAsState()
    val isCatalogLoading by storeViewModel.isCatalogLoading.collectAsState()
    val newsList by storeViewModel.newsList.collectAsState()
    val isNewsRefreshing by storeViewModel.isNewsRefreshing.collectAsState()
    val newsStatusMessage by storeViewModel.newsStatusMessage.collectAsState()
    val currentNewsSource by storeViewModel.currentNewsSource.collectAsState()
    val activeNewsUrl by storeViewModel.activeNewsUrl.collectAsState()
    val updateConfig by storeViewModel.updateConfig.collectAsState()
    val hasUpdate by storeViewModel.hasUpdate.collectAsState()
    val selectedAppForDetail by storeViewModel.selectedAppForDetail.collectAsState()
    val showUpdateDialog by storeViewModel.showUpdateDialog.collectAsState()
    val bookmarkedAppIds by storeViewModel.bookmarkedAppIds.collectAsState()

    // Downloads States from DownloadsViewModel
    val downloadTasks by downloadsViewModel.tasks.collectAsState()
    val activeDownloads by downloadsViewModel.activeDownloads.collectAsState()

    // User auth state tracking
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            storeViewModel.syncBookmarksFromFirestore(auth.currentUser)
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Auto-prompt update dialog on start if update is available
    LaunchedEffect(hasUpdate) {
        if (hasUpdate && updateConfig != null) {
            storeViewModel.setShowUpdateDialog(true)
        }
    }

    BackHandler(enabled = showAdminPanel) {
        showAdminPanel = false
    }

    // Custom back navigation: return to Today tab
    if (!showAdminPanel && currentTab != MainTab.TODAY) {
        BackHandler {
            settingsViewModel.setTab(MainTab.TODAY)
        }
    }

    // Kurdish Sorani Layout Mirroring (RTL vs LTR)
    CompositionLocalProvider(
        LocalLayoutDirection provides if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        if (showAdminPanel) {
            AdminPanelScreen(
                adminViewModel = adminViewModel,
                onNavigateBack = { showAdminPanel = false }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
            // Particle Canvas FX
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
                        // Active Download Mini-Banner
                        if (activeDownloads.isNotEmpty() && currentTab != MainTab.DOWNLOADS) {
                            val active = activeDownloads.first()
                            Surface(
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { settingsViewModel.setTab(MainTab.DOWNLOADS) }
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

                        // Authentic Apple iOS Floating Glass Tab Bar
                        CupertinoFloatingTabBar(
                            currentTab = currentTab,
                            onTabSelected = { settingsViewModel.setTab(it) },
                            currentLanguage = currentLanguage,
                            isDarkTheme = isDarkTheme,
                            activeDownloadsCount = activeDownloads.size
                        )
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
                                    val res = storeViewModel.refreshNews()
                                    Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onToggleParticles = {
                                settingsViewModel.toggleParticles(!particlesEnabled)
                            },
                            onDownloadLinkedApp = { app ->
                                downloadsViewModel.startDownload(app, currentUser)
                                Toast.makeText(context, "Added ${app.name} to downloads", Toast.LENGTH_SHORT).show()
                            },
                            onNavigateToApps = {
                                settingsViewModel.setTab(MainTab.APPS)
                            }
                        )

                        MainTab.APPS -> AppsScreen(
                            apps = apps,
                            bookmarkedIds = bookmarkedAppIds,
                            isRefreshing = isCatalogLoading,
                            currentLanguage = currentLanguage,
                            isOnline = isOnline,
                            onRefresh = {
                                coroutineScope.launch {
                                    val res = storeViewModel.refreshCatalog()
                                    Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onAppSelected = { storeViewModel.selectAppForDetail(it) },
                            onDownloadApp = { app ->
                                downloadsViewModel.startDownload(app, currentUser)
                                Toast.makeText(context, "Added ${app.name} to downloads", Toast.LENGTH_SHORT).show()
                            },
                            onBookmarkToggle = { app ->
                                storeViewModel.toggleBookmark(app, currentUser) { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        MainTab.DOWNLOADS -> DownloadsScreen(
                            tasks = downloadTasks,
                            isRefreshing = false,
                            currentLanguage = currentLanguage,
                            onRefresh = {},
                            onPause = { downloadsViewModel.pauseDownload(it) },
                            onResume = { downloadsViewModel.resumeDownload(it) },
                            onCancel = { downloadsViewModel.cancelDownload(it) },
                            onInstall = { task ->
                                val res = downloadsViewModel.installApk(context, task)
                                if (res.isFailure) {
                                    Toast.makeText(context, "Installer: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        )

                        MainTab.SETTINGS -> AboutScreen(
                            currentPreset = themePreset,
                            isDarkTheme = isDarkTheme,
                            particlesEnabled = particlesEnabled,
                            particleDensity = particleDensity,
                            currentLanguage = currentLanguage,
                            isAdminLoggedIn = adminState.isAdminLoggedIn,
                            adminEmail = adminState.currentAdminEmail,
                            onAdminLogin = { email, pass -> adminViewModel.loginAdmin(email, pass) },
                            onOpenAdminPanel = { showAdminPanel = true },
                            adminLoading = adminState.isLoading,
                            adminError = adminState.errorMessage,
                            onPresetSelected = { settingsViewModel.setThemePreset(it) },
                            onDarkThemeToggled = { settingsViewModel.toggleDarkTheme(it) },
                            onParticlesEnabledToggled = { settingsViewModel.toggleParticles(it) },
                            onParticleDensityChanged = { settingsViewModel.setParticleDensity(it) },
                            onLanguageChanged = { settingsViewModel.setLanguage(it) },
                            onSubmitFeedback = { type, subject, details, deviceInfo ->
                                settingsViewModel.submitFeedback(type, subject, details, deviceInfo)
                            }
                        )
                    }
                }
            }

            // App Detail iOS Modal Bottom Sheet
            if (selectedAppForDetail != null) {
                AppDetailSheet(
                    app = selectedAppForDetail!!,
                    onDismiss = { storeViewModel.selectAppForDetail(null) },
                    onStartDownload = {
                        val app = selectedAppForDetail!!
                        downloadsViewModel.startDownload(app, currentUser)
                        Toast.makeText(context, "Downloading ${app.name}...", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Remote JSON Store Update Dialog
            if (showUpdateDialog && updateConfig != null) {
                UpdateDialog(
                    updateConfig = updateConfig!!,
                    onDismiss = { storeViewModel.dismissUpdateDialog() },
                    onDownloadUpdate = {
                        storeViewModel.dismissUpdateDialog()
                        val updateApp = ModApp(
                            id = "app_hsstore_update",
                            name = "HsStore Cyber Upgrade",
                            packageName = context.packageName,
                            version = updateConfig!!.storeLatestVersion,
                            versionCode = updateConfig!!.storeLatestVersionCode,
                            sizeBytes = 14889779L,
                            sizeDisplay = updateConfig!!.sizeDisplay,
                            downloadUrl = updateConfig!!.downloadUrl,
                            sha256 = updateConfig!!.sha256
                        )
                        downloadsViewModel.startDownload(updateApp, currentUser)
                        settingsViewModel.setTab(MainTab.DOWNLOADS)
                        Toast.makeText(context, "Downloading HsStore v${updateConfig!!.storeLatestVersion} update...", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Floating Cupertino Admin Badge
            if (adminState.isAdminLoggedIn) {
                Surface(
                    onClick = { showAdminPanel = true },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, VerifiedBadgeGreen.copy(alpha = 0.8f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 96.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(VerifiedBadgeGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Admin Studio",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
}
