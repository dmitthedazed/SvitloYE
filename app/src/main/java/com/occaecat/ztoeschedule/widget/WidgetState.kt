package com.occaecat.ztoeschedule.widget

import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.notification.PowerRun
import com.occaecat.ztoeschedule.domain.notification.PowerState
import com.occaecat.ztoeschedule.domain.notification.PowerTimeline
import com.occaecat.ztoeschedule.domain.time.ScheduleZone
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton

/** Everything a widget needs for one render; built from the local cache only, never the network. */
sealed interface WidgetState {
    data object NoAddress : WidgetState

    data class NoData(val addressName: String) : WidgetState

    data class Ready(
        val addressName: String,
        val queue: String,
        val now: Long,
        val current: PowerRun?,
        val next: PowerRun?,
        /** Runs that have not ended yet, starting with the current one. */
        val upcoming: List<PowerRun>,
        /** Runs overlapping today, for the day strip. */
        val today: List<PowerRun>,
        val dayStart: Long,
        val dayEnd: Long
    ) : WidgetState {
        val state: PowerState? get() = current?.state

        /**
         * When the widget shows something stale and must be redrawn: the next period boundary,
         * and at the latest midnight, when the day strip rolls over (runs are merged across days,
         * so a calm night has no boundary at all).
         */
        val nextChangeAt: Long
            get() = listOfNotNull(current?.endMs, upcoming.firstOrNull { it.startMs > now }?.startMs, dayEnd)
                .filter { it > now }
                .minOrNull() ?: dayEnd
    }
}

data class WidgetSnapshot(val state: WidgetState, val dynamicColor: Boolean)

/** Widgets follow the primary address, the same one notifications and the tile use. */
@Singleton
class WidgetStateLoader @Inject constructor(
    private val repository: EnergyRepository,
    private val preferences: EnergyPreferencesManager,
    private val timeProvider: TimeProvider
) {
    suspend fun load(): WidgetSnapshot {
        val dynamicColor = runCatching { preferences.dynamicColorsFlow.first() }.getOrDefault(true)
        return WidgetSnapshot(loadState(), dynamicColor)
    }

    private suspend fun loadState(): WidgetState {
        val address = repository.getSavedAddresses().minByOrNull { it.priority } ?: return WidgetState.NoAddress
        val schedules = repository.getCachedScheduleWithMessages(address.cherga, address.pidcherga)
            .getOrNull()?.schedules
            ?.takeIf { it.isNotEmpty() }
            ?: return WidgetState.NoData(address.name)
        val timeline = PowerTimeline.from(ScheduleMapper.getGroupedSchedule(schedules))
        return readyState(address.name, "${address.cherga}.${address.pidcherga}", timeline.runs, timeProvider.now())
    }

    companion object {
        fun readyState(addressName: String, queue: String, runs: List<PowerRun>, now: Long): WidgetState.Ready {
            // Schedule days are Kyiv days, and "today" comes from the synced clock, not the device's
            val zone = ScheduleZone.zoneId
            val today = ScheduleZone.today(now)
            val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val current = runs.firstOrNull { now >= it.startMs && now < it.endMs }
            // Same contiguity rule as PowerTimeline.nextRun: a gap means the schedule is unknown there
            val next = current?.let { run ->
                runs.getOrNull(runs.indexOf(run) + 1)?.takeIf { abs(it.startMs - run.endMs) <= 60_000L }
            }
            return WidgetState.Ready(
                addressName = addressName,
                queue = queue,
                now = now,
                current = current,
                next = next,
                upcoming = runs.filter { it.endMs > now },
                today = runs.filter { it.endMs > dayStart && it.startMs < dayEnd },
                dayStart = dayStart,
                dayEnd = dayEnd
            )
        }

        /** Believable sample data for the widget picker: power on now, an outage later today. */
        fun previewState(): WidgetState.Ready {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            fun at(hour: Int, day: LocalDate = today) =
                day.atTime(LocalTime.of(hour % 24, 0)).atZone(zone).toInstant().toEpochMilli()
            val tomorrow = today.plusDays(1)
            val runs = listOf(
                PowerRun(PowerState.Off, at(0), at(3)),
                PowerRun(PowerState.On, at(3), at(10)),
                PowerRun(PowerState.Maybe, at(10), at(12)),
                PowerRun(PowerState.On, at(12), at(17)),
                PowerRun(PowerState.Off, at(17), at(20)),
                PowerRun(PowerState.On, at(20), at(0, tomorrow)),
                PowerRun(PowerState.Off, at(0, tomorrow), at(4, tomorrow))
            )
            return readyState("Дім", "3.1", runs, now = at(13) + 25 * 60_000L)
        }
    }
}
