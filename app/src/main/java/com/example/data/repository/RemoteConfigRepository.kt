package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.data.model.DeviceConfigItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class RemoteConfigRepository(private val context: Context) {

    private val httpClient = OkHttpClient()

    val currentDeviceId = "dev_" + Build.FINGERPRINT.hashCode().toString().takeLast(6)
    val currentDeviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    private val _syncedConfig = MutableStateFlow<String>(getDefaultConfigJson())
    val syncedConfig: StateFlow<String> = _syncedConfig.asStateFlow()

    private val _remoteDevices = MutableStateFlow<List<DeviceConfigItem>>(emptyList())
    val remoteDevices: StateFlow<List<DeviceConfigItem>> = _remoteDevices.asStateFlow()

    private val _syncStatus = MutableStateFlow<String>("Ready to sync")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    init {
        // Initial registered mock device
        _remoteDevices.value = listOf(
            DeviceConfigItem(
                id = "dev_phone_primary",
                userId = "current",
                deviceId = currentDeviceId,
                deviceName = "$currentDeviceName (This Device)",
                installedApps = listOf("app_cyber_vpn", "app_retro_emu"),
                customConfigJson = getDefaultConfigJson()
            ),
            DeviceConfigItem(
                id = "dev_tablet_sync",
                userId = "current",
                deviceId = "dev_tab_8921",
                deviceName = "Samsung Galaxy Tab S9 (Synced)",
                installedApps = listOf("app_music_stream", "app_shadow_rpg", "app_photo_fx"),
                customConfigJson = """{"autoUpdate": true, "theme": "CYBERPUNK_NEON", "particleDensity": "Dense"}"""
            )
        )
    }

    private fun getDefaultConfigJson(): String {
        val root = JSONObject()
        root.put("deviceId", currentDeviceId)
        root.put("deviceName", currentDeviceName)
        root.put("autoUpdateCheck", true)
        root.put("verifySha256Checksums", true)
        root.put("particleEffectsEnabled", true)
        root.put("particleDensity", "Medium")
        root.put("activeTheme", "CYBERPUNK_NEON")
        root.put("customCatalogUrl", "https://raw.githubusercontent.com/hsstore/mods-repo/main/catalog.json")
        root.put("remoteSyncProtocol", "v2.0-JSON")

        val installed = JSONArray()
        installed.put("app_cyber_vpn")
        root.put("installedModPackages", installed)

        return root.toString(2)
    }

    suspend fun importRemoteConfigFromUrl(url: String): Result<String> = withContext(Dispatchers.IO) {
        _syncStatus.value = "Fetching remote JSON from $url..."
        try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = "HTTP ${response.code} error fetching remote JSON"
                _syncStatus.value = err
                return@withContext Result.failure(Exception(err))
            }
            val content = response.body?.string() ?: ""
            // Validate JSON
            JSONObject(content)
            _syncedConfig.value = content
            _syncStatus.value = "Successfully applied remote configuration from $url"
            Result.success(content)
        } catch (e: Exception) {
            _syncStatus.value = "Import failed: ${e.localizedMessage}"
            Result.failure(e)
        }
    }

    fun applyCustomConfigJson(jsonText: String): Result<Unit> {
        return try {
            JSONObject(jsonText)
            _syncedConfig.value = jsonText
            _syncStatus.value = "Local settings updated from custom JSON editor."
            Result.success(Unit)
        } catch (e: Exception) {
            _syncStatus.value = "Invalid JSON format: ${e.message}"
            Result.failure(e)
        }
    }

    fun addDevice(deviceName: String, customJson: String) {
        val newDev = DeviceConfigItem(
            id = "dev_" + UUID.randomUUID().toString().take(6),
            userId = "current",
            deviceId = "dev_" + UUID.randomUUID().toString().take(8),
            deviceName = deviceName,
            installedApps = listOf("app_cyber_vpn"),
            customConfigJson = customJson
        )
        _remoteDevices.value = _remoteDevices.value + newDev
        _syncStatus.value = "Added remote device: $deviceName"
    }

    fun removeDevice(deviceId: String) {
        _remoteDevices.value = _remoteDevices.value.filter { it.deviceId != deviceId }
        _syncStatus.value = "Device disconnected."
    }
}
