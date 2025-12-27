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

import com.occaecat.ztoeschedule.domain.time.TimeProvider

@HiltWorker
class PowerMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferencesManager: EnergyPreferencesManager,
    private val repository: EnergyRepository,
    private val notificationManager: PowerNotificationManager,
    private val timeProvider: TimeProvider
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
                
                // Check for schedule updates (only if data is not empty to avoid false alarms)
                if (data.schedules.isNotEmpty()) {
                    val currentHash = data.schedules.hashCode().toString()
                    val lastHash = preferencesManager.lastScheduleHashFlow.first()
                    
                    if (lastHash != null && lastHash != currentHash) {
                        Log.d("PowerMonitorWorker", "Schedule changed, sending update notification")
                        
                        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM"))
                        notificationManager.sendUpdateNotification(
                            "📢 Графік оновлено ($today)",
                            "З'явилися зміни у розкладі відключень. Перевірте актуальний статус."
                        )
                    }
                    
                    if (lastHash != currentHash) {
                        preferencesManager.saveLastScheduleHash(currentHash)
                    }
                }

                val currentStatus = ScheduleMapper.getCurrentGroupedStatus(groupedSchedules, timeProvider.now())
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
                    prefs[LightWidget.KEY_STATUS] = status.status.name.lowercase()
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

    private suspend fun checkAndNotify(currentStatus: GroupedSchedule, advanceMinutes: Int) {
        val isPowerOn = currentStatus.isLightOn
        val endTimeStr = currentStatus.endTime
        val endParts = endTimeStr.split(":")
        if (endParts.size != 2) return
        
        val minutesUntilChange = getMinutesUntilTime(Pair(endParts[0].toInt(), endParts[1].toInt()))
        
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
        val now = timeProvider.nowCalendar() // Use accurate NTP time
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val targetMinutes = targetTime.first * 60 + targetTime.second
        return if (targetMinutes > currentMinutes) targetMinutes - currentMinutes else (1440 - currentMinutes) + targetMinutes
    }
}