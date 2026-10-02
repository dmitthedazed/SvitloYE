package com.occaecat.ztoeschedule.domain

import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.data.model.ScheduleStatus
import com.occaecat.ztoeschedule.domain.time.ScheduleZone
import androidx.compose.runtime.Immutable

@Immutable
data class DailyStats(
    val totalOutageMinutes: Int,
    val totalOnMinutes: Int,
    val totalUnknownMinutes: Int,
    /** "Possible outage": neither a sure outage nor sure power, counted on its own. */
    val totalProbableMinutes: Int = 0,
    /** Share of sure outage among known (on + off + probable) minutes. */
    val percentageOutage: Float
)

object StatisticsCalculator {

    fun calculateDailyStats(schedules: List<Schedule>, targetDate: String? = null): DailyStats {
        val dateToCheck = targetDate ?: ScheduleZone.todayString(System.currentTimeMillis())

        // The API occasionally repeats an interval; count each one once
        val dailySchedules = schedules.filter { it.date == dateToCheck }.distinctBy { it.span }

        if (dailySchedules.isEmpty()) return DailyStats(0, 0, 0, 0, 0f)

        var outageMinutes = 0
        var onMinutes = 0
        var probableMinutes = 0
        var unknownMinutes = 0

        dailySchedules.forEach { schedule ->
            val duration = getDurationInMinutes(schedule.span)
            when (schedule.status) {
                ScheduleStatus.Available -> onMinutes += duration
                ScheduleStatus.Probable -> probableMinutes += duration
                ScheduleStatus.Outage -> outageMinutes += duration
                ScheduleStatus.Unknown -> unknownMinutes += duration
            }
        }

        val totalKnown = outageMinutes + onMinutes + probableMinutes
        val percentage = if (totalKnown > 0) (outageMinutes.toFloat() / totalKnown.toFloat()) else 0f

        return DailyStats(outageMinutes, onMinutes, unknownMinutes, probableMinutes, percentage)
    }

    private fun getDurationInMinutes(span: String): Int {
        return try {
            val times = span.split("-")
            if (times.size != 2) return 0

            val startParts = times[0].trim().split(":")
            val endParts = times[1].trim().split(":")

            val startMin = startParts[0].toInt() * 60 + startParts[1].toInt()
            val endMin = endParts[0].toInt() * 60 + endParts[1].toInt()

            // "24:00" is 1440; a non-positive span wraps past midnight ("00:00-00:00" is a whole day)
            val duration = endMin - startMin
            if (duration <= 0) duration + 24 * 60 else duration
        } catch (e: Exception) {
            0
        }
    }
}
