package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainTab
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalizedStrings
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

private val TabBarShape = RoundedCornerShape(32.dp)
private val ActivePillShape = RoundedCornerShape(26.dp)
private val TabItemShape = RoundedCornerShape(24.dp)

/**
 * Isolated iOS Liquid Glass Floating Tab Bar
 * Uses Kyant0/AndroidLiquidGlass drawBackdrop (vibrancy + blur + lens) on Android 13+ (API 33+)
 * and gracefully falls back to a translucent Surface on Android 12 and below (API < 33).
 */
@Composable
fun CupertinoFloatingTabBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    currentLanguage: AppLanguage,
    isDarkTheme: Boolean,
    activeDownloadsCount: Int,
    backdrop: LayerBackdrop? = null,
    modifier: Modifier = Modifier
) {
    val useLiquidGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backdrop != null

    val specularBorderBrush = remember(isDarkTheme) {
        Brush.linearGradient(
            colors = if (isDarkTheme) {
                listOf(
                    Color.White.copy(alpha = 0.36f),
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.24f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.30f),
                    Color.White.copy(alpha = 0.65f)
                )
            }
        )
    }

    val containerModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .height(64.dp)
        .testTag("cupertino_floating_tab_bar")

    if (useLiquidGlass && backdrop != null) {
        val tintColor = if (isDarkTheme) {
            Color.Black.copy(alpha = 0.28f)
        } else {
            Color.White.copy(alpha = 0.55f)
        }

        Box(
            modifier = containerModifier
                .shadow(
                    elevation = if (isDarkTheme) 10.dp else 12.dp,
                    shape = TabBarShape,
                    ambientColor = Color.Black.copy(alpha = 0.18f),
                    spotColor = Color.Black.copy(alpha = 0.18f)
                )
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { TabBarShape },
                    effects = {
                        vibrancy()
                        blur(20f.dp.toPx())
                        lens(
                            refractionHeight = 12f.dp.toPx(),
                            refractionAmount = 16f.dp.toPx(),
                            chromaticAberration = false
                        )
                    },
                    onDrawSurface = {
                        drawRect(tintColor)
                    }
                )
                .border(1.dp, specularBorderBrush, TabBarShape)
                .clip(TabBarShape)
        ) {
            TabBarContent(
                currentTab = currentTab,
                onTabSelected = onTabSelected,
                currentLanguage = currentLanguage,
                isDarkTheme = isDarkTheme,
                activeDownloadsCount = activeDownloadsCount
            )
        }
    } else {
        val fallbackBg = if (isDarkTheme) {
            Color(0xFF1C1C1E).copy(alpha = 0.92f)
        } else {
            Color(0xFFFFFFFF).copy(alpha = 0.95f)
        }

        val fallbackBorder = if (isDarkTheme) {
            Color(0xFF38383A)
        } else {
            Color(0xFFE5E5EA)
        }

        Surface(
            shape = TabBarShape,
            color = fallbackBg,
            shadowElevation = if (isDarkTheme) 6.dp else 8.dp,
            border = BorderStroke(0.5.dp, fallbackBorder),
            modifier = containerModifier
        ) {
            TabBarContent(
                currentTab = currentTab,
                onTabSelected = onTabSelected,
                currentLanguage = currentLanguage,
                isDarkTheme = isDarkTheme,
                activeDownloadsCount = activeDownloadsCount
            )
        }
    }
}

@Composable
private fun TabBarContent(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    currentLanguage: AppLanguage,
    isDarkTheme: Boolean,
    activeDownloadsCount: Int
) {
    val tabs = MainTab.entries
    val selectedIndex = tabs.indexOf(currentTab).coerceIn(0, tabs.lastIndex)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        val tabWidth = maxWidth / tabs.size.toFloat()
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "active_tab_pill_offset"
        )

        val pillFillColor = if (isDarkTheme) {
            Color.White.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        }
        val pillBorderColor = if (isDarkTheme) {
            Color.White.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        }

        // Smooth spring-animated floating active pill behind the selected tab
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .padding(2.dp)
                .graphicsLayer {
                    clip = true
                    shape = ActivePillShape
                }
                .background(pillFillColor, ActivePillShape)
                .border(0.75.dp, pillBorderColor, ActivePillShape)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CupertinoTabItem(
                isSelected = currentTab == MainTab.TODAY,
                onClick = { onTabSelected(MainTab.TODAY) },
                label = LocalizedStrings.get("tab_today", currentLanguage),
                selectedIcon = Icons.Filled.Today,
                unselectedIcon = Icons.Outlined.Today,
                modifier = Modifier.weight(1f)
            )

            CupertinoTabItem(
                isSelected = currentTab == MainTab.APPS,
                onClick = { onTabSelected(MainTab.APPS) },
                label = LocalizedStrings.get("tab_apps", currentLanguage),
                selectedIcon = Icons.Filled.Apps,
                unselectedIcon = Icons.Outlined.Apps,
                modifier = Modifier.weight(1f)
            )

            CupertinoTabItem(
                isSelected = currentTab == MainTab.DOWNLOADS,
                onClick = { onTabSelected(MainTab.DOWNLOADS) },
                label = LocalizedStrings.get("tab_downloads", currentLanguage),
                selectedIcon = Icons.Filled.Download,
                unselectedIcon = Icons.Outlined.Download,
                badgeCount = activeDownloadsCount,
                modifier = Modifier.weight(1f)
            )

            CupertinoTabItem(
                isSelected = currentTab == MainTab.SETTINGS,
                onClick = { onTabSelected(MainTab.SETTINGS) },
                label = LocalizedStrings.get("tab_settings", currentLanguage),
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CupertinoTabItem(
    isSelected: Boolean,
    onClick: () -> Unit,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "tab_scale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF8E8E93),
        label = "tab_icon_color"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF8E8E93),
        label = "tab_text_color"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(TabItemShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (badgeCount > 0) {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Text("$badgeCount", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            ) {
                Icon(
                    imageVector = if (isSelected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Icon(
                imageVector = if (isSelected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
