package com.occaecat.ztoeschedule.domain.notification

import com.occaecat.ztoeschedule.data.model.NotificationSettings
import com.occaecat.ztoeschedule.data.model.QuietHours
import com.occaecat.ztoeschedule.data.model.ScheduleStatus
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class NotificationLogicTest {

    private val zone = ZoneOffset.UTC
    private val day0 = LocalDate.of(2026, 10, 2).atStartOfDay(zone).toInstant().toEpochMilli()
    private fun at(hour: Int, minute: Int = 0) = day0 + hour * HOUR + minute * MINUTE

    private fun group(status: ScheduleStatus, from: Long, to: Long) = GroupedSchedule(
        date = "", span = "", startTime = "", endTime = "", color = "", status = status,
        text = null, displayText = "", durationHours = 0, durationMinutes = 0, intervalCount = 1,
        startMs = from, endMs = to
    )

    // 00–10 on, 10–14 off, 14–18 on (split in two groups), 18–20 probable, 20–24 on
    private val schedule = listOf(
        group(ScheduleStatus.Available, at(0), at(10)),
        group(ScheduleStatus.Outage, at(10), at(14)),
        group(ScheduleStatus.Available, at(14), at(16)),
        group(ScheduleStatus.Available, at(16), at(18)),
        group(ScheduleStatus.Probable, at(18), at(20)),
        group(ScheduleStatus.Available, at(20), at(24))
    )
    private val timeline = PowerTimeline.from(schedule)

    @Test
    fun `timeline merges adjacent groups with the same state`() {
        assertEquals(5, timeline.runs.size)
        assertEquals(PowerRun(PowerState.On, at(14), at(18)), timeline.runs[2])
        assertEquals(4, timeline.transitions().size)
    }

    @Test
    fun `gap in schedule is not a transition`() {
        val gapped = PowerTimeline.from(
            listOf(group(ScheduleStatus.Available, at(0), at(10)), group(ScheduleStatus.Outage, at(11), at(12)))
        )
        assertTrue(gapped.transitions().isEmpty())
        assertNull(gapped.nextRun(gapped.runs[0]))
    }

    @Test
    fun `plan has warnings before outages and change alerts`() {
        val plan = AlertPlanner.plan(timeline, at(8), NotificationSettings(leadMinutes = 15))
        assertEquals(
            listOf(
                AlertKind.Warning to at(9, 45), AlertKind.Change to at(10),
                AlertKind.Change to at(14),
                AlertKind.Warning to at(17, 45), AlertKind.Change to at(18),
                AlertKind.Change to at(20)
            ),
            plan.map { it.kind to it.atMs }
        )
        val outage = plan[1]
        assertEquals(PowerState.Off, outage.to)
        assertEquals(at(14), outage.untilMs)
        assertTrue(outage.isOutage)
        // Last run has no known end
        assertNull(plan.last().untilMs)
    }

    @Test
    fun `plan respects toggles and skips past events`() {
        val noRestore = AlertPlanner.plan(timeline, at(12), NotificationSettings(restoreAlerts = false, leadMinutes = 0))
        assertEquals(listOf(at(18)), noRestore.map { it.atMs })

        // Warning time already passed, change alert still planned
        val late = AlertPlanner.plan(timeline, at(9, 50), NotificationSettings(leadMinutes = 15))
        assertEquals(AlertKind.Change to at(10), late.first().kind to late.first().atMs)

        assertTrue(AlertPlanner.plan(timeline, at(8), NotificationSettings(alertsEnabled = false)).isEmpty())
    }

    @Test
    fun `off to maybe counts as restore`() {
        val t = PowerTimeline.from(
            listOf(group(ScheduleStatus.Outage, at(0), at(2)), group(ScheduleStatus.Probable, at(2), at(4)))
        )
        val plan = AlertPlanner.plan(t, at(1), NotificationSettings(outageAlerts = false))
        assertEquals(1, plan.size)
        assertTrue(!plan[0].isOutage)
    }

    @Test
    fun `quiet hours wrap midnight and use minutes`() {
        val quiet = QuietHours(enabled = true, startMinute = 22 * 60 + 30, endMinute = 7 * 60)
        assertTrue(quiet.contains(23 * 60))
        assertTrue(quiet.contains(3 * 60))
        assertTrue(!quiet.contains(22 * 60 + 29))
        assertTrue(!quiet.contains(7 * 60))
        assertTrue(!quiet.copy(enabled = false).contains(23 * 60))
    }

    @Test
    fun `rules drop late or quiet warnings and silence quiet changes`() {
        val settings = NotificationSettings(quietHours = QuietHours(enabled = true, startMinute = 22 * 60, endMinute = 7 * 60))
        fun d(kind: AlertKind, delayMin: Int, minuteOfDay: Int) =
            AlertRules.delivery(settings, kind, isOutage = true, atMs = 0, nowMs = delayMin * MINUTE, minuteOfDay = minuteOfDay)

        assertEquals(Delivery.Normal, d(AlertKind.Warning, 0, 12 * 60))
        assertEquals(Delivery.Drop, d(AlertKind.Warning, 11, 12 * 60))
        assertEquals(Delivery.Normal, d(AlertKind.Change, 20, 12 * 60))
        assertEquals(Delivery.Drop, d(AlertKind.Change, 31, 12 * 60))
        assertEquals(Delivery.Drop, d(AlertKind.Warning, 0, 23 * 60))
        assertEquals(Delivery.Silent, d(AlertKind.Change, 0, 23 * 60))
    }

    @Test
    fun `schedule change detection ignores time passing and reports new days`() {
        val key = "1/2"
        val before = ScheduleChangeDetector.snapshot(key, timeline)

        // Same schedule later in the day, past runs dropped by the API: no change
        val trimmed = PowerTimeline.from(schedule.drop(2))
        assertNull(ScheduleChangeDetector.detect(before, ScheduleChangeDetector.snapshot(key, trimmed), at(15), zone))

        // Tomorrow published
        val tomorrow = schedule + listOf(
            group(ScheduleStatus.Outage, at(24), at(28)),
            group(ScheduleStatus.Available, at(28), at(48))
        )
        val change = ScheduleChangeDetector.detect(before, ScheduleChangeDetector.snapshot(key, PowerTimeline.from(tomorrow)), at(15), zone)
        assertNotNull(change)
        // Today's 20–24 run now continues into a known outage, so it differs too; tomorrow is new
        assertEquals(LocalDate.of(2026, 10, 3), change!!.days.last())
        assertTrue(change.outages.any { it.startMs == at(24) })

        // Other address or first run: nothing to report
        assertNull(ScheduleChangeDetector.detect(before, ScheduleChangeDetector.snapshot("9/9", timeline), at(15), zone))
        assertNull(ScheduleChangeDetector.detect(null, before, at(15), zone))
        assertNull(ScheduleChangeDetector.detect("legacy-hash", before, at(15), zone))
    }

    @Test
    fun `changed outage today is reported`() {
        val key = "1/2"
        val before = ScheduleChangeDetector.snapshot(key, timeline)
        val moved = schedule.toMutableList().apply {
            this[0] = group(ScheduleStatus.Available, at(0), at(11))
            this[1] = group(ScheduleStatus.Outage, at(11), at(14))
        }
        val change = ScheduleChangeDetector.detect(before, ScheduleChangeDetector.snapshot(key, PowerTimeline.from(moved)), at(8), zone)
        assertEquals(listOf(LocalDate.of(2026, 10, 2)), change?.days)
        assertEquals(at(11), change!!.outages.first().startMs)
    }

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }
}
