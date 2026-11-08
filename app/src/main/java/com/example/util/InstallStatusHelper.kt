package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

enum class AppInstallState(val label: String) {
    GET("GET"),
    UPDATE("UPDATE"),
    OPEN("OPEN")
}

object InstallStatusHelper {

    fun getAppInstallState(context: Context, packageName: String, remoteVersionCode: Int): AppInstallState {
        if (packageName.isBlank()) return AppInstallState.GET

        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }

            val installedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }

            if (installedVersionCode < remoteVersionCode) {
                AppInstallState.UPDATE
            } else {
                AppInstallState.OPEN
            }
        } catch (e: PackageManager.NameNotFoundException) {
            AppInstallState.GET
        } catch (e: Exception) {
            Log.w("InstallStatusHelper", "Error checking package $packageName: ${e.message}")
            AppInstallState.GET
        }
    }

    fun launchInstalledApp(context: Context, packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("InstallStatusHelper", "Failed to launch $packageName: ${e.message}")
            false
        }
    }
}
