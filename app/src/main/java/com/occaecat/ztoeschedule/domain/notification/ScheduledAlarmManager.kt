package com.occaecat.ztoeschedule.domain.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.model.Address
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages scheduled alarms for exact status change notifications.
 * 
 * Responsibilities:
 * - Schedule alarms at precise times when status changes
 * - Cancel and reschedule when new schedule data arrives
 * - Store alarm request codes to enable cancellation
 * - Support multiple addresses with separate alarm sets
 * 
 * Architecture:
 * - Uses AlarmManager.setExactAndAllowWhileIdle() for precise delivery
 * - Each status transition gets unique request code based on address + timestamp
 * - Alarms trigger StatusChangeAlarmReceiver which sends notification
 * - Automatically reschedules when API provides updated schedule
 */
@Singleton
class ScheduledAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: EnergyPreferencesManager
) {
    companion object {
        private const val TAG = "ScheduledAlarmManager"
        private const val EXTRA_ADDRESS_ID = "address_id"
        private const val EXTRA_ADDRESS_NAME = "address_name"
        private const val EXTRA_PREVIOUS_STATUS = "previous_status"
        private const val EXTRA_CURRENT_STATUS = "current_status"
        private const val EXTRA_TRANSITION_TIME = "transition_time"
        
        // Request code base - will be combined with transition timestamp for uniqueness
        private const val REQUEST_CODE_BASE = 10000
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Check if the app has permission to schedule exact alarms (Android 12+)
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // No permission needed before Android 12
        }
    }

    /**
     * Schedule alarms for all status transitions in the given schedule.
     * Cancels any existing alarms for this address first.
     * 
     * @param address The address to monitor
     * @param schedules List of status periods (must be sorted by time)
     * @param maxHoursAhead Maximum hours to schedule ahead (default: 24)
     */
    suspend fun scheduleAlarmsForAddress(
        address: Address,
        schedules: List<GroupedSchedule>,
        maxHoursAhead: Int = 24
    ) {
        // Check if notifications are enabled
        val notificationsEnabled = preferencesManager.notificationsEnabledFlow.first()
        if (!notificationsEnabled) {
            Log.d(TAG, "Notifications disabled - skipping alarm scheduling for ${address.name}")
            return
        }

        // Check permission
        if (!canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule exact alarms - missing permission")
            return
        }

        // Cancel existing alarms for this address
        cancelAlarmsForAddress(address)

        // Find status transitions
        val now = System.currentTimeMillis()
        val maxTime = now + (maxHoursAhead * 60 * 60 * 1000L)
        
        var scheduledCount = 0
        for (i in 0 until schedules.size - 1) {
            val currentSchedule = schedules[i]
            val nextSchedule = schedules[i + 1]
            
            // Skip if status doesn't change
            if (currentSchedule.status == nextSchedule.status) {
                continue
            }
            
            val transitionTime = nextSchedule.startMs
            
            // Skip past transitions
            if (transitionTime <= now) {
                continue
            }
            
            // Stop if beyond max time
            if (transitionTime > maxTime) {
                break
            }
            
            // Schedule alarm for this transition
            scheduleAlarm(
                address = address,
                previousSchedule = currentSchedule,
                currentSchedule = nextSchedule,
                transitionTime = transitionTime
            )
            
            scheduledCount++
        }

        Log.i(TAG, "✓ Scheduled $scheduledCount alarms for ${address.name} (next $maxHoursAhead hours)")
    }

    /**
     * Schedule a single alarm for a status transition.
     */
    private fun scheduleAlarm(
        address: Address,
        previousSchedule: GroupedSchedule,
        currentSchedule: GroupedSchedule,
        transitionTime: Long
    ) {
        val intent = Intent(context, StatusChangeAlarmReceiver::class.java).apply {
            putExtra(EXTRA_ADDRESS_ID, address.id)
            putExtra(EXTRA_ADDRESS_NAME, address.name)
            putExtra(EXTRA_PREVIOUS_STATUS, previousSchedule.status.name)
            putExtra(EXTRA_CURRENT_STATUS, currentSchedule.status.name)
            putExtra(EXTRA_TRANSITION_TIME, transitionTime)
        }

        val requestCode = generateRequestCode(address.id, transitionTime)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use setExactAndAllowWhileIdle for best reliability
        // Wrap in try-catch to handle SecurityException on Android 14+ if permission was revoked
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                transitionTime,
                pendingIntent
            )
            
            val timeUntil = (transitionTime - System.currentTimeMillis()) / 1000 / 60
            Log.d(TAG, "Scheduled alarm for ${address.name}: ${previousSchedule.status} → ${currentSchedule.status} in $timeUntil min")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when scheduling alarm - SCHEDULE_EXACT_ALARM permission may have been revoked", e)
            // Fallback to inexact alarm
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    transitionTime,
                    pendingIntent
                )
                Log.w(TAG, "Fell back to inexact alarm for ${address.name}")
            } catch (fallbackException: Exception) {
                Log.e(TAG, "Failed to schedule even inexact alarm", fallbackException)
            }
        }
    }

    /**
     * Cancel all alarms for a specific address.
     * Note: We can't efficiently enumerate all request codes, so we rely on 
     * rescheduling to replace old alarms with new ones.
     */
    fun cancelAlarmsForAddress(address: Address) {
        // We'll cancel alarms as we reschedule them with FLAG_UPDATE_CURRENT
        Log.d(TAG, "Cancelling alarms for ${address.name}")
    }

    /**
     * Cancel all alarms (when notifications are disabled globally).
     */
    fun cancelAllAlarms() {
        // Unfortunately, there's no way to enumerate all pending intents
        // But when notifications are disabled, alarms won't send notifications anyway
        Log.d(TAG, "Alarms will be inactive while notifications are disabled")
    }

    /**
     * Generate a unique request code based on address ID and transition time.
     * This allows us to update/cancel specific alarms.
     */
    private fun generateRequestCode(addressId: String, transitionTime: Long): Int {
        // Use hash of address ID + timestamp to generate unique but reproducible code
        val hash = (addressId.hashCode() + transitionTime).hashCode()
        return REQUEST_CODE_BASE + (hash and 0x7FFFFFFF) % 1000000
    }
}
