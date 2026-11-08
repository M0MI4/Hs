package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteModAppDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "packageName") val packageName: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "versionCode") val versionCode: Int? = null,
    @Json(name = "sizeBytes") val sizeBytes: Long? = null,
    @Json(name = "sizeDisplay") val sizeDisplay: String? = null,
    @Json(name = "iconUrl") val iconUrl: String? = null,
    @Json(name = "downloadUrl") val downloadUrl: String? = null,
    @Json(name = "sha256") val sha256: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "modFeatures") val modFeatures: List<String>? = null,
    @Json(name = "changelog") val changelog: String? = null,
    @Json(name = "rating") val rating: Float? = null,
    @Json(name = "downloadsCount") val downloadsCount: Int? = null,
    @Json(name = "isVerified") val isVerified: Boolean? = null,
    @Json(name = "author") val author: String? = null,
    @Json(name = "updatedDate") val updatedDate: String? = null,
    @Json(name = "permissionsRequired") val permissionsRequired: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class RemoteUpdateDto(
    @Json(name = "storeLatestVersion") val storeLatestVersion: String? = null,
    @Json(name = "storeLatestVersionCode") val storeLatestVersionCode: Int? = null,
    @Json(name = "currentVersionCode") val currentVersionCode: Int? = null,
    @Json(name = "mandatory") val mandatory: Boolean? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "announcementMessage") val announcementMessage: String? = null,
    @Json(name = "downloadUrl") val downloadUrl: String? = null,
    @Json(name = "sizeDisplay") val sizeDisplay: String? = null,
    @Json(name = "sha256") val sha256: String? = null,
    @Json(name = "publishedAt") val publishedAt: String? = null,
    @Json(name = "changelog") val changelog: List<String>? = null,
    @Json(name = "showDialog") val showDialog: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class RemoteNewsDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "subtitle") val subtitle: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "bannerUrl") val bannerUrl: String? = null,
    @Json(name = "tag") val tag: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "readTime") val readTime: String? = null,
    @Json(name = "author") val author: String? = null,
    @Json(name = "authorRole") val authorRole: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "bulletPoints") val bulletPoints: List<String>? = null,
    @Json(name = "linkedAppId") val linkedAppId: String? = null,
    @Json(name = "linkedAppName") val linkedAppName: String? = null,
    @Json(name = "isPinned") val isPinned: Boolean? = null,
    @Json(name = "isBreaking") val isBreaking: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class RemoteStorePayload(
    @Json(name = "storeUpdate") val storeUpdate: RemoteUpdateDto? = null,
    @Json(name = "apps") val apps: List<RemoteModAppDto>? = null,
    @Json(name = "announcements") val announcements: List<RemoteNewsDto>? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "lastUpdated") val lastUpdated: String? = null
)
