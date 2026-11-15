package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val TabBarShape = RoundedCornerShape(32.dp)
private val DropletShape = RoundedCornerShape(26.dp)
private val TabItemShape = RoundedCornerShape(24.dp)

/**
 * Interactive Liquid Glass Floating Tab Bar inspired by Kyant0/AndroidLiquidGlass LiquidBottomTabs.
 *
 * - Captures only the outer main content layer via [backdrop] (120fps LazyColumn scrolling preserved).
 * - On Android 13+ (API 33+): renders a frosted Liquid Glass pill container AND an interactive
 *   gliding Liquid Glass Droplet sitting BEHIND the crisp tab items with crystal-clear lens refraction
 *   (no blur on droplet), chromatic aberration, specular rim highlights, jelly velocity stretch/squash,
 *   and press/drag magnification.
 * - On Android 12 and below (API < 33): falls back gracefully to a translucent Surface + interactive
 *   glass capsule so older devices never crash while keeping drag/snap/jelly physics intact.
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
        ) {
            InteractiveLiquidTabBarContent(
                currentTab = currentTab,
                onTabSelected = onTabSelected,
                currentLanguage = currentLanguage,
                isDarkTheme = isDarkTheme,
                activeDownloadsCount = activeDownloadsCount,
                outerBackdrop = backdrop
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
            InteractiveLiquidTabBarContent(
                currentTab = currentTab,
                onTabSelected = onTabSelected,
                currentLanguage = currentLanguage,
                isDarkTheme = isDarkTheme,
                activeDownloadsCount = activeDownloadsCount,
                outerBackdrop = null
            )
        }
    }
}

@Composable
private fun InteractiveLiquidTabBarContent(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    currentLanguage: AppLanguage,
    isDarkTheme: Boolean,
    activeDownloadsCount: Int,
    outerBackdrop: LayerBackdrop?
) {
    val tabs = MainTab.entries
    val selectedIndex = tabs.indexOf(currentTab).coerceIn(0, tabs.lastIndex)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val coroutineScope = rememberCoroutineScope()
    val latestOnTabSelected by rememberUpdatedState(onTabSelected)
    val latestSelectedIndex by rememberUpdatedState(selectedIndex)

    var isPressed by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var jellyTarget by remember { mutableFloatStateOf(0f) }
    var hasInitializedOffset by remember { mutableStateOf(false) }
    var hoveredTabIndex by remember { mutableIntStateOf(selectedIndex) }
    val dropletOffsetPx = remember { Animatable(0f) }

    val isInteractive = isPressed || isDragging

    // Smooth press/drag droplet magnification (1.0f -> 1.22f)
    val dropletBaseScale by animateFloatAsState(
        targetValue = if (isInteractive) 1.22f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.58f,
            stiffness = 380f
        ),
        label = "droplet_press_scale"
    )

    // Smooth intensity blend for lens refraction & specular rim highlight (0f at rest -> 1f active)
    val interactiveIntensity by animateFloatAsState(
        targetValue = if (isInteractive) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 400f
        ),
        label = "droplet_interactive_intensity"
    )

    // Jelly horizontal stretch / vertical squash driven by drag velocity
    val jellyStretch by animateFloatAsState(
        targetValue = jellyTarget,
        animationSpec = spring(
            dampingRatio = 0.48f,
            stiffness = 420f
        ),
        label = "droplet_jelly_stretch"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        val tabWidth = maxWidth / tabs.size.toFloat()
        val tabWidthPx = with(density) { tabWidth.toPx() }.coerceAtLeast(1f)
        val totalWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val maxOffsetPx = (tabWidthPx * (tabs.size - 1)).coerceAtLeast(0f)

        // Sync droplet position when currentTab or layout width changes externally (or on initial layout)
        LaunchedEffect(selectedIndex, tabWidthPx) {
            val targetPx = (selectedIndex * tabWidthPx).coerceIn(0f, maxOffsetPx)
            if (!hasInitializedOffset) {
                dropletOffsetPx.snapTo(targetPx)
                hasInitializedOffset = true
            } else if (!isDragging) {
                dropletOffsetPx.animateTo(
                    targetValue = targetPx,
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = 350f
                    )
                )
            }
            hoveredTabIndex = selectedIndex
        }

        val activeVisualIndex = if (isDragging) hoveredTabIndex else selectedIndex

        // Interactive gesture surface wrapping the entire bar track
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Press detector using Initial pass so child tab clicks continue working seamlessly
                .pointerInput(tabs.size, isRtl) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                        } while (event.changes.any { it.pressed })
                        isPressed = false
                        if (!isDragging) {
                            jellyTarget = 0f
                        }
                    }
                }
                // Horizontal drag tracking + velocity-based jelly physics + nearest-tab spring snap
                .pointerInput(tabs.size, tabWidthPx, totalWidthPx, isRtl) {
                    val velocityTracker = VelocityTracker()
                    var lastHapticStep = latestSelectedIndex

                    detectHorizontalDragGestures(
                        onDragStart = { startOffset ->
                            isDragging = true
                            velocityTracker.resetTracking()
                            val logicalX = if (isRtl) totalWidthPx - startOffset.x else startOffset.x
                            val startTargetPx = (logicalX - tabWidthPx / 2f).coerceIn(0f, maxOffsetPx)
                            coroutineScope.launch {
                                dropletOffsetPx.snapTo(startTargetPx)
                            }
                            val startIndex = (startTargetPx / tabWidthPx).roundToInt().coerceIn(0, tabs.lastIndex)
                            hoveredTabIndex = startIndex
                            lastHapticStep = startIndex
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)

                            val logicalDelta = if (isRtl) -dragAmount else dragAmount
                            val nextOffset = (dropletOffsetPx.value + logicalDelta).coerceIn(0f, maxOffsetPx)
                            coroutineScope.launch {
                                dropletOffsetPx.snapTo(nextOffset)
                            }

                            val dragSpeed = abs(logicalDelta) / 22f
                            jellyTarget = dragSpeed.coerceIn(0f, 0.24f)

                            val nextHovered = (nextOffset / tabWidthPx).roundToInt().coerceIn(0, tabs.lastIndex)
                            hoveredTabIndex = nextHovered
                            if (nextHovered != lastHapticStep) {
                                lastHapticStep = nextHovered
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = {
                            val velocityX = velocityTracker.calculateVelocity().x
                            velocityTracker.resetTracking()
                            val logicalVelocity = if (isRtl) -velocityX else velocityX
                            val projectedOffset = (dropletOffsetPx.value + logicalVelocity * 0.045f)
                                .coerceIn(0f, maxOffsetPx)
                            val nearestIndex = (projectedOffset / tabWidthPx)
                                .roundToInt()
                                .coerceIn(0, tabs.lastIndex)

                            isDragging = false
                            isPressed = false
                            jellyTarget = 0f
                            hoveredTabIndex = nearestIndex
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            latestOnTabSelected(tabs[nearestIndex])
                            coroutineScope.launch {
                                dropletOffsetPx.animateTo(
                                    targetValue = (nearestIndex * tabWidthPx).coerceIn(0f, maxOffsetPx),
                                    animationSpec = spring(
                                        dampingRatio = 0.6f,
                                        stiffness = 350f
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            velocityTracker.resetTracking()
                            isDragging = false
                            isPressed = false
                            jellyTarget = 0f
                            val fallbackIndex = latestSelectedIndex
                            hoveredTabIndex = fallbackIndex
                            coroutineScope.launch {
                                dropletOffsetPx.animateTo(
                                    targetValue = (fallbackIndex * tabWidthPx).coerceIn(0f, maxOffsetPx),
                                    animationSpec = spring(
                                        dampingRatio = 0.6f,
                                        stiffness = 350f
                                    )
                                )
                            }
                        }
                    )
                }
        ) {
            // Layer 1: Moving Liquid Glass Droplet sitting BEHIND the tab items (no blur on droplet)
            val dropletSpecularRim = remember(isDarkTheme, interactiveIntensity) {
                val boost = 0.25f * interactiveIntensity
                Brush.linearGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            Color.White.copy(alpha = (0.58f + boost).coerceAtMost(0.95f)),
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = (0.34f + boost * 0.5f).coerceAtMost(0.75f))
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = (0.85f + boost * 0.5f).coerceAtMost(1f)),
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = (0.60f + boost * 0.5f).coerceAtMost(0.95f))
                        )
                    }
                )
            }

            val dropletScaleX = dropletBaseScale * (1f + jellyStretch)
            val dropletScaleY = dropletBaseScale * (1f - jellyStretch * 0.45f)

            val dropletBaseModifier = Modifier
                .offset { IntOffset(x = dropletOffsetPx.value.roundToInt(), y = 0) }
                .width(tabWidth)
                .fillMaxHeight()
                .padding(2.dp)
                .graphicsLayer {
                    scaleX = dropletScaleX
                    scaleY = dropletScaleY
                }
                .testTag("liquid_glass_droplet")

            if (outerBackdrop != null) {
                val surfaceTint = if (isDarkTheme) {
                    Color.White.copy(alpha = 0.12f + 0.08f * interactiveIntensity)
                } else {
                    Color.White.copy(alpha = 0.22f + 0.10f * interactiveIntensity)
                }

                Box(
                    modifier = dropletBaseModifier
                        .shadow(
                            elevation = (4f + 6f * interactiveIntensity).dp,
                            shape = DropletShape,
                            ambientColor = Color.Black.copy(alpha = 0.14f),
                            spotColor = Color.Black.copy(alpha = 0.14f)
                        )
                        .drawBackdrop(
                            backdrop = outerBackdrop,
                            shape = { DropletShape },
                            effects = {
                                vibrancy()
                                lens(
                                    refractionHeight = (10f + 6f * interactiveIntensity).dp.toPx(),
                                    refractionAmount = (14f + 12f * interactiveIntensity).dp.toPx(),
                                    chromaticAberration = true
                                )
                            },
                            onDrawSurface = {
                                drawRect(surfaceTint)
                            }
                        )
                        .border(
                            width = (1f + 0.4f * interactiveIntensity).dp,
                            brush = dropletSpecularRim,
                            shape = DropletShape
                        )
                )
            } else {
                // Graceful fallback droplet for API < 33 devices
                val fallbackFill = if (isDarkTheme) {
                    Color.White.copy(alpha = 0.14f + 0.06f * interactiveIntensity)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f + 0.06f * interactiveIntensity)
                }
                Box(
                    modifier = dropletBaseModifier
                        .graphicsLayer {
                            clip = true
                            shape = DropletShape
                        }
                        .background(fallbackFill, DropletShape)
                        .border(
                            width = (0.9f + 0.4f * interactiveIntensity).dp,
                            brush = dropletSpecularRim,
                            shape = DropletShape
                        )
                )
            }

            // Layer 2: Crisp tab icons and labels sitting cleanly ON TOP of the droplet
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CupertinoTabItem(
                    isSelected = activeVisualIndex == 0,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(MainTab.TODAY)
                    },
                    label = LocalizedStrings.get("tab_today", currentLanguage),
                    selectedIcon = Icons.Filled.Today,
                    unselectedIcon = Icons.Outlined.Today,
                    testTag = "tab_today",
                    modifier = Modifier.weight(1f)
                )

                CupertinoTabItem(
                    isSelected = activeVisualIndex == 1,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(MainTab.APPS)
                    },
                    label = LocalizedStrings.get("tab_apps", currentLanguage),
                    selectedIcon = Icons.Filled.Apps,
                    unselectedIcon = Icons.Outlined.Apps,
                    testTag = "tab_apps",
                    modifier = Modifier.weight(1f)
                )

                CupertinoTabItem(
                    isSelected = activeVisualIndex == 2,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(MainTab.DOWNLOADS)
                    },
                    label = LocalizedStrings.get("tab_downloads", currentLanguage),
                    selectedIcon = Icons.Filled.Download,
                    unselectedIcon = Icons.Outlined.Download,
                    badgeCount = activeDownloadsCount,
                    testTag = "tab_downloads",
                    modifier = Modifier.weight(1f)
                )

                CupertinoTabItem(
                    isSelected = activeVisualIndex == 3,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(MainTab.SETTINGS)
                    },
                    label = LocalizedStrings.get("tab_settings", currentLanguage),
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    testTag = "tab_settings",
                    modifier = Modifier.weight(1f)
                )
            }
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
    testTag: String,
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
            }
            .testTag(testTag),
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
