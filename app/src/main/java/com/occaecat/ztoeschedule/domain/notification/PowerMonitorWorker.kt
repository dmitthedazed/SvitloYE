package com.occaecat.ztoeschedule.domain.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.TimeUtils
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.TimeZone

import androidx.hilt.work.HiltWorker
import com.occaecat.ztoeschedule.data.model.PriorityMode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.occaecat.ztoeschedule.widget.glance.LightWidget

import android.service.quicksettings.TileService
import android.content.ComponentName

@HiltWorker
class PowerMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferencesManager: EnergyPreferencesManager,
    private val repository: EnergyRepository,
    private val notificationManager: PowerNotificationManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("PowerMonitorWorker", "Background check triggered")
        try {
            val savedSelection = preferencesManager.savedSelectionFlow.first()
            if (savedSelection == null || savedSelection.cherga == 0) {
                Log.d("PowerMonitorWorker", "No selection found, skipping")
                return Result.success()
            }

            updateGlanceAddressOnly(savedSelection.addressName)

            val notificationsEnabled = preferencesManager.notificationsEnabledFlow.first()
            val advanceMinutes = preferencesManager.notificationAdvanceMinutesFlow.first()

            val result = repository.getScheduleWithMessages(savedSelection.cherga, savedSelection.pidcherga)

            result.onSuccess { data ->
                val groupedSchedules = ScheduleMapper.getGroupedSchedule(data.schedules)
                
                // Check for schedule updates
                val currentHash = data.schedules.hashCode().toString()
                val lastHash = preferencesManager.lastScheduleHashFlow.first()
                if (lastHash != null && lastHash != currentHash) {
                    Log.d("PowerMonitorWorker", "Schedule changed, sending update notification")
                    notificationManager.sendUpdateNotification(
                        "📢 Графік оновлено",
                        "Житомиробленерго оновило розклад відключень"
                    )
                }
                if (lastHash != currentHash) {
                    preferencesManager.saveLastScheduleHash(currentHash)
                }

                val currentStatus = getCurrentGroupedStatus(groupedSchedules)
                if (currentStatus != null) {
                    if (notificationsEnabled) {
                        checkAndNotify(currentStatus, advanceMinutes)
                    }
                    updateGlanceWidgets(currentStatus, savedSelection.addressName)
                    updateQuickSettingsTile()
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("PowerMonitorWorker", "Worker failed", e)
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun updateQuickSettingsTile() {
        try {
            TileService.requestListeningState(
                applicationContext,
                ComponentName(applicationContext, PowerStatusTileService::class.java)
            )
        } catch (e: Exception) {
            Log.e("PowerMonitorWorker", "Failed to update QS Tile", e)
        }
    }

    private suspend fun updateGlanceAddressOnly(address: String) {
        try {
            val manager = GlanceAppWidgetManager(applicationContext)
            val widget = LightWidget()
            val glanceIds = manager.getGlanceIds(widget.javaClass)
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(applicationContext, glanceId) { prefs ->
                    prefs[LightWidget.KEY_ADDRESS] = address
                }
                widget.update(applicationContext, glanceId)
            }
        } catch (e: Exception) {
            Log.e("PowerMonitorWorker", "Widget update failed", e)
        }
    }

    private suspend fun updateGlanceWidgets(status: GroupedSchedule, address: String) {
        try {
            val manager = GlanceAppWidgetManager(applicationContext)
            val widget = LightWidget()
            val glanceIds = manager.getGlanceIds(widget.javaClass)
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(applicationContext, glanceId) { prefs ->
                    prefs[LightWidget.KEY_STATUS] = status.color.lowercase()
                    prefs[LightWidget.KEY_NEXT_EVENT] = status.endTime
                    prefs[LightWidget.KEY_ADDRESS] = address
                    prefs[LightWidget.KEY_UPDATED] = System.currentTimeMillis()
                }
                widget.update(applicationContext, glanceId)
            }
        } catch (e: Exception) {
            Log.e("PowerMonitorWorker", "Full widget update failed", e)
        }
    }

    private fun getCurrentGroupedStatus(schedules: List<GroupedSchedule>): GroupedSchedule? {
        if (schedules.isEmpty()) return null
        val kyivZone = TimeZone.getTimeZone("Europe/Kyiv")
        val now = Calendar.getInstance(kyivZone)
        val currentTimeInMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

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
        return try { parts[0].toInt() * 60 + parts[1].toInt() } catch (e: Exception) { null }
    }

    private suspend fun checkAndNotify(currentStatus: GroupedSchedule, advanceMinutes: Int) {
        val isPowerOn = currentStatus.isLightOn
        val endTimeStr = Regex("""(\d{2}:\d{2})""").findAll(currentStatus.span).lastOrNull()?.value ?: return
        val minutesUntilChange = getMinutesUntilTime(Pair(endTimeStr.split(":")[0].toInt(), endTimeStr.split(":")[1].toInt()))
        
        val mode = preferencesManager.notificationModeFlow.first()
        if (mode == 2) return
        if (mode == 1 && !isPowerOn) return // IMPORTANT only: focus on outages

        if (minutesUntilChange in (advanceMinutes - 5)..advanceMinutes) {
            val title = if (isPowerOn) "⚠️ Скоро відключення" else "✅ Світло повертається"
            notificationManager.sendPowerChangeNotification(title, "До зміни залишилось $minutesUntilChange хв", isPowerOn)
        }
        if (minutesUntilChange in 0..2) {
            val title = if (isPowerOn) "🔴 Відключення" else "🟢 Світло увімкнено!"
            notificationManager.sendPowerChangeNotification(title, "Статус змінився за графіком", isPowerOn)
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