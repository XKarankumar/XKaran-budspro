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

@Immutable
data class KaranThemeConfig(
    val preset: ThemePreset = ThemePreset.AMOLED_BLACK,
    val accentColor: Color = IceCyan,
    val secondaryGlow: Color = IndigoAura,
    val backgroundMode: BackgroundMode = BackgroundMode.DARK,
    val cardTransparency: Float = 0.72f,
    val blurIntensity: Float = 0.65f,
    val animationSpeed: AnimationSpeed = AnimationSpeed.BALANCED,
    val iconStyle: IconStyle = IconStyle.FILLED,
    val customWallpaperUri: String? = null
)

@Immutable
data class KaranExtendedColors(
    val isDark: Boolean,
    val background: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val accent: Color,
    val secondaryGlow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val cardAlpha: Float,
    val blurIntensity: Float,
    val animMultiplier: Float,
    val iconStyle: IconStyle
)

val LocalKaranColors = staticCompositionLocalOf {
    KaranExtendedColors(
        isDark = true,
        background = AmoledBlack,
        cardBackground = ObsidianSurface,
        cardBorder = GlassBorderDark,
        accent = IceCyan,
        secondaryGlow = IndigoAura,
        textPrimary = TextPrimaryDark,
        textSecondary = TextSecondaryDark,
        textMuted = TextMutedDark,
        success = MintEmerald,
        warning = AmberPulse,
        danger = CrimsonWarn,
        cardAlpha = 0.72f,
        blurIntensity = 0.65f,
        animMultiplier = 1.0f,
        iconStyle = IconStyle.FILLED
    )
}

private val KaranShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun KaranBudsTheme(
    config: KaranThemeConfig = KaranThemeConfig(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (config.backgroundMode) {
        BackgroundMode.DARK -> true
        BackgroundMode.LIGHT -> false
        BackgroundMode.AUTO -> when (config.preset) {
            ThemePreset.PURE_WHITE -> false
            else -> systemDark
        }
    } && config.preset != ThemePreset.PURE_WHITE

    val accent = if (config.preset == ThemePreset.CUSTOM || config.accentColor != IceCyan) {
        config.accentColor
    } else {
        config.preset.defaultAccent
    }
    val glow = config.preset.secondaryGlow

    val bg = if (useDark) {
        if (config.preset == ThemePreset.PURE_WHITE) AmoledBlack else config.preset.defaultBackground
    } else {
        Color(0xFFF8FAFC)
    }

    val surfaceBase = if (useDark) {
        if (config.preset == ThemePreset.PURE_WHITE) ObsidianSurface else config.preset.defaultSurface
    } else {
        Color(0xFFFFFFFF)
    }

    val extended = KaranExtendedColors(
        isDark = useDark,
        background = bg,
        cardBackground = surfaceBase.copy(alpha = config.cardTransparency.coerceIn(0.35f, 0.98f)),
        cardBorder = if (useDark) Color.White.copy(alpha = 0.10f + config.blurIntensity * 0.08f) else GlassBorderLight,
        accent = accent,
        secondaryGlow = glow,
        textPrimary = if (useDark) TextPrimaryDark else TextPrimaryLight,
        textSecondary = if (useDark) TextSecondaryDark else TextSecondaryLight,
        textMuted = if (useDark) TextMutedDark else Color(0xFF94A3B8),
        success = MintEmerald,
        warning = AmberPulse,
        danger = CrimsonWarn,
        cardAlpha = config.cardTransparency,
        blurIntensity = config.blurIntensity,
        animMultiplier = config.animationSpeed.multiplier,
        iconStyle = config.iconStyle
    )

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = if (accent == Color.White || accent == Color(0xFFE5E7EB)) Color.Black else Color(0xFF041018),
            secondary = glow,
            onSecondary = Color.White,
            tertiary = MintEmerald,
            background = bg,
            onBackground = TextPrimaryDark,
            surface = surfaceBase,
            onSurface = TextPrimaryDark,
            surfaceVariant = surfaceBase.copy(alpha = 0.85f),
            onSurfaceVariant = TextSecondaryDark,
            outline = Color.White.copy(alpha = 0.14f),
            error = CrimsonWarn
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            secondary = glow,
            onSecondary = Color.White,
            tertiary = Color(0xFF059669),
            background = bg,
            onBackground = TextPrimaryLight,
            surface = surfaceBase,
            onSurface = TextPrimaryLight,
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = TextSecondaryLight,
            outline = Color(0xFFCBD5E1),
            error = Color(0xFFE11D48)
        )
    }

    CompositionLocalProvider(LocalKaranColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KaranTypography,
            shapes = KaranShapes,
            content = content
        )
    }
}
