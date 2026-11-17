package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.components.CupertinoFloatingTabBar
import com.example.ui.i18n.AppLanguage
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CupertinoTabBarRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `clicking tabs and dragging droplet switches active tab in LTR English`() {
        var currentTab by mutableStateOf(MainTab.TODAY)

        composeTestRule.setContent {
            val bg = Color(0xFFF2F2F7)
            val backdrop = rememberLayerBackdrop {
                drawRect(bg)
                drawContent()
            }
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bg)
                        .layerBackdrop(backdrop)
                )
                CupertinoFloatingTabBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    currentLanguage = AppLanguage.ENGLISH,
                    isDarkTheme = false,
                    activeDownloadsCount = 1,
                    backdrop = backdrop
                )
            }
        }

        composeTestRule.onNodeWithTag("cupertino_floating_tab_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("liquid_glass_droplet").assertIsDisplayed()

        // Tap Apps tab directly
        composeTestRule.onNodeWithTag("tab_apps").performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainTab.APPS, currentTab)

        // Drag droplet across bar toward the right edge (Settings)
        composeTestRule.onNodeWithTag("cupertino_floating_tab_bar").performTouchInput {
            swipeRight(startX = width * 0.15f, endX = width * 0.88f, durationMillis = 180)
        }
        composeTestRule.waitForIdle()
        assertEquals(MainTab.SETTINGS, currentTab)
    }

    @Test
    fun `dragging droplet respects RTL layout direction when Kurdish Sorani is active`() {
        var currentTab by mutableStateOf(MainTab.TODAY)

        composeTestRule.setContent {
            val bg = Color(0xFF000000)
            val backdrop = rememberLayerBackdrop {
                drawRect(bg)
                drawContent()
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bg)
                            .layerBackdrop(backdrop)
                    )
                    CupertinoFloatingTabBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        currentLanguage = AppLanguage.KURDISH_SORANI,
                        isDarkTheme = true,
                        activeDownloadsCount = 0,
                        backdrop = backdrop
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("liquid_glass_droplet").assertIsDisplayed()

        // In RTL, Today is on the right and Settings is on the left, so swiping left selects Settings
        composeTestRule.onNodeWithTag("cupertino_floating_tab_bar").performTouchInput {
            swipeLeft(startX = width * 0.85f, endX = width * 0.12f, durationMillis = 180)
        }
        composeTestRule.waitForIdle()
        assertEquals(MainTab.SETTINGS, currentTab)
    }

    @Test
    fun `holding over 150ms and dragging moves droplet directly under finger and snaps to nearest tab`() {
        var currentTab by mutableStateOf(MainTab.TODAY)

        composeTestRule.setContent {
            val bg = Color(0xFF0A0E17)
            val backdrop = rememberLayerBackdrop {
                drawRect(bg)
                drawContent()
            }
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bg)
                        .layerBackdrop(backdrop)
                )
                CupertinoFloatingTabBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    currentLanguage = AppLanguage.ENGLISH,
                    isDarkTheme = true,
                    activeDownloadsCount = 0,
                    backdrop = backdrop
                )
            }
        }

        // Hold > 150ms then drag to the 3rd tab (Downloads ~ 62% of bar width)
        composeTestRule.onNodeWithTag("cupertino_floating_tab_bar").performTouchInput {
            down(androidx.compose.ui.geometry.Offset(width * 0.15f, centerY))
            advanceEventTime(200L)
            moveTo(androidx.compose.ui.geometry.Offset(width * 0.62f, centerY))
            advanceEventTime(60L)
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(MainTab.DOWNLOADS, currentTab)
        composeTestRule.onNodeWithTag("liquid_glass_droplet").assertIsDisplayed()
    }

    @Test
    fun `apps tab displays numeric badge when updatesAvailableCount is positive`() {
        composeTestRule.setContent {
            CupertinoFloatingTabBar(
                currentTab = MainTab.TODAY,
                onTabSelected = {},
                currentLanguage = AppLanguage.ENGLISH,
                isDarkTheme = true,
                activeDownloadsCount = 0,
                updatesAvailableCount = 3,
                backdrop = null
            )
        }
        composeTestRule.onNodeWithTag("tab_apps_badge", useUnmergedTree = true).assertIsDisplayed()
    }
}
