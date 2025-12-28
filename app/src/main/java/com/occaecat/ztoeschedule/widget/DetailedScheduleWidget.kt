package com.occaecat.ztoeschedule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.ScheduleDomainLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Detailed Schedule Widget (4x2)
 * Shows current power status + today's schedule
 */
@AndroidEntryPoint
class DetailedScheduleWidget : AppWidgetProvider() {

    @Inject lateinit var repository: EnergyRepository
    @Inject lateinit var preferencesManager: EnergyPreferencesManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        scope.launch {
            try {
                // Get saved address
                val savedSelection = preferencesManager.savedSelectionFlow.first()

                if (savedSelection == null || savedSelection.cherga == 0 || savedSelection.pidcherga == 0) {
                    updateWidgetView(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        isConfigured = false,
                        isPowerOn = false,
                        statusText = "Налаштуйте адресу",
                        scheduleText = "Відкрийте додаток для налаштування",
                        addressText = "",
                        status = com.occaecat.ztoeschedule.data.model.ScheduleStatus.Unknown
                    )
                    return@launch
                }

                // Fetch schedule
                val result = repository.getScheduleWithMessages(
                    savedSelection!!.cherga,
                    savedSelection!!.pidcherga
                )

                result.onSuccess { data ->
                    val currentStatus = ScheduleDomainLogic.getCurrentStatus(data.schedules)
                    val scheduleStatus = currentStatus?.status ?: com.occaecat.ztoeschedule.data.model.ScheduleStatus.Unknown
                    val isPowerOn = scheduleStatus == com.occaecat.ztoeschedule.data.model.ScheduleStatus.Available

                    val statusText = currentStatus?.displayText ?: "Немає даних"

                    // Format schedule for today
                    val scheduleText = formatScheduleList(context, data.schedules)

                    // Format address
                    val addressText = formatAddress(savedSelection!!)

                    updateWidgetView(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        isConfigured = true,
                        isPowerOn = isPowerOn,
                        statusText = statusText,
                        scheduleText = scheduleText,
                        addressText = addressText,
                        status = scheduleStatus
                    )
                }.onFailure {
                    updateWidgetView(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        isConfigured = true,
                        isPowerOn = false,
                        statusText = "Помилка",
                        scheduleText = "Не вдалося завантажити графік",
                        addressText = "",
                        status = com.occaecat.ztoeschedule.data.model.ScheduleStatus.Unknown
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
                    scheduleText = e.message ?: "Невідома помилка",
                    addressText = "",
                    status = com.occaecat.ztoeschedule.data.model.ScheduleStatus.Unknown
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
        scheduleText: String,
        addressText: String,
        status: com.occaecat.ztoeschedule.data.model.ScheduleStatus
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_detailed_schedule)

        // Update text views
        views.setTextViewText(R.id.widget_status_text, statusText)
        views.setTextViewText(R.id.widget_schedule_text, scheduleText)
        views.setTextViewText(R.id.widget_address_text, addressText)

        // Set icon
        val iconRes = if (isPowerOn) android.R.drawable.presence_online else android.R.drawable.presence_busy
        if (!isConfigured) {
             views.setImageViewResource(R.id.widget_icon, R.drawable.ic_launcher_foreground)
        } else {
             views.setImageViewResource(R.id.widget_icon, iconRes)
        }

        // Set Colors
        setWidgetColor(context, views, status)

        // Set click action to open app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        // Update widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    
    private fun setWidgetColor(context: Context, views: RemoteViews, status: com.occaecat.ztoeschedule.data.model.ScheduleStatus) {
        val colorRes = when (status) {
            com.occaecat.ztoeschedule.data.model.ScheduleStatus.Outage -> R.color.widget_status_negative
            com.occaecat.ztoeschedule.data.model.ScheduleStatus.Available -> R.color.widget_status_positive
            com.occaecat.ztoeschedule.data.model.ScheduleStatus.Probable -> R.color.widget_status_warning
            else -> R.color.widget_text_secondary
        }
        
        if (Build.VERSION.SDK_INT >= 31) {
             val color = ContextCompat.getColor(context, colorRes)
             views.setInt(R.id.widget_container, "setBackgroundTintList", ColorStateList.valueOf(color).defaultColor)
        }
    }

    private fun formatScheduleList(context: Context, schedules: List<com.occaecat.ztoeschedule.data.model.Schedule>): String {
        if (schedules.isEmpty()) return "Немає графіку"

        // Grouping logic for display could be good, but simple list is fine too
        // Let's use simple formatting for the widget list to keep it dense
        return schedules.joinToString("\n") { schedule ->
            val emoji = when (schedule.status) {
                com.occaecat.ztoeschedule.data.model.ScheduleStatus.Outage -> "🔴" // Red Circle
                com.occaecat.ztoeschedule.data.model.ScheduleStatus.Available -> "🟢" // Green Circle
                com.occaecat.ztoeschedule.data.model.ScheduleStatus.Probable -> "🟡" // Yellow Circle
                else -> "⚪"
            }
            val systemSpan = com.occaecat.ztoeschedule.domain.TimeUtils.formatSpanToSystem(context, schedule.span)
            "$emoji $systemSpan"
        }
    }

    private fun formatAddress(selection: com.occaecat.ztoeschedule.data.local.SavedSelection): String {
        return buildString {
            if (selection.streetName != null) {
                append(selection.streetName)
                if (selection.addressName.isNotEmpty()) {
                    append(", ${selection.addressName}")
                }
            } else {
                append("Адреса не вибрана")
            }
        }
    }
}

