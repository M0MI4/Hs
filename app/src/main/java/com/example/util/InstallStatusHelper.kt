package com.example.util

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

object InstallStatusHelper {

    val packageStateVersion: StateFlow<Int>
        get() = InstalledAppDetector.packageStateVersion

    fun preloadInstalledPackages(context: Context, packageNames: Collection<String> = emptyList()) {
        if (packageNames.isNotEmpty()) {
            InstalledAppDetector.preloadPackages(context, packageNames)
        } else {
            InstalledAppDetector.refreshAllKnownPackages(context)
        }
    }

    fun getAppInstallState(
        context: Context,
        packageName: String,
        remoteVersionCode: Int,
        expectedSignatureSha256: String = ""
    ): AppInstallState {
        return InstalledAppDetector.detectAppState(
            context = context,
            packageName = packageName,
            storeVersionCode = remoteVersionCode,
            expectedSignatureSha256 = expectedSignatureSha256
        )
    }

    fun updateInstalledPackage(packageName: String, versionCode: Int) {
        InstalledAppDetector.updateInstalledPackage(packageName, versionCode)
    }

    fun uninstallPackage(context: Context, packageName: String): Boolean {
        return InstalledAppDetector.uninstallPackage(context, packageName)
    }

    fun launchInstalledApp(context: Context, packageName: String): Boolean {
        return InstalledAppDetector.launchInstalledApp(context, packageName)
    }
}
