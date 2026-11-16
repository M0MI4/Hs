package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.lerp
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
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val TabBarShape = RoundedCornerShape(32.dp)
private val DropletShape = RoundedCornerShape(26.dp)
private val TabItemShape = RoundedCornerShape(24.dp)

private const val HOLD_ACTIVATION_DELAY_MS = 150L

/**
 * Apple iOS 26 Liquid Glass Floating Tab Bar using Kyant0/AndroidLiquidGlass two-backdrop pattern:
 *
 * 1. `pageBackdrop` (passed as [backdrop]) is blurred (`blur(22.dp)`) + tinted on the outer tab bar
 *    container so raw page text behind the bar never shows sharply.
 * 2. Local `tabBarBackdrop` captures BOTH the already-blurred outer bar background AND the crisp
 *    tab icons/labels row.
 * 3. Resting / Quick Tap State: renders a sleek near-transparent dark glass pill with a 1.dp specular
 *    border and zero lens distortion.
 * 4. Hold (>150ms) / Drag State: inflates the moving Water Droplet OVER `tabBarBackdrop` with zero
 *    droplet blur (`blur = 0`) and full optical `lens(depthEffect = true, chromaticAberration = true)`
 *    warping + RGB edge dispersion directly over the tab icons and labels.
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
    val pageBackdrop = backdrop
    val useLiquidGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && pageBackdrop != null
    val tabBarBackdrop = if (useLiquidGlass) rememberLayerBackdrop() else null

    val outerSpecularBorder = remember(isDarkTheme) {
        Brush.linearGradient(
            colors = if (isDarkTheme) {
                listOf(
                    Color.White.copy(alpha = 0.42f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.14f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.88f),
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.55f)
                )
            }
        )
    }

    val containerModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .height(64.dp)
        .shadow(
            elevation = if (isDarkTheme) 12.dp else 14.dp,
            shape = TabBarShape,
            ambientColor = Color.Black.copy(alpha = 0.20f),
            spotColor = Color.Black.copy(alpha = 0.20f)
        )
        .testTag("cupertino_floating_tab_bar")

    val tabs = MainTab.entries
    val selectedIndex = tabs.indexOf(currentTab).coerceIn(0, tabs.lastIndex)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val coroutineScope = rememberCoroutineScope()
    val latestOnTabSelected by rememberUpdatedState(onTabSelected)
    val latestSelectedIndex by rememberUpdatedState(selectedIndex)

    var isPointerDown by remember { mutableStateOf(false) }
    var isHoldActivated by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var latestTouchX by remember { mutableFloatStateOf(0f) }
    var jellyTarget by remember { mutableFloatStateOf(0f) }
    var hoveredTabIndex by remember { mutableIntStateOf(selectedIndex) }
    val animatedTabIndex = remember { Animatable(selectedIndex.toFloat()) }

    // Trigger water-droplet inflation after holding (>150ms)
    LaunchedEffect(isPointerDown) {
        if (isPointerDown) {
            delay(HOLD_ACTIVATION_DELAY_MS)
            if (isPointerDown && !isHoldActivated) {
                isHoldActivated = true
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } else {
            isHoldActivated = false
        }
    }

    // Smooth glide to tapped tab driven solely by selectedIndex so finger-up never interrupts animation
    LaunchedEffect(selectedIndex) {
        if (!isDragging) {
            hoveredTabIndex = selectedIndex
            animatedTabIndex.animateTo(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = 280f
                )
            )
        }
    }

    val isDropletLensTarget by remember(selectedIndex) {
        derivedStateOf {
            isDragging || isHoldActivated || abs(animatedTabIndex.value - selectedIndex) > 0.05f
        }
    }

    val lensIntensity by animateFloatAsState(
        targetValue = if (isDropletLensTarget) 1f else 0f,
        animationSpec = tween(
            durationMillis = 340,
            easing = FastOutSlowInEasing
        ),
        label = "droplet_lens_intensity"
    )

    val dropletBaseScale = 1f + 0.12f * lensIntensity

    val jellyStretch by animateFloatAsState(
        targetValue = jellyTarget,
        animationSpec = spring(
            dampingRatio = 0.48f,
            stiffness = 420f
        ),
        label = "droplet_jelly_stretch"
    )

    BoxWithConstraints(modifier = containerModifier) {
        val innerHorizontalPadding = 6.dp
        val innerVerticalPadding = 5.dp
        val trackWidth = (maxWidth - innerHorizontalPadding * 2).coerceAtLeast(1.dp)
        val tabWidth = trackWidth / tabs.size.toFloat()
        val tabWidthPx = with(density) { tabWidth.toPx() }.coerceAtLeast(1f)
        val trackWidthPx = with(density) { trackWidth.toPx() }.coerceAtLeast(1f)
        val paddingStartPx = with(density) { innerHorizontalPadding.toPx() }
        val maxTabIndex = tabs.lastIndex.toFloat()
        val maxOffsetPx = (tabWidthPx * (tabs.size - 1)).coerceAtLeast(0f)

        // Move droplet under finger on >150ms hold; return cleanly if released without dragging
        LaunchedEffect(isHoldActivated) {
            if (isHoldActivated && !isDragging) {
                val localTrackX = latestTouchX - paddingStartPx
                val logicalX = if (isRtl) trackWidthPx - localTrackX else localTrackX
                val targetIndexFloat = ((logicalX - tabWidthPx / 2f) / tabWidthPx).coerceIn(0f, maxTabIndex)
                animatedTabIndex.animateTo(
                    targetValue = targetIndexFloat,
                    animationSpec = spring(
                        dampingRatio = 0.65f,
                        stiffness = 400f
                    )
                )
            } else if (!isHoldActivated && !isDragging) {
                animatedTabIndex.animateTo(
                    targetValue = latestSelectedIndex.toFloat(),
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = 280f
                    )
                )
            }
        }

        val activeVisualIndex by remember(selectedIndex) {
            derivedStateOf {
                if (isDragging) {
                    hoveredTabIndex
                } else {
                    animatedTabIndex.value.roundToInt().coerceIn(0, tabs.lastIndex)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // Hold detector (>150ms) via Initial pass so single taps pass cleanly to tab items
                .pointerInput(tabs.size, isRtl) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        latestTouchX = down.position.x
                        isPointerDown = true
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val primary = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                            if (primary != null) {
                                latestTouchX = primary.position.x
                            }
                        } while (event.changes.any { it.pressed })
                        isPointerDown = false
                        if (!isDragging) {
                            jellyTarget = 0f
                        }
                    }
                }
                // Horizontal drag tracking directly under user's finger (`change.position.x`) + jelly physics
                .pointerInput(tabs.size, tabWidthPx, trackWidthPx, paddingStartPx, isRtl) {
                    val velocityTracker = VelocityTracker()
                    var lastHapticStep = latestSelectedIndex

                    detectHorizontalDragGestures(
                        onDragStart = { startOffset ->
                            isDragging = true
                            latestTouchX = startOffset.x
                            velocityTracker.resetTracking()
                            val localTrackX = startOffset.x - paddingStartPx
                            val logicalX = if (isRtl) trackWidthPx - localTrackX else localTrackX
                            val startIndexFloat = ((logicalX - tabWidthPx / 2f) / tabWidthPx).coerceIn(0f, maxTabIndex)
                            coroutineScope.launch {
                                animatedTabIndex.snapTo(startIndexFloat)
                            }
                            val startIndex = startIndexFloat.roundToInt().coerceIn(0, tabs.lastIndex)
                            hoveredTabIndex = startIndex
                            lastHapticStep = startIndex
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            latestTouchX = change.position.x
                            velocityTracker.addPosition(change.uptimeMillis, change.position)

                            val localTrackX = change.position.x - paddingStartPx
                            val logicalX = if (isRtl) trackWidthPx - localTrackX else localTrackX
                            val nextIndexFloat = ((logicalX - tabWidthPx / 2f) / tabWidthPx).coerceIn(0f, maxTabIndex)
                            coroutineScope.launch {
                                animatedTabIndex.snapTo(nextIndexFloat)
                            }

                            val logicalDelta = if (isRtl) -dragAmount else dragAmount
                            val dragSpeed = abs(logicalDelta) / 22f
                            jellyTarget = dragSpeed.coerceIn(0f, 0.24f)

                            val nextHovered = nextIndexFloat.roundToInt().coerceIn(0, tabs.lastIndex)
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
                            val projectedIndex = (animatedTabIndex.value + (logicalVelocity * 0.045f) / tabWidthPx)
                                .coerceIn(0f, maxTabIndex)
                            val nearestIndex = projectedIndex
                                .roundToInt()
                                .coerceIn(0, tabs.lastIndex)

                            isDragging = false
                            isHoldActivated = false
                            isPointerDown = false
                            jellyTarget = 0f
                            hoveredTabIndex = nearestIndex
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            latestOnTabSelected(tabs[nearestIndex])
                            coroutineScope.launch {
                                animatedTabIndex.animateTo(
                                    targetValue = nearestIndex.toFloat(),
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
                            isHoldActivated = false
                            isPointerDown = false
                            jellyTarget = 0f
                            val fallbackIndex = latestSelectedIndex
                            hoveredTabIndex = fallbackIndex
                            coroutineScope.launch {
                                animatedTabIndex.animateTo(
                                    targetValue = fallbackIndex.toFloat(),
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
            // LAYER 1 (Captured into local tabBarBackdrop):
            //   1) Blurred outer bar background (pageBackdrop with blur(22.dp))
            //   2) Crisp tab icons and text labels Row
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (tabBarBackdrop != null) {
                            Modifier.layerBackdrop(tabBarBackdrop)
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (useLiquidGlass && pageBackdrop != null) {
                    val frostedTintColor = if (isDarkTheme) {
                        Color(0xFF0A0D14).copy(alpha = 0.38f)
                    } else {
                        Color.White.copy(alpha = 0.58f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBackdrop(
                                backdrop = pageBackdrop,
                                shape = { TabBarShape },
                                effects = {
                                    vibrancy()
                                    blur(22f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(frostedTintColor)
                                }
                            )
                            .border(0.8.dp, outerSpecularBorder, TabBarShape)
                    )
                } else {
                    val fallbackBg = if (isDarkTheme) {
                        Color(0xFF0A0D14).copy(alpha = 0.90f)
                    } else {
                        Color.White.copy(alpha = 0.95f)
                    }
                    val fallbackBorder = if (isDarkTheme) {
                        Color(0xFF38383A)
                    } else {
                        Color(0xFFE5E5EA)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(TabBarShape)
                            .background(fallbackBg, TabBarShape)
                            .border(0.5.dp, fallbackBorder, TabBarShape)
                    )
                }

                // Crisp tab icons & labels sitting inside the blurred bar
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = innerHorizontalPadding, vertical = innerVerticalPadding),
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

            // LAYER 2 (Overlaid sibling OUTSIDE tabBarBackdrop — zero recursion):
            //   ONE single invariant indicator Box at all times:
            //   - Smooth scale = 1f + 0.12f * lensIntensity
            //   - lens() stays continuously mounted (refractionAmount smoothly decays to 0f and
            //     refractionHeight uses coerceAtLeast(0.001f)) so the glass rim melts away smoothly
            //     without any sudden unmount pop or resting double-reflection.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = innerHorizontalPadding, vertical = innerVerticalPadding)
            ) {
                val indicatorSpecularBorder = remember(isDarkTheme, lensIntensity) {
                    if (isDarkTheme) {
                        Brush.linearGradient(
                            colors = listOf(
                                lerp(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.80f), lensIntensity),
                                lerp(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.16f), lensIntensity),
                                lerp(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.48f), lensIntensity)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                lerp(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.96f), lensIntensity),
                                lerp(Color.Black.copy(alpha = 0.06f), Color.White.copy(alpha = 0.28f), lensIntensity),
                                lerp(Color.Black.copy(alpha = 0.10f), Color.White.copy(alpha = 0.72f), lensIntensity)
                            )
                        )
                    }
                }

                val surfaceFill = if (isDarkTheme) {
                    lerp(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.04f), lensIntensity)
                } else {
                    lerp(Color.Black.copy(alpha = 0.04f), Color.White.copy(alpha = 0.08f), lensIntensity)
                }

                val dropletScaleX = dropletBaseScale * (1f + jellyStretch * lensIntensity)
                val dropletScaleY = dropletBaseScale * (1f - jellyStretch * 0.45f * lensIntensity)
                val dropletShadowPx = with(density) { (8f * lensIntensity).dp.toPx() }

                val indicatorModifier = Modifier
                    .offset {
                        val offsetX = (animatedTabIndex.value * tabWidthPx)
                            .coerceIn(0f, maxOffsetPx)
                            .roundToInt()
                        IntOffset(x = offsetX, y = 0)
                    }
                    .width(tabWidth)
                    .fillMaxHeight()
                    .padding(2.dp)
                    .graphicsLayer {
                        scaleX = dropletScaleX
                        scaleY = dropletScaleY
                        shadowElevation = dropletShadowPx
                        shape = DropletShape
                        clip = false
                        ambientShadowColor = Color.Black.copy(alpha = 0.18f)
                        spotShadowColor = Color.Black.copy(alpha = 0.18f)
                    }
                    .testTag("liquid_glass_droplet")

                if (tabBarBackdrop != null) {
                    Box(
                        modifier = indicatorModifier
                            .drawBackdrop(
                                backdrop = tabBarBackdrop,
                                shape = { DropletShape },
                                effects = {
                                    if (lensIntensity > 0.005f) {
                                        vibrancy()
                                        lens(
                                            refractionHeight = 12f.dp.toPx(),
                                            refractionAmount = 16f.dp.toPx() * lensIntensity,
                                            depthEffect = true,
                                            chromaticAberration = true
                                        )
                                    }
                                },
                                onDrawSurface = {
                                    drawRect(surfaceFill)
                                }
                            )
                            .border(
                                width = (1f + 0.35f * lensIntensity).dp,
                                brush = indicatorSpecularBorder,
                                shape = DropletShape
                            )
                    )
                } else {
                    Box(
                        modifier = indicatorModifier
                            .graphicsLayer {
                                clip = true
                                shape = DropletShape
                            }
                            .background(surfaceFill, DropletShape)
                            .border(
                                width = 1.dp,
                                brush = indicatorSpecularBorder,
                                shape = DropletShape
                            )
                    )
                }
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
