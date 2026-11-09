package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ModApp
import com.example.data.model.NewsPost
import com.example.data.model.NewsType
import com.example.data.repository.NewsSource
import com.example.ui.components.CupertinoGetButton
import com.example.ui.components.CupertinoPullToRefreshBox
import com.example.ui.components.CupertinoSearchField
import com.example.ui.components.CupertinoSegmentedControl
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalizedStrings
import com.example.ui.theme.iOSSystemBlue
import com.example.ui.theme.iOSSystemGreen
import com.example.ui.theme.iOSSystemIndigo
import com.example.ui.theme.iOSSystemOrange
import com.example.ui.theme.iOSSystemPurple
import com.example.ui.theme.iOSSystemRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayNewsScreen(
    newsList: List<NewsPost>,
    isRefreshing: Boolean,
    statusMessage: String,
    currentSource: NewsSource,
    activeNewsUrl: String,
    particlesEnabled: Boolean,
    appsCatalog: List<ModApp>,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    isOnline: Boolean = true,
    onRefreshNews: () -> Unit,
    onToggleParticles: () -> Unit,
    onDownloadLinkedApp: (ModApp) -> Unit,
    onNavigateToApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }

    val filterOptions = listOf(
        LocalizedStrings.get("today_filter_all", currentLanguage),
        LocalizedStrings.get("today_filter_banners", currentLanguage),
        LocalizedStrings.get("today_filter_text", currentLanguage),
        LocalizedStrings.get("today_filter_patch", currentLanguage)
    )

    var selectedStoryForDetail by remember { mutableStateOf<NewsPost?>(null) }

    val todayFormatted = remember(currentLanguage) {
        val locale = if (currentLanguage == AppLanguage.KURDISH_SORANI) Locale("ku") else Locale.getDefault()
        val sdf = SimpleDateFormat("EEEE, MMMM d", locale)
        sdf.format(Date()).uppercase(locale)
    }

    // Filter and search
    val filteredNews = remember(newsList, searchQuery, selectedFilterIndex) {
        var list = newsList
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                it.subtitle.lowercase().contains(q) ||
                it.content.lowercase().contains(q) ||
                it.tag.lowercase().contains(q) ||
                it.author.lowercase().contains(q) ||
                it.bulletPoints.any { b -> b.lowercase().contains(q) }
            }
        }
        when (selectedFilterIndex) {
            1 -> list.filter { it.type == NewsType.BANNER || it.bannerUrl.isNotBlank() }
            2 -> list.filter { it.type == NewsType.TEXT_ONLY && it.bannerUrl.isBlank() }
            3 -> list.filter { it.type == NewsType.PATCH_NOTES || it.bulletPoints.isNotEmpty() }
            else -> list
        }
    }

    CupertinoPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefreshNews,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("today_news_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
        ) {
            // 1. iOS Today Header: Date Kicker + Large Title + Particle Toggle
            item(key = "today_header") {
                Column(modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)) {
                    Text(
                        text = todayFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 0.8.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = LocalizedStrings.get("today_title", currentLanguage),
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 34.sp,
                                    letterSpacing = (-0.5).sp
                                )
                            )

                            if (!isOnline) {
                                Spacer(modifier = Modifier.width(8.dp))
                                com.example.ui.components.OfflineHeaderBadge(
                                    isOnline = false,
                                    currentLanguage = currentLanguage
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Pull-to-refresh manual trigger icon
                            IconButton(
                                onClick = onRefreshNews,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = LocalizedStrings.get("action_refresh", currentLanguage),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Search Field & Segmented Control
            item(key = "today_controls") {
                CupertinoSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = LocalizedStrings.get("today_search_placeholder", currentLanguage),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                CupertinoSegmentedControl(
                    items = filterOptions,
                    selectedIndex = selectedFilterIndex,
                    onItemSelected = { selectedFilterIndex = it },
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            // 3. News Items: Banner News & Just Text News
            if (filteredNews.isEmpty()) {
                item(key = "today_empty") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Feed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = LocalizedStrings.get("today_no_stories", currentLanguage),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = LocalizedStrings.get("today_no_stories_sub", currentLanguage),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredNews,
                    key = { it.id },
                    contentType = { it.type.name }
                ) { newsItem ->
                    Box(modifier = Modifier.padding(bottom = 16.dp)) {
                        when (newsItem.type) {
                            NewsType.BANNER -> {
                                BannerNewsCard(
                                    news = newsItem,
                                    currentLanguage = currentLanguage,
                                    onCardClick = { selectedStoryForDetail = newsItem },
                                    appsCatalog = appsCatalog,
                                    onDownloadLinkedApp = onDownloadLinkedApp
                                )
                            }
                            NewsType.TEXT_ONLY -> {
                                TextOnlyNewsCard(
                                    news = newsItem,
                                    currentLanguage = currentLanguage,
                                    onCardClick = { selectedStoryForDetail = newsItem }
                                )
                            }
                            NewsType.PATCH_NOTES -> {
                                PatchNotesCard(
                                    news = newsItem,
                                    onCardClick = { selectedStoryForDetail = newsItem }
                                )
                            }
                            NewsType.SPOTLIGHT -> {
                                BannerNewsCard(
                                    news = newsItem,
                                    currentLanguage = currentLanguage,
                                    onCardClick = { selectedStoryForDetail = newsItem },
                                    appsCatalog = appsCatalog,
                                    onDownloadLinkedApp = onDownloadLinkedApp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 5. iOS Modal Bottom Sheet: Full Story Reading View
    if (selectedStoryForDetail != null) {
        NewsDetailModalSheet(
            news = selectedStoryForDetail!!,
            currentLanguage = currentLanguage,
            appsCatalog = appsCatalog,
            onDismiss = { selectedStoryForDetail = null },
            onDownloadLinkedApp = onDownloadLinkedApp
        )
    }
}

/**
 * Kind 1: News with Banner (Apple App Store Hero Card)
 */
@Composable
fun BannerNewsCard(
    news: NewsPost,
    currentLanguage: AppLanguage,
    onCardClick: () -> Unit,
    appsCatalog: List<ModApp>,
    onDownloadLinkedApp: (ModApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val linkedApp = remember(news.linkedAppId, appsCatalog) {
        if (news.linkedAppId != null) {
            appsCatalog.find { it.id == news.linkedAppId }
        } else null
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onCardClick)
            .testTag("news_banner_card_${news.id}")
    ) {
        Column {
            // Hero Image Container with Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (news.bannerUrl.isNotBlank()) {
                    AsyncImage(
                        model = news.bannerUrl,
                        contentDescription = news.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(iOSSystemIndigo, iOSSystemPurple)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                // Top Tag & Read time pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = news.tag.uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = news.readTime,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Content Section
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = news.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        lineHeight = 24.sp
                    )
                )

                if (news.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = news.subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Byline & Linked App or Read Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(iOSSystemBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = news.author.take(1),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = news.author,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = "${news.authorRole} • ${news.date}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    if (linkedApp != null) {
                        CupertinoGetButton(
                            text = LocalizedStrings.get("action_get", currentLanguage),
                            onClick = { onDownloadLinkedApp(linkedApp) }
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable(onClick = onCardClick)
                        ) {
                            Text(
                                text = LocalizedStrings.get("action_read_story", currentLanguage),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Kind 2: Just Text News (Apple Editorial Column / Bulletin)
 */
@Composable
fun TextOnlyNewsCard(
    news: NewsPost,
    currentLanguage: AppLanguage,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        shadowElevation = 1.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onCardClick)
            .testTag("news_text_card_${news.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Tag Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        news.isBreaking -> iOSSystemRed.copy(alpha = 0.15f)
                        news.tag.contains("SECURITY", ignoreCase = true) -> iOSSystemOrange.copy(alpha = 0.15f)
                        else -> iOSSystemIndigo.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = news.tag.uppercase(),
                        color = when {
                            news.isBreaking -> iOSSystemRed
                            news.tag.contains("SECURITY", ignoreCase = true) -> iOSSystemOrange
                            else -> iOSSystemIndigo
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "${news.date} • ${news.readTime}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large Title
            Text(
                text = news.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 23.sp
                )
            )

            if (news.subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = news.subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body Snippet
            Text(
                text = news.content,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Bullet highlights preview
            if (news.bulletPoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    news.bulletPoints.take(2).forEach { bullet ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = bullet,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Divider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                thickness = 0.5.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Author & Read Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By ${news.author} (${news.authorRole})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Text(
                    text = LocalizedStrings.get("action_read_more", currentLanguage),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

/**
 * Kind 3: Patch Notes & Changelog Card
 */
@Composable
fun PatchNotesCard(
    news: NewsPost,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        shadowElevation = 1.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onCardClick)
            .testTag("news_patch_card_${news.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = iOSSystemGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "VERSION LOG",
                        color = iOSSystemGreen,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = news.date,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = news.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bullet list of changes
            if (news.bulletPoints.isNotEmpty()) {
                news.bulletPoints.forEach { point ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = iOSSystemGreen,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = point,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            } else {
                Text(
                    text = news.content,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    ),
                    maxLines = 3
                )
            }
        }
    }
}

/**
 * iOS Modal Bottom Sheet for Reading the Full Story
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailModalSheet(
    news: NewsPost,
    currentLanguage: AppLanguage,
    appsCatalog: List<ModApp>,
    onDismiss: () -> Unit,
    onDownloadLinkedApp: (ModApp) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val linkedApp = remember(news.linkedAppId, appsCatalog) {
        if (news.linkedAppId != null) appsCatalog.find { it.id == news.linkedAppId } else null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        },
        modifier = Modifier.statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // If banner image present, full-width banner
            if (news.bannerUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    AsyncImage(
                        model = news.bannerUrl,
                        contentDescription = news.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                // Header tags & close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = news.tag.uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Share story intent
                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TITLE, news.title)
                                    putExtra(Intent.EXTRA_TEXT, "${news.title}\n\n${news.content}\n\nvia HsStore News")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share story")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = LocalizedStrings.get("action_share", currentLanguage),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = LocalizedStrings.get("action_close", currentLanguage),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = news.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp
                    )
                )

                if (news.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = news.subtitle,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Author Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(iOSSystemBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = news.author.take(1),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = news.author,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${news.authorRole} • Published ${news.date} • ${news.readTime}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Full Story Content
                Text(
                    text = news.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Bullet Points / Highlights
                if (news.bulletPoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = LocalizedStrings.get("today_key_highlights", currentLanguage),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    news.bulletPoints.forEach { item ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = iOSSystemGreen,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            )
                        }
                    }
                }

                // Linked App Card (Download directly from story)
                if (linkedApp != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = LocalizedStrings.get("today_featured_mod", currentLanguage),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = linkedApp.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${linkedApp.version} • ${linkedApp.sizeDisplay}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            CupertinoGetButton(
                                text = LocalizedStrings.get("action_get", currentLanguage),
                                onClick = {
                                    onDownloadLinkedApp(linkedApp)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
