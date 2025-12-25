package com.occaecat.ztoeschedule.data.model

data class SmartNotificationSettings(
    val quietHoursStart: Int = 22, // 22:00
    val quietHoursEnd: Int = 7,    // 07:00
    val workdayMode: Boolean = false,
    val priorityMode: PriorityMode = PriorityMode.SMART
) {
    fun isQuietHour(currentHour: Int): Boolean {
        return if (quietHoursStart <= quietHoursEnd) {
            currentHour in quietHoursStart until quietHoursEnd
        } else {
            currentHour >= quietHoursStart || currentHour < quietHoursEnd
        }
    }
}

enum class PriorityMode {
    ALL,           // Всі сповіщення (увімк/вимк/попередження)
    SMART,         // Тільки реальні зміни статусу (без попереджень, якщо статус не змінився)
    CRITICAL_ONLY, // Тільки про ВІДКЛЮЧЕННЯ (коли світло зникає)
    SILENT         // Без звуку/вібрації (тільки візуально в шторці)
}