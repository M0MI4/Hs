package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.data.model.ModApp
import com.example.data.model.StoreSelfUpdateConfig
import com.example.ui.components.AppDetailSheet
import com.example.ui.components.CupertinoSelfUpdateDialog
import com.example.ui.components.SUPPORT_RECIPIENT_EMAIL
import com.example.ui.components.buildSupportEmailIntent
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalizedStrings
import com.example.ui.screens.AppsScreen
import com.example.util.InstalledAppDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UpgradesRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `mini download bar renders high-contrast text and View action`() {
        val sampleTask = DownloadTask(
            id = "task_1",
            appId = "snap_prime",
            appName = "Snapchat Prime",
            version = "6.0",
            versionCode = 6,
            packageName = "com.snapchat.android",
            totalBytes = 100L,
            downloadedBytes = 25L,
            progress = 0.25f,
            status = DownloadStatus.DOWNLOADING
        )
        var clicked = false

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                MiniDownloadBar(
                    task = sampleTask,
                    currentLanguage = AppLanguage.ENGLISH,
                    isDarkTheme = true,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("mini_download_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Downloading: Snapchat Prime (25%)").assertIsDisplayed()
        composeTestRule.onNodeWithText("View").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mini_download_bar").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `app detail sheet shows high contrast progress card and broken link report button`() {
        val modApp = ModApp(
            id = "app_test_detail",
            name = "Snapchat Prime",
            packageName = "com.example.nonexistent.snap",
            version = "6.0",
            versionCode = 6,
            sizeDisplay = "120 MB",
            downloadUrl = "https://cdn.example/snap.apk",
            isPublished = true
        )
        val activeTask = DownloadTask(
            id = "task_detail_1",
            appId = modApp.id,
            appName = modApp.name,
            version = modApp.version,
            versionCode = modApp.versionCode,
            packageName = modApp.packageName,
            totalBytes = 100L,
            downloadedBytes = 40L,
            progress = 0.40f,
            status = DownloadStatus.DOWNLOADING
        )

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AppDetailSheet(
                    app = modApp,
                    onDismiss = {},
                    onStartDownload = {},
                    currentLanguage = AppLanguage.ENGLISH,
                    downloadTask = activeTask,
                    isDarkTheme = true
                )
            }
        }

        composeTestRule.onNodeWithTag("detail_download_progress_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Downloading • 40%").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_progress_pause_resume_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_progress_cancel_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("report_broken_link_btn").assertExists()

        // Tapping Report Broken Link opens FeedbackModalSheet pre-selected to BROKEN_LINK
        composeTestRule.onNodeWithTag("report_broken_link_btn").performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodes(hasTestTag("feedback_modal_sheet")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("feedback_modal_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("send_via_email_fallback_btn").assertExists()
    }

    @Test
    fun `apps screen displays Updates Available banner and filter pill when installed mod has newer version`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPm = Shadows.shadowOf(context.packageManager)
        val sigBytes = "trusted-hsstore-mod-cert".toByteArray()
        val expectedHex = InstalledAppDetector.computeSha256Hex(sigBytes)!!
        val sig = android.content.pm.Signature(sigBytes)
        val signingInfoObj = android.content.pm.SigningInfo::class.java
            .getConstructor(
                Int::class.javaPrimitiveType,
                Collection::class.java,
                Collection::class.java,
                Collection::class.java
            )
            .newInstance(3, listOf(sig), emptyList<android.content.pm.Signature>(), emptyList<Int>())
        val pkgName = "com.example.installedmod.updatetest"
        val pkgInfo = android.content.pm.PackageInfo().apply {
            packageName = pkgName
            longVersionCode = 5L
            signingInfo = signingInfoObj
            @Suppress("DEPRECATION")
            signatures = arrayOf(sig)
        }
        shadowPm.installPackage(pkgInfo)
        InstalledAppDetector.invalidateCache(pkgName)

        val modWithUpdate = ModApp(
            id = "mod_update_1",
            name = "YouTube Pro",
            packageName = pkgName,
            version = "19.2",
            versionCode = 10,
            expectedSignatureSha256 = expectedHex,
            downloadUrl = "https://cdn.example/yt.apk",
            isPublished = true,
            category = "Apps"
        )
        val otherMod = ModApp(
            id = "mod_other_2",
            name = "Spotify Ultra",
            packageName = "com.example.not.installed",
            version = "8.9",
            versionCode = 9,
            downloadUrl = "https://cdn.example/sp.apk",
            isPublished = true,
            category = "Apps"
        )

        composeTestRule.setContent {
            MaterialTheme {
                AppsScreen(
                    apps = listOf(modWithUpdate, otherMod),
                    bookmarkedIds = emptySet(),
                    onAppSelected = {},
                    onDownloadApp = {},
                    onBookmarkToggle = {}
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(androidx.compose.ui.test.hasTestTag("updates_available_banner"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("updates_available_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Updates Available (1)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("updates_view_all_pill").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("YouTube Pro").assertIsDisplayed()
    }

    @Test
    fun `self update dialog shows changelog triggers update and feedback intent targets momiadesigner`() {
        val cfg = StoreSelfUpdateConfig(
            latestVersionCode = (BuildConfig.VERSION_CODE + 1).toLong(),
            latestVersionName = "2.5.0",
            downloadUrl = "https://cdn.example/HsStore-v2.5.0.apk",
            changelog = "Faster downloads & Liquid Glass polish",
            isMandatory = false
        )
        var updateClicked = false
        var dismissed = false

        composeTestRule.setContent {
            MaterialTheme {
                CupertinoSelfUpdateDialog(
                    config = cfg,
                    currentLanguage = AppLanguage.KURDISH_SORANI,
                    downloadTask = null,
                    onDismiss = { dismissed = true },
                    onUpdateClick = { updateClicked = true },
                    onInstallClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("self_update_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText(LocalizedStrings.get("store_update_btn", AppLanguage.KURDISH_SORANI))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("self_update_confirm_btn").performClick()
        assertTrue(updateClicked)
        composeTestRule.onNodeWithTag("self_update_dismiss_btn").performClick()
        assertTrue(dismissed)

        val emailIntent = buildSupportEmailIntent(
            category = "BROKEN_LINK",
            appName = "Snapchat Prime",
            subject = "Dead link",
            details = "Returns HTTP 404",
            deviceInfo = "Pixel 9 | Android 16"
        )
        assertEquals(Intent.ACTION_SENDTO, emailIntent.action)
        assertTrue(emailIntent.dataString?.startsWith("mailto:$SUPPORT_RECIPIENT_EMAIL") == true)
        assertTrue(emailIntent.getStringExtra(Intent.EXTRA_SUBJECT)?.contains("HsStore Feedback / Report") == true)
        assertTrue(emailIntent.getStringExtra(Intent.EXTRA_TEXT)?.contains("Snapchat Prime") == true)
        assertTrue(emailIntent.getStringExtra(Intent.EXTRA_TEXT)?.contains("Pixel 9") == true)
    }
}
