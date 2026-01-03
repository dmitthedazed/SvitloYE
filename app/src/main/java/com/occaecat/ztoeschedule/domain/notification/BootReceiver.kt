package com.occaecat.ztoeschedule.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that restarts notification services after device boot.
 *
 * Triggered on: Intent.ACTION_BOOT_COMPLETED
 *
 * Actions:
 * 1. Schedule periodic power monitoring via WorkManager
 * 2. Start PowerStatusService if enabled in preferences
 *
 * Note: Runs on separate thread to avoid ANRs.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        Log.d(TAG, "Device boot detected")

        // Run on background thread to avoid ANR
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // 1. Always schedule periodic monitoring
                Log.d(TAG, "Scheduling power monitor")
                NotificationScheduler.schedulePowerMonitoring(context)

                // 2. Start status service if enabled
                val preferencesManager = EnergyPreferencesManager(context)
                val statusNotificationEnabled = preferencesManager.statusNotificationEnabledFlow.first()

                if (statusNotificationEnabled) {
                    Log.d(TAG, "Starting PowerStatusService")
                    PowerStatusService.start(context)
                } else {
                    Log.d(TAG, "PowerStatusService disabled, skipping")
                }

                Log.i(TAG, "Boot initialization completed")

            } catch (e: Exception) {
                Log.e(TAG, "Error during boot initialization", e)
            }
        }
    }
}
