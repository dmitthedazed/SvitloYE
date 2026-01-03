package com.occaecat.ztoeschedule.domain.notification.model

import com.occaecat.ztoeschedule.data.model.ScheduleStatus

/**
 * Information about an alert notification that was sent or attempted.
 *
 * Used for tracking, deduplication, and debugging alert history.
 */
data class AlertInfo(
    /**
     * Address name that triggered this alert.
     */
    val addressName: String,

    /**
     * Previous status before the change.
     */
    val previousStatus: ScheduleStatus,

    /**
     * New status after the change.
     */
    val newStatus: ScheduleStatus,

    /**
     * Timestamp when alert was sent (milliseconds since epoch).
     */
    val sentAtMs: Long,

    /**
     * Human-readable alert title that was sent.
     */
    val title: String,

    /**
     * Human-readable alert message that was sent.
     */
    val message: String,

    /**
     * Whether this was an outage alert (power off) or restoration (power on).
     */
    val isOutage: Boolean
) {
    /**
     * Check if this alert is still within the debounce window.
     * @param currentTimeMs Current time in milliseconds since epoch
     * @param debounceMs Debounce window duration in milliseconds
     */
    fun isWithinDebounce(currentTimeMs: Long, debounceMs: Long): Boolean {
        return (currentTimeMs - sentAtMs) < debounceMs
    }

    /**
     * Check if this alert matches another in terms of status change.
     * Used to identify duplicate alerts even if addresses differ.
     */
    fun matchesStatusChange(other: AlertInfo): Boolean {
        return this.previousStatus == other.previousStatus &&
               this.newStatus == other.newStatus &&
               this.isOutage == other.isOutage
    }
}
