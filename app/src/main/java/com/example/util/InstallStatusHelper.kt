package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

enum class AppInstallState(val label: String) {
    GET("GET"),
    UPDATE("UPDATE"),
    OPEN("OPEN")
}

object InstallStatusHelper {

    // In-memory cache mapping packageName -> versionCode (-1 represents verified not installed)
    private val installedPackagesCache = ConcurrentHashMap<String, Int>()
    @Volatile private var isCacheFullyInitialized = false
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Preloads all installed applications in the background once to completely eliminate
     * main-thread Binder IPC calls and NameNotFoundException overhead during list scrolling.
     */
    fun preloadInstalledPackages(context: Context) {
        scope.launch {
            try {
                val pm = context.applicationContext.packageManager
                val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstalledPackages(0)
                }

                for (pInfo in packages) {
                    val pName = pInfo.packageName ?: continue
                    val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        pInfo.versionCode
                    }
                    installedPackagesCache[pName] = vCode
                }
                isCacheFullyInitialized = true
            } catch (e: Exception) {
                Log.w("InstallStatusHelper", "Failed to batch preload packages: ${e.message}")
            }
        }
    }

    /**
     * O(1) in-memory lookup. Zero Binder IPC overhead when scrolling.
     */
    fun getAppInstallState(context: Context, packageName: String, remoteVersionCode: Int): AppInstallState {
        if (packageName.isBlank()) return AppInstallState.GET

        // If the cache was fully populated, any package not in cache is not installed
        if (isCacheFullyInitialized) {
            val installedVersion = installedPackagesCache[packageName] ?: return AppInstallState.GET
            return if (installedVersion < remoteVersionCode) AppInstallState.UPDATE else AppInstallState.OPEN
        }

        // Check if individual query was already cached
        val cached = installedPackagesCache[packageName]
        if (cached != null) {
            if (cached < 0) return AppInstallState.GET
            return if (cached < remoteVersionCode) AppInstallState.UPDATE else AppInstallState.OPEN
        }

        // Trigger asynchronous full cache population if not already started
        preloadInstalledPackages(context)

        // Fallback for first query before batch completes
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

            installedPackagesCache[packageName] = installedVersionCode
            if (installedVersionCode < remoteVersionCode) AppInstallState.UPDATE else AppInstallState.OPEN
        } catch (e: PackageManager.NameNotFoundException) {
            installedPackagesCache[packageName] = -1
            AppInstallState.GET
        } catch (e: Exception) {
            AppInstallState.GET
        }
    }

    fun updateInstalledPackage(packageName: String, versionCode: Int) {
        installedPackagesCache[packageName] = versionCode
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
