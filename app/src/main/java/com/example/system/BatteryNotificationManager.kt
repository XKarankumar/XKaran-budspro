package com.example.system

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class BatteryNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_BATTERY = "karan_buds_battery_alerts"
        const val CHANNEL_CONNECTION = "karan_buds_connection_status"
        private const val NOTIF_ID_BATTERY = 1001
        private const val NOTIF_ID_CONNECTION = 1002
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val batteryChannel = NotificationChannel(
                CHANNEL_BATTERY,
                context.getString(R.string.notif_channel_battery_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_battery_desc)
            }
            val connChannel = NotificationChannel(
                CHANNEL_CONNECTION,
                context.getString(R.string.notif_channel_connection_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_connection_desc)
            }
            nm.createNotificationChannel(batteryChannel)
            nm.createNotificationChannel(connChannel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun postLowBatteryNotification(deviceName: String, percent: Int) {
        if (!hasNotificationPermission()) return
        val launchIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_BATTERY)
            .setSmallIcon(R.drawable.ic_tile_buds)
            .setContentTitle("$deviceName • Low Battery ($percent%)")
            .setContentText("Place earbuds in their charging case soon to maintain playback.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(launchIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_BATTERY, notification)
        }
    }

    fun postConnectionNotification(deviceName: String, brandName: String, batteryPercent: Int?) {
        if (!hasNotificationPermission()) return
        val launchIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val battText = batteryPercent?.let { " • Battery $it%" } ?: ""
        val notification = NotificationCompat.Builder(context, CHANNEL_CONNECTION)
            .setSmallIcon(R.drawable.ic_tile_buds)
            .setContentTitle("$deviceName Connected")
            .setContentText("$brandName Companion Active$battText")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(launchIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_CONNECTION, notification)
        }
    }
}
