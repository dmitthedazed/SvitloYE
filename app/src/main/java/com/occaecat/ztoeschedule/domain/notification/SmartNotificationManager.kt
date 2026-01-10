package com.occaecat.ztoeschedule.domain.notification

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart Notification Manager - refactored for centralized orchestration
 *
 * Handles:
 * - Sending high-priority alerts with policy enforcement (quiet hours, priority mode)
 * - Creating status notifications for persistent display
 * - Permission checking before notification display
 * - Deduplication through NotificationCoordinator
 *
 * Note: All alert logic and deduplication is now delegated to NotificationCoordinator.
 * This manager focuses purely on creating and posting notifications.
 */
@Singleton
class SmartNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notificationFactory: NotificationFactory,
    private val notificationPolicy: NotificationPolicy,
    private val notificationState: NotificationState
) {
    companion object {
        private const val TAG = "SmartNotificationManager"
    }

    init {
        NotificationHelper.createAllChannels(context)
        Log.d(TAG, "SmartNotificationManager initialized")
    }

    /**
     * Sends a high-priority alert about power status change.
     * Respects NotificationPolicy (quiet hours, DND, user preferences).
     *
     * Called by NotificationCoordinator after deduplication checks pass.
     */
    suspend fun sendAlert(title: String, message: String, isOutage: Boolean): Boolean {
        Log.d(TAG, "sendAlert called: $title")

        if (!hasPermission()) {
            Log.w(TAG, "Cannot post alert - POST_NOTIFICATIONS permission not granted")
            return false
        }

        // Check notification policy (quiet hours, DND, global switch)
        if (!notificationPolicy.canSendAlert()) {
            Log.i(TAG, "Alert blocked by notification policy: $title")
            return false
        }

        try {
            val notification = notificationFactory.createAlert(title, message, isOutage)
            notify(NotificationIds.ALERT, notification)
            notificationState.notificationShown(NotificationIds.ALERT)
            Log.i(TAG, "✓ Alert sent successfully: $title")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error sending alert: ${e.message}", e)
            return false
        }
    }

    /**
     * Creates a status notification for persistent display.
     * Does NOT post it - returns the notification for caller to handle.
     *
     * Useful for NotificationCoordinator to batch updates.
     */
    fun createStatusNotification(
        currentStatus: GroupedSchedule,
        allSchedules: List<GroupedSchedule>,
        address: String,
        style: StatusNotificationStyle = StatusNotificationStyle.SIMPLE
    ): Notification {
        Log.i(TAG, ">>> createStatusNotification for $address, style=$style, status=${currentStatus.status}")

        val notification = notificationFactory.createStatus(
            current = currentStatus,
            schedules = allSchedules,
            address = address,
            style = style
        )
        
        Log.i(TAG, "<<< createStatusNotification complete")
        return notification
    }

    /**
     * Creates a status notification using the best supported style for the device/settings.
     */
    fun createStatusNotificationAuto(
        currentStatus: GroupedSchedule,
        allSchedules: List<GroupedSchedule>,
        address: String
    ): Notification {
        val style = notificationFactory.getBestSupportedStyle()
        return createStatusNotification(
            currentStatus = currentStatus,
            allSchedules = allSchedules,
            address = address,
            style = style
        )
    }

    /**
     * Update an already-posted status notification.
     * Safe to call from anywhere (thread-safe via NotificationManagerCompat).
     */
    fun updateStatusNotification(notification: Notification) {
        Log.i(TAG, ">>> updateStatusNotification, flags=${notification.flags}")

        try {
            notify(NotificationIds.STATUS, notification)
            notificationState.notificationShown(NotificationIds.STATUS)
            Log.i(TAG, "<<< updateStatusNotification: posted notification id=${NotificationIds.STATUS}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating status notification", e)
        }
    }

    /**
     * Removes the status notification.
     * Called when STATUS notification is disabled or service stops.
     */
    fun cancelStatusNotification() {
        Log.d(TAG, "cancelStatusNotification")

        try {
            NotificationManagerCompat.from(context).cancel(NotificationIds.STATUS)
            notificationState.notificationDismissed(NotificationIds.STATUS)
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling status notification", e)
        }
    }

    /**
     * Check if we have permission to post notifications.
     * Required for Android 13+ (TIRAMISU).
     */
    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPost = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPost) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
            }
            return hasPost
        }
        return true // Pre-Android 13 always has permission
    }

    /**
     * Internal: Post notification with permission check.
     */
    private fun notify(id: Int, notification: Notification) {
        if (!hasPermission()) {
            Log.w(TAG, "Cannot post notification - missing permission")
            return
        }

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException posting notification", e)
        }
    }
}
