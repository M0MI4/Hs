package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemePreset(val displayName: String, val primaryColor: Color) {
    APPLE_BLUE("iOS Classic Blue", iOSSystemBlue),
    INDIGO_PRO("iOS Indigo", iOSSystemIndigo),
    PURPLE_NEON("iOS Purple", iOSSystemPurple),
    EMERALD_MINT("iOS Mint", iOSSystemGreen)
}

@Composable
fun HsStoreTheme(
    preset: ThemePreset = ThemePreset.APPLE_BLUE,
    darkTheme: Boolean = true,
    isKurdish: Boolean = false,
    content: @Composable () -> Unit
) {
    val primaryAccent = if (darkTheme) {
        when (preset) {
            ThemePreset.APPLE_BLUE -> iOSSystemBlueDark
            ThemePreset.INDIGO_PRO -> iOSSystemIndigo
            ThemePreset.PURPLE_NEON -> iOSSystemPurple
            ThemePreset.EMERALD_MINT -> iOSSystemGreen
        }
    } else {
        preset.primaryColor
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            primaryContainer = primaryAccent.copy(alpha = 0.2f),
            onPrimaryContainer = primaryAccent,
            secondary = iOSSystemIndigo,
            onSecondary = Color.White,
            tertiary = iOSSystemGreen,
            onTertiary = Color.White,
            background = iOSDarkBackground,
            onBackground = iOSDarkTextPrimary,
            surface = iOSDarkCard,
            onSurface = iOSDarkTextPrimary,
            surfaceVariant = iOSDarkCardSecondary,
            onSurfaceVariant = iOSDarkTextSecondary,
            outline = iOSDarkSeparator,
            error = iOSSystemRed
        )
    } else {
        lightColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            primaryContainer = primaryAccent.copy(alpha = 0.15f),
            onPrimaryContainer = primaryAccent,
            secondary = iOSSystemIndigo,
            onSecondary = Color.White,
            tertiary = iOSSystemGreen,
            onTertiary = Color.White,
            background = iOSLightBackground,
            onBackground = iOSLightTextPrimary,
            surface = iOSLightCard,
            onSurface = iOSLightTextPrimary,
            surfaceVariant = iOSLightCardSecondary,
            onSurfaceVariant = iOSLightTextSecondary,
            outline = iOSLightSeparator,
            error = iOSSystemRed
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = hsStoreTypography(isKurdish = isKurdish),
        content = content
    )
}
