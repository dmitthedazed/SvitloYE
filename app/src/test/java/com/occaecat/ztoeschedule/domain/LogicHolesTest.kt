package com.occaecat.ztoeschedule.domain

import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.domain.notification.PowerRun
import com.occaecat.ztoeschedule.domain.notification.PowerState
import com.occaecat.ztoeschedule.domain.notification.ScheduleChangeDetector
import com.occaecat.ztoeschedule.domain.time.ScheduleZone
import com.occaecat.ztoeschedule.widget.WidgetStateLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDateTime

/** Regression tests for the logic holes fixed after the read-only audit. */
class LogicHolesTest {

    private fun kyiv(y: Int, mo: Int, d: Int, h: Int, mi: Int = 0) =
        LocalDateTime.of(y, mo, d, h, mi).atZone(ScheduleZone.zoneId).toInstant().toEpochMilli()

    @Test
    fun `current status covers the last slot ending at 24_00`() {
        val schedules = listOf(
            Schedule("02.10.2026", "22:00-23:00", "green"),
            Schedule("02.10.2026", "23:00-24:00", "red")
        )
        val current = ScheduleDomainLogic.getCurrentStatus(schedules, kyiv(2026, 10, 2, 23, 30))
        assertEquals("23:00-24:00", current?.span)
    }

    @Test
    fun `group end follows the wall clock across a DST switch`() {
        // 25.10.2026: clocks go back at 04:00 in Kyiv, the day has 25 hours
        val grouped = ScheduleMapper.getGroupedSchedule(listOf(Schedule("25.10.2026", "00:00-24:00", "green")))
        assertEquals(kyiv(2026, 10, 26, 0), grouped.single().endMs)
    }

    @Test
    fun `whole-day span counts as 24 hours`() {
        val grouped = ScheduleMapper.getGroupedSchedule(listOf(Schedule("02.10.2026", "00:00-00:00", "green")))
        assertEquals(24, grouped.single().durationHours)
        assertEquals(kyiv(2026, 10, 3, 0), grouped.single().endMs)
    }

    @Test
    fun `stats keep possible outages apart and ignore duplicates`() {
        val stats = StatisticsCalculator.calculateDailyStats(
            listOf(
                Schedule("02.10.2026", "00:00-12:00", "green"),
                Schedule("02.10.2026", "12:00-18:00", "red"),
                Schedule("02.10.2026", "12:00-18:00", "red"),
                Schedule("02.10.2026", "18:00-24:00", "yellow")
            ),
            "02.10.2026"
        )
        assertEquals(720, stats.totalOnMinutes)
        assertEquals(360, stats.totalOutageMinutes)
        assertEquals(360, stats.totalProbableMinutes)
        assertEquals(0.25f, stats.percentageOutage, 0.001f)
    }

    @Test
    fun `snapshots are kept per queue`() {
        val packed = ScheduleChangeDetector.packAll(mapOf("1/1" to "1/1|0:1:0", "2/2" to "2/2|5:6:1"))
        val unpacked = ScheduleChangeDetector.unpackAll(packed)
        assertEquals("1/1|0:1:0", unpacked["1/1"])
        assertEquals("2/2|5:6:1", unpacked["2/2"])
        // The old single-hash format is dropped, not misread
        assertEquals(emptyMap<String, String>(), ScheduleChangeDetector.unpackAll("legacyhash"))
    }

    @Test
    fun `widget redraws at midnight even when power stays on overnight`() {
        val now = kyiv(2026, 10, 2, 21)
        val runs = listOf(PowerRun(PowerState.On, kyiv(2026, 10, 2, 20), kyiv(2026, 10, 3, 10)))
        val state = WidgetStateLoader.readyState("Дім", "1.1", runs, now)
        assertEquals(kyiv(2026, 10, 3, 0), state.nextChangeAt)
    }

    @Test
    fun `widget still schedules a redraw past the end of the schedule`() {
        val now = kyiv(2026, 10, 2, 21)
        val runs = listOf(PowerRun(PowerState.On, kyiv(2026, 10, 2, 8), kyiv(2026, 10, 2, 20)))
        val state = WidgetStateLoader.readyState("Дім", "1.1", runs, now)
        assertNotNull(state.nextChangeAt)
        assertEquals(kyiv(2026, 10, 3, 0), state.nextChangeAt)
    }
}
