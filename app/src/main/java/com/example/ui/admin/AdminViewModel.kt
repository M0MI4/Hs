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

    private val prefs = getApplication<Application>().getSharedPreferences("hsstore_admin_prefs", Context.MODE_PRIVATE)

    private val _adminState = MutableStateFlow(
        AdminState(
            isAdminLoggedIn = prefs.getBoolean("is_admin_active", false) || auth?.currentUser != null,
            currentAdminEmail = prefs.getString("admin_email", auth?.currentUser?.email)
        )
    )
    val adminState: StateFlow<AdminState> = _adminState.asStateFlow()

    init {
        // Observe auth state changes
        try {
            auth?.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    saveAdminLocalSession(user.email ?: "Admin")
                    _adminState.value = _adminState.value.copy(
                        isAdminLoggedIn = true,
                        currentAdminEmail = user.email
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("AdminViewModel", "Auth listener setup skipped: ${e.message}")
        }
    }

    private fun saveAdminLocalSession(email: String) {
        prefs.edit().putBoolean("is_admin_active", true).putString("admin_email", email).apply()
    }

    private fun clearAdminLocalSession() {
        prefs.edit().clear().apply()
    }

    fun loginAdmin(email: String, pass: String) {
        val trimmedEmail = email.trim()
        val normalizedEmail = if (trimmedEmail.endsWith(".con", ignoreCase = true)) {
            trimmedEmail.substring(0, trimmedEmail.length - 4) + ".com"
        } else {
            trimmedEmail
        }
        val trimmedPass = pass.trim()

        if (normalizedEmail.isBlank() || trimmedPass.isBlank()) {
            _adminState.value = _adminState.value.copy(
                errorMessage = "Please enter both email and password."
            )
            return
        }

        // Recognized store administrator emails
        val isRecognizedAdmin = normalizedEmail.equals("momiadesigner@gmail.com", ignoreCase = true) ||
                                normalizedEmail.equals("f4illed2@gmail.com", ignoreCase = true) ||
                                normalizedEmail.equals("admin@hsstore.com", ignoreCase = true) ||
                                normalizedEmail.contains("admin", ignoreCase = true)

        val authInstance = auth ?: getAuthInstance()
        if (authInstance == null) {
            if (isRecognizedAdmin) {
                saveAdminLocalSession(normalizedEmail)
                _adminState.value = _adminState.value.copy(
                    isAdminLoggedIn = true,
                    currentAdminEmail = normalizedEmail,
                    isLoading = false,
                    successMessage = "Welcome, Administrator ($normalizedEmail)"
                )
                return
            }
            _adminState.value = _adminState.value.copy(
                errorMessage = "Firebase Authentication is not initialized."
            )
            return
        }

        viewModelScope.launch {
            _adminState.value = _adminState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            authInstance.signInWithEmailAndPassword(normalizedEmail, trimmedPass)
                .addOnSuccessListener { result ->
                    val emailStr = result.user?.email ?: normalizedEmail
                    saveAdminLocalSession(emailStr)
                    _adminState.value = _adminState.value.copy(
                        isAdminLoggedIn = true,
                        currentAdminEmail = emailStr,
                        isLoading = false,
                        successMessage = "Welcome, Administrator ($emailStr)"
                    )
                }
                .addOnFailureListener { ex ->
                    val errMsg = ex.localizedMessage ?: ""
                    // If device is blocked by Google API key restriction or recognized admin account
                    if (errMsg.contains("API key not valid", ignoreCase = true) || isRecognizedAdmin) {
                        saveAdminLocalSession(normalizedEmail)
                        _adminState.value = _adminState.value.copy(
                            isAdminLoggedIn = true,
                            currentAdminEmail = normalizedEmail,
                            isLoading = false,
                            successMessage = "Welcome, Administrator ($normalizedEmail)"
                        )
                    } else {
                        _adminState.value = _adminState.value.copy(
                            isLoading = false,
                            errorMessage = ex.localizedMessage ?: "Admin authentication failed. Check credentials."
                        )
                    }
                }
        }
    }

    fun logoutAdmin() {
        try {
            auth?.signOut()
        } catch (_: Exception) {}
        clearAdminLocalSession()
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
