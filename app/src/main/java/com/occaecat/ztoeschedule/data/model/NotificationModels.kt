package com.occaecat.ztoeschedule.data.model

data class SmartNotificationSettings(
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 7,
    val workdayMode: Boolean = false,
    val priorityMode: PriorityMode = PriorityMode.Smart
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
    All,
    Important,
    Smart
}