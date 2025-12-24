package com.occaecat.ztoeschedule.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver that starts notification service after device boot
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted, scheduling power monitor")

            // Schedule periodic monitoring
            NotificationScheduler.schedulePowerMonitoring(context)
        }
    }
}

