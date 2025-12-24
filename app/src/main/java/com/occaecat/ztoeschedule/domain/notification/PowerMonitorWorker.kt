package com.occaecat.ztoeschedule.domain.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.network.RetrofitClient
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.ScheduleDomainLogic
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Background worker that checks power schedule and sends notifications
 * Runs periodically to monitor upcoming power changes
 */
class PowerMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val preferencesManager = EnergyPreferencesManager(applicationContext)
    private val repository = EnergyRepository(RetrofitClient.apiService, preferencesManager)
    private val notificationManager = PowerNotificationManager(applicationContext)

    override suspend fun doWork(): Result {
        try {
            // Check if notifications are enabled
            val notificationsEnabled = preferencesManager.notificationsEnabledFlow.first()
            if (!notificationsEnabled) {
                return Result.success()
            }

            // Get saved address
            val savedSelection = preferencesManager.savedSelectionFlow.first()
            if (savedSelection == null || savedSelection.cherga == 0 || savedSelection.pidcherga == 0) {
                return Result.success()
            }

            // Fetch schedule
            val result = repository.getScheduleWithMessages(
                savedSelection.cherga,
                savedSelection.pidcherga
            )

            result.onSuccess { data ->
                val currentStatus = ScheduleDomainLogic.getCurrentStatus(data.schedules)

                if (currentStatus != null) {
                    checkAndNotify(currentStatus)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            // Retry on failure
            return if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun checkAndNotify(currentStatus: com.occaecat.ztoeschedule.data.model.Schedule) {
        val isPowerOn = currentStatus.color.lowercase() != "red"

        // Parse end time
        val endTime = parseEndTime(currentStatus.span) ?: return
        val minutesUntilChange = getMinutesUntilTime(endTime)

        // Get notification advance time (default 15 minutes)
        val advanceMinutes = 15 // TODO: Make this configurable

        // Check if we should notify
        // Notify if: time until change is between advanceMinutes and (advanceMinutes - 5)
        // This 5-minute window prevents duplicate notifications
        if (minutesUntilChange in (advanceMinutes - 5)..advanceMinutes) {
            val title = if (isPowerOn) {
                "⚠️ Скоро відключення"
            } else {
                "✅ Скоро увімкнення"
            }

            val message = if (isPowerOn) {
                "Через $minutesUntilChange хв очікується відключення світла"
            } else {
                "Через $minutesUntilChange хв очікується увімкнення світла"
            }

            notificationManager.sendPowerChangeNotification(
                title = title,
                message = message,
                isOutage = !isPowerOn // Will change TO outage if currently on
            )
        }

        // Also notify on status change (within 2 minutes of change)
        if (minutesUntilChange <= 2 && minutesUntilChange >= 0) {
            val title = if (isPowerOn) {
                "🔴 Відключення світла"
            } else {
                "🟢 Світло увімкнено"
            }

            val message = if (isPowerOn) {
                "Зараз відбувається відключення електропостачання"
            } else {
                "Електропостачання відновлено"
            }

            notificationManager.sendPowerChangeNotification(
                title = title,
                message = message,
                isOutage = !isPowerOn
            )
        }
    }

    private fun parseEndTime(span: String): Pair<Int, Int>? {
        return try {
            val parts = span.split("-")
            if (parts.size != 2) return null

            val endPart = parts[1].trim()
            val timeParts = endPart.split(":")
            if (timeParts.size != 2) return null

            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            Pair(hour, minute)
        } catch (e: Exception) {
            null
        }
    }

    private fun getMinutesUntilTime(targetTime: Pair<Int, Int>): Int {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val targetMinutes = targetTime.first * 60 + targetTime.second

        return if (targetMinutes > currentMinutes) {
            targetMinutes - currentMinutes
        } else {
            // Next day
            (24 * 60 - currentMinutes) + targetMinutes
        }
    }
}

