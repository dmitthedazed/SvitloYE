package com.occaecat.ztoeschedule.domain.notification.model

import com.occaecat.ztoeschedule.domain.GroupedSchedule

/**
 * Represents a complete UI update event triggered by notification coordinator.
 *
 * Used to ensure consistent updates across multiple UI elements
 * (notification, widget, quick settings tile) in a single atomic operation.
 */
sealed class NotificationUpdate {
    /**
     * Update status notification and all related UI.
     *
     * @param addressName Current selected address name
     * @param currentStatus Current power status
     * @param allSchedules All grouped schedules for progress calculation
     * @param updatedAtMs When this update was triggered
     */
    data class StatusUpdated(
        val addressName: String,
        val currentStatus: GroupedSchedule,
        val allSchedules: List<GroupedSchedule>,
        val updatedAtMs: Long = System.currentTimeMillis()
    ) : NotificationUpdate()

    /**
     * Alert was sent about status change.
     *
     * @param alertInfo Complete information about the alert
     */
    data class AlertSent(
        val alertInfo: AlertInfo
    ) : NotificationUpdate()

    /**
     * Address selection changed - may require restart of updates.
     *
     * @param oldAddressName Previous address
     * @param newAddressName New address
     * @param changedAtMs When change was detected
     */
    data class AddressChanged(
        val oldAddressName: String,
        val newAddressName: String,
        val changedAtMs: Long = System.currentTimeMillis()
    ) : NotificationUpdate()

    /**
     * Notifications were disabled or enabled.
     *
     * @param enabled Whether notifications are now enabled
     * @param changedAtMs When change occurred
     */
    data class NotificationsToggled(
        val enabled: Boolean,
        val changedAtMs: Long = System.currentTimeMillis()
    ) : NotificationUpdate()

    /**
     * Service is shutting down gracefully.
     *
     * @param stoppedAtMs When shutdown was initiated
     */
    data class ServiceStopped(
        val stoppedAtMs: Long = System.currentTimeMillis()
    ) : NotificationUpdate()
}
