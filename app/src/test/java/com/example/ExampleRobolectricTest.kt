package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
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
}
