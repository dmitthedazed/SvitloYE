package com.occaecat.ztoeschedule.data.model

/**
 * Everything the user can tune about notifications.
 *
 * [alertsEnabled] is the master switch for pop-up alerts (warnings, changes, schedule updates).
 * The persistent status notification is independent and controlled by [statusNotification].
 */
data class NotificationSettings(
    val alertsEnabled: Boolean = true,
    val outageAlerts: Boolean = true,
    val restoreAlerts: Boolean = true,
    /** Minutes before an outage to warn; 0 disables the warning. */
    val leadMinutes: Int = 15,
    val scheduleChangeAlerts: Boolean = true,
    val statusNotification: Boolean = false,
    val quietHours: QuietHours = QuietHours()
) {
    companion object {
        val LeadMinuteOptions = listOf(0, 5, 10, 15, 30, 60)
    }
}

/** Quiet hours window in minutes of day; may wrap around midnight (e.g. 22:00–07:00). */
data class QuietHours(
    val enabled: Boolean = false,
    val startMinute: Int = 22 * 60,
    val endMinute: Int = 7 * 60
) {
    fun contains(minuteOfDay: Int): Boolean = when {
        !enabled || startMinute == endMinute -> false
        startMinute < endMinute -> minuteOfDay in startMinute until endMinute
        else -> minuteOfDay >= startMinute || minuteOfDay < endMinute
    }
}
