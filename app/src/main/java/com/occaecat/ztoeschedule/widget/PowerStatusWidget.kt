package com.occaecat.ztoeschedule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.network.RetrofitClient
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.ScheduleDomainLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

/**
 * Compact Power Status Widget (4x1)
 * Shows current power status with simple icon and text
 */
class PowerStatusWidget : AppWidgetProvider() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update all widgets
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Called when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Called when the last widget is removed
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        scope.launch {
            try {
                val preferencesManager = EnergyPreferencesManager(context)
                val repository = EnergyRepository(RetrofitClient.apiService, preferencesManager)

                // Get saved address
                val savedSelection = preferencesManager.savedSelectionFlow.first()

                if (savedSelection == null || savedSelection.cherga == 0 || savedSelection.pidcherga == 0) {
                    // No address configured
                    updateWidgetView(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        isConfigured = false,
                        isPowerOn = false,
                        statusText = "Налаштуйте адресу",
                        timeInfo = "",
                        nextChangeTime = "",
                        progressPercent = 0
                    )
                    return@launch
                }

                // Fetch schedule
                val result = repository.getScheduleWithMessages(
                    savedSelection.cherga,
                    savedSelection.pidcherga
                )

                result.onSuccess { data ->
                    val currentStatus = ScheduleDomainLogic.getCurrentStatus(data.schedules)
                    val isPowerOn = currentStatus?.color?.lowercase() != "red"

                    val statusText = if (isPowerOn) "Світло є" else "Відключення"

                    // Calculate time until next change and progress
                    var timeInfo = ""
                    var nextChangeTime = ""
                    var progressPercent = 0

                    currentStatus?.let { status ->
                        val endTime = parseEndTime(status.span)
                        if (endTime != null) {
                            val minutesRemaining = getMinutesUntilTime(endTime)
                            timeInfo = formatTimeRemaining(minutesRemaining)
                            nextChangeTime = "⚡ ${if (isPowerOn) "Відключення" else "Увімкнення"} о ${endTime.first.toString().padStart(2, '0')}:${endTime.second.toString().padStart(2, '0')}"

                            // Calculate progress (assume typical 4-hour blocks)
                            val startTime = parseStartTime(status.span)
                            if (startTime != null) {
                                val totalMinutes = calculateMinutesBetween(startTime, endTime)
                                val elapsedMinutes = totalMinutes - minutesRemaining
                                progressPercent = if (totalMinutes > 0) {
                                    ((elapsedMinutes.toFloat() / totalMinutes.toFloat()) * 100).toInt().coerceIn(0, 100)
                                } else 0
                            }
                        }
                    }

                    updateWidgetView(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        isConfigured = true,
                        isPowerOn = isPowerOn,
                        statusText = statusText,
                        timeInfo = timeInfo,
                        nextChangeTime = nextChangeTime,
                        progressPercent = progressPercent
                    )
                }.onFailure {
                    updateWidgetView(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        isConfigured = true,
                        isPowerOn = false,
                        statusText = "Помилка завантаження",
                        timeInfo = "",
                        nextChangeTime = "",
                        progressPercent = 0
                    )
                }
            } catch (e: Exception) {
                updateWidgetView(
                    context,
                    appWidgetManager,
                    appWidgetId,
                    isConfigured = true,
                    isPowerOn = false,
                    statusText = "Помилка",
                    timeInfo = "",
                    nextChangeTime = "",
                    progressPercent = 0
                )
            }
        }
    }

    private fun updateWidgetView(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        isConfigured: Boolean,
        isPowerOn: Boolean,
        statusText: String,
        timeInfo: String,
        nextChangeTime: String,
        progressPercent: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_power_status)

        // Update UI elements
        views.setTextViewText(R.id.widget_status_text, statusText)
        views.setTextViewText(R.id.widget_remaining_text, timeInfo)
        views.setTextViewText(R.id.widget_time_info, nextChangeTime)

        // Set icon
        val icon = if (isPowerOn) "✓" else "✗"
        views.setTextViewText(R.id.widget_icon, icon)

        // Set background drawable based on status
        val backgroundDrawable = when {
            !isConfigured -> R.drawable.widget_card_gray
            isPowerOn -> R.drawable.widget_card_green
            else -> R.drawable.widget_card_red
        }
        views.setInt(R.id.widget_card_container, "setBackgroundResource", backgroundDrawable)

        // Set progress bar
        views.setProgressBar(R.id.widget_progress, 100, progressPercent, false)

        // Set click action to open app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
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

    private fun parseStartTime(span: String): Pair<Int, Int>? {
        return try {
            val parts = span.split("-")
            if (parts.isEmpty()) return null

            val startPart = parts[0].trim()
            val timeParts = startPart.split(":")
            if (timeParts.size != 2) return null

            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            Pair(hour, minute)
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateMinutesBetween(startTime: Pair<Int, Int>, endTime: Pair<Int, Int>): Int {
        val startMinutes = startTime.first * 60 + startTime.second
        val endMinutes = endTime.first * 60 + endTime.second

        return if (endMinutes >= startMinutes) {
            endMinutes - startMinutes
        } else {
            // Crosses midnight
            (24 * 60 - startMinutes) + endMinutes
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

    private fun formatTimeRemaining(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60

        return when {
            hours > 0 -> "До відключення: ${hours}г ${mins}хв"
            else -> "До відключення: ${mins}хв"
        }
    }

    companion object {
        const val ACTION_UPDATE = "com.occaecat.ztoeschedule.widget.UPDATE"
    }
}

