package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ModApp
import com.example.ui.components.CupertinoPullToRefreshBox
import com.example.ui.components.CupertinoSearchField
import com.example.ui.components.CupertinoSegmentedControl
import com.example.ui.components.ModAppCard
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalizedStrings

@Composable
fun AppsScreen(
    apps: List<ModApp>,
    bookmarkedIds: Set<String>,
    isRefreshing: Boolean = false,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    isOnline: Boolean = true,
    onRefresh: () -> Unit = {},
    onAppSelected: (ModApp) -> Unit,
    onDownloadApp: (ModApp) -> Unit,
    onBookmarkToggle: (ModApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val rawCategories = listOf("All", "Games", "Tools", "Tweaks", "Emulators", "Media")
    val categoryKeys = listOf("cat_all", "cat_games", "cat_tools", "cat_tweaks", "cat_emulators", "cat_media")
    val localizedCategories = remember(currentLanguage) {
        categoryKeys.map { LocalizedStrings.get(it, currentLanguage) }
    }

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(apps, selectedCategoryIndex, searchQuery) {
        val selectedRawCat = rawCategories[selectedCategoryIndex]
        var list = apps
        if (selectedRawCat != "All") {
            list = list.filter { it.category.equals(selectedRawCat, ignoreCase = true) }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.author.lowercase().contains(q)
            }
        }
        list
    }

    val searchPlaceholder = remember(currentLanguage, localizedCategories, selectedCategoryIndex) {
        val prefix = LocalizedStrings.get("apps_search_prefix", currentLanguage)
        val cat = localizedCategories.getOrElse(selectedCategoryIndex) { "Apps" }
        try { String.format(prefix, cat) } catch (e: Exception) { "Search in $cat..." }
    }

    val categoryHeader = remember(localizedCategories, selectedCategoryIndex, filteredApps.size) {
        val cat = localizedCategories.getOrElse(selectedCategoryIndex) { "All" }.uppercase()
        "$cat (${filteredApps.size})"
    }

    CupertinoPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("apps_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
        ) {
            // iOS Large Title Header
            item(key = "header_title") {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = LocalizedStrings.get("apps_kicker", currentLanguage),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = LocalizedStrings.get("apps_title", currentLanguage),
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp
                            )
                        )

                        if (!isOnline) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                            com.example.ui.components.OfflineHeaderBadge(
                                isOnline = false,
                                currentLanguage = currentLanguage
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Capsule
                    CupertinoSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = searchPlaceholder
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Segmented Control
                    CupertinoSegmentedControl(
                        items = localizedCategories,
                        selectedIndex = selectedCategoryIndex,
                        onItemSelected = { selectedCategoryIndex = it }
                    )
                }
            }

            item(key = "header_category_count") {
                Text(
                    text = categoryHeader,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (apps.isEmpty() && isRefreshing) {
                items(
                    count = 5,
                    key = { "skeleton_$it" },
                    contentType = { "skeleton" }
                ) {
                    Box(modifier = Modifier.padding(vertical = 5.dp)) {
                        com.example.ui.components.CupertinoModCardSkeleton()
                    }
                }
            } else if (filteredApps.isEmpty()) {
                item(key = "empty_search_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            text = LocalizedStrings.get("search_no_results", currentLanguage),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
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
}
