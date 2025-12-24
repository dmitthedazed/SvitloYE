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

/**
 * Detailed Schedule Widget (4x2)
 * Shows current power status + today's schedule
 */
class DetailedScheduleWidget : AppWidgetProvider() {

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
                val preferencesManager = EnergyPreferencesManager(context)
                val repository = EnergyRepository(RetrofitClient.apiService, preferencesManager)

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
                        addressText = ""
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
                    val isPowerOn = currentStatus?.color?.lowercase() != "red"

                    val statusText = if (isPowerOn) "✓ Світло є" else "✗ Відключення"

                    // Format schedule for today
                    val scheduleText = formatScheduleList(data.schedules)

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
                        addressText = addressText
                    )
                }.onFailure {
                    updateWidgetView(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        isConfigured = true,
                        isPowerOn = false,
                        statusText = "Помилка завантаження",
                        scheduleText = "Не вдалося завантажити графік",
                        addressText = ""
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
                    addressText = ""
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
        addressText: String
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_detailed_schedule)

        // Update text views
        views.setTextViewText(R.id.widget_status_text, statusText)
        views.setTextViewText(R.id.widget_schedule_text, scheduleText)
        views.setTextViewText(R.id.widget_address_text, addressText)

        // Set icon
        val icon = if (isPowerOn) "✓" else "✗"
        views.setTextViewText(R.id.widget_status_icon, icon)

        // Set background drawable based on status
        val backgroundDrawable = when {
            !isConfigured -> R.drawable.widget_card_gray
            isPowerOn -> R.drawable.widget_card_green
            else -> R.drawable.widget_card_red
        }
        views.setInt(R.id.widget_container, "setBackgroundResource", backgroundDrawable)

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

    private fun formatScheduleList(schedules: List<com.occaecat.ztoeschedule.data.model.Schedule>): String {
        if (schedules.isEmpty()) return "Немає графіку"

        return schedules.take(3).joinToString("\n") { schedule ->
            val status = if (schedule.color.lowercase() != "red") "✓" else "✗"
            "$status ${schedule.span}"
        } + if (schedules.size > 3) "\n..." else ""
    }

    private fun formatAddress(selection: com.occaecat.ztoeschedule.data.local.SavedSelection): String {
        return buildString {
            if (selection.streetName != null) {
                append(selection.streetName)
                append(", ${selection.addressName}")
            } else {
                append("Адреса не вибрана")
            }
        }
    }
}

