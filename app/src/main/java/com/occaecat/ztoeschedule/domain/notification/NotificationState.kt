package com.occaecat.ztoeschedule.domain.notification

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks active notification states and history.
 *
 * Enhanced with timestamp tracking for:
 * - Debugging notification lifecycle
 * - Supporting future deduplication features
 * - Monitoring notification dismissal patterns
 */
@Singleton
class NotificationState @Inject constructor() {
    companion object {
        private const val TAG = "NotificationState"
    }

    /**
     * Currently active notification IDs.
     */
    private val _activeNotifications = MutableStateFlow<Set<Int>>(emptySet())
    val activeNotifications: StateFlow<Set<Int>> = _activeNotifications.asStateFlow()

    /**
     * Timestamps when notifications were shown (for debugging and deduplication).
     * Map of NotificationId -> System.currentTimeMillis()
     */
    private val _notificationTimestamps = MutableStateFlow<Map<Int, Long>>(emptyMap())
    val notificationTimestamps: StateFlow<Map<Int, Long>> = _notificationTimestamps.asStateFlow()

    /**
     * Mark notification as shown and record timestamp.
     *
     * @param id NotificationIds constant
     */
    fun notificationShown(id: Int) {
        _activeNotifications.update { it + id }
        _notificationTimestamps.update { it + (id to System.currentTimeMillis()) }

        Log.d(TAG, "Notification shown: $id at ${System.currentTimeMillis()}")
    }

    /**
     * Mark notification as dismissed/cancelled.
     *
     * @param id NotificationIds constant
     */
    fun notificationDismissed(id: Int) {
        _activeNotifications.update { it - id }
        _notificationTimestamps.update { it - id }

        Log.d(TAG, "Notification dismissed: $id")
    }

    /**
     * Check if an alert is currently active.
     *
     * @return true if ALERT notification (NotificationIds.ALERT) is in active set
     */
    fun hasActiveAlert(): Boolean {
        return NotificationIds.ALERT in _activeNotifications.value
    }

    /**
     * Get timestamp when a notification was shown.
     * Useful for debugging notification lifecycle.
     *
     * @param id NotificationIds constant
     * @return timestamp in milliseconds, or null if never shown
     */
    fun getShownTimestamp(id: Int): Long? {
        return _notificationTimestamps.value[id]
    }

    /**
     * Get all currently active notification info.
     * Useful for debugging.
     *
     * @return map of active notification IDs to their shown timestamps
     */
    fun getActiveNotificationsInfo(): Map<Int, Long> {
        return _activeNotifications.value.associate { id ->
            id to (_notificationTimestamps.value[id] ?: 0L)
        }
    }

    /**
     * Clear all state (called on app reset or migration).
     */
    fun clear() {
        Log.d(TAG, "NotificationState cleared")
        _activeNotifications.value = emptySet()
        _notificationTimestamps.value = emptyMap()
    }
}
