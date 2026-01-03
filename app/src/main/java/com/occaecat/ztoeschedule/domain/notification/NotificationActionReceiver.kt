package com.occaecat.ztoeschedule.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * BroadcastReceiver that handles notification action intents.
 *
 * Currently handles:
 * - ACTION_REFRESH - user tapped "Refresh" action in notification
 *
 * Note: Toast feedback removed as it's unreliable in background.
 * Refresh action will update the status notification when complete.
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REFRESH = "com.occaecat.ztoeschedule.ACTION_REFRESH_STATUS"
        private const val TAG = "NotificationActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "BroadcastReceiver.onReceive() action=${intent.action}")

        when (intent.action) {
            ACTION_REFRESH -> {
                Log.i(TAG, "Refresh action requested")

                // Trigger immediate network check
                NotificationScheduler.runImmediateCheck(context)

                // No Toast feedback - status notification will update when complete
                // This is more reliable than Toast which may not show in background
            }

            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
            }
        }
    }
}
