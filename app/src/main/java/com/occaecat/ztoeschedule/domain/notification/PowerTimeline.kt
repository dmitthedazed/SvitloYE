package com.occaecat.ztoeschedule.domain.notification

import com.occaecat.ztoeschedule.data.model.ScheduleStatus
import com.occaecat.ztoeschedule.domain.GroupedSchedule

enum class PowerState { On, Off, Maybe }

fun ScheduleStatus.toPowerState(): PowerState? = when (this) {
    ScheduleStatus.Available -> PowerState.On
    ScheduleStatus.Outage -> PowerState.Off
    ScheduleStatus.Probable -> PowerState.Maybe
    ScheduleStatus.Unknown -> null
}

/** A continuous stretch of time with one power state; `state == null` means unknown. */
data class PowerRun(val state: PowerState?, val startMs: Long, val endMs: Long)

/**
 * Schedule groups merged into runs of equal power state.
 *
 * Grouped schedules are split by colour and date, so two adjacent groups can share a state;
 * notifications care only about real on/off transitions, which is what runs expose.
 */
class PowerTimeline private constructor(val runs: List<PowerRun>) {

    fun currentRun(nowMs: Long): PowerRun? = runs.firstOrNull { nowMs >= it.startMs && nowMs < it.endMs }

    /** The run right after [run], or null if the schedule ends or has a gap there. */
    fun nextRun(run: PowerRun): PowerRun? {
        val index = runs.indexOf(run)
        val next = runs.getOrNull(index + 1) ?: return null
        return next.takeIf { isContiguous(run, next) }
    }

    /** Pairs of adjacent runs whose states are both known and different. */
    fun transitions(): List<Pair<PowerRun, PowerRun>> = runs.zipWithNext().filter { (a, b) ->
        isContiguous(a, b) && a.state != null && b.state != null && a.state != b.state
    }

    companion object {
        private const val CONTIGUITY_TOLERANCE_MS = 60_000L

        private fun isContiguous(a: PowerRun, b: PowerRun) =
            kotlin.math.abs(b.startMs - a.endMs) <= CONTIGUITY_TOLERANCE_MS

        fun from(schedules: List<GroupedSchedule>): PowerTimeline {
            val runs = mutableListOf<PowerRun>()
            for (group in schedules.sortedBy { it.startMs }) {
                if (group.endMs <= group.startMs) continue
                val state = group.status.toPowerState()
                val last = runs.lastOrNull()
                if (last != null && last.state == state && isContiguous(last, PowerRun(state, group.startMs, group.endMs))) {
                    runs[runs.lastIndex] = last.copy(endMs = maxOf(last.endMs, group.endMs))
                } else {
                    runs += PowerRun(state, group.startMs, group.endMs)
                }
            }
            return PowerTimeline(runs)
        }
    }
}
