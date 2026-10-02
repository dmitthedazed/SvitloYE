package com.occaecat.ztoeschedule.domain.notification

import com.occaecat.ztoeschedule.data.model.NotificationSettings

enum class Delivery { Normal, Silent, Drop }

/**
 * Decides at fire time how an alert is delivered. Pure.
 *
 * - Late alarms (doze, no exact-alarm permission) are dropped once they stop being useful.
 * - During quiet hours warnings are dropped (stale by morning), changes are posted silently.
 * - System Do Not Disturb is left to Android itself.
 */
object AlertRules {
    const val WARNING_MAX_DELAY_MS = 10 * 60_000L
    const val CHANGE_MAX_DELAY_MS = 30 * 60_000L

    fun delivery(
        settings: NotificationSettings,
        kind: AlertKind,
        isOutage: Boolean,
        atMs: Long,
        nowMs: Long,
        minuteOfDay: Int
    ): Delivery {
        if (!settings.alertsEnabled) return Delivery.Drop
        if (if (isOutage) !settings.outageAlerts else !settings.restoreAlerts) return Delivery.Drop
        if (kind == AlertKind.Warning && settings.leadMinutes == 0) return Delivery.Drop

        val maxDelay = if (kind == AlertKind.Warning) WARNING_MAX_DELAY_MS else CHANGE_MAX_DELAY_MS
        if (nowMs - atMs > maxDelay) return Delivery.Drop

        if (settings.quietHours.contains(minuteOfDay)) {
            return if (kind == AlertKind.Warning) Delivery.Drop else Delivery.Silent
        }
        return Delivery.Normal
    }

    /** Schedule updates are informational: never loud during quiet hours. */
    fun scheduleChangeDelivery(settings: NotificationSettings, minuteOfDay: Int): Delivery = when {
        !settings.alertsEnabled || !settings.scheduleChangeAlerts -> Delivery.Drop
        settings.quietHours.contains(minuteOfDay) -> Delivery.Silent
        else -> Delivery.Normal
    }
}
