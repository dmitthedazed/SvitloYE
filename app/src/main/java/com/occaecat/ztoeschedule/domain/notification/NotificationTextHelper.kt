package com.occaecat.ztoeschedule.domain.notification

import java.util.Calendar
import kotlin.random.Random

object NotificationTextHelper {

    fun getStatusTitle(isPowerOn: Boolean): String {
        return if (isPowerOn) {
            listOf(
                "⚡ Світло є",
                "🟢 Електроенергія подається",
                "🔋 Живлення відновлено",
                "💡 Житомир світиться"
            ).random()
        } else {
            listOf(
                "🌑 Відключення за графіком",
                "🔴 Енергосистема відпочиває",
                "🕯️ Час запалити свічки",
                "⏳ Режим енергозбереження"
            ).random()
        }
    }

    fun getEasterEgg(isPowerOn: Boolean): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        return if (isPowerOn) {
            when {
                hour in 0..6 -> "Заряджайте все, поки тихо 🌙"
                hour in 7..10 -> "Час готувати каву! ☕"
                hour in 18..22 -> "Затишного вечора зі світлом ✨"
                else -> listOf(
                    "Час зарядити павербанк! 🔋",
                    "Енергетики — супергерої 🦸‍♂️",
                    "Інтернет літає! 🚀",
                    "Пральна машина чекає на тебе 🧺"
                ).random()
            }
        } else {
            when {
                hour in 0..6 -> "Спи спокійно, сни зі світлом 💤"
                hour in 18..23 -> "Чудова нагода почитати книгу 📖"
                else -> listOf(
                    "Миколаїч обіцяв скоро ввімкнути 😉",
                    "Тримаємось, ми ж з Житомира! 💪",
                    "Кращий час для медитації 🧘",
                    "Офлайн — це теж життя 🌳"
                ).random()
            }
        }
    }

    fun formatRemainingTime(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 -> "Ще $hours год $mins хв"
            else -> "Залишилось $mins хв"
        }
    }
}
