package com.example.system

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.KaranBudsApplication
import com.example.MainActivity
import com.example.R

class KaranBudsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == "android.bluetooth.device.action.ACL_CONNECTED" ||
            action == "android.bluetooth.device.action.ACL_DISCONNECTED"
        ) {
            refreshAllWidgets(context)
        }
    }

    companion object {
        fun refreshAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, KaranBudsWidgetProvider::class.java))
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val app = context.applicationContext as? KaranBudsApplication
            val state = app?.bluetoothController?.state?.value
            val views = RemoteViews(context.packageName, R.layout.widget_karan_buds)

            if (state != null && state.isConnected) {
                views.setTextViewText(R.id.widget_device_name, state.deviceName)
                val batt = buildString {
                    state.overallBatteryPercent?.let { append("Battery: $it%") }
                        ?: append("Connected (${state.profileStatus})")
                    if (state.leftBatteryPercent != null && state.rightBatteryPercent != null) {
                        append(" • L ${state.leftBatteryPercent}% R ${state.rightBatteryPercent}%")
                    }
                    state.caseBatteryPercent?.let { append(" • Case $it%") }
                }
                views.setTextViewText(R.id.widget_battery_status, batt)
            } else {
                views.setTextViewText(R.id.widget_device_name, "No Earbuds Connected")
                views.setTextViewText(R.id.widget_battery_status, "Tap to launch KARAN BUDS PREMIUM")
            }

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
