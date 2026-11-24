package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class MedPrepExtendedColors(
    val success: Color,
    val successContainer: Color,
    val onSuccess: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val glassFillTop: Color,
    val glassFillBottom: Color,
    val glassBorderHighlight: Color,
    val glassBorderSubtle: Color,
    val glassActivePillStart: Color,
    val glassActivePillEnd: Color
)

private fun buildLightColorScheme(palette: AppColorPalette): ColorScheme = when (palette) {
    AppColorPalette.CLINICAL_TEAL -> lightColorScheme(
        primary = ClinicalTeal,
        onPrimary = Color.White,
        primaryContainer = ClinicalMintContainer,
        onPrimaryContainer = ClinicalMintOnContainer,
        secondary = SlateNavy,
        onSecondary = Color.White,
        secondaryContainer = SlateNavyContainer,
        onSecondaryContainer = SlateNavyOnContainer,
        tertiary = AmberHighlight,
        onTertiary = Color.White,
        tertiaryContainer = AmberContainer,
        onTertiaryContainer = AmberOnContainer,
        error = CrimsonError,
        onError = Color.White,
        errorContainer = CrimsonErrorContainer,
        onErrorContainer = Color(0xFF7F1D1D),
        background = PaperBackground,
        onBackground = InkPrimary,
        surface = CrispSurface,
        onSurface = InkPrimary,
        surfaceVariant = SoftSurfaceVariant,
        onSurfaceVariant = InkSecondary,
        outline = HairlineOutline,
        outlineVariant = Color(0xFFDFEAEB)
    )
    AppColorPalette.ROYAL_SAPPHIRE -> lightColorScheme(
        primary = Color(0xFF1D4ED8),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDBEAFE),
        onPrimaryContainer = Color(0xFF1E3A8A),
        secondary = Color(0xFF0F766E),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCCFBF1),
        onSecondaryContainer = Color(0xFF134E4A),
        tertiary = Color(0xFFD97706),
        onTertiary = Color.White,
        tertiaryContainer = AmberContainer,
        onTertiaryContainer = AmberOnContainer,
        error = CrimsonError,
        onError = Color.White,
        errorContainer = CrimsonErrorContainer,
        onErrorContainer = Color(0xFF7F1D1D),
        background = Color(0xFFF4F7FD),
        onBackground = Color(0xFF0F172A),
        surface = CrispSurface,
        onSurface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFFE7EEF9),
        onSurfaceVariant = Color(0xFF475569),
        outline = Color(0xFFCBD5E1),
        outlineVariant = Color(0xFFE2E8F0)
    )
    AppColorPalette.EMERALD_BOTANICAL -> lightColorScheme(
        primary = Color(0xFF047857),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD1FAE5),
        onPrimaryContainer = Color(0xFF064E3B),
        secondary = Color(0xFF0E7490),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCFFAFE),
        onSecondaryContainer = Color(0xFF164E63),
        tertiary = Color(0xFFD97706),
        onTertiary = Color.White,
        tertiaryContainer = AmberContainer,
        onTertiaryContainer = AmberOnContainer,
        error = CrimsonError,
        onError = Color.White,
        errorContainer = CrimsonErrorContainer,
        onErrorContainer = Color(0xFF7F1D1D),
        background = Color(0xFFF3FAF7),
        onBackground = Color(0xFF0F241D),
        surface = CrispSurface,
        onSurface = Color(0xFF0F241D),
        surfaceVariant = Color(0xFFE3F2EC),
        onSurfaceVariant = Color(0xFF3E5E53),
        outline = Color(0xFFC3DFD5),
        outlineVariant = Color(0xFFDCEEE7)
    )
    AppColorPalette.AMETHYST_GLASS -> lightColorScheme(
        primary = Color(0xFF6D28D9),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEDE9FE),
        onPrimaryContainer = Color(0xFF4C1D95),
        secondary = Color(0xFF0284C7),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0F2FE),
        onSecondaryContainer = Color(0xFF0C4A6E),
        tertiary = Color(0xFFDB2777),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFCE7F3),
        onTertiaryContainer = Color(0xFF831843),
        error = CrimsonError,
        onError = Color.White,
        errorContainer = CrimsonErrorContainer,
        onErrorContainer = Color(0xFF7F1D1D),
        background = Color(0xFFF7F5FD),
        onBackground = Color(0xFF1A1330),
        surface = CrispSurface,
        onSurface = Color(0xFF1A1330),
        surfaceVariant = Color(0xFFECE7FA),
        onSurfaceVariant = Color(0xFF564A72),
        outline = Color(0xFFD4CBEB),
        outlineVariant = Color(0xFFE6E0F5)
    )
    AppColorPalette.AMBER_SUNSET -> lightColorScheme(
        primary = Color(0xFFB45309),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFEF3C7),
        onPrimaryContainer = Color(0xFF78350F),
        secondary = Color(0xFF0F766E),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCCFBF1),
        onSecondaryContainer = Color(0xFF134E4A),
        tertiary = Color(0xFFBE185D),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFCE7F3),
        onTertiaryContainer = Color(0xFF831843),
        error = CrimsonError,
        onError = Color.White,
        errorContainer = CrimsonErrorContainer,
        onErrorContainer = Color(0xFF7F1D1D),
        background = Color(0xFFFDF9F3),
        onBackground = Color(0xFF231910),
        surface = CrispSurface,
        onSurface = Color(0xFF231910),
        surfaceVariant = Color(0xFFF5ECE0),
        onSurfaceVariant = Color(0xFF6B5744),
        outline = Color(0xFFE3D3BF),
        outlineVariant = Color(0xFFF0E4D4)
    )
}

private fun buildDarkColorScheme(palette: AppColorPalette): ColorScheme = when (palette) {
    AppColorPalette.CLINICAL_TEAL -> darkColorScheme(
        primary = ClinicalTealLight,
        onPrimary = Color(0xFF022F2E),
        primaryContainer = Color(0xFF104044),
        onPrimaryContainer = Color(0xFFBDF5EE),
        secondary = Color(0xFF93C5D8),
        onSecondary = Color(0xFF0E2A35),
        secondaryContainer = Color(0xFF1B3A47),
        onSecondaryContainer = Color(0xFFD5ECF4),
        tertiary = Color(0xFFFBBF24),
        onTertiary = Color(0xFF422006),
        tertiaryContainer = Color(0xFF533008),
        onTertiaryContainer = Color(0xFFFEF3C7),
        error = CrimsonErrorDark,
        onError = Color(0xFF450A0A),
        errorContainer = CrimsonErrorContainerDark,
        onErrorContainer = Color(0xFFFECACA),
        background = DarkSlateBackground,
        onBackground = DarkInkPrimary,
        surface = DarkSlateSurface,
        onSurface = DarkInkPrimary,
        surfaceVariant = DarkSlateSurfaceVariant,
        onSurfaceVariant = DarkInkSecondary,
        outline = DarkOutline,
        outlineVariant = Color(0xFF1E3038)
    )
    AppColorPalette.ROYAL_SAPPHIRE -> darkColorScheme(
        primary = Color(0xFF60A5FA),
        onPrimary = Color(0xFF0B1F4E),
        primaryContainer = Color(0xFF172D61),
        onPrimaryContainer = Color(0xFFDBEAFE),
        secondary = Color(0xFF5EEAD4),
        onSecondary = Color(0xFF09312F),
        secondaryContainer = Color(0xFF123E3D),
        onSecondaryContainer = Color(0xFFCCFBF1),
        tertiary = Color(0xFFFBBF24),
        onTertiary = Color(0xFF422006),
        tertiaryContainer = Color(0xFF533008),
        onTertiaryContainer = Color(0xFFFEF3C7),
        error = CrimsonErrorDark,
        onError = Color(0xFF450A0A),
        errorContainer = CrimsonErrorContainerDark,
        onErrorContainer = Color(0xFFFECACA),
        background = Color(0xFF0A1020),
        onBackground = Color(0xFFEAF0FC),
        surface = Color(0xFF111A2E),
        onSurface = Color(0xFFEAF0FC),
        surfaceVariant = Color(0xFF1C2944),
        onSurfaceVariant = Color(0xFFA0B3D1),
        outline = Color(0xFF2D4063),
        outlineVariant = Color(0xFF1E2D49)
    )
    AppColorPalette.EMERALD_BOTANICAL -> darkColorScheme(
        primary = Color(0xFF34D399),
        onPrimary = Color(0xFF032D1E),
        primaryContainer = Color(0xFF0E3F2E),
        onPrimaryContainer = Color(0xFFD1FAE5),
        secondary = Color(0xFF67E8F9),
        onSecondary = Color(0xFF08313D),
        secondaryContainer = Color(0xFF113F4C),
        onSecondaryContainer = Color(0xFFCFFAFE),
        tertiary = Color(0xFFFBBF24),
        onTertiary = Color(0xFF422006),
        tertiaryContainer = Color(0xFF533008),
        onTertiaryContainer = Color(0xFFFEF3C7),
        error = CrimsonErrorDark,
        onError = Color(0xFF450A0A),
        errorContainer = CrimsonErrorContainerDark,
        onErrorContainer = Color(0xFFFECACA),
        background = Color(0xFF091410),
        onBackground = Color(0xFFE8F5EE),
        surface = Color(0xFF10201A),
        onSurface = Color(0xFFE8F5EE),
        surfaceVariant = Color(0xFF1A3129),
        onSurfaceVariant = Color(0xFF9AC0B2),
        outline = Color(0xFF2A4A3E),
        outlineVariant = Color(0xFF1C352C)
    )
    AppColorPalette.AMETHYST_GLASS -> darkColorScheme(
        primary = Color(0xFFA78BFA),
        onPrimary = Color(0xFF230E52),
        primaryContainer = Color(0xFF351D72),
        onPrimaryContainer = Color(0xFFEDE9FE),
        secondary = Color(0xFF7DD3FC),
        onSecondary = Color(0xFF0B2C42),
        secondaryContainer = Color(0xFF143C57),
        onSecondaryContainer = Color(0xFFE0F2FE),
        tertiary = Color(0xFFF472B6),
        onTertiary = Color(0xFF4E0C2D),
        tertiaryContainer = Color(0xFF671940),
        onTertiaryContainer = Color(0xFFFCE7F3),
        error = CrimsonErrorDark,
        onError = Color(0xFF450A0A),
        errorContainer = CrimsonErrorContainerDark,
        onErrorContainer = Color(0xFFFECACA),
        background = Color(0xFF0E0B1A),
        onBackground = Color(0xFFF0ECFA),
        surface = Color(0xFF171329),
        onSurface = Color(0xFFF0ECFA),
        surfaceVariant = Color(0xFF241E3E),
        onSurfaceVariant = Color(0xFFB3A9D1),
        outline = Color(0xFF3A315E),
        outlineVariant = Color(0xFF272042)
    )
    AppColorPalette.AMBER_SUNSET -> darkColorScheme(
        primary = Color(0xFFFBBF24),
        onPrimary = Color(0xFF3D2004),
        primaryContainer = Color(0xFF54300A),
        onPrimaryContainer = Color(0xFFFEF3C7),
        secondary = Color(0xFF5EEAD4),
        onSecondary = Color(0xFF07312E),
        secondaryContainer = Color(0xFF113F3D),
        onSecondaryContainer = Color(0xFFCCFBF1),
        tertiary = Color(0xFFF472B6),
        onTertiary = Color(0xFF4E0C2D),
        tertiaryContainer = Color(0xFF671940),
        onTertiaryContainer = Color(0xFFFCE7F3),
        error = CrimsonErrorDark,
        onError = Color(0xFF450A0A),
        errorContainer = CrimsonErrorContainerDark,
        onErrorContainer = Color(0xFFFECACA),
        background = Color(0xFF14100B),
        onBackground = Color(0xFFF7EFE6),
        surface = Color(0xFF1F1912),
        onSurface = Color(0xFFF7EFE6),
        surfaceVariant = Color(0xFF2F261C),
        onSurfaceVariant = Color(0xFFC8B6A2),
        outline = Color(0xFF4B3D2E),
        outlineVariant = Color(0xFF33291E)
    )
}

private fun buildExtendedColors(palette: AppColorPalette, darkTheme: Boolean): MedPrepExtendedColors {
    val (heroStart, heroEnd) = when (palette) {
        AppColorPalette.CLINICAL_TEAL -> if (darkTheme) Color(0xFF105E5E) to Color(0xFF082D33) else ClinicalTeal to ClinicalTealDark
        AppColorPalette.ROYAL_SAPPHIRE -> if (darkTheme) Color(0xFF1E3A8A) to Color(0xFF0F1F4E) else Color(0xFF1D4ED8) to Color(0xFF1E3A8A)
        AppColorPalette.EMERALD_BOTANICAL -> if (darkTheme) Color(0xFF065F46) to Color(0xFF052E22) else Color(0xFF047857) to Color(0xFF064E3B)
        AppColorPalette.AMETHYST_GLASS -> if (darkTheme) Color(0xFF4C1D95) to Color(0xFF230E52) else Color(0xFF6D28D9) to Color(0xFF4C1D95)
        AppColorPalette.AMBER_SUNSET -> if (darkTheme) Color(0xFF92400E) to Color(0xFF451A03) else Color(0xFFB45309) to Color(0xFF78350F)
    }

    return if (darkTheme) {
        MedPrepExtendedColors(
            success = SuccessGreenDark,
            successContainer = SuccessGreenContainerDark,
            onSuccess = Color(0xFF052E16),
            onSuccessContainer = Color(0xFFBBF7D0),
            warning = Color(0xFFFBBF24),
            warningContainer = Color(0xFF452708),
            onWarningContainer = Color(0xFFFEF3C7),
            heroStart = heroStart,
            heroEnd = heroEnd,
            glassFillTop = Color.White.copy(alpha = 0.12f),
            glassFillBottom = Color.White.copy(alpha = 0.05f),
            glassBorderHighlight = Color.White.copy(alpha = 0.28f),
            glassBorderSubtle = Color.White.copy(alpha = 0.08f),
            glassActivePillStart = palette.swatchAccent.copy(alpha = 0.34f),
            glassActivePillEnd = palette.swatchPrimary.copy(alpha = 0.28f)
        )
    } else {
        MedPrepExtendedColors(
            success = SuccessGreen,
            successContainer = SuccessGreenContainer,
            onSuccess = Color.White,
            onSuccessContainer = Color(0xFF0A4721),
            warning = AmberHighlight,
            warningContainer = AmberContainer,
            onWarningContainer = AmberOnContainer,
            heroStart = heroStart,
            heroEnd = heroEnd,
            glassFillTop = Color.White.copy(alpha = 0.86f),
            glassFillBottom = Color.White.copy(alpha = 0.64f),
            glassBorderHighlight = Color.White.copy(alpha = 0.95f),
            glassBorderSubtle = palette.swatchPrimary.copy(alpha = 0.16f),
            glassActivePillStart = palette.swatchPrimary.copy(alpha = 0.18f),
            glassActivePillEnd = palette.swatchAccent.copy(alpha = 0.24f)
        )
    }
}

val LocalMedPrepColors = staticCompositionLocalOf {
    buildExtendedColors(AppColorPalette.CLINICAL_TEAL, darkTheme = false)
}

val MedPrepShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MedPrepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorPalette: AppColorPalette = AppColorPalette.CLINICAL_TEAL,
    fontStyle: AppFontStyle = AppFontStyle.MODERN_SANS,
    textScale: AppTextScale = AppTextScale.NORMAL,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(darkTheme, colorPalette) {
        if (darkTheme) buildDarkColorScheme(colorPalette) else buildLightColorScheme(colorPalette)
    }
    val extendedColors = remember(darkTheme, colorPalette) {
        buildExtendedColors(colorPalette, darkTheme)
    }
    val typography = remember(fontStyle, textScale) {
        buildMedPrepTypography(fontStyle, textScale)
    }

    CompositionLocalProvider(LocalMedPrepColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = MedPrepShapes,
            content = content
        )
    }
}
