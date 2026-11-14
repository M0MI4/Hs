package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.example.data.model.ModApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

enum class AppInstallState(val label: String) {
    NOT_INSTALLED("GET"),
    INSTALLED_LATEST("OPEN"),
    UPDATE_AVAILABLE("UPDATE"),
    SIGNATURE_MISMATCH("REPLACE")
}

data class InstalledAppInfo(
    val isInstalled: Boolean,
    val versionCode: Long = -1L,
    val signaturesSha256: List<String> = emptyList()
) {
    val primarySignatureSha256: String?
        get() = signaturesSha256.firstOrNull()
}

object InstalledAppDetector {

    const val DEFAULT_TRUSTED_MOD_SIGNATURE_SHA256 =
        "84A1CA341EFBF3029602477FE3B5C0AD21BD925E173606BAB46375F7AFCE07D0"

    private const val TAG = "InstalledAppDetector"

    private val appInfoCache = ConcurrentHashMap<String, InstalledAppInfo>()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _packageStateVersion = MutableStateFlow(0)
    val packageStateVersion: StateFlow<Int> = _packageStateVersion.asStateFlow()

    fun resolveTrustedSignature(expectedSignatureSha256: String): String {
        val cleaned = expectedSignatureSha256
            .trim()
            .replace(":", "")
            .uppercase()
        return cleaned.ifBlank { DEFAULT_TRUSTED_MOD_SIGNATURE_SHA256 }
    }

    fun inspectInstalledApp(context: Context, packageName: String): InstalledAppInfo {
        if (packageName.isBlank()) return InstalledAppInfo(isInstalled = false)
        return try {
            val pm = context.applicationContext.packageManager
            val packageInfo = getPackageInfoWithSigning(pm, packageName)
            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            val hashes = extractSigningSha256Hexes(packageInfo)
            InstalledAppInfo(
                isInstalled = true,
                versionCode = versionCode,
                signaturesSha256 = hashes
            )
        } catch (_: PackageManager.NameNotFoundException) {
            InstalledAppInfo(isInstalled = false)
        } catch (e: Exception) {
            Log.w(TAG, "Failed inspecting package $packageName: ${e.message}")
            InstalledAppInfo(isInstalled = false)
        }
    }

    fun getCachedOrInspect(context: Context, packageName: String): InstalledAppInfo {
        if (packageName.isBlank()) return InstalledAppInfo(isInstalled = false)
        appInfoCache[packageName]?.let { return it }
        val info = inspectInstalledApp(context, packageName)
        appInfoCache[packageName] = info
        return info
    }

    fun detectAppState(
        context: Context,
        app: ModApp
    ): AppInstallState {
        return detectAppState(
            context = context,
            packageName = app.packageName,
            storeVersionCode = app.versionCode,
            expectedSignatureSha256 = app.expectedSignatureSha256
        )
    }

    fun detectAppState(
        context: Context,
        packageName: String,
        storeVersionCode: Int,
        expectedSignatureSha256: String = ""
    ): AppInstallState {
        if (packageName.isBlank()) return AppInstallState.NOT_INSTALLED
        val info = getCachedOrInspect(context, packageName)
        return evaluateState(
            info = info,
            storeVersionCode = storeVersionCode,
            expectedSignatureSha256 = expectedSignatureSha256
        )
    }

    fun evaluateState(
        info: InstalledAppInfo,
        storeVersionCode: Int,
        expectedSignatureSha256: String = ""
    ): AppInstallState {
        if (!info.isInstalled) return AppInstallState.NOT_INSTALLED
        val trustedSig = resolveTrustedSignature(expectedSignatureSha256)
        val signatureMatches = info.signaturesSha256.any { it.equals(trustedSig, ignoreCase = true) }
        if (!signatureMatches) {
            return AppInstallState.SIGNATURE_MISMATCH
        }
        return if (info.versionCode >= storeVersionCode.toLong()) {
            AppInstallState.INSTALLED_LATEST
        } else {
            AppInstallState.UPDATE_AVAILABLE
        }
    }

    fun preloadPackages(context: Context, packageNames: Collection<String>) {
        val cleanNames = packageNames.filter { it.isNotBlank() }.distinct()
        if (cleanNames.isEmpty()) return
        scope.launch {
            var changed = false
            for (pkg in cleanNames) {
                val fresh = inspectInstalledApp(context, pkg)
                val previous = appInfoCache.put(pkg, fresh)
                if (previous != fresh) {
                    changed = true
                }
            }
            if (changed) {
                _packageStateVersion.value = _packageStateVersion.value + 1
            }
        }
    }

    fun refreshAllKnownPackages(context: Context, additionalPackages: Collection<String> = emptyList()) {
        val keys = (appInfoCache.keys + additionalPackages).filter { it.isNotBlank() }.distinct()
        scope.launch {
            for (pkg in keys) {
                appInfoCache[pkg] = inspectInstalledApp(context, pkg)
            }
            _packageStateVersion.value = _packageStateVersion.value + 1
        }
    }

    fun onPackageBroadcast(context: Context, packageName: String?) {
        if (packageName.isNullOrBlank()) {
            refreshAllKnownPackages(context)
            return
        }
        scope.launch {
            appInfoCache[packageName] = inspectInstalledApp(context, packageName)
            _packageStateVersion.value = _packageStateVersion.value + 1
        }
    }

    fun invalidateCache(packageName: String? = null) {
        if (packageName.isNullOrBlank()) {
            appInfoCache.clear()
        } else {
            appInfoCache.remove(packageName)
        }
        _packageStateVersion.value = _packageStateVersion.value + 1
    }

    fun updateInstalledPackage(packageName: String, versionCode: Int, signatureSha256: String = DEFAULT_TRUSTED_MOD_SIGNATURE_SHA256) {
        if (packageName.isBlank()) return
        appInfoCache[packageName] = InstalledAppInfo(
            isInstalled = true,
            versionCode = versionCode.toLong(),
            signaturesSha256 = listOf(signatureSha256.uppercase())
        )
        _packageStateVersion.value = _packageStateVersion.value + 1
    }

    fun uninstallPackage(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return try {
            val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch uninstall for $packageName: ${e.message}")
            false
        }
    }

    fun launchInstalledApp(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
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
            Log.e(TAG, "Failed to launch $packageName: ${e.message}")
            false
        }
    }

    private fun getPackageInfoWithSigning(pm: PackageManager, packageName: String): PackageInfo {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            }
            else -> {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }
        }
    }

    private fun extractSigningSha256Hexes(packageInfo: PackageInfo): List<String> {
        val signatureList = mutableListOf<Signature>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo
            if (signingInfo != null) {
                signingInfo.apkContentsSigners?.let { signatureList.addAll(it) }
                if (signingInfo.hasPastSigningCertificates()) {
                    signingInfo.signingCertificateHistory?.let { signatureList.addAll(it) }
                }
            }
        }
        if (signatureList.isEmpty()) {
            @Suppress("DEPRECATION")
            packageInfo.signatures?.let { signatureList.addAll(it) }
        }

        if (signatureList.isEmpty()) return emptyList()
        return signatureList.mapNotNull { sig ->
            computeSha256Hex(sig.toByteArray())
        }.distinct()
    }

    fun computeSha256Hex(bytes: ByteArray): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            val sb = StringBuilder(digest.size * 2)
            for (b in digest) {
                sb.append("%02X".format(b))
            }
            sb.toString()
        } catch (_: Exception) {
            null
        }
    }
}
