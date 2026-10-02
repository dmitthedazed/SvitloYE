package com.occaecat.ztoeschedule.domain.notification

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Days whose upcoming schedule differs from what we saw last time, with their off/maybe runs. */
data class ScheduleChange(val days: List<LocalDate>, val outages: List<PowerRun>)

/**
 * Detects schedule updates by comparing compact snapshots of the timeline. Pure.
 *
 * Only the future matters: runs that already ended are ignored on both sides, so time
 * passing never counts as a change, while a newly published day does.
 */
object ScheduleChangeDetector {

    /** Several per-queue snapshots in one stored string, one per line. */
    fun unpackAll(stored: String?): Map<String, String> =
        stored.orEmpty().lines()
            .filter { it.contains('|') } // drops blanks and the legacy hash format
            .associateBy { it.substringBefore('|') }

    fun packAll(snapshots: Map<String, String>): String =
        // Keep only a handful: snapshots for removed addresses would otherwise pile up
        snapshots.entries.toList().takeLast(MAX_STORED).joinToString("\n") { it.value }

    private const val MAX_STORED = 8

    fun snapshot(key: String, timeline: PowerTimeline): String =
        key + "|" + timeline.runs.joinToString(";") { "${it.startMs}:${it.endMs}:${it.state?.ordinal ?: -1}" }

    /**
     * @return the change, or null if there is nothing to report (first sync, other address,
     * empty data or identical schedule).
     */
    fun detect(previous: String?, current: String, nowMs: Long, zone: ZoneId): ScheduleChange? {
        val (oldKey, oldRuns) = parse(previous) ?: return null
        val (newKey, newRuns) = parse(current) ?: return null
        if (oldKey != newKey || newRuns.isEmpty()) return null

        val oldByDay = byDay(oldRuns.filter { it.endMs > nowMs }, zone)
        val newByDay = byDay(newRuns.filter { it.endMs > nowMs }, zone)
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()

        // A day that vanished from the new data is just the API trimming it, not news
        val changed = newByDay.keys.filter { it >= today && newByDay[it] != oldByDay[it] }.sorted()
        if (changed.isEmpty()) return null

        val outages = changed.flatMap { newByDay[it].orEmpty() }
            .filter { it.state == PowerState.Off || it.state == PowerState.Maybe }
            .filter { it.endMs > nowMs }
            .distinct()
            .sortedBy { it.startMs }
        return ScheduleChange(changed, outages)
    }

    private fun byDay(runs: List<PowerRun>, zone: ZoneId): Map<LocalDate, List<PowerRun>> {
        val result = mutableMapOf<LocalDate, MutableList<PowerRun>>()
        for (run in runs) {
            var day = Instant.ofEpochMilli(run.startMs).atZone(zone).toLocalDate()
            val lastDay = Instant.ofEpochMilli(run.endMs - 1).atZone(zone).toLocalDate()
            while (day <= lastDay) {
                result.getOrPut(day) { mutableListOf() } += run
                day = day.plusDays(1)
            }
        }
        return result
    }

    private fun parse(snapshot: String?): Pair<String, List<PowerRun>>? = runCatching {
        val separator = snapshot!!.indexOf('|')
        require(separator >= 0) // legacy hash format
        val body = snapshot.substring(separator + 1)
        val runs = if (body.isEmpty()) emptyList() else body.split(';').map { part ->
            val (start, end, state) = part.split(':')
            PowerRun(PowerState.entries.getOrNull(state.toInt()), start.toLong(), end.toLong())
        }
        snapshot.substring(0, separator) to runs
    }.getOrNull()
}
