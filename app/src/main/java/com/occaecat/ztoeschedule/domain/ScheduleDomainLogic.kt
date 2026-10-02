package com.occaecat.ztoeschedule.domain

import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.domain.time.ScheduleZone
import java.util.*

/**
 * Domain logic for processing schedules
 */
object ScheduleDomainLogic {

    /**
     * Determines the currently active schedule based on current time
     *
     * Parses the 'span' field (format "HH:mm-HH:mm", the end may be "24:00") and compares
     * with the current Kyiv time to find which schedule entry is active right now.
     *
     * @param schedules List of Schedule objects to analyze
     * @param nowMs Current time (preferably NTP-synced)
     * @return The currently active Schedule object, or null if none is active
     */
    fun getCurrentStatus(schedules: List<Schedule>, nowMs: Long): Schedule? {
        if (schedules.isEmpty()) return null

        val now = Calendar.getInstance(ScheduleZone.timeZone).apply { timeInMillis = nowMs }
        val currentDateStr = ScheduleZone.todayString(nowMs)
        val currentTimeInMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        return schedules.filter { it.date == currentDateStr }.firstOrNull { schedule ->
            parseTimeSpan(schedule.span)?.let { (startMinutes, endMinutes) ->
                isTimeInRange(currentTimeInMinutes, startMinutes, endMinutes)
            } ?: false
        }
    }

    /**
     * Parse time span string in format "HH:mm-HH:mm" to minutes since midnight
     *
     * @param span Time span string (e.g., "08:00-12:00", "23:00-24:00")
     * @return Pair of (startMinutes, endMinutes) or null if parsing fails
     */
    private fun parseTimeSpan(span: String): Pair<Int, Int>? {
        val parts = span.split("-")
        if (parts.size != 2) return null

        val startTime = parseTime(parts[0].trim()) ?: return null
        val endTime = parseTime(parts[1].trim()) ?: return null
        // "24:00" is only meaningful as an end of day
        if (startTime == MINUTES_PER_DAY) return null
        return startTime to endTime
    }

    /**
     * Parse time string in format "HH:mm" to minutes since midnight; "24:00" is the end of the day.
     */
    private fun parseTime(time: String): Int? {
        val parts = time.split(":")
        if (parts.size != 2) return null

        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null

        return when {
            hours == 24 && minutes == 0 -> MINUTES_PER_DAY
            hours in 0..23 && minutes in 0..59 -> hours * 60 + minutes
            else -> null
        }
    }

    /**
     * Check if a time is within a range, handling ranges that cross midnight
     */
    private fun isTimeInRange(timeMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
        return if (endMinutes >= startMinutes) {
            // Normal range: start < end (e.g., 08:00-17:00, 23:00-24:00)
            timeMinutes in startMinutes until endMinutes
        } else {
            // Range crosses midnight (e.g., 23:00-02:00)
            timeMinutes >= startMinutes || timeMinutes < endMinutes
        }
    }

    /**
     * Format time in minutes to HH:mm string
     */
    fun formatTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hours, mins)
    }

    private const val MINUTES_PER_DAY = 24 * 60
}
