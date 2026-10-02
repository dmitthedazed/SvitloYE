package com.occaecat.ztoeschedule.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Buttons and swipe-dismiss on the status notification. */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var preferences: EnergyPreferencesManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REFRESH -> {
                StatusNotificationService.refresh()
                NotificationSync.syncNow(context, fetch = true, urgent = true)
            }
            // Swiping the status away means "I don't want it": respect that until re-enabled in settings
            ACTION_DISMISSED -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        preferences.setStatusNotificationEnabled(false)
                        StatusNotificationService.stop(context)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.occaecat.ztoeschedule.ACTION_REFRESH_STATUS"
        const val ACTION_DISMISSED = "com.occaecat.ztoeschedule.ACTION_NOTIFICATION_DISMISSED"
    }
}
