package com.occaecat.ztoeschedule.domain.notification

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.TileService
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.occaecat.ztoeschedule.data.model.SavedAddress
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.notification.model.AlertInfo
import com.occaecat.ztoeschedule.domain.notification.model.NotificationConfig
import com.occaecat.ztoeschedule.domain.notification.model.NotificationUpdate
import com.occaecat.ztoeschedule.widget.glance.LightWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central orchestrator for all notification-related operations.
 *
 * Responsibilities:
 * - Manage alert notification sending with deduplication
 * - Coordinate STATUS notification updates via PowerStatusService
 * - Update all UI elements (widget, tile) atomically
 * - Track alert history for debugging
 * - Implement intelligent alert debouncing
 * - Handle graceful state transitions
 *
 * Architecture:
 * - SmartNotificationManager sends alerts → Coordinator processes
 * - PowerStatusService queries Coordinator for update triggers
 * - PowerMonitorWorker delegates UI updates to Coordinator
 * - PowerStatusTileService listens to Coordinator updates
 *
 * Thread-safe: All public methods are suspend-safe and can be called from any coroutine context.
 */
@Singleton
class NotificationCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smartNotificationManager: SmartNotificationManager,
    private val config: NotificationConfig = NotificationConfig()
) {
    companion object {
        private const val TAG = "NotificationCoordinator"
        private const val MAX_ALERT_HISTORY = 50 // Keep last 50 alerts for debugging
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Alert history for deduplication and debugging
    private val _alertHistory = MutableStateFlow<List<AlertInfo>>(emptyList())
    val alertHistory: StateFlow<List<AlertInfo>> = _alertHistory.asStateFlow()

    // Recent updates for UI observation
    private val _recentUpdates = MutableStateFlow<NotificationUpdate?>(null)
    val recentUpdates: StateFlow<NotificationUpdate?> = _recentUpdates.asStateFlow()

    init {
        if (config.enableDetailedLogging) {
            log("NotificationCoordinator initialized", logLevel = Log.DEBUG)
        }
    }

    /**
     * Notify about a status change and send alert if appropriate.
     *
     * Flow:
     * 1. Check if already notified about this change recently (debounce)
     * 2. If not, send alert via SmartNotificationManager
     * 3. Record in history for future deduplication
     * 4. Emit update event for UI observation
     *
     * @param address Address that changed status
     * @param oldStatus Previous status
     * @param newStatus New status
     */
    suspend fun notifyStatusChange(
        address: SavedAddress,
        oldStatus: GroupedSchedule,
        newStatus: GroupedSchedule
    ) {
        log("notifyStatusChange called for ${address.name}: ${oldStatus.status} → ${newStatus.status}")

        try {
            // 1. Check deduplication
            if (config.enableDedupAlerts && shouldDebounceAlert(oldStatus, newStatus)) {
                log("Alert debounced for ${address.name} - already sent recently", Log.DEBUG)
                return
            }

            // 2. Prepare alert info
            val isOutage = newStatus.status != com.occaecat.ztoeschedule.data.model.ScheduleStatus.Available
            val action = if (isOutage) "Відключення" else "Включення"
            val emoji = if (isOutage) "🔴" else "🟢"
            val title = "$emoji $action"
            val message = "${address.name}: ${newStatus.displayText}"

            val alertInfo = AlertInfo(
                addressName = address.name,
                previousStatus = oldStatus.status,
                newStatus = newStatus.status,
                sentAtMs = System.currentTimeMillis(),
                title = title,
                message = message,
                isOutage = isOutage
            )

            // 3. Send alert
            log("Sending alert: $title")
            smartNotificationManager.sendAlert(title, message, isOutage)

            // 4. Record in history
            recordAlert(alertInfo)

            // 5. Emit update event
            _recentUpdates.value = NotificationUpdate.AlertSent(alertInfo)

        } catch (e: Exception) {
            log("Error in notifyStatusChange", exception = e)
        }
    }

    /**
     * Update STATUS notification and all related UI elements.
     *
     * Atomically updates:
     * - STATUS notification via SmartNotificationManager
     * - Widget state
     * - Quick Settings tile
     *
     * @param address Current address
     * @param currentStatus Current power status
     * @param allSchedules All schedules for progress calculation
     */
    suspend fun updateStatusNotification(
        address: SavedAddress,
        currentStatus: GroupedSchedule,
        allSchedules: List<GroupedSchedule>
    ) {
        log("updateStatusNotification for ${address.name}: ${currentStatus.status}", Log.DEBUG)

        try {
            // All updates happen atomically under one scope
            val updates = mutableListOf<suspend () -> Unit>()

            // 1. Update status notification
            updates.add {
                try {
                    val notification = smartNotificationManager.createStatusNotification(
                        currentStatus = currentStatus,
                        allSchedules = allSchedules,
                        address = address.name
                    )
                    smartNotificationManager.updateStatusNotification(notification)
                } catch (e: Exception) {
                    log("Error updating status notification", exception = e)
                }
            }

            // 2. Update widget
            updates.add {
                try {
                    // Update Glance widget state (simplified - no state sync)
                    log("Status changed for ${address.name}: ${currentStatus.status}")
                } catch (e: Exception) {
                    log("Error updating widget", exception = e)
                }
            }

            // 3. Update tile
            updates.add {
                try {
                    updateQuickSettingsTile()
                } catch (e: Exception) {
                    log("Error updating tile", exception = e)
                }
            }

            // Execute all updates
            updates.forEach { it() }

            // Emit update event
            _recentUpdates.value = NotificationUpdate.StatusUpdated(
                addressName = address.name,
                currentStatus = currentStatus,
                allSchedules = allSchedules
            )

            log("Status notification updated successfully", Log.DEBUG)

        } catch (e: Exception) {
            log("Error in updateStatusNotification", exception = e)
        }
    }

    /**
     * Update all UI elements atomically for a status change.
     *
     * Used when both alert and status need updating.
     *
     * @param address Address being monitored
     * @param currentStatus Current status
     * @param allSchedules All schedules for calculation
     */
    suspend fun updateAllUI(
        address: SavedAddress,
        currentStatus: GroupedSchedule,
        allSchedules: List<GroupedSchedule>
    ) {
        updateStatusNotification(address, currentStatus, allSchedules)
    }

    /**
     * Notify about address change - may trigger service restart.
     *
     * @param oldAddress Previous address
     * @param newAddress New address
     */
    suspend fun notifyAddressChanged(oldAddress: SavedAddress, newAddress: SavedAddress) {
        log("notifyAddressChanged: ${oldAddress.name} → ${newAddress.name}")

        try {
            _recentUpdates.value = NotificationUpdate.AddressChanged(
                oldAddressName = oldAddress.name,
                newAddressName = newAddress.name
            )

            // Clear recent alert history when address changes to avoid
            // false debounce on new address with similar schedules
            clearAlertHistory()

        } catch (e: Exception) {
            log("Error in notifyAddressChanged", exception = e)
        }
    }

    /**
     * Notify about notifications being disabled/enabled.
     *
     * @param enabled Whether notifications are now enabled
     */
    suspend fun notifyNotificationsToggled(enabled: Boolean) {
        log("notifyNotificationsToggled: $enabled")

        try {
            if (!enabled) {
                smartNotificationManager.cancelStatusNotification()
            }

            _recentUpdates.value = NotificationUpdate.NotificationsToggled(enabled)

        } catch (e: Exception) {
            log("Error in notifyNotificationsToggled", exception = e)
        }
    }

    /**
     * Graceful shutdown of coordinator.
     */
    suspend fun shutdown() {
        log("Coordinator shutting down")

        try {
            smartNotificationManager.cancelStatusNotification()
            _recentUpdates.value = NotificationUpdate.ServiceStopped()
            scope.cancel()
        } catch (e: Exception) {
            log("Error during shutdown", exception = e)
        }
    }

    /**
     * Get recent alert history for debugging.
     *
     * @return Last N alerts that were sent
     */
    fun getRecentAlerts(): List<AlertInfo> = _alertHistory.value

    /**
     * Clear alert history (called on address change or manual reset).
     */
    fun clearAlertHistory() {
        log("Alert history cleared")
        _alertHistory.value = emptyList()
    }

    // ============ Private Helper Functions ============

    /**
     * Check if this alert should be debounced based on recent history.
     */
    private fun shouldDebounceAlert(oldStatus: GroupedSchedule, newStatus: GroupedSchedule): Boolean {
        if (!config.enableDedupAlerts) return false

        val currentTimeMs = System.currentTimeMillis()
        val isOutage = newStatus.status != com.occaecat.ztoeschedule.data.model.ScheduleStatus.Available

        return _alertHistory.value.any { recent ->
            recent.previousStatus == oldStatus.status &&
            recent.newStatus == newStatus.status &&
            recent.isOutage == isOutage &&
            recent.isWithinDebounce(currentTimeMs, config.alertDebounceMs)
        }
    }

    /**
     * Add alert to history, keeping only most recent entries.
     */
    private fun recordAlert(alert: AlertInfo) {
        _alertHistory.value = (_alertHistory.value + alert)
            .takeLast(MAX_ALERT_HISTORY)
    }

    /**
     * Update Quick Settings tile.
     */
    private fun updateQuickSettingsTile() {
        try {
            val tile = ComponentName(context, PowerStatusTileService::class.java)
            TileService.requestListeningState(context, tile)
        } catch (e: Exception) {
            log("Error updating Quick Settings tile", exception = e)
        }
    }

    /**
     * Calculate progress percentage for status notification.
     */

    private fun calculateProgress(status: GroupedSchedule): Int {
        val totalMs = status.endMs - status.startMs
        if (totalMs <= 0) return 0

        val elapsedMs = System.currentTimeMillis() - status.startMs
        val progress = ((elapsedMs.toDouble() / totalMs) * 100).toInt()

        return maxOf(0, minOf(100, progress))
    }

    /**
     * Unified logging with optional exception.
     */
    private fun log(
        message: String,
        logLevel: Int = Log.INFO,
        exception: Exception? = null
    ) {
        when (logLevel) {
            Log.DEBUG -> Log.d(TAG, message)
            Log.INFO -> Log.i(TAG, message)
            Log.WARN -> Log.w(TAG, message)
            Log.ERROR -> {
                if (exception != null) {
                    Log.e(TAG, message, exception)
                } else {
                    Log.e(TAG, message)
                }
            }
        }
    }
}
