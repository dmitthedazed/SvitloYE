package com.occaecat.ztoeschedule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
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

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Adaptive Power Status Widget
 * Supports 1x1, 4x1, and 2x2 sizes with Material 3 styling
 */
@AndroidEntryPoint
class PowerStatusWidget : AppWidgetProvider() {

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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
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
                    updateWidgetState(context, appWidgetManager, appWidgetId, WidgetState.NotConfigured)
                    return@launch
                }

                val result = repository.getScheduleWithMessages(
                    savedSelection.cherga,
                    savedSelection.pidcherga
                )

                result.onSuccess { data ->
                    val currentStatus = ScheduleDomainLogic.getCurrentStatus(data.schedules)
                    
                    if (currentStatus != null) {
                         // Parse time
                         val endTime = parseEndTime(currentStatus.span)
                         val minutesRemaining = endTime?.let { getMinutesUntilTime(it) } ?: 0
                         val timeRemainingStr = formatTimeRemaining(minutesRemaining)
                         val statusText = if (currentStatus.color.lowercase() != "red") "Світло є" else "Відключення"
                         val systemUntilTime = com.occaecat.ztoeschedule.domain.TimeUtils.formatToSystemTime(context, endTime?.let { "${it.first.toString().padStart(2, '0')}:${it.second.toString().padStart(2, '0')}" } ?: "00:00")
                         
                         updateWidgetState(
                             context, 
                             appWidgetManager, 
                             appWidgetId, 
                             WidgetState.Loaded(
                                 isPowerOn = currentStatus.color.lowercase() != "red",
                                 status = currentStatus.color.lowercase(),
                                 statusText = statusText,
                                 timeRemaining = timeRemainingStr,
                                 untilTime = "До $systemUntilTime"
                             )
                         )
                    } else {
                         updateWidgetState(context, appWidgetManager, appWidgetId, WidgetState.Error("Немає даних"))
                    }

                }.onFailure {
                    updateWidgetState(context, appWidgetManager, appWidgetId, WidgetState.Error("Помилка"))
                }
            } catch (e: Exception) {
                updateWidgetState(context, appWidgetManager, appWidgetId, WidgetState.Error("Помилка"))
            }
        }
    }

    private fun updateWidgetState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        state: WidgetState
    ) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

        val layoutId = when {
            minWidth < 110 && minHeight < 110 -> R.layout.widget_status_small // 1x1
            minHeight >= 110 -> R.layout.widget_status_medium // 2x2
            else -> R.layout.widget_status_wide // 4x1
        }

        val views = RemoteViews(context.packageName, layoutId)

        // Bind data based on specific layout to avoid missing view ID errors
        when (layoutId) {
            R.layout.widget_status_small -> {
                // 1x1 Layout
                when (state) {
                    is WidgetState.NotConfigured -> {
                        views.setTextViewText(R.id.widget_remaining_small, "?")
                        views.setImageViewResource(R.id.widget_icon, R.drawable.ic_launcher_foreground)
                        setWidgetColor(context, views, "gray")
                    }
                    is WidgetState.Error -> {
                        views.setTextViewText(R.id.widget_remaining_small, "!")
                        views.setImageViewResource(R.id.widget_icon, android.R.drawable.stat_notify_error)
                        setWidgetColor(context, views, "gray")
                    }
                    is WidgetState.Loaded -> {
                        val iconRes = if (state.isPowerOn) android.R.drawable.presence_online else android.R.drawable.presence_busy
                        views.setImageViewResource(R.id.widget_icon, iconRes)
                        
                        val shortTime = state.timeRemaining
                            .replace("До відключення: ", "")
                            .replace("До увімкнення: ", "")
                            .replace(" ", "")
                        views.setTextViewText(R.id.widget_remaining_small, shortTime)
                        
                        setWidgetColor(context, views, state.status)
                    }
                }
            }
            R.layout.widget_status_wide -> {
                // 4x1 Layout
                when (state) {
                    is WidgetState.NotConfigured -> {
                        views.setTextViewText(R.id.widget_status_text, "Налаштуйте")
                        views.setTextViewText(R.id.widget_subtitle, "адресу в додатку")
                        views.setTextViewText(R.id.widget_remaining_pill, "⚙")
                        views.setImageViewResource(R.id.widget_icon, R.drawable.ic_launcher_foreground)
                        setWidgetColor(context, views, "gray")
                    }
                    is WidgetState.Error -> {
                        views.setTextViewText(R.id.widget_status_text, "Помилка")
                        views.setTextViewText(R.id.widget_subtitle, state.message)
                        views.setTextViewText(R.id.widget_remaining_pill, "!")
                        views.setImageViewResource(R.id.widget_icon, android.R.drawable.stat_notify_error)
                        setWidgetColor(context, views, "gray")
                    }
                    is WidgetState.Loaded -> {
                        val iconRes = if (state.isPowerOn) android.R.drawable.presence_online else android.R.drawable.presence_busy
                        views.setImageViewResource(R.id.widget_icon, iconRes)
                        
                        views.setTextViewText(R.id.widget_status_text, state.statusText)
                        views.setTextViewText(R.id.widget_subtitle, state.untilTime)
                        
                        val shortTime = state.timeRemaining
                            .replace("До відключення: ", "")
                            .replace("До увімкнення: ", "")
                        views.setTextViewText(R.id.widget_remaining_pill, shortTime)
                        
                        setWidgetColor(context, views, state.status)
                    }
                }
            }
            R.layout.widget_status_medium -> {
                // 2x2 Layout
                when (state) {
                    is WidgetState.NotConfigured -> {
                        views.setTextViewText(R.id.widget_status_text, "Налаштуйте")
                        views.setTextViewText(R.id.widget_remaining_text, "адресу в додатку")
                        views.setImageViewResource(R.id.widget_icon, R.drawable.ic_launcher_foreground)
                        setWidgetColor(context, views, "gray")
                    }
                    is WidgetState.Error -> {
                        views.setTextViewText(R.id.widget_status_text, "Помилка")
                        views.setTextViewText(R.id.widget_remaining_text, state.message)
                        views.setImageViewResource(R.id.widget_icon, android.R.drawable.stat_notify_error)
                        setWidgetColor(context, views, "gray")
                    }
                    is WidgetState.Loaded -> {
                        val iconRes = if (state.isPowerOn) android.R.drawable.presence_online else android.R.drawable.presence_busy
                        views.setImageViewResource(R.id.widget_icon, iconRes)
                        
                        views.setTextViewText(R.id.widget_status_text, state.statusText)
                        views.setTextViewText(R.id.widget_remaining_text, state.timeRemaining)
                        
                        setWidgetColor(context, views, state.status)
                    }
                }
            }
        }

        // Click Action
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setWidgetColor(context: Context, views: RemoteViews, status: String) {
        val drawableRes = when (status.lowercase()) {
            "red" -> R.drawable.widget_card_red
            "green", "white" -> R.drawable.widget_card_green
            "yellow" -> R.drawable.widget_card_yellow
            else -> R.drawable.widget_card_gray
        }
        
        // Safe background setting
        views.setInt(R.id.widget_container, "setBackgroundResource", drawableRes)
        
        // Determine contrasting text/icon color
        // Red/Green backgrounds are dark/saturated -> White text
        // Yellow/Gray backgrounds are light -> Black text
        val textColor = if (status.lowercase() == "yellow" || status.lowercase() == "gray") {
             Color.BLACK
        } else {
             Color.WHITE
        }
        
        // Apply to all potential text views (safe even if view doesn't exist in layout)
        views.setTextColor(R.id.widget_status_text, textColor)
        views.setTextColor(R.id.widget_remaining_small, textColor)
        views.setTextColor(R.id.widget_subtitle, textColor)
        views.setTextColor(R.id.widget_remaining_text, textColor)
        // Pill usually has its own background, let's keep it legible
        views.setTextColor(R.id.widget_remaining_pill, Color.BLACK) 
        
        // Tint Icon
        views.setInt(R.id.widget_icon, "setColorFilter", textColor)
    }
    
    private fun createTintedBitmap(context: Context, iconResId: Int, color: Int): android.graphics.Bitmap? {
        return null // Unused now
    }

    // Helpers (copied/adapted from previous)
    sealed class WidgetState {
        object NotConfigured : WidgetState()
        data class Error(val message: String) : WidgetState()
        data class Loaded(
            val isPowerOn: Boolean,
            val status: String, // red/green/yellow
            val statusText: String,
            val timeRemaining: String,
            val untilTime: String
        ) : WidgetState()
    }

    private fun parseEndTime(span: String): Pair<Int, Int>? {
        return try {
            val parts = span.split("-")
            if (parts.size != 2) return null
            val endPart = parts[1].trim()
            val timeParts = endPart.split(":")
            if (timeParts.size != 2) return null
            Pair(timeParts[0].toInt(), timeParts[1].toInt())
        } catch (e: Exception) { null }
    }

    private fun getMinutesUntilTime(targetTime: Pair<Int, Int>): Int {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val targetMinutes = targetTime.first * 60 + targetTime.second
        return if (targetMinutes > currentMinutes) targetMinutes - currentMinutes else (24 * 60 - currentMinutes) + targetMinutes
    }

    private fun formatTimeRemaining(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "${hours}г ${mins}хв" else "${mins}хв"
    }
}

