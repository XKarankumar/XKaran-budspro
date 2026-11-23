package com.example.bluetooth

import androidx.compose.ui.graphics.Color

enum class SilhouetteType {
    STEM_PRO,
    STEM_CLASSIC,
    DOT_BUD,
    OVER_EAR,
    UNIVERSAL
}

enum class BudsBrand(
    val id: String,
    val displayName: String,
    val defaultSubtitle: String,
    val brandAccent: Color,
    val brandGlow: Color,
    val silhouette: SilhouetteType
) {
    APPLE_AIRPODS(
        id = "apple_airpods",
        displayName = "Apple AirPods",
        defaultSubtitle = "Spatial Acoustic Profile",
        brandAccent = Color(0xFFE2E8F0),
        brandGlow = Color(0xFF38BDF8),
        silhouette = SilhouetteType.STEM_PRO
    ),
    SAMSUNG_GALAXY_BUDS(
        id = "samsung_galaxy_buds",
        displayName = "Samsung Galaxy Buds",
        defaultSubtitle = "Seamless Wearable Audio",
        brandAccent = Color(0xFF60A5FA),
        brandGlow = Color(0xFF818CF8),
        silhouette = SilhouetteType.DOT_BUD
    ),
    ONEPLUS_BUDS(
        id = "oneplus_buds",
        displayName = "OnePlus Buds",
        defaultSubtitle = "Flagship BassWave Acoustics",
        brandAccent = Color(0xFFF43F5E),
        brandGlow = Color(0xFFFB7185),
        silhouette = SilhouetteType.STEM_PRO
    ),
    NOTHING_EAR(
        id = "nothing_ear",
        displayName = "Nothing Ear",
        defaultSubtitle = "Transparent Precision Sound",
        brandAccent = Color(0xFFF8FAFC),
        brandGlow = Color(0xFFEF4444),
        silhouette = SilhouetteType.STEM_PRO
    ),
    REALME_BUDS(
        id = "realme_buds",
        displayName = "Realme Buds",
        defaultSubtitle = "Dynamic Deep Bass Driver",
        brandAccent = Color(0xFFFACC15),
        brandGlow = Color(0xFFF59E0B),
        silhouette = SilhouetteType.STEM_CLASSIC
    ),
    OPPO_ENCO(
        id = "oppo_enco",
        displayName = "Oppo Enco",
        defaultSubtitle = "ColorOS Pure Acoustic Engine",
        brandAccent = Color(0xFF34D399),
        brandGlow = Color(0xFF38BDF8),
        silhouette = SilhouetteType.STEM_PRO
    ),
    VIVO_TWS(
        id = "vivo_tws",
        displayName = "Vivo TWS",
        defaultSubtitle = "DeepX Stereo Architecture",
        brandAccent = Color(0xFF3B82F6),
        brandGlow = Color(0xFF22D3EE),
        silhouette = SilhouetteType.STEM_CLASSIC
    ),
    SONY(
        id = "sony",
        displayName = "Sony Audio",
        defaultSubtitle = "Hi-Res LDAC Acoustic Series",
        brandAccent = Color(0xFFD4AF37),
        brandGlow = Color(0xFF94A3B8),
        silhouette = SilhouetteType.DOT_BUD
    ),
    JBL(
        id = "jbl",
        displayName = "JBL",
        defaultSubtitle = "Signature Pure Bass Sound",
        brandAccent = Color(0xFFFF6B2B),
        brandGlow = Color(0xFFF97316),
        silhouette = SilhouetteType.STEM_CLASSIC
    ),
    REDMI_BUDS(
        id = "redmi_buds",
        displayName = "Redmi Buds",
        defaultSubtitle = "Active Dual-Mic Acoustics",
        brandAccent = Color(0xFFFB923C),
        brandGlow = Color(0xFFF43F5E),
        silhouette = SilhouetteType.DOT_BUD
    ),
    XIAOMI(
        id = "xiaomi",
        displayName = "Xiaomi",
        defaultSubtitle = "Smart Flagship Audio",
        brandAccent = Color(0xFFFF7828),
        brandGlow = Color(0xFFFBBF24),
        silhouette = SilhouetteType.STEM_PRO
    ),
    BOAT(
        id = "boat",
        displayName = "boAt",
        defaultSubtitle = "Signature High-Impact Audio",
        brandAccent = Color(0xFFEF4444),
        brandGlow = Color(0xFFDC2626),
        silhouette = SilhouetteType.STEM_CLASSIC
    ),
    OTHER(
        id = "other",
        displayName = "KARAN BUDS Universal",
        defaultSubtitle = "Universal Bluetooth Audio Companion",
        brandAccent = Color(0xFF38BDF8),
        brandGlow = Color(0xFF818CF8),
        silhouette = SilhouetteType.UNIVERSAL
    )
}

data class DetectedBrandResult(
    val brand: BudsBrand,
    val inferredModel: String
)

object BrandDetector {
    fun detect(deviceName: String?): DetectedBrandResult {
        val raw = deviceName?.trim().orEmpty()
        if (raw.isEmpty()) {
            return DetectedBrandResult(BudsBrand.OTHER, "Bluetooth Audio Device")
        }
        val lower = raw.lowercase()

        return when {
            lower.contains("airpod") || lower.contains("apple") -> {
                val model = when {
                    lower.contains("max") -> "AirPods Max"
                    lower.contains("pro") -> "AirPods Pro"
                    lower.contains("4") -> "AirPods (4th Gen)"
                    lower.contains("3") -> "AirPods (3rd Gen)"
                    else -> raw
                }
                DetectedBrandResult(BudsBrand.APPLE_AIRPODS, model)
            }
            lower.contains("oneplus") || lower.contains("nord buds") || lower.contains("buds z") -> {
                DetectedBrandResult(BudsBrand.ONEPLUS_BUDS, raw)
            }
            lower.contains("nothing") || lower.contains("cmf buds") ||
                lower.contains("ear (") || lower.contains("ear(1)") ||
                lower.contains("ear(2)") || lower.contains("ear (a)") -> {
                DetectedBrandResult(BudsBrand.NOTHING_EAR, raw)
            }
            lower.contains("realme") || lower.contains("techlife buds") -> {
                DetectedBrandResult(BudsBrand.REALME_BUDS, raw)
            }
            lower.contains("oppo") || lower.contains("enco") -> {
                DetectedBrandResult(BudsBrand.OPPO_ENCO, raw)
            }
            lower.contains("vivo") || lower.contains("iqoo tws") -> {
                DetectedBrandResult(BudsBrand.VIVO_TWS, raw)
            }
            lower.contains("sony") || lower.contains("wf-1000") ||
                lower.contains("wh-1000") || lower.contains("linkbuds") ||
                lower.contains("wf-c") || lower.contains("wh-ch") -> {
                DetectedBrandResult(BudsBrand.SONY, raw)
            }
            lower.contains("jbl") -> {
                DetectedBrandResult(BudsBrand.JBL, raw)
            }
            lower.contains("redmi") -> {
                DetectedBrandResult(BudsBrand.REDMI_BUDS, raw)
            }
            lower.contains("xiaomi") || lower.contains("mi buds") ||
                lower.contains("flipbuds") -> {
                DetectedBrandResult(BudsBrand.XIAOMI, raw)
            }
            lower.contains("boat") || lower.contains("airdopes") ||
                lower.contains("nirvana") || lower.contains("immortal") -> {
                DetectedBrandResult(BudsBrand.BOAT, raw)
            }
            lower.contains("samsung") || lower.contains("galaxy buds") ||
                lower.contains("buds2") || lower.contains("buds3") ||
                lower.contains("buds pro") || lower.contains("buds fe") ||
                lower.contains("buds live") -> {
                DetectedBrandResult(BudsBrand.SAMSUNG_GALAXY_BUDS, raw)
            }
            else -> DetectedBrandResult(BudsBrand.OTHER, raw)
        }
    }
}
