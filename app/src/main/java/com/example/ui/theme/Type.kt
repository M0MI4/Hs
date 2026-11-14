package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val KurdishFontFamily = FontFamily(
    Font(R.font.roboto_f4illed, FontWeight.Normal),
    Font(R.font.roboto_f4illed, FontWeight.Medium),
    Font(R.font.roboto_f4illed, FontWeight.SemiBold),
    Font(R.font.roboto_f4illed, FontWeight.Bold),
    Font(R.font.roboto_f4illed, FontWeight.Black)
)

/**
 * Premium Apple Cupertino Typography System configured with generous line-heights
 * and proportional tracking. Applies KurdishFontFamily and strict 0.sp letterSpacing
 * when Kurdish Sorani is active so connected Arabic/Kurdish glyphs never split.
 */
fun hsStoreTypography(isKurdish: Boolean): Typography {
    val activeFontFamily = if (isKurdish) KurdishFontFamily else FontFamily.Default
    return Typography(
        displayLarge = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 34.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        ),
        titleSmall = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = if (isKurdish) 0.sp else 0.2.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            letterSpacing = if (isKurdish) 0.sp else 0.2.sp
        ),
        bodySmall = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            letterSpacing = if (isKurdish) 0.sp else 0.1.sp
        ),
        labelLarge = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = if (isKurdish) 0.sp else 0.3.sp
        ),
        labelMedium = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = if (isKurdish) 0.sp else 0.3.sp
        ),
        labelSmall = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = if (isKurdish) 0.sp else 0.3.sp
        )
    )
}

val Typography = hsStoreTypography(isKurdish = false)
