package com.occaecat.ztoeschedule.domain.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.network.RetrofitClient
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.TimeZone

import androidx.hilt.work.HiltWorker
import com.occaecat.ztoeschedule.data.model.PriorityMode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that checks power schedule and sends notifications.
 * Uses Europe/Kyiv timezone for all internal logic to match the utility provider.
 */
@HiltWorker
class PowerMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferencesManager: EnergyPreferencesManager,
    private val repository: EnergyRepository,
    private val notificationManager: PowerNotificationManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val notificationsEnabled = preferencesManager.notificationsEnabledFlow.first()
            if (!notificationsEnabled) return Result.success()

            val savedSelection = preferencesManager.savedSelectionFlow.first()
            if (savedSelection == null || savedSelection.cherga == 0 || savedSelection.pidcherga == 0) {
                return Result.success()
            }

            val advanceMinutes = preferencesManager.notificationAdvanceMinutesFlow.first()

            val result = repository.getScheduleWithMessages(savedSelection.cherga, savedSelection.pidcherga)

            result.onSuccess { data ->
                val groupedSchedules = ScheduleMapper.getGroupedSchedule(data.schedules)
                
                // Check for schedule updates
                val currentHash = data.schedules.hashCode().toString()
                val lastHash = preferencesManager.lastScheduleHashFlow.first()
                
                if (lastHash != null && lastHash != currentHash) {
                    notificationManager.sendPowerChangeNotification(
                        "📢 Графік оновлено",
                        "Житомиробленерго оновило розклад відключень",
                        false
                    )
                }
                
                if (lastHash != currentHash) {
                    preferencesManager.saveLastScheduleHash(currentHash)
                }

                val currentStatus = getCurrentGroupedStatus(groupedSchedules)
                if (currentStatus != null) {
                    checkAndNotify(currentStatus, advanceMinutes)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun getCurrentGroupedStatus(schedules: List<GroupedSchedule>): GroupedSchedule? {
        if (schedules.isEmpty()) return null
        val kyivZone = TimeZone.getTimeZone("Europe/Kyiv")
        val now = Calendar.getInstance(kyivZone)
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        return schedules.firstOrNull { schedule ->
            val parts = schedule.span.split("-")
            if (parts.size >= 2) {
                val start = parseTime(parts[0].trim())
                val end = parseTime(parts[1].trim())
                if (start != null && end != null) {
                    if (end >= start) currentTimeInMinutes in start until end
                    else currentTimeInMinutes >= start || currentTimeInMinutes < end
                } else false
            } else false
        }
    }

    private fun parseTime(timeStr: String): Int? {
        val parts = timeStr.split(":")
        if (parts.size != 2) return null
        return try {
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) { null }
    }

    private suspend fun checkAndNotify(currentStatus: GroupedSchedule, advanceMinutes: Int) {
        val settings = preferencesManager.smartNotificationSettingsFlow.first()
        
        // 1. Quiet Hours Check
        val kyivZone = TimeZone.getTimeZone("Europe/Kyiv")
        val currentHour = Calendar.getInstance(kyivZone).get(Calendar.HOUR_OF_DAY)
        if (settings.isQuietHour(currentHour)) return

        // 2. Priority Check (SILENT)
        if (settings.priorityMode == PriorityMode.SILENT) return

        val isPowerOn = currentStatus.isLightOn
        val endTimeStr = Regex("""(\d{2}:\d{2})""").findAll(currentStatus.span).lastOrNull()?.value ?: return
        val endTimeParts = endTimeStr.split(":")
        val minutesUntilChange = getMinutesUntilTime(Pair(endTimeParts[0].toInt(), endTimeParts[1].toInt()))

        // Alert types
        val isOutageAlert = isPowerOn // Warning: Light is ON, so next is OFF
        val isRestoreAlert = !isPowerOn // Warning: Light is OFF, so next is ON
        
        // Apply Priority Filters
        if (settings.priorityMode == PriorityMode.CRITICAL_ONLY || settings.priorityMode == PriorityMode.SMART) {
            // Skip "Restore" alerts in Critical/Smart modes (focus on outages)
            if (isRestoreAlert) return
        }

        if (minutesUntilChange in (advanceMinutes - 5)..advanceMinutes) {
            val title = if (isPowerOn) "⚠️ Скоро відключення" else "✅ Скоро увімкнення"
            val message = if (isPowerOn) "Через $minutesUntilChange хв очікується відключення світла" else "Через $minutesUntilChange хв очікується увімкнення світла"
            notificationManager.sendPowerChangeNotification(title, message, !isPowerOn)
        }

        if (minutesUntilChange in 0..2) {
            val title = if (isPowerOn) "🔴 Відключення світла" else "🟢 Світло увімкнено"
            val message = if (isPowerOn) "Зараз відбувається відключення електропостачання" else "Електропостачання відновлено"
            notificationManager.sendPowerChangeNotification(title, message, !isPowerOn)
        }
    }

    private fun getMinutesUntilTime(targetTime: Pair<Int, Int>): Int {
        val kyivZone = TimeZone.getTimeZone("Europe/Kyiv")
        val now = Calendar.getInstance(kyivZone)
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val targetMinutes = targetTime.first * 60 + targetTime.second
        return if (targetMinutes > currentMinutes) targetMinutes - currentMinutes else (1440 - currentMinutes) + targetMinutes
    }
}
