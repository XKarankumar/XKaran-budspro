package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class ThemePreset(
    val displayName: String,
    val subtitle: String,
    val defaultBackground: Color,
    val defaultSurface: Color,
    val defaultAccent: Color,
    val secondaryGlow: Color,
    val isLightByDefault: Boolean = false
) {
    AMOLED_BLACK(
        displayName = "AMOLED Black",
        subtitle = "Pure #000000 luxury obsidian",
        defaultBackground = Color(0xFF000000),
        defaultSurface = Color(0xFF0E1118),
        defaultAccent = Color(0xFF5EEAD4),
        secondaryGlow = Color(0xFF38BDF8)
    ),
    COLOR_OS(
        displayName = "ColorOS 17",
        subtitle = "Luminous fluid obsidian & ice blue",
        defaultBackground = Color(0xFF05070B),
        defaultSurface = Color(0xFF111622),
        defaultAccent = Color(0xFF38BDF8),
        secondaryGlow = Color(0xFF818CF8)
    ),
    PURE_WHITE(
        displayName = "Pure White",
        subtitle = "Crisp ceramic studio daylight",
        defaultBackground = Color(0xFFF8FAFC),
        defaultSurface = Color(0xFFFFFFFF),
        defaultAccent = Color(0xFF0284C7),
        secondaryGlow = Color(0xFF6366F1),
        isLightByDefault = true
    ),
    AURORA(
        displayName = "Aurora",
        subtitle = "Northern emerald & violet shimmer",
        defaultBackground = Color(0xFF03070B),
        defaultSurface = Color(0xFF0E1722),
        defaultAccent = Color(0xFF34D399),
        secondaryGlow = Color(0xFFA78BFA)
    ),
    NEON(
        displayName = "Neon",
        subtitle = "High-contrast electric magenta & cyan",
        defaultBackground = Color(0xFF030208),
        defaultSurface = Color(0xFF130E24),
        defaultAccent = Color(0xFFF43F5E),
        secondaryGlow = Color(0xFF22D3EE)
    ),
    CYBER(
        displayName = "Cyber",
        subtitle = "Tactical amber & carbon graphite",
        defaultBackground = Color(0xFF060608),
        defaultSurface = Color(0xFF141418),
        defaultAccent = Color(0xFFFBBF24),
        secondaryGlow = Color(0xFF38BDF8)
    ),
    MINIMAL(
        displayName = "Minimal",
        subtitle = "Nothing-inspired monochrome precision",
        defaultBackground = Color(0xFF000000),
        defaultSurface = Color(0xFF121214),
        defaultAccent = Color(0xFFE5E7EB),
        secondaryGlow = Color(0xFFEF4444)
    ),
    CUSTOM(
        displayName = "Custom Studio",
        subtitle = "Your personalized palette & glass",
        defaultBackground = Color(0xFF020408),
        defaultSurface = Color(0xFF101520),
        defaultAccent = Color(0xFF60A5FA),
        secondaryGlow = Color(0xFFC084FC)
    )
}

enum class BackgroundMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    AUTO("Auto")
}

enum class PopupStyle(val displayName: String, val description: String) {
    BRAND_STYLE("Brand-Style", "Dynamic brand aura with 3D earbuds & battery rings"),
    LARGE("Large Card", "Apple-inspired bottom sheet with full pod & case showcase"),
    GLASS("Glassmorphic", "Translucent frosted overlay with ambient edge glow"),
    COMPACT("Compact Pill", "Top floating pill showing quick device & battery status"),
    MINIMAL("Minimal Dot", "Clean monochrome telemetry banner")
}

enum class AnimationSpeed(val label: String, val multiplier: Float) {
    RELAXED("Relaxed", 1.35f),
    BALANCED("Balanced", 1.0f),
    SNAPPY("Snappy", 0.65f)
}

enum class IconStyle(val label: String) {
    FILLED("Filled"),
    OUTLINED("Outlined"),
    DUOTONE("Duotone")
}

enum class HomeCardId(val title: String) {
    BATTERY_TELEMETRY("Battery Telemetry Rings"),
    QUICK_MODES("Listening Modes"),
    ANC_CONTROL("Noise Control"),
    AUDIO_LAB("Pro Audio Lab"),
    SPATIAL_STUDIO("Spatial Audio"),
    DEVICE_DETAILS("Hardware & Codec Info")
}
