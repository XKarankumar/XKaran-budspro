package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.bluetooth.ConnectionPopupEvent
import com.example.bluetooth.PopupEventKind
import com.example.ui.theme.LocalKaranColors
import com.example.ui.theme.PopupStyle

@Composable
fun ConnectionPopupOverlay(
    event: ConnectionPopupEvent?,
    popupStyle: PopupStyle,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val karan = LocalKaranColors.current
    val isTopCompact = popupStyle == PopupStyle.COMPACT || popupStyle == PopupStyle.MINIMAL

    Box(modifier = modifier.fillMaxSize()) {
        if (event != null && !isTopCompact) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        AnimatedVisibility(
            visible = event != null && isTopCompact,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (event != null) {
                CompactOrMinimalPopupCard(event = event, style = popupStyle, onDismiss = onDismiss)
            }
        }

        AnimatedVisibility(
            visible = event != null && !isTopCompact,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            if (event != null) {
                AppleStyleSheetPopupCard(
                    event = event,
                    style = popupStyle,
                    onDismiss = onDismiss,
                    fallbackAccent = karan.accent
                )
            }
        }
    }
}

@Composable
private fun CompactOrMinimalPopupCard(
    event: ConnectionPopupEvent,
    style: PopupStyle,
    onDismiss: () -> Unit
) {
    val karan = LocalKaranColors.current
    val accent = when (event.kind) {
        PopupEventKind.LOW_BATTERY -> karan.danger
        PopupEventKind.DISCONNECTED -> karan.textMuted
        else -> event.brand.brandAccent
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = if (style == PopupStyle.MINIMAL) Color(0xFF0A0A0C) else karan.cardBackground.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_buds_hero),
                contentDescription = event.deviceName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.deviceName,
                    style = MaterialTheme.typography.titleSmall,
                    color = karan.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.overallBattery?.let { "Battery $it% • ${event.message}" } ?: event.message,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Dismiss popup", tint = karan.textSecondary)
            }
        }
    }
}

@Composable
private fun AppleStyleSheetPopupCard(
    event: ConnectionPopupEvent,
    style: PopupStyle,
    onDismiss: () -> Unit,
    fallbackAccent: Color
) {
    val karan = LocalKaranColors.current
    val brandAccent = if (style == PopupStyle.BRAND_STYLE) event.brand.brandAccent else fallbackAccent
    val glow = if (style == PopupStyle.BRAND_STYLE) event.brand.brandGlow else karan.secondaryGlow
    val statusColor = when (event.kind) {
        PopupEventKind.DISCONNECTED -> karan.textMuted
        PopupEventKind.LOW_BATTERY -> karan.danger
        else -> brandAccent
    }
    val statusIcon = when (event.kind) {
        PopupEventKind.DISCONNECTED -> Icons.Rounded.BluetoothDisabled
        PopupEventKind.LOW_BATTERY -> Icons.Rounded.WarningAmber
        else -> if (event.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BluetoothConnected
    }

    val heroScale by animateFloatAsState(
        targetValue = if (event.kind == PopupEventKind.DISCONNECTED) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow),
        label = "popup_scale"
    )

    val cardBg = when (style) {
        PopupStyle.GLASS -> karan.cardBackground.copy(alpha = 0.78f)
        PopupStyle.BRAND_STYLE -> Color(0xFF090B11).copy(alpha = 0.96f)
        else -> Color(0xFF0D1017).copy(alpha = 0.97f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        cardBg,
                        Color.Black.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(listOf(statusColor.copy(alpha = 0.6f), glow.copy(alpha = 0.25f))),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.brand.displayName.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close connection popup", tint = karan.textSecondary)
            }
        }

        Text(
            text = event.deviceName,
            style = MaterialTheme.typography.headlineMedium,
            color = karan.textPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = event.message,
            style = MaterialTheme.typography.bodySmall,
            color = karan.textSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .size(154.dp)
                .graphicsLayer(scaleX = heroScale, scaleY = heroScale),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(statusColor.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
            )
            Image(
                painter = painterResource(id = R.drawable.img_buds_hero),
                contentDescription = "${event.brand.displayName} illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(24.dp))
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (event.leftBattery != null || event.rightBattery != null || event.caseBattery != null) {
            EarbudsTripleBatteryRow(
                leftBattery = event.leftBattery,
                rightBattery = event.rightBattery,
                caseBattery = event.caseBattery,
                isCharging = event.isCharging,
                accentColor = statusColor
            )
        } else {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = statusColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Bluetooth Battery Telemetry",
                        style = MaterialTheme.typography.bodyMedium,
                        color = karan.textSecondary
                    )
                    Text(
                        text = event.overallBattery?.let { "$it%" } ?: "Not reported",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = karan.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = statusColor,
                contentColor = Color(0xFF04070D)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = if (event.kind == PopupEventKind.LOW_BATTERY) "Acknowledge" else "Done",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
