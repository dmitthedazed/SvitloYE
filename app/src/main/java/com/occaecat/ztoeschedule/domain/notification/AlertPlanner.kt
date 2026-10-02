package com.occaecat.ztoeschedule.domain.notification

import com.occaecat.ztoeschedule.data.model.NotificationSettings

enum class AlertKind { Warning, Change }

/**
 * One alert to fire at [atMs] about the transition [from] → [to] happening at [eventMs].
 * [untilMs] is when the new state is expected to end (null if the schedule doesn't say).
 */
data class PlannedAlert(
    val kind: AlertKind,
    val atMs: Long,
    val eventMs: Long,
    val from: PowerState,
    val to: PowerState,
    val untilMs: Long?
) {
    /** Light goes away (or may go away), as opposed to coming back. */
    val isOutage: Boolean get() = to == PowerState.Off || (to == PowerState.Maybe && from == PowerState.On)
}

/** Turns a schedule into the list of alerts to schedule, according to user settings. Pure. */
object AlertPlanner {
    const val MAX_ALERTS = 24
    const val HORIZON_MS = 36 * 60 * 60 * 1000L

    fun plan(
        timeline: PowerTimeline,
        nowMs: Long,
        settings: NotificationSettings,
        horizonMs: Long = HORIZON_MS
    ): List<PlannedAlert> {
        if (!settings.alertsEnabled) return emptyList()

        val alerts = mutableListOf<PlannedAlert>()
        for ((before, after) in timeline.transitions()) {
            val eventMs = after.startMs
            if (eventMs <= nowMs || eventMs > nowMs + horizonMs) continue

            val change = PlannedAlert(
                kind = AlertKind.Change,
                atMs = eventMs,
                eventMs = eventMs,
                from = before.state!!,
                to = after.state!!,
                untilMs = timeline.nextRun(after)?.let { after.endMs }
            )
            if (if (change.isOutage) !settings.outageAlerts else !settings.restoreAlerts) continue

            val warnAt = eventMs - settings.leadMinutes * 60_000L
            if (change.isOutage && change.from == PowerState.On && settings.leadMinutes > 0 && warnAt > nowMs) {
                alerts += change.copy(kind = AlertKind.Warning, atMs = warnAt)
            }
            alerts += change
        }
        return alerts.sortedBy { it.atMs }.take(MAX_ALERTS)
    }
}
