package com.occaecat.ztoeschedule.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver that handles notification action intents.
 *
 * Handles:
 * - ACTION_REFRESH - user tapped "Refresh" action in notification
 * - ACTION_DISMISSED - notification was swiped away (cleanup for Live Updates)
 *
 * Note: Toast feedback removed as it's unreliable in background.
 * Refresh action will update the status notification when complete.
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REFRESH = "com.occaecat.ztoeschedule.ACTION_REFRESH_STATUS"
        const val ACTION_DISMISSED = "com.occaecat.ztoeschedule.ACTION_NOTIFICATION_DISMISSED"
        private const val TAG = "NotificationActionReceiver"
    }

    @Inject
    lateinit var preferencesManager: EnergyPreferencesManager

    @Inject
    lateinit var smartNotificationManager: SmartNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "BroadcastReceiver.onReceive() action=${intent.action}")

        when (intent.action) {
            ACTION_REFRESH -> {
                Log.i(TAG, "Refresh action requested")

                // Trigger immediate network check
                NotificationScheduler.runImmediateCheck(context)
                // Refresh the foreground status notification immediately from cache.
                PowerStatusService.requestImmediateRefresh(context)

                // No Toast feedback - status notification will update when complete
                // This is more reliable than Toast which may not show in background
            }

            ACTION_DISMISSED -> {
                Log.i(TAG, "Notification dismissed by user")
                // Respect dismissal by disabling status notifications until user re-enables them.
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        preferencesManager.setStatusNotificationEnabled(false)
                        smartNotificationManager.cancelStatusNotification()
                        PowerStatusService.stop(context)
                        Log.i(TAG, "Status notifications disabled after dismissal")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to disable status notifications on dismissal", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
            }
        }
    }
}
