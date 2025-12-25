package com.occaecat.ztoeschedule.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.occaecat.ztoeschedule.BuildConfig
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receiver that starts notification service after device boot
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (BuildConfig.DEBUG) {
                Log.d("BootReceiver", "Device booted, scheduling power monitor")
            }

            // Schedule periodic monitoring
            NotificationScheduler.schedulePowerMonitoring(context)

            // Start status notification service if enabled
            val preferencesManager = EnergyPreferencesManager(context)
            CoroutineScope(Dispatchers.IO).launch {
                val statusNotificationEnabled = preferencesManager.statusNotificationEnabledFlow.first()
                if (statusNotificationEnabled) {
                    val liveActivityEnabled = preferencesManager.liveActivityEnabledFlow.first()
                    if (liveActivityEnabled) {
                        LiveActivityNotificationService.start(context)
                    } else {
                        StatusNotificationService.start(context)
                    }
                }
            }
        }
    }
}
