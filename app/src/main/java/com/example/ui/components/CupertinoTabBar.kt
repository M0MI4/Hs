package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainTab
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalizedStrings

/**
 * Authentic Apple iOS Floating Glass Tab Bar
 * Displays elevated, rounded floating dock with blur/glass effect and responsive tap scaling
 */
@Composable
fun CupertinoFloatingTabBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    currentLanguage: AppLanguage,
    isDarkTheme: Boolean,
    activeDownloadsCount: Int,
    modifier: Modifier = Modifier
) {
    val glassBg = if (isDarkTheme) {
        Color(0xFF1C1C1E).copy(alpha = 0.92f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.95f)
    }

    val glassBorder = if (isDarkTheme) {
        Color(0xFF38383A)
    } else {
        Color(0xFFE5E5EA)
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = glassBg,
        shadowElevation = if (isDarkTheme) 6.dp else 8.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, glassBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(62.dp)
            .testTag("cupertino_floating_tab_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CupertinoTabItem(
                tab = MainTab.TODAY,
                isSelected = currentTab == MainTab.TODAY,
                onClick = { onTabSelected(MainTab.TODAY) },
                label = LocalizedStrings.get("tab_today", currentLanguage),
                selectedIcon = Icons.Filled.Today,
                unselectedIcon = Icons.Outlined.Today
            )

            CupertinoTabItem(
                tab = MainTab.APPS,
                isSelected = currentTab == MainTab.APPS,
                onClick = { onTabSelected(MainTab.APPS) },
                label = LocalizedStrings.get("tab_apps", currentLanguage),
                selectedIcon = Icons.Filled.Apps,
                unselectedIcon = Icons.Outlined.Apps
            )

            CupertinoTabItem(
                tab = MainTab.DOWNLOADS,
                isSelected = currentTab == MainTab.DOWNLOADS,
                onClick = { onTabSelected(MainTab.DOWNLOADS) },
                label = LocalizedStrings.get("tab_downloads", currentLanguage),
                selectedIcon = Icons.Filled.Download,
                unselectedIcon = Icons.Outlined.Download,
                badgeCount = activeDownloadsCount
            )

            CupertinoTabItem(
                tab = MainTab.SETTINGS,
                isSelected = currentTab == MainTab.SETTINGS,
                onClick = { onTabSelected(MainTab.SETTINGS) },
                label = LocalizedStrings.get("tab_settings", currentLanguage),
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings
            )
        }
    }
}

@Composable
private fun CupertinoTabItem(
    tab: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    badgeCount: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.05f else 1.0f, label = "tab_scale")

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF8E8E93),
        label = "tab_icon_color"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF8E8E93),
        label = "tab_text_color"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .scale(scale),
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
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Icon(
                imageVector = if (isSelected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}
