package com.occaecat.ztoeschedule.domain.notification

import java.util.Calendar
import kotlin.random.Random

object NotificationTextHelper {

    fun getStatusTitle(isPowerOn: Boolean, nextChangeTime: String): String {
        return if (isPowerOn) {
            "Світло є ✅ До $nextChangeTime"
        } else {
            "Світла немає 🔴 До $nextChangeTime"
        }
    }

    fun getDetailedStatus(isPowerOn: Boolean, nextChangeTime: String, minutesRemaining: Long): String {
        val timeLabel = formatRemainingTime(minutesRemaining)
        return if (isPowerOn) {
            "Насолоджуйтесь! Наступне відключення о $nextChangeTime ($timeLabel)"
        } else {
            "Тримаємось! Світло мають ввімкнути о $nextChangeTime ($timeLabel)"
        }
    }

    fun getEasterEgg(isPowerOn: Boolean): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        val eggsOn = listOf(
            "Час зварити каву! ☕",
            "Пральна машина чекає 🧺",
            "Зарядіть павербанки про запас 🔋",
            "Інтернет працює — життя чудове! 🚀",
            "Енергетики працюють для вас 🦸‍♂️"
        )
        
        val eggsOff = listOf(
            "Миколаїч обіцяв скоро ввімкнути 😉",
            "Час для настільних ігор 🎲",
            "Найкращий момент для читання 📖",
            "Відпочиньте від гаджетів 🌳",
            "Житомир не зламати! 💪"
        )

        return if (isPowerOn) eggsOn.random() else eggsOff.random()
    }

    fun formatRemainingTime(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 -> "через $hours год $mins хв"
            else -> "через $mins хв"
        }
    }
}
