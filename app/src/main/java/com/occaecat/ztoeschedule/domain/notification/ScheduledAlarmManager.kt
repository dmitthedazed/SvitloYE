package com.occaecat.ztoeschedule.domain.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.model.Address
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
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
        private const val MAX_GROUPED_SCHEDULES_FOR_FULL_DAY = 300
        private const val MAX_ALARMS_TO_SCHEDULE = 10 // Limit pending alarms to prevent system overload
        private const val HIGH_FREQUENCY_WINDOW_MS = 2 * 60 * 60 * 1000L
        
        // SharedPreferences for storing alarm request codes
        private const val PREFS_NAME = "scheduled_alarms"
        private const val KEY_PREFIX_CODES = "alarm_codes_"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val alarmPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmLock = Any()
    
    // In-memory cache of scheduled alarm codes per address
    private val scheduledAlarmCodes = mutableMapOf<String, MutableSet<Int>>()

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
        withContext(Dispatchers.IO) {
            // Check if notifications are enabled
            val notificationsEnabled = preferencesManager.notificationsEnabledFlow.first()
            if (!notificationsEnabled) {
                Log.d(TAG, "Notifications disabled - skipping alarm scheduling for ${address.name}")
                return@withContext
            }

            synchronized(alarmLock) {
            // Check permission - log warning but proceed to fallback logic
            if (!canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms - missing permission. Will attempt inexact fallback.")
            }

            // Cancel existing alarms for this address
            cancelAlarmsForAddress(address)

            // Find status transitions
            val now = System.currentTimeMillis()
            val maxTime = now + (maxHoursAhead * 60 * 60 * 1000L)
            val effectiveMaxTime = if (schedules.size > MAX_GROUPED_SCHEDULES_FOR_FULL_DAY) {
                Log.w(
                    TAG,
                    "Large schedule (${schedules.size} groups) for ${address.name}; limiting alarms to next 2 hours."
                )
                minOf(maxTime, now + HIGH_FREQUENCY_WINDOW_MS)
            } else {
                maxTime
            }

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
                if (transitionTime > effectiveMaxTime) {
                    break
                }

                // Stop if we have scheduled enough alarms
                if (scheduledCount >= MAX_ALARMS_TO_SCHEDULE) {
                    Log.i(TAG, "Reached max alarm limit ($MAX_ALARMS_TO_SCHEDULE) for ${address.name}")
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

            val windowHours = ((effectiveMaxTime - now) / (60 * 60 * 1000L)).coerceAtLeast(0)
            Log.i(TAG, "Scheduled $scheduledCount alarms for ${address.name} (next $windowHours hours)")
            }
        }
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

        // Use setExactAndAllowWhileIdle if permitted, otherwise setAndAllowWhileIdle
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    transitionTime,
                    pendingIntent
                )
                // Save request code for later cancellation
                saveAlarmCode(address.id, requestCode)
                
                val timeUntil = (transitionTime - System.currentTimeMillis()) / 1000 / 60
                Log.d(TAG, "Scheduled exact alarm for ${address.name}: ${previousSchedule.status} -> ${currentSchedule.status} in $timeUntil min")
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    transitionTime,
                    pendingIntent
                )
                // Save request code for later cancellation
                saveAlarmCode(address.id, requestCode)
                
                Log.w(TAG, "Scheduled inexact alarm (no permission) for ${address.name}")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when scheduling alarm - trying fallback", e)
            // Fallback to inexact alarm if exact failed despite check
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    transitionTime,
                    pendingIntent
                )
                // Save request code for later cancellation
                saveAlarmCode(address.id, requestCode)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to schedule fallback alarm", ex)
            }
        }
    }

    /**
     * Cancel all alarms for a specific address.
     * Uses stored request codes to properly cancel pending intents.
     */
    fun cancelAlarmsForAddress(address: Address) {
        synchronized(alarmLock) {
        Log.d(TAG, "Cancelling alarms for ${address.name}")
        
        // Get stored codes from SharedPreferences
        val storedCodesStr = alarmPrefs.getString(KEY_PREFIX_CODES + address.id, "") ?: ""
        val storedCodes = if (storedCodesStr.isNotEmpty()) {
            storedCodesStr.split(",").mapNotNull { it.toIntOrNull() }.toMutableSet()
        } else {
            mutableSetOf()
        }
        
        // Merge with in-memory cache
        val allCodes = (scheduledAlarmCodes[address.id] ?: mutableSetOf()) + storedCodes
        
        var cancelledCount = 0
        for (requestCode in allCodes) {
            val intent = Intent(context, StatusChangeAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                cancelledCount++
            }
        }
        
        // Clear stored codes
        scheduledAlarmCodes.remove(address.id)
        alarmPrefs.edit { remove(KEY_PREFIX_CODES + address.id) }
        
        Log.i(TAG, "Cancelled $cancelledCount alarms for ${address.name}")
        }
    }

    /**
     * Cancel all alarms (when notifications are disabled globally).
     */
    fun cancelAllAlarms() {
        synchronized(alarmLock) {
        Log.d(TAG, "Cancelling ALL scheduled alarms")
        
        // Get all address IDs from preferences
        val allKeys = alarmPrefs.all.keys.filter { it.startsWith(KEY_PREFIX_CODES) }
        var totalCancelled = 0
        
        for (key in allKeys) {
            val addressId = key.removePrefix(KEY_PREFIX_CODES)
            val codesStr = alarmPrefs.getString(key, "") ?: ""
            val codes = if (codesStr.isNotEmpty()) {
                codesStr.split(",").mapNotNull { it.toIntOrNull() }
            } else {
                emptyList()
            }
            
            for (requestCode in codes) {
                val intent = Intent(context, StatusChangeAlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                    totalCancelled++
                }
            }
        }
        
        // Clear all stored codes
        scheduledAlarmCodes.clear()
        alarmPrefs.edit { clear() }
        
        Log.i(TAG, "Cancelled $totalCancelled alarms total")
        }
    }
    
    /**
     * Save alarm request code for later cancellation.
     */
    private fun saveAlarmCode(addressId: String, requestCode: Int) {
        synchronized(alarmLock) {
        // Update in-memory cache
        val codes = scheduledAlarmCodes.getOrPut(addressId) { mutableSetOf() }
        codes.add(requestCode)
        
        // Persist to SharedPreferences
        val codesStr = codes.toList().joinToString(",")
        alarmPrefs.edit { putString(KEY_PREFIX_CODES + addressId, codesStr) }
        }
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
