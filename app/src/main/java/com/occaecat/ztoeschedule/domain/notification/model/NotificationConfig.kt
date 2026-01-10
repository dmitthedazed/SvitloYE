package com.occaecat.ztoeschedule.domain.notification.model

/**
 * Configuration for notification behavior and feature flags.
 *
 * Allows A/B testing and fine-tuning of notification system parameters.
 */
data class NotificationConfig(
    /**
     * Enable centralized NotificationCoordinator orchestration.
     * If false, falls back to legacy behavior (service + worker managing separately).
     */
    val enableCoordinator: Boolean = true,

    /**
     * Enable deduplication of alert notifications using timestamps.
     * Prevents sending duplicate alerts within the debounce window.
     */
    val enableDedupAlerts: Boolean = true,

    /**
     * Enable PROMOTED notification style for API 36+ (Android 16).
     * If false, uses LIVE_ACTIVITY for API 31+ and SIMPLE for older versions.
     */
    val enablePromotedStyle: Boolean = true,

    /**
     * Debounce window in milliseconds for alert deduplication.
     * Alerts sent within this time window for the same status change will be ignored.
     * Default: 60 seconds.
     */
    val alertDebounceMs: Long = 60_000L,

    /**
     * Update interval for STATUS notification in milliseconds.
     * How often PowerStatusService updates the persistent notification.
     * Default: 60 seconds.
     */
    val statusUpdateIntervalMs: Long = 60_000L,

    /**
     * Periodic check interval for PowerMonitorWorker in minutes.
     * How often background monitoring checks for status changes.
     * Default: 15 minutes.
     */
    val workerCheckIntervalMinutes: Long = 15L,

    /**
     * Network timeout for repository calls in milliseconds.
     * Prevents Service from hanging on slow/unavailable network.
     * Default: 10 seconds.
     */
    val networkTimeoutMs: Long = 10_000L,

    /**
     * Enable detailed logging for debugging notification system.
     * All components will log entry/exit of critical functions.
     */
    val enableDetailedLogging: Boolean = false
)
