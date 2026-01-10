package com.occaecat.ztoeschedule.domain

import com.occaecat.ztoeschedule.data.model.Schedule
import java.util.Calendar
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Mapper для группировки интервалов графика.
 * Объединяет последовательные интервалы одного цвета в один блок, 
 * поддерживая переходы между датами.
 */
object ScheduleMapper {

    fun getGroupedSchedule(raw: List<Schedule>): List<GroupedSchedule> {
        if (raw.isEmpty()) return emptyList()

        val sortedRaw = raw.sortedWith(compareBy({ it.date.split(".").reversed().joinToString("") }, { it.span.split("-")[0] }))

        val grouped = mutableListOf<GroupedSchedule>()
        var currentGroup: MutableList<Schedule> = mutableListOf(sortedRaw[0])

        for (i in 1 until sortedRaw.size) {
            val current = sortedRaw[i]
            val previous = sortedRaw[i - 1]

            if (current.color == previous.color && areConsecutive(previous, current)) {
                currentGroup.add(current)
            } else {
                grouped.add(createGroupedSchedule(currentGroup))
                currentGroup = mutableListOf(current)
            }
        }

        if (currentGroup.isNotEmpty()) {
            grouped.add(createGroupedSchedule(currentGroup))
        }

        return grouped
    }

    private fun createGroupedSchedule(group: List<Schedule>): GroupedSchedule {
        val first = group.first()
        val last = group.last()

        val startTime = first.span.split("-")[0].trim()
        val endTime = last.span.split("-")[1].trim()
        
        val mergedSpan = if (first.date != last.date) {
            "$startTime (${first.date.substring(0, 5)}) - $endTime (${last.date.substring(0, 5)})"
        } else {
            "$startTime-$endTime"
        }

        val totalMinutes = calculateTotalMinutes(group)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val startMs = parseDateTimeToMs(first.date, startTime)
        val endMs = startMs + (totalMinutes * 60 * 1000L)

        return GroupedSchedule(
            date = first.date,
            span = mergedSpan,
            startTime = startTime,
            endTime = endTime,
            color = first.color,
            status = first.status,
            text = first.text,
            displayText = first.displayText,
            durationHours = hours,
            durationMinutes = minutes,
            intervalCount = group.size,
            startMs = startMs,
            endMs = endMs
        )
    }

    private fun parseDateTimeToMs(date: String, time: String): Long {
        return try {
            val kyivZone = TimeZone.getTimeZone("Europe/Kyiv")
            val cal = Calendar.getInstance(kyivZone)
            val dateParts = date.split(".")
            val timeParts = time.split(":")
            cal.set(dateParts[2].toInt(), dateParts[1].toInt() - 1, dateParts[0].toInt(), timeParts[0].toInt(), timeParts[1].toInt(), 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            0L
        }
    }

    private fun calculateTotalMinutes(group: List<Schedule>): Int {
        var total = 0
        for (item in group) {
            try {
                val parts = item.span.split("-")
                val start = parseTimeToMinutes(parts[0].trim())
                val end = parseTimeToMinutes(parts[1].trim())
                var duration = end - start
                if (duration <= 0 && parts[1].trim() == "24:00") duration = 1440 - start
                else if (duration < 0) duration += 1440
                total += duration
            } catch (e: Exception) {}
        }
        return total
    }

    private fun areConsecutive(s1: Schedule, s2: Schedule): Boolean {
        return try {
            val end1 = s1.span.split("-")[1].trim()
            val start2 = s2.span.split("-")[0].trim()
            if (s1.date == s2.date) {
                end1 == start2
            } else {
                (end1 == "24:00" || end1 == "00:00") && start2 == "00:00"
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Находит текущий активный сгруппированный интервал (абсолютное сравнение времени)
     * @param nowMs Текущее время в миллисекундах (желательно синхронизированное с NTP)
     */
    fun getCurrentGroupedStatus(schedules: List<GroupedSchedule>, nowMs: Long): GroupedSchedule? {
        if (schedules.isEmpty()) return null

        return schedules.firstOrNull { group ->
            nowMs >= group.startMs && nowMs < group.endMs
        }
    }

    fun formatDuration(hours: Int, minutes: Int): String {
        return when {
            hours > 0 && minutes > 0 -> "${hours}г ${minutes}хв"
            hours > 0 -> "$hours год"
            minutes > 0 -> "$minutes хв"
            else -> "0 хв"
        }
    }

    private fun parseTimeToMinutes(time: String): Int {
        return try {
            val parts = time.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) { 0 }
    }
}

@androidx.compose.runtime.Immutable
data class GroupedSchedule(
    val date: String,
    val span: String,
    val startTime: String,
    val endTime: String,
    val color: String,
    val status: com.occaecat.ztoeschedule.data.model.ScheduleStatus,
    val text: String?,
    val displayText: String,
    val durationHours: Int,
    val durationMinutes: Int,
    val intervalCount: Int,
    val startMs: Long,
    val endMs: Long
) {
    val formattedDuration: String
        get() = ScheduleMapper.formatDuration(durationHours, durationMinutes)

    val isLightOn: Boolean
        get() = status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.Available
}
