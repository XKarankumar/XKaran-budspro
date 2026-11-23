package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.BrandMarkBadge
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalKaranColors

@Composable
fun SplashScreenView() {
    val karan = LocalKaranColors.current
    val infinite = rememberInfiniteTransition(label = "splash_glow")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splash_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .graphicsLayer(scaleX = pulse, scaleY = pulse)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(karan.accent.copy(alpha = 0.24f), Color.Transparent)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_buds_hero),
                contentDescription = "KARAN BUDS Showcase",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(36.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            BrandMarkBadge(edition = "PREMIUM")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ultimate Audio Experience",
                style = MaterialTheme.typography.bodyLarge,
                color = karan.textSecondary
            )
        }

        Text(
            text = "© Karan",
            style = MaterialTheme.typography.labelMedium,
            color = karan.textMuted,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        )
    }
}

@Composable
fun OnboardingScreen(
    hasBluetoothPermission: Boolean,
    onPermissionsGranted: () -> Unit,
    onContinue: () -> Unit
) {
    val karan = LocalKaranColors.current
    var requestedOnce by remember { mutableStateOf(false) }

    val permissionsToRequest = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        requestedOnce = true
        onPermissionsGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black, Color(0xFF070B12), Color.Black)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        BrandMarkBadge(edition = "PREMIUM")
        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier.size(210.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(karan.accent.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
            )
            Image(
                painter = painterResource(id = R.drawable.img_buds_hero),
                contentDescription = "Flagship earbuds illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(186.dp)
                    .clip(RoundedCornerShape(32.dp))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ultimate Audio Experience",
            style = MaterialTheme.typography.headlineMedium,
            color = karan.textPrimary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Real Bluetooth telemetry, 10-band Android AudioFX tuning, spatial soundstage, and instant Theme Studio customization.",
            style = MaterialTheme.typography.bodyMedium,
            color = karan.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        OnboardingFeatureRow(
            icon = Icons.Rounded.BluetoothSearching,
            title = "Live Brand & Telemetry Detection",
            desc = "Auto-identifies 12+ earbuds brands and displays genuine Android Bluetooth battery, codec & BLE telemetry."
        )
        Spacer(modifier = Modifier.height(10.dp))
        OnboardingFeatureRow(
            icon = Icons.Rounded.GraphicEq,
            title = "Pro Audio Lab & Spatial Studio",
            desc = "10-band EQ, Bass Boost, Loudness, Balance, and Android 13+ Spatializer inspection."
        )
        Spacer(modifier = Modifier.height(10.dp))
        OnboardingFeatureRow(
            icon = Icons.Rounded.SurroundSound,
            title = "Smart Earbud Finder & Alerts",
            desc = "Stereo Left/Right acoustic ping, BLE RSSI proximity meter & low-battery alerts."
        )
        Spacer(modifier = Modifier.height(10.dp))
        OnboardingFeatureRow(
            icon = Icons.Rounded.Shield,
            title = "100% Honest Hardware Rule",
            desc = "Never fakes battery percentages, GPS locations, or unsupported vendor ANC protocols."
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!hasBluetoothPermission && permissionsToRequest.isNotEmpty()) {
            Button(
                onClick = { launcher.launch(permissionsToRequest) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = karan.accent,
                    contentColor = Color(0xFF04080E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("grant_bluetooth_button")
            ) {
                Text(
                    text = if (requestedOnce) "Retry Nearby & Notification Permissions" else "Grant Bluetooth & Alert Access",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Bluetooth permission active",
                    tint = karan.success,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bluetooth Nearby Access Active",
                    style = MaterialTheme.typography.labelLarge,
                    color = karan.success
                )
            }
        }

        OutlinedButton(
            onClick = onContinue,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("continue_dashboard_button")
        ) {
            Text(
                text = "Enter KARAN BUDS Studio",
                style = MaterialTheme.typography.labelLarge,
                color = karan.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "© Karan",
            style = MaterialTheme.typography.labelMedium,
            color = karan.textMuted
        )
    }
}

@Composable
private fun OnboardingFeatureRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    val karan = LocalKaranColors.current
    GlassCard(cornerRadius = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(karan.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = karan.accent)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = desc, style = MaterialTheme.typography.bodySmall, color = karan.textSecondary)
            }
        }
    }
}
