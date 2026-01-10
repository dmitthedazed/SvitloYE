package com.occaecat.ztoeschedule.domain.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.occaecat.ztoeschedule.R

object NotificationHelper {
    // 1. Alerts: High priority, sound, vibration. For outages and restore events.
    const val CHANNEL_ALERTS_ID = "power_alerts_channel"
    
    // 2. Status: Low priority, no sound, ongoing. For persistent status bar icon.
    const val CHANNEL_STATUS_ID = "power_status_channel"
    
    // 3. Live Update: Default priority, for Android 16+ Live Updates (promoted notifications)
    const val CHANNEL_LIVE_UPDATE_ID = "live_update_channel"
    
    // 4. Info: Low/Default priority, standard sound. For general updates and news.
    const val CHANNEL_INFO_ID = "power_info_channel"

    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = NotificationManagerCompat.from(context)

        // 1. Alerts Channel
        val alertsChannel = NotificationChannel(CHANNEL_ALERTS_ID, context.getString(R.string.channel_alerts_name), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.channel_alerts_desc)
            enableVibration(true)
            enableLights(true)
            lightColor = android.graphics.Color.RED
        }

        // 2. Status Channel (standard)
        val statusChannel = NotificationChannel(CHANNEL_STATUS_ID, context.getString(R.string.channel_status_name), NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(R.string.channel_status_desc)
            setShowBadge(false)
        }

        // 3. Live Update Channel (for Android 16+ Promoted notifications)
        // IMPORTANCE_HIGH is required for proper Live Update promotion
        val liveUpdateChannel = NotificationChannel(CHANNEL_LIVE_UPDATE_ID, context.getString(R.string.channel_live_update_name), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.channel_live_update_desc)
            setShowBadge(false)
            // No sound for ongoing notification - it updates frequently
            setSound(null, null)
            enableVibration(false)
        }

        // 4. Info Channel
        val infoChannel = NotificationChannel(CHANNEL_INFO_ID, context.getString(R.string.channel_info_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.channel_info_desc)
        }

        nm.createNotificationChannel(alertsChannel)
        nm.createNotificationChannel(statusChannel)
        nm.createNotificationChannel(liveUpdateChannel)
        nm.createNotificationChannel(infoChannel)
    }
}
