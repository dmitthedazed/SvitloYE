package com.occaecat.ztoeschedule.domain.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    const val CHANNEL_STATUS_ID = "power_status_channel"
    const val CHANNEL_PLANNED_ID = "planned_outages_channel"
    const val CHANNEL_EMERGENCY_ID = "emergency_updates_channel"
    const val CHANNEL_LIVE_ID = "live_activity_channel"

    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(CHANNEL_STATUS_ID, "СвітлоЄ? Статус", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Постійне сповіщення про наявність світла"
                setShowBadge(false)
            },
            NotificationChannel(CHANNEL_PLANNED_ID, "Планові відключення", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Попередження про зміну графіку за розкладом"
                enableVibration(true)
            },
            NotificationChannel(CHANNEL_EMERGENCY_ID, "Екстрені оновлення", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Термінові зміни та оновлення графіків"
                enableVibration(true)
            },
            NotificationChannel(CHANNEL_LIVE_ID, "СвітлоЄ? Таймер", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Live Activity з таймером"
                setShowBadge(false)
            }
        )

        channels.forEach { nm.createNotificationChannel(it) }
    }
}