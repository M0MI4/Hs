package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import com.example.util.AppInstallState
import com.example.util.InstallStatusHelper
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ModApp
import com.example.ui.theme.VerifiedBadgeGreen

// Top-level static shapes to eliminate per-frame allocations during scrolling
private val ModCardShape = RoundedCornerShape(16.dp)
private val ModIconSquircleShape = RoundedCornerShape(14.dp)

@Composable
fun ModAppCard(
    app: ModApp,
    isBookmarked: Boolean,
    onAppClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installState = remember(app.packageName, app.versionCode) {
        InstallStatusHelper.getAppInstallState(context, app.packageName, app.versionCode)
    }

    val bylineText = remember(app.author, app.category) {
        "${app.author} • ${app.category}"
    }

    val ratingText = remember(app.rating) {
        "%.1f".format(app.rating)
    }

    val modFeaturesText = remember(app.modFeatures) {
        if (app.modFeatures.isNotEmpty()) "⚡ " + app.modFeatures.joinToString(" • ") else ""
    }

    val imageRequest = remember(app.iconUrl, app.packageName, app.versionCode) {
        if (app.iconUrl.isNotBlank()) {
            ImageRequest.Builder(context)
                .data(app.iconUrl)
                .size(186, 186)
                .memoryCacheKey("mod_icon_${app.packageName}_${app.versionCode}")
                .diskCacheKey("mod_icon_${app.packageName}_${app.versionCode}")
                .allowHardware(true)
                .crossfade(false)
                .build()
        } else null
    }

    Surface(
        shape = ModCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(ModCardShape)
            .clickable(onClick = onAppClick)
            .testTag("mod_card_${app.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // iOS App Icon (Squircle shape)
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(ModIconSquircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageRequest != null) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = "${app.name} icon",
                            modifier = Modifier.size(62.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // App Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (app.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Verified Mod",
                                tint = VerifiedBadgeGreen,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = bylineText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFF9500),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = ratingText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${app.sizeDisplay}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Dynamic Action Capsule Button (GET, UPDATE, OPEN)
                CupertinoGetButton(
                    text = installState.label,
                    onClick = {
                        if (installState == AppInstallState.OPEN) {
                            val launched = InstallStatusHelper.launchInstalledApp(context, app.packageName)
                            if (!launched) onDownloadClick()
                        } else {
                            onDownloadClick()
                        }
                    }
                )

                // Bookmark Icon
                IconButton(
                    onClick = onBookmarkToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("bookmark_btn_${app.id}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) Color(0xFFFF2D55) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (modFeaturesText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 76.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = modFeaturesText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 76.dp)
                )
            }
        }
    }
}
