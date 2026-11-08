package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ModApp

@Entity(tableName = "mod_apps")
data class ModAppEntity(
    @PrimaryKey val id: String,
    val name: String,
    val packageName: String,
    val category: String,
    val version: String,
    val versionCode: Int,
    val sizeBytes: Long,
    val sizeDisplay: String,
    val iconUrl: String,
    val downloadUrl: String,
    val sha256: String,
    val description: String,
    val modFeatures: List<String>,
    val changelog: String,
    val rating: Float,
    val downloadsCount: Int,
    val isVerified: Boolean,
    val author: String,
    val updatedDate: String,
    val permissionsRequired: List<String>
) {
    fun toDomain(): ModApp {
        return ModApp(
            id = id,
            name = name,
            packageName = packageName,
            category = category,
            version = version,
            versionCode = versionCode,
            sizeBytes = sizeBytes,
            sizeDisplay = sizeDisplay,
            iconUrl = iconUrl,
            downloadUrl = downloadUrl,
            sha256 = sha256,
            description = description,
            modFeatures = modFeatures,
            changelog = changelog,
            rating = rating,
            downloadsCount = downloadsCount,
            isVerified = isVerified,
            author = author,
            updatedDate = updatedDate,
            permissionsRequired = permissionsRequired
        )
    }

    companion object {
        fun fromDomain(app: ModApp): ModAppEntity {
            return ModAppEntity(
                id = app.id,
                name = app.name,
                packageName = app.packageName,
                category = app.category,
                version = app.version,
                versionCode = app.versionCode,
                sizeBytes = app.sizeBytes,
                sizeDisplay = app.sizeDisplay,
                iconUrl = app.iconUrl,
                downloadUrl = app.downloadUrl,
                sha256 = app.sha256,
                description = app.description,
                modFeatures = app.modFeatures,
                changelog = app.changelog,
                rating = app.rating,
                downloadsCount = app.downloadsCount,
                isVerified = app.isVerified,
                author = app.author,
                updatedDate = app.updatedDate,
                permissionsRequired = app.permissionsRequired
            )
        }
    }
}
