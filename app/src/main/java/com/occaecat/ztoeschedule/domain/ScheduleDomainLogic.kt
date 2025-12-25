package com.occaecat.ztoeschedule.domain

import com.occaecat.ztoeschedule.data.model.Schedule
import java.text.SimpleDateFormat
import java.util.*

/**
 * Domain logic for processing schedules
 */
object ScheduleDomainLogic {

    /**
     * Determines the currently active schedule based on current time
     *
     * Parses the 'span' field (format "HH:mm-HH:mm") and compares with current time
     * to find which schedule entry is active right now.
     *
     * @param schedules List of Schedule objects to analyze
     * @return The currently active Schedule object, or null if none is active
     */
    fun getCurrentStatus(schedules: List<Schedule>): Schedule? {
        if (schedules.isEmpty()) return null

        val kyivZone = TimeZone.getTimeZone("Europe/Kyiv")
        val now = Calendar.getInstance(kyivZone)
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        dateFormat.timeZone = kyivZone
        val currentDateStr = dateFormat.format(now.time)
        
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        // First try to find a schedule for TODAY in Kyiv
        val todaySchedules = schedules.filter { it.date == currentDateStr }
        
        return todaySchedules.firstOrNull { schedule ->
            val timeRange = parseTimeSpan(schedule.span)
            timeRange?.let { (startMinutes, endMinutes) ->
                isTimeInRange(currentTimeInMinutes, startMinutes, endMinutes)
            } ?: false
        }
    }

    /**
     * Parse time span string in format "HH:mm-HH:mm" to minutes since midnight
     *
     * @param span Time span string (e.g., "08:00-12:00")
     * @return Pair of (startMinutes, endMinutes) or null if parsing fails
     */
    private fun parseTimeSpan(span: String): Pair<Int, Int>? {
        return try {
            val parts = span.split("-")
            if (parts.size != 2) return null

            val startTime = parseTime(parts[0].trim())
            val endTime = parseTime(parts[1].trim())

            if (startTime != null && endTime != null) {
                Pair(startTime, endTime)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse time string in format "HH:mm" to minutes since midnight
     *
     * @param time Time string (e.g., "08:00")
     * @return Minutes since midnight, or null if parsing fails
     */
    private fun parseTime(time: String): Int? {
        return try {
            val parts = time.split(":")
            if (parts.size != 2) return null

            val hours = parts[0].toIntOrNull() ?: return null
            val minutes = parts[1].toIntOrNull() ?: return null

            if (hours in 0..23 && minutes in 0..59) {
                hours * 60 + minutes
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if a time is within a range, handling ranges that cross midnight
     *
     * @param timeMinutes Current time in minutes since midnight
     * @param startMinutes Range start time in minutes since midnight
     * @param endMinutes Range end time in minutes since midnight
     * @return true if time is within range, false otherwise
     */
    private fun isTimeInRange(timeMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
        return if (endMinutes >= startMinutes) {
            // Normal range: start < end (e.g., 08:00-17:00)
            timeMinutes in startMinutes until endMinutes
        } else {
            // Range crosses midnight (e.g., 23:00-02:00)
            timeMinutes >= startMinutes || timeMinutes < endMinutes
        }
    }

    /**
     * Format time in minutes to HH:mm string
     *
     * @param minutes Minutes since midnight
     * @return Formatted time string
     */
    fun formatTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hours, mins)
    }
}

