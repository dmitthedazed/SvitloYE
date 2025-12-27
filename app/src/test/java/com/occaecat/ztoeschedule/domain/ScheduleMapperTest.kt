package com.occaecat.ztoeschedule.domain

import com.occaecat.ztoeschedule.data.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleMapperTest {

    @Test
    fun `getGroupedSchedule merges consecutive intervals of same color`() {
        // Given: Two consecutive green intervals
        val rawSchedules = listOf(
            Schedule("25.12.2025", "08:00-09:00", "green", "Світло є"),
            Schedule("25.12.2025", "09:00-10:00", "green", "Світло є"),
            Schedule("25.12.2025", "10:00-11:00", "red", "Світла немає")
        )

        // When
        val grouped = ScheduleMapper.getGroupedSchedule(rawSchedules)

        // Then
        assertEquals(2, grouped.size) // Should be 2 groups (one merged green, one red)
        
        // Check Green Group
        val greenGroup = grouped[0]
        assertEquals("green", greenGroup.color)
        assertEquals("08:00", greenGroup.startTime)
        assertEquals("10:00", greenGroup.endTime)
        assertEquals(2, greenGroup.durationHours) // 2 hours total
        assertEquals(0, greenGroup.durationMinutes)

        // Check Red Group
        val redGroup = grouped[1]
        assertEquals("red", redGroup.color)
        assertEquals("10:00", redGroup.startTime)
        assertEquals("11:00", redGroup.endTime)
    }

    @Test
    fun `getGroupedSchedule handles simple list correctly`() {
        val raw = listOf(
            Schedule("25.12.2025", "00:00-02:00", "red", "Off"),
            Schedule("25.12.2025", "02:00-06:00", "green", "On")
        )

        val grouped = ScheduleMapper.getGroupedSchedule(raw)

        assertEquals(2, grouped.size)
        assertEquals("00:00", grouped[0].startTime)
        assertEquals("02:00", grouped[0].endTime)
        assertEquals(2, grouped[0].durationHours)

        assertEquals("02:00", grouped[1].startTime)
        assertEquals("06:00", grouped[1].endTime)
        assertEquals(4, grouped[1].durationHours)
    }

    @Test
    fun `getCurrentGroupedStatus returns correct group for given time`() {
        val grouped = listOf(
            createMockGroup("25.12.2025", "08:00", "10:00"),
            createMockGroup("25.12.2025", "10:00", "12:00")
        )

        // Time is 09:00 (within first group)
        val nowMs = parseTimeToMs("25.12.2025", "09:00")
        val status = ScheduleMapper.getCurrentGroupedStatus(grouped, nowMs)
        
        assertEquals("08:00", status?.startTime)
    }

    private fun createMockGroup(date: String, start: String, end: String): GroupedSchedule {
        val startMs = parseTimeToMs(date, start)
        val endMs = parseTimeToMs(date, end)
        return GroupedSchedule(
            date = date, span = "$start-$end", startTime = start, endTime = end,
            color = "green", status = com.occaecat.ztoeschedule.data.model.ScheduleStatus.AVAILABLE,
            text = null, displayText = "On", durationHours = 2, durationMinutes = 0,
            intervalCount = 1, startMs = startMs, endMs = endMs
        )
    }

    private fun parseTimeToMs(date: String, time: String): Long {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Kyiv"))
        val d = date.split("."); val t = time.split(":")
        cal.set(d[2].toInt(), d[1].toInt() - 1, d[0].toInt(), t[0].toInt(), t[1].toInt(), 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
