package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val DisplayFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold, loadingStrategy = FontLoadingStrategy.OptionalLocal)
)

val BodyFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.inter, FontWeight.Medium, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.inter, FontWeight.SemiBold, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.inter, FontWeight.Bold, loadingStrategy = FontLoadingStrategy.OptionalLocal)
)

val MonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.jetbrains_mono, FontWeight.Medium, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.jetbrains_mono, FontWeight.Bold, loadingStrategy = FontLoadingStrategy.OptionalLocal)
)

fun buildMedPrepTypography(
    fontStyle: AppFontStyle = AppFontStyle.MODERN_SANS,
    textScale: AppTextScale = AppTextScale.NORMAL
): Typography {
    val displayFamily = when (fontStyle) {
        AppFontStyle.MODERN_SANS -> DisplayFontFamily
        AppFontStyle.CLASSIC_SERIF -> FontFamily.Serif
        AppFontStyle.TECHNICAL_MONO -> MonoFontFamily
        AppFontStyle.CLEAN_SYSTEM -> FontFamily.SansSerif
    }
    val bodyFamily = when (fontStyle) {
        AppFontStyle.MODERN_SANS -> BodyFontFamily
        AppFontStyle.CLASSIC_SERIF -> FontFamily.Serif
        AppFontStyle.TECHNICAL_MONO -> MonoFontFamily
        AppFontStyle.CLEAN_SYSTEM -> FontFamily.SansSerif
    }
    val s = textScale.multiplier

    return Typography(
        displaySmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (32f * s).sp,
            lineHeight = (38f * s).sp,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (24f * s).sp,
            lineHeight = (30f * s).sp,
            letterSpacing = (-0.25).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (20f * s).sp,
            lineHeight = (26f * s).sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18f * s).sp,
            lineHeight = (24f * s).sp
        ),
        titleMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15f * s).sp,
            lineHeight = (21f * s).sp
        ),
        titleSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13f * s).sp,
            lineHeight = (18f * s).sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (16f * s).sp,
            lineHeight = (24f * s).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14f * s).sp,
            lineHeight = (20f * s).sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12f * s).sp,
            lineHeight = (16f * s).sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14f * s).sp,
            lineHeight = (20f * s).sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12f * s).sp,
            lineHeight = (16f * s).sp,
            letterSpacing = 0.25.sp
        ),
        labelSmall = TextStyle(
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11f * s).sp,
            lineHeight = (14f * s).sp,
            letterSpacing = 0.3.sp
        )
    )
}

val MedPrepTypography = buildMedPrepTypography()
