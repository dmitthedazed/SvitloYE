package com.occaecat.ztoeschedule.domain

import com.occaecat.ztoeschedule.data.model.Schedule
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Mapper для группировки интервалов графика
 * Объединяет последовательные интервалы одного цвета в один блок
 */
object ScheduleMapper {

    /**
     * Группирует последовательные интервалы с одинаковым цветом
     *
     * @param raw Исходный список интервалов по 30 минут
     * @return Сгруппированный список с объединенными интервалами
     */
    fun getGroupedSchedule(raw: List<Schedule>): List<GroupedSchedule> {
        if (raw.isEmpty()) return emptyList()

        val grouped = mutableListOf<GroupedSchedule>()
        var currentGroup: MutableList<Schedule> = mutableListOf(raw[0])

        for (i in 1 until raw.size) {
            val current = raw[i]
            val previous = raw[i - 1]

            // Проверяем, одинаковый ли цвет и последовательны ли интервалы
            if (current.color == previous.color && areConsecutive(previous.span, current.span)) {
                currentGroup.add(current)
            } else {
                // Завершаем текущую группу и начинаем новую
                grouped.add(createGroupedSchedule(currentGroup))
                currentGroup = mutableListOf(current)
            }
        }

        // Добавляем последнюю группу
        if (currentGroup.isNotEmpty()) {
            grouped.add(createGroupedSchedule(currentGroup))
        }

        return grouped
    }

    /**
     * Создает объединенный интервал из группы последовательных интервалов
     */
    private fun createGroupedSchedule(group: List<Schedule>): GroupedSchedule {
        val first = group.first()
        val last = group.last()

        val startTime = first.span.split("-")[0].trim()
        val endTime = last.span.split("-")[1].trim()
        val mergedSpan = "$startTime-$endTime"

        val duration = calculateDuration(startTime, endTime)

        return GroupedSchedule(
            date = first.date,
            span = mergedSpan,
            color = first.color,
            text = first.text,
            displayText = first.displayText,
            durationHours = duration.first,
            durationMinutes = duration.second,
            intervalCount = group.size
        )
    }

    /**
     * Проверяет, являются ли два интервала последовательными
     */
    private fun areConsecutive(span1: String, span2: String): Boolean {
        return try {
            val end1 = span1.split("-")[1].trim()
            val start2 = span2.split("-")[0].trim()
            end1 == start2
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Вычисляет продолжительность интервала
     *
     * @return Pair<часы, минуты>
     */
    private fun calculateDuration(startTime: String, endTime: String): Pair<Int, Int> {
        return try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val start = LocalTime.parse(startTime, formatter)
            var end = LocalTime.parse(endTime, formatter)

            // Обработка перехода через полночь (24:00 -> 00:00)
            if (endTime == "24:00") {
                end = LocalTime.of(23, 59)
            }

            var totalMinutes = if (end.isBefore(start)) {
                // Переход через полночь
                val minutesToMidnight = java.time.Duration.between(start, LocalTime.MAX).toMinutes()
                val minutesFromMidnight = java.time.Duration.between(LocalTime.MIN, end).toMinutes()
                (minutesToMidnight + minutesFromMidnight + 1).toInt()
            } else {
                java.time.Duration.between(start, end).toMinutes().toInt()
            }

            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60

            Pair(hours, minutes)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    /**
     * Форматирует продолжительность для отображения
     */
    fun formatDuration(hours: Int, minutes: Int): String {
        return when {
            hours > 0 && minutes > 0 -> "$hours год $minutes хв"
            hours > 0 -> "$hours год"
            minutes > 0 -> "$minutes хв"
            else -> ""
        }
    }
}

/**
 * Модель сгруппированного интервала графика
 */
data class GroupedSchedule(
    val date: String,
    val span: String,
    val color: String,
    val text: String?,
    val displayText: String,
    val durationHours: Int,
    val durationMinutes: Int,
    val intervalCount: Int
) {
    /**
     * Форматированная продолжительность
     */
    val formattedDuration: String
        get() = ScheduleMapper.formatDuration(durationHours, durationMinutes)

    /**
     * Время начала
     */
    val startTime: String
        get() = span.split("-")[0].trim()

    /**
     * Время окончания
     */
    val endTime: String
        get() = span.split("-")[1].trim()

    /**
     * Статус активности (свет есть или нет)
     */
    val isLightOn: Boolean
        get() = color.lowercase() in listOf("white", "green")
}

