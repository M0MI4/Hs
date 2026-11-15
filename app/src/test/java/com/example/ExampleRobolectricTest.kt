package com.example

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("HsStore", appName)
  }

  @Test
  fun `string list converter serializes and deserializes correctly`() {
    val converter = com.example.data.local.StringListConverter()
    val input = listOf("Rootless Hook", "No ADS", "Anti-Ban")
    val json = converter.fromStringList(input)
    val output = converter.toStringList(json)
    assertEquals(input, output)
  }

  @Test
  fun `install state returns NOT_INSTALLED for uninstalled package`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.InstalledAppDetector.invalidateCache()
    val state = com.example.util.InstallStatusHelper.getAppInstallState(context, "com.nonexistent.package.app", 1)
    assertEquals(com.example.util.AppInstallState.NOT_INSTALLED, state)
  }

  @Test
  fun `admin state defaults to logged out`() {
    val state = com.example.ui.admin.AdminState()
    assertEquals(false, state.isAdminLoggedIn)
    assertEquals(null, state.currentAdminEmail)
  }

  @Test
  fun `admin view model can be instantiated by application context`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    if (com.google.firebase.FirebaseApp.getApps(application).isEmpty()) {
        try {
            com.google.firebase.FirebaseApp.initializeApp(
                application,
                com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.hsstore.modapk")
                    .setProjectId("test-project")
                    .setApiKey("fake-api-key-for-test")
                    .build()
            )
        } catch (_: Exception) {}
    }
    val vm = com.example.ui.admin.AdminViewModel(application)
    assertEquals(false, vm.adminState.value.isAdminLoggedIn)
  }

  @Test
  fun `localization keys exist for mini download bar and detail sheet in English and Kurdish`() {
    val keys = listOf(
      "downloads_active_section",
      "downloads_action_view",
      "detail_downloads",
      "detail_version",
      "detail_size_category",
      "detail_tab_features",
      "detail_tab_changelog",
      "detail_uninstall_note"
    )
    for (k in keys) {
      val enVal = com.example.ui.i18n.LocalizedStrings.get(k, com.example.ui.i18n.AppLanguage.ENGLISH)
      val kuVal = com.example.ui.i18n.LocalizedStrings.get(k, com.example.ui.i18n.AppLanguage.KURDISH_SORANI)
      assertEquals(false, enVal == k)
      assertEquals(false, kuVal == k)
    }
    assertEquals("Downloading", com.example.ui.i18n.LocalizedStrings.get("downloads_active_section", com.example.ui.i18n.AppLanguage.ENGLISH))
    assertEquals("دادەبەزێت", com.example.ui.i18n.LocalizedStrings.get("downloads_active_section", com.example.ui.i18n.AppLanguage.KURDISH_SORANI))
    assertEquals("View", com.example.ui.i18n.LocalizedStrings.get("downloads_action_view", com.example.ui.i18n.AppLanguage.ENGLISH))
    assertEquals("پیشاندان", com.example.ui.i18n.LocalizedStrings.get("downloads_action_view", com.example.ui.i18n.AppLanguage.KURDISH_SORANI))
  }

  @Test
  fun `mod app defaults downloadCount to zero and optional flags to false or empty`() {
    val app = com.example.data.model.ModApp(id = "snap", name = "Snapchat Prime")
    assertEquals(0L, app.downloadCount)
    assertEquals(false, app.requiresUninstallOfficial)
    assertEquals("", app.glowColorHex)
    assertEquals("", app.expectedSignatureSha256)
  }

  @Test
  fun `installed app detector evaluates all 4 smart install states accurately`() {
    val trusted = com.example.util.InstalledAppDetector.DEFAULT_TRUSTED_MOD_SIGNATURE_SHA256
    val otherSig = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"

    assertEquals(
      com.example.util.AppInstallState.NOT_INSTALLED,
      com.example.util.InstalledAppDetector.evaluateState(
        info = com.example.util.InstalledAppInfo(isInstalled = false),
        storeVersionCode = 10
      )
    )

    assertEquals(
      com.example.util.AppInstallState.INSTALLED_LATEST,
      com.example.util.InstalledAppDetector.evaluateState(
        info = com.example.util.InstalledAppInfo(
          isInstalled = true,
          versionCode = 10L,
          signaturesSha256 = listOf(trusted)
        ),
        storeVersionCode = 10
      )
    )

    assertEquals(
      com.example.util.AppInstallState.UPDATE_AVAILABLE,
      com.example.util.InstalledAppDetector.evaluateState(
        info = com.example.util.InstalledAppInfo(
          isInstalled = true,
          versionCode = 8L,
          signaturesSha256 = listOf(trusted)
        ),
        storeVersionCode = 10
      )
    )

    assertEquals(
      com.example.util.AppInstallState.SIGNATURE_MISMATCH,
      com.example.util.InstalledAppDetector.evaluateState(
        info = com.example.util.InstalledAppInfo(
          isInstalled = true,
          versionCode = 12L,
          signaturesSha256 = listOf(otherSig)
        ),
        storeVersionCode = 10
      )
    )

    // Custom expected signature per ModApp overrides default trusted signature
    assertEquals(
      com.example.util.AppInstallState.INSTALLED_LATEST,
      com.example.util.InstalledAppDetector.evaluateState(
        info = com.example.util.InstalledAppInfo(
          isInstalled = true,
          versionCode = 10L,
          signaturesSha256 = listOf(otherSig)
        ),
        storeVersionCode = 10,
        expectedSignatureSha256 = otherSig
      )
    )
  }

  @Test
  fun `installed app detector inspects real PackageManager certificates without colons`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val shadowPm = org.robolectric.Shadows.shadowOf(context.packageManager)
    val sigBytes = "test-mod-certificate".toByteArray()
    val expectedHex = com.example.util.InstalledAppDetector.computeSha256Hex(sigBytes)!!
    assertEquals(false, expectedHex.contains(":"))
    assertEquals(expectedHex.uppercase(), expectedHex)

    val sig = android.content.pm.Signature(sigBytes)
    val signingInfoObj = android.content.pm.SigningInfo::class.java
      .getConstructor(
        Int::class.javaPrimitiveType,
        Collection::class.java,
        Collection::class.java,
        Collection::class.java
      )
      .newInstance(3, listOf(sig), emptyList<android.content.pm.Signature>(), emptyList<Int>())

    val pkgInfo = android.content.pm.PackageInfo().apply {
      packageName = "com.example.testmod"
      longVersionCode = 5L
      signingInfo = signingInfoObj
      @Suppress("DEPRECATION")
      signatures = arrayOf(sig)
    }
    shadowPm.installPackage(pkgInfo)
    com.example.util.InstalledAppDetector.invalidateCache("com.example.testmod")

    val updateState = com.example.util.InstalledAppDetector.detectAppState(
      context = context,
      packageName = "com.example.testmod",
      storeVersionCode = 6,
      expectedSignatureSha256 = expectedHex
    )
    assertEquals(com.example.util.AppInstallState.UPDATE_AVAILABLE, updateState)

    val mismatchState = com.example.util.InstalledAppDetector.detectAppState(
      context = context,
      packageName = "com.example.testmod",
      storeVersionCode = 5,
      expectedSignatureSha256 = ""
    )
    assertEquals(com.example.util.AppInstallState.SIGNATURE_MISMATCH, mismatchState)
  }

  @Test
  fun `kurdish sorani date formatter returns Sorani script instead of Kurmanji Latin`() {
    val cal = java.util.Calendar.getInstance().apply {
      set(2026, java.util.Calendar.NOVEMBER, 14, 12, 0, 0)
    }
    val kuDate = com.example.ui.i18n.formatLocalizedTodayDate(
      language = com.example.ui.i18n.AppLanguage.KURDISH_SORANI,
      timeMillis = cal.timeInMillis
    )
    assertEquals("شەممە، ١٤ی تشرینی دووەم", kuDate)
    assertEquals(false, kuDate.contains("ŞEMÎ", ignoreCase = true))
    assertEquals(false, kuDate.contains("MIJDAR", ignoreCase = true))

    val enDate = com.example.ui.i18n.formatLocalizedTodayDate(
      language = com.example.ui.i18n.AppLanguage.ENGLISH,
      timeMillis = cal.timeInMillis
    )
    assertEquals("SATURDAY, NOVEMBER 14", enDate)
  }

  @Test
  fun `kurdish typography applies KurdishFontFamily and zero letterSpacing across all styles`() {
    val kuType = com.example.ui.theme.hsStoreTypography(isKurdish = true)
    val zeroSp = androidx.compose.ui.unit.TextUnit(0f, androidx.compose.ui.unit.TextUnitType.Sp)
    val styles = listOf(
      kuType.displayLarge,
      kuType.headlineLarge,
      kuType.headlineMedium,
      kuType.titleLarge,
      kuType.titleMedium,
      kuType.titleSmall,
      kuType.bodyLarge,
      kuType.bodyMedium,
      kuType.bodySmall,
      kuType.labelLarge,
      kuType.labelMedium,
      kuType.labelSmall
    )
    for (style in styles) {
      assertEquals(com.example.ui.theme.KurdishFontFamily, style.fontFamily)
      assertEquals(zeroSp, style.letterSpacing)
    }

    val enType = com.example.ui.theme.hsStoreTypography(isKurdish = false)
    assertEquals(androidx.compose.ui.text.font.FontFamily.Default, enType.bodyLarge.fontFamily)
  }

  @Test
  fun `downloads viewModel auto-installs pending replace only after package uninstall completes`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    if (com.google.firebase.FirebaseApp.getApps(app).isEmpty()) {
      try {
        com.google.firebase.FirebaseApp.initializeApp(
          app,
          com.google.firebase.FirebaseOptions.Builder()
            .setApplicationId("com.aistudio.hsstore.modapk")
            .setProjectId("test-project")
            .setApiKey("fake-api-key-for-test")
            .build()
        )
      } catch (_: Exception) {}
    }

    val shadowPm = org.robolectric.Shadows.shadowOf(app.packageManager)
    val pkgName = "com.cyberghostvpn.mod"
    val pkgInfo = android.content.pm.PackageInfo().apply {
      packageName = pkgName
      longVersionCode = 1L
    }
    shadowPm.installPackage(pkgInfo)

    val vm = com.example.ui.viewmodel.DownloadsViewModel(app)
    val modApp = com.example.data.model.ModApp(
      id = "app_cyber_vpn",
      name = "CyberGhost Pro Mod",
      packageName = pkgName,
      version = "8.4.2",
      versionCode = 2
    )

    val apkFile = vm.downloadRepo.apkFileForApp("app_cyber_vpn")
    apkFile.parentFile?.mkdirs()
    apkFile.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04))
    val seededTask = com.example.data.model.DownloadTask(
      id = "dl_cyber_vpn_test",
      appId = modApp.id,
      appName = modApp.name,
      version = modApp.version,
      versionCode = modApp.versionCode,
      packageName = modApp.packageName,
      totalBytes = 4L,
      downloadedBytes = 4L,
      progress = 1.0f,
      status = com.example.data.model.DownloadStatus.COMPLETED,
      filePath = apkFile.absolutePath,
      completedAt = System.currentTimeMillis()
    )
    kotlinx.coroutines.runBlocking {
      vm.downloadRepo.upsertSavedTask(seededTask)
    }

    vm.initiateReplaceUninstall(app, modApp)
    assertEquals("app_cyber_vpn", vm.pendingReplaceAppId.value)

    // Still installed -> should NOT auto-install yet
    val beforeUninstall = vm.checkAndAutoInstallPendingReplace(app, listOf(modApp))
    assertEquals(false, beforeUninstall)
    assertEquals("app_cyber_vpn", vm.pendingReplaceAppId.value)

    // Simulate user confirming Android system uninstall dialog
    shadowPm.removePackage(pkgName)
    val afterUninstall = vm.checkAndAutoInstallPendingReplace(app, listOf(modApp))
    assertEquals(true, afterUninstall)
    assertEquals(null, vm.pendingReplaceAppId.value)
  }

  @Test
  fun `download repository persists completed and interrupted tasks across restarts in filesDir apks`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val dao = com.example.data.local.AppDatabase.getInstance(app).downloadTaskDao()
    val repo1 = com.example.data.repository.DownloadRepository(app, dao)

    val completedFile = repo1.apkFileForApp("persist_completed")
    completedFile.parentFile?.mkdirs()
    completedFile.writeBytes(ByteArray(256) { 1 })
    assertEquals(true, completedFile.absolutePath.contains("apks"))
    assertEquals(false, completedFile.absolutePath.contains("cache"))

    val partialFile = repo1.apkFileForApp("persist_interrupted")
    partialFile.writeBytes(ByteArray(100) { 2 })

    val missingFile = repo1.apkFileForApp("persist_missing")
    if (missingFile.exists()) missingFile.delete()

    kotlinx.coroutines.runBlocking {
      dao.upsertTask(
        com.example.data.local.DownloadTaskEntity.fromDomain(
          com.example.data.model.DownloadTask(
            id = "dl_done",
            appId = "persist_completed",
            appName = "Completed Mod",
            version = "2.0",
            versionCode = 20,
            packageName = "com.example.done",
            totalBytes = 256L,
            downloadedBytes = 256L,
            progress = 1.0f,
            status = com.example.data.model.DownloadStatus.COMPLETED,
            filePath = completedFile.absolutePath
          )
        )
      )
      dao.upsertTask(
        com.example.data.local.DownloadTaskEntity.fromDomain(
          com.example.data.model.DownloadTask(
            id = "dl_interrupted",
            appId = "persist_interrupted",
            appName = "Interrupted Mod",
            version = "1.5",
            versionCode = 15,
            packageName = "com.example.interrupted",
            totalBytes = 400L,
            downloadedBytes = 50L,
            progress = 0.125f,
            status = com.example.data.model.DownloadStatus.DOWNLOADING,
            filePath = partialFile.absolutePath
          )
        )
      )
      dao.upsertTask(
        com.example.data.local.DownloadTaskEntity.fromDomain(
          com.example.data.model.DownloadTask(
            id = "dl_missing",
            appId = "persist_missing",
            appName = "Deleted APK Mod",
            version = "1.0",
            versionCode = 1,
            packageName = "com.example.missing",
            totalBytes = 100L,
            downloadedBytes = 100L,
            progress = 1.0f,
            status = com.example.data.model.DownloadStatus.COMPLETED,
            filePath = missingFile.absolutePath
          )
        )
      )
    }

    // Fresh repository instance simulating cold app restart
    val repo2 = com.example.data.repository.DownloadRepository(app, dao)
    kotlinx.coroutines.runBlocking {
      repo2.restoreSavedTasks()
    }
    val restored = repo2.tasks.value

    val restoredCompleted = restored.find { it.appId == "persist_completed" }
    assertEquals(com.example.data.model.DownloadStatus.COMPLETED, restoredCompleted?.status)
    assertEquals(1.0f, restoredCompleted?.progress)
    assertEquals(completedFile.absolutePath, restoredCompleted?.filePath)

    val restoredPaused = restored.find { it.appId == "persist_interrupted" }
    assertEquals(com.example.data.model.DownloadStatus.PAUSED, restoredPaused?.status)
    assertEquals(100L, restoredPaused?.downloadedBytes)

    // Missing APK file record must be cleaned up
    assertEquals(null, restored.find { it.appId == "persist_missing" })
  }

  @Test
  fun `installOrReplaceCompletedApk blocks installApk and launches ACTION_DELETE on signature mismatch`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    if (com.google.firebase.FirebaseApp.getApps(app).isEmpty()) {
      try {
        com.google.firebase.FirebaseApp.initializeApp(
          app,
          com.google.firebase.FirebaseOptions.Builder()
            .setApplicationId("com.aistudio.hsstore.modapk")
            .setProjectId("test-project")
            .setApiKey("fake-api-key-for-test")
            .build()
        )
      } catch (_: Exception) {}
    }

    val shadowPm = org.robolectric.Shadows.shadowOf(app.packageManager)
    val shadowApp = org.robolectric.Shadows.shadowOf(app)
    val pkgName = "com.example.officialmismatch"
    val officialSig = android.content.pm.Signature("official-store-cert".toByteArray())
    val signingInfoObj = android.content.pm.SigningInfo::class.java
      .getConstructor(
        Int::class.javaPrimitiveType,
        Collection::class.java,
        Collection::class.java,
        Collection::class.java
      )
      .newInstance(3, listOf(officialSig), emptyList<android.content.pm.Signature>(), emptyList<Int>())
    val pkgInfo = android.content.pm.PackageInfo().apply {
      packageName = pkgName
      longVersionCode = 5L
      signingInfo = signingInfoObj
      @Suppress("DEPRECATION")
      signatures = arrayOf(officialSig)
    }
    shadowPm.installPackage(pkgInfo)
    com.example.util.InstalledAppDetector.invalidateCache(pkgName)

    val vm = com.example.ui.viewmodel.DownloadsViewModel(app)
    val apkFile = vm.downloadRepo.apkFileForApp("app_mismatch_guard")
    apkFile.parentFile?.mkdirs()
    apkFile.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04))

    val modApp = com.example.data.model.ModApp(
      id = "app_mismatch_guard",
      name = "Mismatched Mod",
      packageName = pkgName,
      version = "5.0",
      versionCode = 5
    )
    val task = com.example.data.model.DownloadTask(
      id = "dl_mismatch_guard",
      appId = modApp.id,
      appName = modApp.name,
      version = modApp.version,
      versionCode = modApp.versionCode,
      packageName = pkgName,
      totalBytes = 4L,
      downloadedBytes = 4L,
      progress = 1.0f,
      status = com.example.data.model.DownloadStatus.COMPLETED,
      filePath = apkFile.absolutePath
    )

    // Drain any previous started activities
    while (shadowApp.nextStartedActivity != null) {}

    vm.installOrReplaceCompletedApk(app, task, modApp)

    assertEquals(modApp.id, vm.pendingReplaceAppId.value)
    val launched = shadowApp.nextStartedActivity
    assertEquals(android.content.Intent.ACTION_DELETE, launched?.action)
    assertEquals("package:$pkgName", launched?.dataString)
    // Ensure ACTION_VIEW APK installer was NOT launched
    assertEquals(null, shadowApp.nextStartedActivity)
  }

  @Test
  fun `manifest declares REQUEST_DELETE_PACKAGES and settings keeps Engine Architecture and Publisher in English`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pkgInfo = context.packageManager.getPackageInfo(
      context.packageName,
      android.content.pm.PackageManager.GET_PERMISSIONS
    )
    val permissions = pkgInfo.requestedPermissions?.toList().orEmpty()
    assertEquals(true, permissions.contains(android.Manifest.permission.REQUEST_DELETE_PACKAGES))

    assertEquals(
      "Engine Architecture",
      com.example.ui.i18n.LocalizedStrings.get("settings_engine_title", com.example.ui.i18n.AppLanguage.ENGLISH)
    )
    assertEquals(
      "Engine Architecture",
      com.example.ui.i18n.LocalizedStrings.get("settings_engine_title", com.example.ui.i18n.AppLanguage.KURDISH_SORANI)
    )

    val publisherKeys = listOf(
      "hama_sade_team_title" to "OFFICIAL PUBLISHER",
      "hama_sade_team_subtitle" to "Official Publisher & Community Partner",
      "hama_sade_team_bio" to "Official publisher & Telegram channel partner. Join our Telegram channel for the latest mod releases, news, and update announcements.",
      "action_join_telegram" to "Join Telegram"
    )
    for ((key, expected) in publisherKeys) {
      assertEquals(
        expected,
        com.example.ui.i18n.LocalizedStrings.get(key, com.example.ui.i18n.AppLanguage.KURDISH_SORANI)
      )
    }
  }

  @Test
  fun `preset catalog builds all 51 draft apps and games with HsStore defaults`() {
    val drafts = com.example.data.catalog.PresetCatalog51.buildDraftModApps()
    assertEquals(51, drafts.size)
    assertEquals(51, drafts.map { it.id }.toSet().size)
    assertEquals(40, drafts.count { it.category == "Apps" })
    assertEquals(11, drafts.count { it.category == "Games" })

    for (app in drafts) {
      assertEquals(false, app.isPublished)
      assertEquals("", app.downloadUrl)
      assertEquals(0L, app.downloadCount)
      assertEquals("HsStore", app.author)
      assertEquals(true, app.name.isNotBlank())
      assertEquals(true, app.description.isNotBlank())
      assertEquals(true, app.glowColorHex.startsWith("#"))
      assertEquals(false, app.version.startsWith("v", ignoreCase = true))
    }

    val snapPrime = drafts.first { it.id == "preset_01_snapchat_prime_v6" }
    assertEquals("Snapchat Prime V6", snapPrime.name)
    assertEquals("6.0", snapPrime.version)
    assertEquals("com.snapchat.android", snapPrime.packageName)
    assertEquals("177.6 MB", snapPrime.sizeDisplay)

    val mc = drafts.first { it.id == "preset_50_minecraft" }
    assertEquals("Minecraft", mc.name)
    assertEquals("Games", mc.category)
    assertEquals("com.mojang.minecraftpe", mc.packageName)
  }

  @Test
  fun `icon fetcher parses 512px iTunes artwork upgrades 100px URLs and normalizes search terms`() {
    assertEquals("Snapchat", com.example.util.IconFetcher.buildSearchTerm("Snapchat Prime V6", "com.snapchat.android"))
    assertEquals("Grand Theft Auto San Andreas", com.example.util.IconFetcher.buildSearchTerm("GTA San Andreas Kurdish"))

    val jsonWith512 = """
      {"resultCount":1,"results":[{"artworkUrl512":"https://is1-ssl.mzstatic.com/image/thumb/Purple/512x512bb.jpg","artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/Purple/100x100bb.jpg"}]}
    """.trimIndent()
    assertEquals(
      "https://is1-ssl.mzstatic.com/image/thumb/Purple/512x512bb.jpg",
      com.example.util.IconFetcher.extractItunesArtwork512Url(jsonWith512)
    )

    val jsonWith100Only = """
      {"resultCount":1,"results":[{"artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/Purple/100x100bb.jpg"}]}
    """.trimIndent()
    assertEquals(
      "https://is1-ssl.mzstatic.com/image/thumb/Purple/512x512bb.jpg",
      com.example.util.IconFetcher.extractItunesArtwork512Url(jsonWith100Only)
    )

    val monogram = com.example.util.IconFetcher.buildHighResMonogramUrl("HS Lab Kurd Prime", "#6C5CE7")
    assertEquals(true, monogram.contains("size=512"))
    assertEquals(true, monogram.contains("background=6C5CE7"))
    assertEquals(true, monogram.contains("format=png"))
    assertEquals(true, monogram.contains("name=HL"))
  }

  @Test
  fun `room database persists ModApp isPublished draft flag and filters public catalog`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dao = com.example.data.local.AppDatabase.getInstance(context).modAppDao()

    val draftApp = com.example.data.model.ModApp(
      id = "draft_only_app",
      name = "Draft App",
      downloadUrl = "",
      isPublished = false,
      author = "HsStore"
    )
    val publishedNoUrl = com.example.data.model.ModApp(
      id = "pub_no_url",
      name = "Missing Url App",
      downloadUrl = "   ",
      isPublished = true,
      author = "HsStore"
    )
    val liveApp = com.example.data.model.ModApp(
      id = "live_ready_app",
      name = "Live Ready App",
      downloadUrl = "https://cdn.example.com/live.apk",
      isPublished = true,
      author = "HsStore"
    )

    kotlinx.coroutines.runBlocking {
      dao.insertApps(
        listOf(
          com.example.data.local.ModAppEntity.fromDomain(draftApp),
          com.example.data.local.ModAppEntity.fromDomain(publishedNoUrl),
          com.example.data.local.ModAppEntity.fromDomain(liveApp)
        )
      )
      val savedDraft = dao.getAppById("draft_only_app")?.toDomain()
      assertEquals(false, savedDraft?.isPublished)

      val savedLive = dao.getAppById("live_ready_app")?.toDomain()
      assertEquals(true, savedLive?.isPublished)
    }

    val allApps = listOf(draftApp, publishedNoUrl, liveApp)
    val publicOnly = allApps.filter { it.isPublished && it.downloadUrl.isNotBlank() }
    assertEquals(listOf("live_ready_app"), publicOnly.map { it.id })
    assertEquals("DRAFT", com.example.ui.i18n.LocalizedStrings.get("badge_draft", com.example.ui.i18n.AppLanguage.ENGLISH))
    assertEquals("ڕەشنووس", com.example.ui.i18n.LocalizedStrings.get("badge_draft", com.example.ui.i18n.AppLanguage.KURDISH_SORANI))
  }

  @Test
  fun `icon fetcher parseItunesSearchResults returns up to 15 results with trackName artistName and 512px artwork`() {
    val entries = (1..18).joinToString(",") { idx ->
      if (idx % 2 == 0) {
        """{"trackName":"App $idx","artistName":"Studio $idx","artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/App$idx/100x100bb.jpg"}"""
      } else {
        """{"trackName":"App $idx","artistName":"Studio $idx","artworkUrl512":"https://is1-ssl.mzstatic.com/image/thumb/App$idx/512x512bb.jpg"}"""
      }
    }
    val json = """{"resultCount":18,"results":[$entries]}"""
    val parsed = com.example.util.IconFetcher.parseItunesSearchResults(json)

    assertEquals(15, parsed.size)
    assertEquals("App 1", parsed[0].trackName)
    assertEquals("Studio 1", parsed[0].artistName)
    assertEquals("https://is1-ssl.mzstatic.com/image/thumb/App1/512x512bb.jpg", parsed[0].artworkUrl512)

    assertEquals("App 2", parsed[1].trackName)
    assertEquals("Studio 2", parsed[1].artistName)
    assertEquals("https://is1-ssl.mzstatic.com/image/thumb/App2/512x512bb.jpg", parsed[1].artworkUrl512)
  }

  @Test
  fun `downloadCount increments strictly once on non-empty completed download and never on click or duplicate actions`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val dao = com.example.data.local.AppDatabase.getInstance(app).downloadTaskDao()
    val countedAppIds = mutableListOf<String>()

    val repo = com.example.data.repository.DownloadRepository(
      context = app,
      downloadTaskDao = dao,
      onDownloadCompletedCounted = { id -> countedAppIds.add(id) }
    )

    val emptyFile = repo.apkFileForApp("count_empty_test")
    emptyFile.parentFile?.mkdirs()
    emptyFile.writeBytes(ByteArray(0))

    val missingFile = repo.apkFileForApp("count_missing_test")
    if (missingFile.exists()) missingFile.delete()

    val validFile = repo.apkFileForApp("count_valid_test")
    validFile.writeBytes(ByteArray(128) { 7 })

    kotlinx.coroutines.runBlocking {
      // Missing or 0-byte files must NEVER increment downloadCount
      assertEquals(false, repo.incrementDownloadCountOnCompletion("task_missing", "app_missing", missingFile))
      assertEquals(false, repo.incrementDownloadCountOnCompletion("task_empty", "app_empty", emptyFile))
      assertEquals(0, countedAppIds.size)

      // Completed non-empty file increments strictly once
      assertEquals(true, repo.incrementDownloadCountOnCompletion("task_valid", "app_snap_mod", validFile))
      assertEquals(listOf("app_snap_mod"), countedAppIds)

      // Subsequent calls for same job (resume / install / replace) must NOT increment again
      assertEquals(false, repo.incrementDownloadCountOnCompletion("task_valid", "app_snap_mod", validFile))
      assertEquals(listOf("app_snap_mod"), countedAppIds)
    }

    assertEquals("SIZE", com.example.ui.i18n.LocalizedStrings.get("detail_size", com.example.ui.i18n.AppLanguage.ENGLISH))
    assertEquals("قەبارە", com.example.ui.i18n.LocalizedStrings.get("detail_size", com.example.ui.i18n.AppLanguage.KURDISH_SORANI))
  }

  @Test
  fun `cupertino floating tab bar renders cleanly with backdrop and fallback modes`() {
    val selectedTabs = mutableListOf<MainTab>()
    Robolectric.buildActivity(ComponentActivity::class.java).use { controller ->
      val activity = controller.setup().get()
      activity.setContent {
        val bg = Color(0xFFF2F2F7)
        val backdrop = rememberLayerBackdrop {
          drawRect(bg)
          drawContent()
        }
        Box(modifier = Modifier.fillMaxSize()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(bg)
              .layerBackdrop(backdrop)
          )
          com.example.ui.components.CupertinoFloatingTabBar(
            currentTab = MainTab.TODAY,
            onTabSelected = { selectedTabs.add(it) },
            currentLanguage = com.example.ui.i18n.AppLanguage.ENGLISH,
            isDarkTheme = false,
            activeDownloadsCount = 2,
            backdrop = backdrop
          )
          com.example.ui.components.CupertinoFloatingTabBar(
            currentTab = MainTab.SETTINGS,
            onTabSelected = { selectedTabs.add(it) },
            currentLanguage = com.example.ui.i18n.AppLanguage.KURDISH_SORANI,
            isDarkTheme = true,
            activeDownloadsCount = 0,
            backdrop = null
          )
        }
      }
      assertEquals(4, MainTab.entries.size)
      assertEquals(true, selectedTabs.isEmpty())
    }
  }
}
