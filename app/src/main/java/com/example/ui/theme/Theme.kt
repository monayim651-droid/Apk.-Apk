package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MontanuDarkColorScheme = darkColorScheme(
    primary = AccentCrimson,
    onPrimary = PureWhite,
    primaryContainer = BloodRed,
    onPrimaryContainer = PureWhite,
    secondary = ElectricCyan,
    onSecondary = PremiumBlack,
    secondaryContainer = ObsidianSurfaceElevated,
    onSecondaryContainer = PureWhite,
    tertiary = LiveEmerald,
    onTertiary = PremiumBlack,
    background = PremiumBlack,
    onBackground = PureWhite,
    surface = ObsidianSurface,
    onSurface = PureWhite,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = MutedSlate,
    outline = ObsidianGlassBorder,
    error = AccentCrimson,
    onError = PureWhite
)

private val MontanuLightColorScheme = lightColorScheme(
    primary = BloodRed,
    onPrimary = PureWhite,
    primaryContainer = AccentCrimson,
    onPrimaryContainer = PureWhite,
    secondary = BloodRedDark,
    onSecondary = PureWhite,
    secondaryContainer = FrostedPearlContainer,
    onSecondaryContainer = FrostedSlateText,
    tertiary = LiveEmerald,
    onTertiary = PremiumBlack,
    background = FrostedPearlBg,
    onBackground = FrostedSlateText,
    surface = FrostedPearlSurface,
    onSurface = FrostedSlateText,
    surfaceVariant = FrostedPearlContainer,
    onSurfaceVariant = FrostedMutedText,
    outline = FrostedGlassBorder,
    error = BloodRed,
    onError = PureWhite
)

@Immutable
data class MontanuExtendedColors(
    val glassFillStart: Color,
    val glassFillEnd: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val crimsonGlow: Color,
    val liveGreen: Color,
    val gold: Color,
    val isDark: Boolean
)

val LocalMontanuColors = staticCompositionLocalOf {
    MontanuExtendedColors(
        glassFillStart = Color(0xD91E222D),
        glassFillEnd = Color(0xB312151C),
        glassBorderTop = Color(0x47FFFFFF),
        glassBorderBottom = Color(0x26FF405B),
        crimsonGlow = Color(0x33FF405B),
        liveGreen = LiveEmerald,
        gold = AmberGold,
        isDark = true
    )
}

val MontanuShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun MontanuScoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) MontanuDarkColorScheme else MontanuLightColorScheme
    val colorScheme = if (highContrast) {
        if (darkTheme) {
            baseScheme.copy(
                background = Color.Black,
                surface = Color(0xFF0E1015),
                onSurfaceVariant = PureWhite,
                outline = Color(0x88FFFFFF)
            )
        } else {
            baseScheme.copy(
                background = PureWhite,
                surface = PureWhite,
                onSurfaceVariant = Color.Black,
                outline = Color(0xAA000000)
            )
        }
    } else {
        baseScheme
    }

    val extended = if (darkTheme) {
        MontanuExtendedColors(
            glassFillStart = Color(0xE01B1F29),
            glassFillEnd = Color(0xB811141B),
            glassBorderTop = if (highContrast) Color(0x88FFFFFF) else Color(0x42FFFFFF),
            glassBorderBottom = Color(0x38FF405B),
            crimsonGlow = Color(0x38B5122A),
            liveGreen = LiveEmerald,
            gold = AmberGold,
            isDark = true
        )
    } else {
        MontanuExtendedColors(
            glassFillStart = Color(0xEBFFFFFF),
            glassFillEnd = Color(0xCCF2F4F9),
            glassBorderTop = if (highContrast) Color(0x88000000) else Color(0xFFFFFFFF),
            glassBorderBottom = Color(0x33B5122A),
            crimsonGlow = Color(0x1EB5122A),
            liveGreen = Color(0xFF129A45),
            gold = Color(0xFFD48806),
            isDark = false
        )
    }

    CompositionLocalProvider(LocalMontanuColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MontanuTypography,
            shapes = MontanuShapes,
            content = content
        )
    }
}
