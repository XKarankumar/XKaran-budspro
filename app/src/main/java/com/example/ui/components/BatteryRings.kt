package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalKaranColors

@Composable
fun AnimatedBatteryRingPod(
    label: String,
    shortTag: String,
    percent: Int?,
    isCharging: Boolean,
    drawableRes: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
    ringSize: Dp = 74.dp
) {
    val karan = LocalKaranColors.current
    val targetFraction = ((percent ?: 0) / 100f).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "battery_sweep_$shortTag"
    )

    val pulseTransition = rememberInfiniteTransition(label = "charging_pulse_$shortTag")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val ringColor = when {
        percent == null -> karan.textMuted.copy(alpha = 0.35f)
        isCharging -> karan.success
        percent <= 20 -> karan.danger
        percent <= 40 -> karan.warning
        else -> accentColor
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(karan.cardBackground.copy(alpha = 0.65f))
            .border(1.dp, karan.cardBorder, RoundedCornerShape(22.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 5.dp.toPx()
                drawArc(
                    color = karan.textMuted.copy(alpha = 0.18f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                if (percent != null) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                ringColor.copy(alpha = if (isCharging) pulseAlpha else 1f),
                                karan.secondaryGlow.copy(alpha = if (isCharging) pulseAlpha else 0.9f),
                                ringColor
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedFraction,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = "$label render",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ringSize - 18.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isCharging && percent != null) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = "Charging",
                    tint = karan.success,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = percent?.let { "$it%" } ?: "N/A",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (percent != null) karan.textPrimary else karan.textMuted
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = karan.textSecondary,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (percent != null) (if (isCharging) "Charging" else "Live") else "Not reported",
            style = MaterialTheme.typography.labelSmall,
            color = if (percent != null) ringColor else karan.textMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EarbudsTripleBatteryRow(
    leftBattery: Int?,
    rightBattery: Int?,
    caseBattery: Int?,
    isCharging: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedBatteryRingPod(
            label = "Left Bud",
            shortTag = "L",
            percent = leftBattery,
            isCharging = isCharging,
            drawableRes = R.drawable.img_buds_left,
            accentColor = accentColor,
            modifier = Modifier.weight(1f)
        )
        AnimatedBatteryRingPod(
            label = "Right Bud",
            shortTag = "R",
            percent = rightBattery,
            isCharging = isCharging,
            drawableRes = R.drawable.img_buds_right,
            accentColor = accentColor,
            modifier = Modifier.weight(1f)
        )
        AnimatedBatteryRingPod(
            label = "Case",
            shortTag = "CASE",
            percent = caseBattery,
            isCharging = isCharging,
            drawableRes = R.drawable.img_buds_case,
            accentColor = accentColor,
            modifier = Modifier.weight(1f)
        )
    }
}
