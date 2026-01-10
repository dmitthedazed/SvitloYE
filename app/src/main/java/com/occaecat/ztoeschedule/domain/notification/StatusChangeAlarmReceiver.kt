package com.occaecat.ztoeschedule.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.occaecat.ztoeschedule.data.model.ScheduleStatus
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * BroadcastReceiver that handles scheduled alarms for status changes.
 * 
 * Flow:
 * 1. AlarmManager triggers this receiver at exact time of status change
 * 2. Extract address and status info from intent
 * 3. Send notification via SmartNotificationManager
 * 4. Log for debugging
 * 
 * Note: Uses goAsync() to handle coroutines properly in BroadcastReceiver.
 */
@AndroidEntryPoint
class StatusChangeAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var smartNotificationManager: SmartNotificationManager
    
    @Inject
    lateinit var preferencesManager: EnergyPreferencesManager

    companion object {
        private const val TAG = "StatusChangeAlarmReceiver"
        const val EXTRA_ADDRESS_ID = "address_id"
        const val EXTRA_ADDRESS_NAME = "address_name"
        const val EXTRA_PREVIOUS_STATUS = "previous_status"
        const val EXTRA_CURRENT_STATUS = "current_status"
        const val EXTRA_TRANSITION_TIME = "transition_time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "⏰ Alarm triggered!")

        // Extract data from intent
        val addressId = intent.getStringExtra(EXTRA_ADDRESS_ID) ?: return
        val addressName = intent.getStringExtra(EXTRA_ADDRESS_NAME) ?: return
        val previousStatusName = intent.getStringExtra(EXTRA_PREVIOUS_STATUS) ?: return
        val currentStatusName = intent.getStringExtra(EXTRA_CURRENT_STATUS) ?: return
        val transitionTime = intent.getLongExtra(EXTRA_TRANSITION_TIME, 0L)

        // Parse status enums
        val previousStatus = try {
            ScheduleStatus.valueOf(previousStatusName)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid previous status: $previousStatusName")
            return
        }

        val currentStatus = try {
            ScheduleStatus.valueOf(currentStatusName)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid current status: $currentStatusName")
            return
        }

        Log.i(TAG, "Status change detected for $addressName: $previousStatus → $currentStatus")

        // Use goAsync() to handle coroutine
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                sendNotification(addressName, previousStatus, currentStatus)
                val statusNotificationsEnabled = preferencesManager.statusNotificationEnabledFlow.first()
                if (statusNotificationsEnabled) {
                    PowerStatusService.requestImmediateRefresh(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending notification: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Send notification about status change
     */
    private suspend fun sendNotification(
        addressName: String,
        previousStatus: ScheduleStatus,
        currentStatus: ScheduleStatus
    ): Boolean {
        // Generate notification title and message
        val title = when (currentStatus) {
            ScheduleStatus.Available -> "⚡ Світло є!"
            ScheduleStatus.Outage -> "❌ Вимкнення"
            ScheduleStatus.Probable -> "⚠️ Ймовірне вимкнення"
            ScheduleStatus.Unknown -> "❓ Невідомий статус"
        }

        val message = buildNotificationMessage(addressName, previousStatus, currentStatus)
        val isOutage = currentStatus == ScheduleStatus.Outage

        // Send alert
        val sent = smartNotificationManager.sendAlert(title, message, isOutage)
        if (sent) {
            Log.i(TAG, "Notification sent: $title - $message")
        } else {
            Log.i(TAG, "Notification suppressed: $title")
        }
        return sent
    }

    /**
     * Build notification message text
     */
    private fun buildNotificationMessage(
        addressName: String,
        previousStatus: ScheduleStatus,
        currentStatus: ScheduleStatus
    ): String {
        val statusText = when (currentStatus) {
            ScheduleStatus.Available -> "світло є"
            ScheduleStatus.Outage -> "вимкнення"
            ScheduleStatus.Probable -> "ймовірне вимкнення"
            ScheduleStatus.Unknown -> "невідомо"
        }

        val previousText = when (previousStatus) {
            ScheduleStatus.Available -> "світла"
            ScheduleStatus.Outage -> "вимкнення"
            ScheduleStatus.Probable -> "ймовірного вимкнення"
            ScheduleStatus.Unknown -> "невідомо"
        }

        return "$addressName: $statusText (було: $previousText)"
    }
}
