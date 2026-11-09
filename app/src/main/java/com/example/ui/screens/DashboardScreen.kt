package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ModApp
import com.example.data.repository.CatalogSource
import com.example.data.repository.SortOption
import com.example.ui.components.CupertinoGetButton
import com.example.ui.components.CupertinoSearchField
import com.example.ui.components.ModAppCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    apps: List<ModApp>,
    bookmarkedIds: Set<String>,
    isLoading: Boolean,
    statusMessage: String?,
    currentSource: CatalogSource,
    activeRemoteUrl: String,
    particlesEnabled: Boolean,
    hasUpdateAvailable: Boolean,
    onAppSelected: (ModApp) -> Unit,
    onDownloadApp: (ModApp) -> Unit,
    onBookmarkToggle: (ModApp) -> Unit,
    onOpenUpdateDialog: () -> Unit,
    onSyncRetrofit: () -> Unit,
    onToggleParticles: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf(SortOption.POPULAR) }
    var showSortMenu by remember { mutableStateOf(false) }

    val todayFormatted = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        sdf.format(Date()).uppercase()
    }

    val filteredApps = remember(apps, searchQuery, selectedSort) {
        var list = apps
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.author.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.modFeatures.any { feat -> feat.lowercase().contains(q) }
            }
        }
        when (selectedSort) {
            SortOption.POPULAR -> list.sortedByDescending { it.downloadsCount }
            SortOption.RATING -> list.sortedByDescending { it.rating }
            SortOption.SIZE -> list.sortedBy { it.sizeBytes }
            SortOption.RECENT -> list.sortedByDescending { it.versionCode }
        }
    }

    val featuredApp = remember(apps) {
        apps.find { it.id == "app_retro_emu" } ?: apps.firstOrNull()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
    ) {
        // iOS Today Header (Date kicker + Large Title)
        item(key = "dashboard_header") {
            Column(modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = todayFormatted,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 34.sp,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick Particle Toggle Icon
                        Surface(
                            onClick = onToggleParticles,
                            shape = CircleShape,
                            color = if (particlesEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp).testTag("quick_particle_toggle")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (particlesEnabled) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                    contentDescription = "Toggle Particles",
                                    tint = if (particlesEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Refresh / Sync Icon
                        Surface(
                            onClick = onSyncRetrofit,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp).testTag("refresh_catalog_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Update notification banner pill
                if (hasUpdateAvailable) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        onClick = onOpenUpdateDialog,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth().testTag("header_update_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Store Update Available",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                            }
                            Text(
                                text = "View Details",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // iOS Search Field Capsule
                CupertinoSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Games, Apps, Tools and more"
                )
            }
        }

        // iOS Hero Card (Featured Today)
        if (featuredApp != null && searchQuery.isEmpty()) {
            item(key = "featured_${featuredApp.id}") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onAppSelected(featuredApp) }
                        .padding(bottom = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "FEATURED MOD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp,
                                fontSize = 11.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = featuredApp.name,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp
                            )
                        )

                        Text(
                            text = featuredApp.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            ),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "v${featuredApp.version} • ${featuredApp.sizeDisplay}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace
                                )
                            )

                            CupertinoGetButton(
                                text = "GET",
                                isHighlighted = true,
                                onClick = { onDownloadApp(featuredApp) }
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Apps List
        item(key = "popular_mods_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "POPULAR MODS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 12.sp
                    )
                )

                Box {
                    IconButton(onClick = { showSortMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Sort", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(text = { Text("Most Popular") }, onClick = { selectedSort = SortOption.POPULAR; showSortMenu = false })
                        DropdownMenuItem(text = { Text("Highest Rated") }, onClick = { selectedSort = SortOption.RATING; showSortMenu = false })
                        DropdownMenuItem(text = { Text("Smallest Size") }, onClick = { selectedSort = SortOption.SIZE; showSortMenu = false })
                    }
                }
            }
        }

        // App rows or Shimmer Skeletons
        if (apps.isEmpty() && isLoading) {
            items(
                count = 4,
                key = { "dashboard_skeleton_$it" },
                contentType = { "skeleton" }
            ) {
                Box(modifier = Modifier.padding(vertical = 5.dp)) {
                    com.example.ui.components.CupertinoModCardSkeleton()
                }
            }
        } else {
            items(
                items = filteredApps,
                key = { it.id },
                contentType = { "mod_app_card" }
            ) { app ->
                Box(modifier = Modifier.padding(vertical = 5.dp)) {
                    ModAppCard(
                        app = app,
                        isBookmarked = bookmarkedIds.contains(app.id),
                        onAppClick = { onAppSelected(app) },
                        onDownloadClick = { onDownloadApp(app) },
                        onBookmarkToggle = { onBookmarkToggle(app) }
                    )
                }
            }
        }
    }
}
