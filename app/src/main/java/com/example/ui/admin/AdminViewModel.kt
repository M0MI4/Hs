package com.example.ui.admin

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppUpdateConfig
import com.example.data.model.ModApp
import com.example.data.model.NewsPost
import com.example.data.repository.CatalogRepository
import com.example.data.repository.NewsRepository
import com.example.data.repository.UpdateRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

data class AdminState(
    val isAdminLoggedIn: Boolean = false,
    val currentAdminEmail: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class PartialApkInfo(
    val title: String,
    val packageName: String,
    val version: String,
    val versionCode: Int,
    val sizeBytes: Long,
    val sizeDisplay: String,
    val sha256: String
)

class AdminViewModel(
    application: Application
) : AndroidViewModel(application) {

    private fun getAuthInstance(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            try {
                com.google.firebase.FirebaseApp.initializeApp(getApplication())
                FirebaseAuth.getInstance()
            } catch (ex: Exception) {
                null
            }
        }
    }

    private val auth: FirebaseAuth? = getAuthInstance()
    val catalogRepository: CatalogRepository = CatalogRepository(application.applicationContext)
    val newsRepository: NewsRepository = NewsRepository(application.applicationContext)
    val updateRepository: UpdateRepository = UpdateRepository(application.applicationContext)

    private val _adminState = MutableStateFlow(
        AdminState(
            isAdminLoggedIn = auth?.currentUser != null,
            currentAdminEmail = auth?.currentUser?.email
        )
    )
    val adminState: StateFlow<AdminState> = _adminState.asStateFlow()

    init {
        // Observe auth state changes
        try {
            auth?.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                _adminState.value = _adminState.value.copy(
                    isAdminLoggedIn = user != null,
                    currentAdminEmail = user?.email
                )
            }
        } catch (e: Exception) {
            Log.w("AdminViewModel", "Auth listener setup skipped: ${e.message}")
        }
    }

    fun loginAdmin(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _adminState.value = _adminState.value.copy(
                errorMessage = "Please enter both email and password."
            )
            return
        }

        val authInstance = auth ?: getAuthInstance()
        if (authInstance == null) {
            _adminState.value = _adminState.value.copy(
                errorMessage = "Firebase Authentication is not initialized."
            )
            return
        }

        viewModelScope.launch {
            _adminState.value = _adminState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            authInstance.signInWithEmailAndPassword(email.trim(), pass)
                .addOnSuccessListener { result ->
                    _adminState.value = _adminState.value.copy(
                        isAdminLoggedIn = true,
                        currentAdminEmail = result.user?.email,
                        isLoading = false,
                        successMessage = "Welcome, Administrator (${result.user?.email ?: "Admin"})"
                    )
                }
                .addOnFailureListener { ex ->
                    _adminState.value = _adminState.value.copy(
                        isLoading = false,
                        errorMessage = ex.localizedMessage ?: "Admin authentication failed. Check credentials."
                    )
                }
        }
    }

    fun logoutAdmin() {
        try {
            auth?.signOut()
        } catch (_: Exception) {}
        _adminState.value = AdminState(
            isAdminLoggedIn = false,
            currentAdminEmail = null,
            successMessage = "Logged out from Admin Studio."
        )
    }

    fun clearMessages() {
        _adminState.value = _adminState.value.copy(errorMessage = null, successMessage = null)
    }

    fun saveApp(app: ModApp, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _adminState.value = _adminState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = catalogRepository.saveApp(app)
            if (result.isSuccess) {
                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    successMessage = "App '${app.name}' successfully published to Firestore!"
                )
                onComplete()
            } else {
                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to save app"
                )
            }
        }
    }

    fun deleteApp(appId: String, appName: String = "App") {
        viewModelScope.launch {
            _adminState.value = _adminState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = catalogRepository.deleteApp(appId)
            _adminState.value = _adminState.value.copy(
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.localizedMessage,
                successMessage = if (result.isSuccess) "Deleted '$appName' from catalog" else null
            )
        }
    }

    fun saveNews(post: NewsPost, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _adminState.value = _adminState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = newsRepository.saveNews(post)
            if (result.isSuccess) {
                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    successMessage = "Announcement broadcasted to /news successfully!"
                )
                onComplete()
            } else {
                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to broadcast story"
                )
            }
        }
    }

    fun deleteNews(newsId: String) {
        viewModelScope.launch {
            _adminState.value = _adminState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = newsRepository.deleteNews(newsId)
            _adminState.value = _adminState.value.copy(
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.localizedMessage,
                successMessage = if (result.isSuccess) "Story deleted from Firestore" else null
            )
        }
    }

    fun saveStoreConfig(config: AppUpdateConfig, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _adminState.value = _adminState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = updateRepository.saveUpdateConfig(config)
            if (result.isSuccess) {
                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    successMessage = "Store version config updated (Latest: v${config.storeLatestVersion})!"
                )
                onComplete()
            } else {
                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to update store config"
                )
            }
        }
    }

    /**
     * Reads a selected local APK URI, copies to cache, parses package information, and calculates its SHA-256 hash.
     */
    suspend fun parseLocalApkFromUri(context: Context, uri: Uri): PartialApkInfo? = withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, "admin_import_${System.currentTimeMillis()}.apk")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            parseLocalApk(context, cacheFile)
        } catch (e: Exception) {
            Log.e("AdminViewModel", "Error importing APK: ${e.message}", e)
            null
        } finally {
            try { cacheFile.delete() } catch (_: Exception) {}
        }
    }

    /**
     * Reads an APK file, parses package information, and calculates its SHA-256 hash.
     */
    fun parseLocalApk(context: Context, apkFile: File): PartialApkInfo? {
        val pm = context.packageManager
        val info = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_META_DATA) ?: return null
        val appInfo = info.applicationInfo
        if (appInfo != null) {
            appInfo.sourceDir = apkFile.absolutePath
            appInfo.publicSourceDir = apkFile.absolutePath
        }

        val appLabel = try {
            if (appInfo != null) pm.getApplicationLabel(appInfo).toString()
            else apkFile.nameWithoutExtension.replace("_", " ").replace("-", " ")
        } catch (e: Exception) {
            apkFile.nameWithoutExtension.replace("_", " ").replace("-", " ")
        }

        val packageName = info.packageName
        val versionName = info.versionName ?: "1.0.0"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode
        }

        val sizeBytes = apkFile.length()
        val sizeDisplay = "%.1f MB".format(sizeBytes / (1024.0 * 1024.0))
        val sha256 = calculateSha256(apkFile)

        return PartialApkInfo(
            title = if (appLabel.isNotBlank()) appLabel else packageName,
            packageName = packageName,
            version = versionName,
            versionCode = versionCode,
            sizeBytes = sizeBytes,
            sizeDisplay = sizeDisplay,
            sha256 = sha256
        )
    }

    private fun calculateSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("AdminViewModel", "SHA256 calculation error: ${e.message}")
            ""
        }
    }
}
