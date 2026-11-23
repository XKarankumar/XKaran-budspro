package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bluetooth.FeatureSupport
import com.example.ui.theme.LocalKaranColors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    accentGlow: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val karan = LocalKaranColors.current
    val haptics = LocalHapticFeedback.current
    val borderBrush = if (accentGlow != null) {
        Brush.linearGradient(
            listOf(
                accentGlow.copy(alpha = 0.45f),
                karan.cardBorder,
                karan.secondaryGlow.copy(alpha = 0.25f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                karan.cardBorder,
                karan.cardBorder.copy(alpha = 0.35f)
            )
        )
    }

    val shape = RoundedCornerShape(cornerRadius)
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            karan.cardBackground,
            karan.cardBackground.copy(alpha = (karan.cardAlpha * 0.86f).coerceIn(0.28f, 0.96f))
        )
    )

    Column(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                } else Modifier
            )
            .padding(18.dp),
        content = content
    )
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val karan = LocalKaranColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = karan.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = karan.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun HardwareSupportBadge(support: FeatureSupport, modifier: Modifier = Modifier) {
    val karan = LocalKaranColors.current
    val (label, tint) = when (support) {
        is FeatureSupport.Supported -> "HARDWARE SUPPORTED" to karan.success
        is FeatureSupport.SoftwareOnly -> "SOFTWARE DSP" to karan.accent
        is FeatureSupport.Unsupported -> "NOT SUPPORTED BY DEVICE" to karan.textMuted
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(0.75.dp, tint.copy(alpha = 0.45f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun UnsupportedFeatureBanner(
    title: String,
    reason: String,
    modifier: Modifier = Modifier
) {
    val karan = LocalKaranColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(karan.textMuted.copy(alpha = 0.12f))
            .border(1.dp, karan.textMuted.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "Hardware limitation info",
            tint = karan.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = karan.textPrimary
            )
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = karan.textSecondary
            )
        }
    }
}

@Composable
fun PremiumPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color? = null
) {
    val karan = LocalKaranColors.current
    val haptics = LocalHapticFeedback.current
    val activeColor = accentColor ?: karan.accent
    val bg by animateColorAsState(
        targetValue = if (selected) activeColor.copy(alpha = 0.20f) else karan.cardBackground.copy(alpha = 0.6f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip_bg"
    )
    val border by animateColorAsState(
        targetValue = if (selected) activeColor.copy(alpha = 0.75f) else karan.cardBorder,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip_border"
    )
    val contentColor = if (selected) activeColor else karan.textSecondary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) karan.textPrimary else contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BrandMarkBadge(
    edition: String = "PREMIUM",
    modifier: Modifier = Modifier
) {
    val karan = LocalKaranColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(karan.accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "KARAN BUDS",
            style = MaterialTheme.typography.titleMedium,
            color = karan.textPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = karan.accent.copy(alpha = 0.16f),
            border = BorderStroke(0.75.dp, karan.accent.copy(alpha = 0.55f))
        ) {
            Text(
                text = edition,
                style = MaterialTheme.typography.labelSmall,
                color = karan.accent,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
