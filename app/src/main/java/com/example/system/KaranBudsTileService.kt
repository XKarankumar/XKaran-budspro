package com.example.system

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.KaranBudsApplication
import com.example.MainActivity

class KaranBudsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = android.app.PendingIntent.getActivity(
                this,
                42,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val app = applicationContext as? KaranBudsApplication
        val budsState = app?.bluetoothController?.state?.value
        if (budsState != null && budsState.isConnected) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = budsState.deviceName.take(18)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = budsState.overallBatteryPercent?.let { "$it% • ${budsState.codecTelemetry.codecName}" }
                    ?: budsState.profileStatus
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Karan Buds"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Standby"
            }
        }
        tile.updateTile()
    }
}
