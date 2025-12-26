package com.occaecat.ztoeschedule.domain

import com.occaecat.ztoeschedule.data.model.Schedule
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DailyStats(
    val totalOutageMinutes: Int,
    val totalOnMinutes: Int,
    val totalUnknownMinutes: Int,
    val percentageOutage: Float
)

object StatisticsCalculator {

    fun calculateDailyStats(schedules: List<Schedule>, targetDate: String? = null): DailyStats {
        val dateToCheck = targetDate ?: LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        
        // Filter schedules for the specific date
        val dailySchedules = schedules.filter { it.date == dateToCheck }
        
        if (dailySchedules.isEmpty()) return DailyStats(0, 0, 0, 0f)

        var outageMinutes = 0
        var onMinutes = 0
        var unknownMinutes = 0

        dailySchedules.forEach { schedule ->
            val duration = getDurationInMinutes(schedule.span)
            
            // Check status based on color
            val colorLower = schedule.color.lowercase()
            val isLightOn = colorLower == "green" || colorLower == "white"
            
            val textLower = schedule.displayText.lowercase()
            val actuallyOn = textLower.contains("світло є") || isLightOn
            
            if (actuallyOn) {
                onMinutes += duration
            } else {
                outageMinutes += duration
            }
        }

        val totalKnown = outageMinutes + onMinutes
        val percentage = if (totalKnown > 0) (outageMinutes.toFloat() / totalKnown.toFloat()) else 0f

        return DailyStats(outageMinutes, onMinutes, unknownMinutes, percentage)
    }

    private fun getDurationInMinutes(span: String): Int {
        return try {
            val times = span.split("-")
            if (times.size != 2) return 0
            
            val startParts = times[0].trim().split(":")
            val endParts = times[1].trim().split(":")
            
            val startMin = startParts[0].toInt() * 60 + startParts[1].toInt()
            var endMin = endParts[0].toInt() * 60 + endParts[1].toInt()
            
            if (endMin < startMin) endMin += 24 * 60 // Next day wrapping
            
            endMin - startMin
        } catch (e: Exception) {
            0
        }
    }
}
