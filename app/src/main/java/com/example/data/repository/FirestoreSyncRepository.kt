package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.DeviceConfigItem
import com.example.data.model.FeedbackReportItem
import com.example.data.model.UserBookmarkItem
import com.example.data.model.UserDownloadRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class FirestoreSyncRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val currentUserEmail: String?
        get() = auth.currentUser?.email

    val isUserSignedIn: Boolean
        get() = auth.currentUser != null

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // 1. Observe Bookmarks
    fun observeBookmarks(): Flow<List<UserBookmarkItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/bookmarks"
        emitAll(
            db.collection("users").document(uid).collection("bookmarks")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(UserBookmarkItem::class.java) }
                .catch { error ->
                    if (error is CancellationException) throw error
                    handleFirestoreError(error as? Exception ?: Exception(error), OperationType.LIST, path)
                    throw error
                }
        )
    }

    // Toggle Bookmark
    suspend fun toggleBookmark(appId: String, appName: String): Boolean {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("bookmarks").document(appId)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.delete().await()
                false
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "appId" to appId,
                    "appName" to appName,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // 2. Observe Download Records
    fun observeUserDownloads(): Flow<List<UserDownloadRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/downloads"
        emitAll(
            db.collection("users").document(uid).collection("downloads")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(UserDownloadRecord::class.java) }
                .catch { error ->
                    if (error is CancellationException) throw error
                    handleFirestoreError(error as? Exception ?: Exception(error), OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun recordDownload(appId: String, appName: String, version: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("downloads").document(appId)
        val payload = mapOf(
            "userId" to uid,
            "appId" to appId,
            "appName" to appName,
            "version" to version,
            "status" to "COMPLETED",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun incrementAppDownloadCount(appId: String) {
        if (appId.isBlank() || appId == "app_hsstore_update") return
        val docRef = db.collection("apps").document(appId)
        try {
            docRef.update("downloadCount", FieldValue.increment(1L)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Non-fatal telemetry update (e.g., offline or local bundled item not yet seeded)
        }
    }

    // 3. Observe Remote Device Configs
    fun observeDeviceConfigs(): Flow<List<DeviceConfigItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/device_configs"
        emitAll(
            db.collection("users").document(uid).collection("device_configs")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(DeviceConfigItem::class.java) }
                .catch { error ->
                    if (error is CancellationException) throw error
                    handleFirestoreError(error as? Exception ?: Exception(error), OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun syncDeviceConfig(deviceId: String, deviceName: String, installedApps: List<String>, customConfigJson: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("device_configs").document(deviceId)
        val payload = mapOf(
            "userId" to uid,
            "deviceId" to deviceId,
            "deviceName" to deviceName,
            "installedApps" to installedApps,
            "customConfigJson" to customConfigJson,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    companion object {
        const val SUPPORT_EMAIL = "momiadesigner@gmail.com"
        private val webhookClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()
        }
    }

    // 4. Submit Feedback or Broken Link Report -> /feedback + optional /config/feedback_config webhook
    suspend fun submitFeedback(
        type: String,
        subject: String,
        details: String,
        deviceInfo: String,
        appName: String = ""
    ): String {
        val uid = auth.currentUser?.uid ?: "ANONYMOUS_USER"
        val docRef = db.collection("feedback").document()
        val payload = mapOf(
            "userId" to uid,
            "type" to type,
            "subject" to subject,
            "details" to details,
            "appName" to appName,
            "recipientEmail" to SUPPORT_EMAIL,
            "deviceInfo" to deviceInfo,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }

        // Optional silent background email webhook delivery (/config/feedback_config -> emailWebhookUrl)
        deliverViaWebhookIfConfigured(
            ticketId = docRef.id,
            type = type,
            subject = subject,
            details = details,
            deviceInfo = deviceInfo,
            appName = appName
        )

        return docRef.id
    }

    suspend fun getFeedbackWebhookUrl(): String {
        return try {
            val snap = db.collection("config").document("feedback_config").get().await()
            snap.getString("emailWebhookUrl")?.trim().orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun saveFeedbackWebhookUrl(url: String): Result<Unit> = runCatching {
        db.collection("config").document("feedback_config")
            .set(mapOf("emailWebhookUrl" to url.trim()), SetOptions.merge())
            .await()
    }

    private suspend fun deliverViaWebhookIfConfigured(
        ticketId: String,
        type: String,
        subject: String,
        details: String,
        deviceInfo: String,
        appName: String
    ) {
        val webhookUrl = getFeedbackWebhookUrl()
        if (webhookUrl.isBlank() || !webhookUrl.startsWith("http", ignoreCase = true)) return

        withContext(Dispatchers.IO) {
            try {
                val formattedBody = buildString {
                    appendLine("Ticket ID: $ticketId")
                    appendLine("Report Type: $type")
                    if (appName.isNotBlank()) appendLine("App Name: $appName")
                    appendLine("Recipient: $SUPPORT_EMAIL")
                    appendLine("Device: $deviceInfo")
                    appendLine()
                    appendLine("Message:")
                    appendLine(details)
                }
                val jsonPayload = JSONObject().apply {
                    put("to", SUPPORT_EMAIL)
                    put("_replyto", SUPPORT_EMAIL)
                    put("_subject", "HsStore Feedback / Report: $subject")
                    put("subject", "HsStore Feedback / Report: $subject")
                    put("from_name", "HsStore Android App")
                    put("type", type)
                    put("appName", appName)
                    put("deviceInfo", deviceInfo)
                    put("ticketId", ticketId)
                    put("message", formattedBody)
                }.toString()

                val request = Request.Builder()
                    .url(webhookUrl)
                    .addHeader("Accept", "application/json")
                    .post(jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                webhookClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w("FirestoreSyncRepo", "Webhook POST returned HTTP ${response.code}")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("FirestoreSyncRepo", "Background webhook delivery failed (non-fatal): ${e.message}")
            }
        }
    }
}
