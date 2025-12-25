package com.occaecat.ztoeschedule.domain

import com.occaecat.ztoeschedule.data.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ScheduleMapperTest {

    @Test
    fun `getGroupedSchedule merges consecutive intervals of same color`() {
        // Given: Two consecutive green intervals
        val rawSchedules = listOf(
            Schedule("25.12.2025", "08:00-09:00", "green", "Світло є", "Світло є", 1),
            Schedule("25.12.2025", "09:00-10:00", "green", "Світло є", "Світло є", 2),
            Schedule("25.12.2025", "10:00-11:00", "red", "Світла немає", "Світла немає", 3)
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
            Schedule("25.12.2025", "00:00-02:00", "red", "Off", "Off", 1),
            Schedule("25.12.2025", "02:00-06:00", "green", "On", "On", 2)
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
    fun `formatDuration formats correctly`() {
        assertEquals("2 год", ScheduleMapper.formatDuration(2, 0))
        assertEquals("30 хв", ScheduleMapper.formatDuration(0, 30))
        assertEquals("1 год 15 хв", ScheduleMapper.formatDuration(1, 15))
        assertEquals("0 хв", ScheduleMapper.formatDuration(0, 0))
    }
}