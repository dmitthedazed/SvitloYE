package com.occaecat.ztoeschedule.domain.notification

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Restores notifications after events that wipe or shift alarms:
 * reboot, app update, clock/time zone change, exact-alarm permission change.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var preferences: EnergyPreferencesManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in handledActions) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                NotificationSync.schedulePeriodic(context)
                NotificationSync.syncNow(context, fetch = false)
                if (preferences.statusNotificationEnabledFlow.first()) StatusNotificationService.start(context)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val handledActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
    }
}
