package com.occaecat.ztoeschedule.domain.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    // 1. Alerts: High priority, sound, vibration. For outages and restore events.
    const val CHANNEL_ALERTS_ID = "power_alerts_channel"
    
    // 2. Status: Low priority, no sound, ongoing. For persistent status bar icon.
    const val CHANNEL_STATUS_ID = "power_status_channel"
    
    // 3. Info: Low/Default priority, standard sound. For general updates and news.
    const val CHANNEL_INFO_ID = "power_info_channel"

    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = NotificationManagerCompat.from(context)

        // 1. Alerts Channel
        val alertsChannel = NotificationChannel(CHANNEL_ALERTS_ID, "Оперативні сповіщення", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Сповіщення про відключення та включення світла. Важливі."
            enableVibration(true)
            enableLights(true)
            lightColor = android.graphics.Color.RED
        }

        // 2. Status Channel
        val statusChannel = NotificationChannel(CHANNEL_STATUS_ID, "Статус світла", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Постійне відображення поточного стану в шторці."
            setShowBadge(false)
        }

        // 3. Info Channel
        val infoChannel = NotificationChannel(CHANNEL_INFO_ID, "Інформаційні повідомлення", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Оновлення графіків, новини від енергетиків."
        }

        nm.createNotificationChannel(alertsChannel)
        nm.createNotificationChannel(statusChannel)
        nm.createNotificationChannel(infoChannel)
    }
}