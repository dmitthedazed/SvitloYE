package com.occaecat.ztoeschedule.domain.notification

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.model.PriorityMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification policy enforcement.
 *
 * Determines if/when notifications should be sent based on:
 * - Global notifications enabled toggle
 * - Quiet hours configuration (start/end time)
 * - Priority mode (All/Important/Silent)
 * - Workday mode (Mon-Fri only)
 * - System Do-Not-Disturb mode (Android 6+)
 *
 * Usage:
 * - SmartNotificationManager calls canSendAlert() before sending
 * - Returns false if any policy restriction applies
 *
 * Policies are evaluated in order of strictness:
 * 1. Check global "notifications enabled" switch
 * 2. Check system DND mode
 * 3. Check quiet hours
 * 4. Check priority mode
 * 5. Check workday mode
 */
@Singleton
class NotificationPolicy @Inject constructor(
    private val preferencesManager: EnergyPreferencesManager,
    @param:ApplicationContext private val context: Context // For AudioManager DND check
) {
    companion object {
        private const val TAG = "NotificationPolicy"
    }

    /**
     * Determine if an alert notification can be sent right now.
     *
     * Respects all configured notification policies.
     *
     * @return true if notification should be sent, false if blocked by policy
     */
    suspend fun canSendAlert(): Boolean {
        // 1. Check Global Switch - if disabled, never send
        val isGloballyEnabled = preferencesManager.notificationsEnabledFlow.first()
        if (!isGloballyEnabled) {
            Log.d(TAG, "Alert blocked: notifications globally disabled")
            return false
        }

        // 2. Check System DND Mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isSystemDNDEnabled()) {
                Log.d(TAG, "Alert blocked: system DND mode is active")
                return false
            }
        }

        // 3. Check Smart Settings
        val settings = preferencesManager.smartNotificationSettingsFlow.first()

        // 3a. Check Priority Mode
        when (settings.priorityMode) {
            PriorityMode.Smart -> {
                // Smart mode: Allow important alerts (outages), respect quiet hours
                Log.d(TAG, "Alert: priority mode is SMART, will check importance and quiet hours")
                // Continue to check quiet hours below
            }
            PriorityMode.All -> {
                // Allow all notifications regardless of quiet hours
                Log.d(TAG, "Alert allowed: priority mode is ALL (overrides quiet hours)")
                return true
            }
            PriorityMode.Important -> {
                // Fall through to check quiet hours
                Log.d(TAG, "Alert: priority mode is IMPORTANT, checking quiet hours")
            }
            else -> {} // Default/null - check quiet hours
        }

        // 3b. Check Quiet Hours
        val now = LocalTime.now()
        val start = settings.quietHoursStart
        val end = settings.quietHoursEnd

        val isQuietTime = if (start < end) {
            // Normal case: 22:00 - 08:00
            now.hour in start until end
        } else {
            // Wrapped case: 23:00 - 07:00 (crosses midnight)
            now.hour >= start || now.hour < end
        }

        if (isQuietTime) {
            Log.d(TAG, "Alert blocked: quiet hours active ($start:00 - $end:00), current time: ${now.hour}:${now.minute}")
            return false
        }

        // 4. Check Workday Mode
        if (settings.workdayMode) {
            val dayOfWeek = LocalDate.now().dayOfWeek
            val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

            if (isWeekend) {
                Log.d(TAG, "Alert blocked: workday mode enabled, today is weekend ($dayOfWeek)")
                return false
            }
        }

        Log.d(TAG, "Alert allowed: all policies passed")
        return true
    }

    /**
     * Check if status notification updates should continue.
     *
     * More lenient than alerts - status updates happen regardless,
     * but may be silenced/hidden based on settings.
     *
     * @return true if status updates should continue
     */
    suspend fun canUpdateStatus(): Boolean {
        // Status updates always continue to show current state
        // But they won't produce sounds/vibrations if muted
        val isGloballyEnabled = preferencesManager.notificationsEnabledFlow.first()
        return isGloballyEnabled
    }

    /**
     * Check if system Do-Not-Disturb mode is currently active.
     *
     * Requires Android 6.0+ (API 23).
     * On older devices, always returns false (doesn't enforce DND).
     *
     * @return true if system DND mode is on
     */
    private fun isSystemDNDEnabled(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return false
            }

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager == null) {
                Log.w(TAG, "AudioManager not available for DND check")
                return false
            }

            val isDND = audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                       (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                        audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE)

            if (isDND) {
                Log.d(TAG, "System DND detected: ringerMode=${audioManager.ringerMode}")
            }

            isDND

        } catch (e: Exception) {
            Log.w(TAG, "Error checking system DND mode", e)
            false // Don't block on error
        }
    }

    /**
     * Get human-readable reason why alert is blocked (for debugging).
     *
     * @return Description of blocking reason, or null if not blocked
     */
    suspend fun getBlockReason(): String? {
        val isGloballyEnabled = preferencesManager.notificationsEnabledFlow.first()
        if (!isGloballyEnabled) {
            return "Notifications are globally disabled"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isSystemDNDEnabled()) {
            return "System Do-Not-Disturb mode is active"
        }

        val settings = preferencesManager.smartNotificationSettingsFlow.first()

        if (settings.priorityMode != PriorityMode.All) {
            val now = LocalTime.now()
            val start = settings.quietHoursStart
            val end = settings.quietHoursEnd

            val isQuietTime = if (start < end) {
                now.hour in start until end
            } else {
                now.hour >= start || now.hour < end
            }

            if (isQuietTime) {
                return "Quiet hours are active ($start:00 - $end:00)"
            }
        }

        if (settings.workdayMode) {
            val dayOfWeek = LocalDate.now().dayOfWeek
            val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

            if (isWeekend) {
                return "Workday mode enabled and today is weekend"
            }
        }

        return null // Not blocked
    }
}
