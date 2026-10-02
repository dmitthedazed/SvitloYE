package com.occaecat.ztoeschedule.domain.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.occaecat.ztoeschedule.R

object NotificationChannels {
    const val ALERTS = "power_alerts_channel"
    const val SCHEDULE = "schedule_updates_channel"
    const val STATUS = "power_status_channel"
    /** Android 16 Live Updates need at least default importance, unlike the quiet status channel. */
    const val LIVE_UPDATE = "live_update_channel"

    private val obsolete = listOf("power_info_channel")

    fun createAll(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        obsolete.forEach(nm::deleteNotificationChannel)

        nm.createNotificationChannels(
            listOf(
                NotificationChannel(ALERTS, context.getString(R.string.channel_alerts_name), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.channel_alerts_desc)
                    enableVibration(true)
                },
                NotificationChannel(SCHEDULE, context.getString(R.string.channel_schedule_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.channel_schedule_desc)
                },
                NotificationChannel(STATUS, context.getString(R.string.channel_status_name), NotificationManager.IMPORTANCE_LOW).apply {
                    description = context.getString(R.string.channel_status_desc)
                    setShowBadge(false)
                },
                NotificationChannel(LIVE_UPDATE, context.getString(R.string.channel_live_update_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.channel_live_update_desc)
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        )
    }
}

object NotificationIds {
    const val STATUS = 1002
    const val WARNING = 1010
    const val CHANGE = 1011
    const val SCHEDULE = 1012
    const val SYNC = 1013
}
