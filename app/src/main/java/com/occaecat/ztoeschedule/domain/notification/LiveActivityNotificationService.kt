package com.occaecat.ztoeschedule.domain.notification

import android.content.pm.ServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.TimeUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.TimeZone
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LiveActivityNotificationService : Service() {

    companion object {
        private const val CHANNEL_ID = "live_activity_channel"
        private const val CHANNEL_NAME = "СвітлоЄ? Таймер"
        private const val NOTIFICATION_ID = 2002
        private const val UPDATE_INTERVAL_MS = 60_000L

        fun start(context: Context) {
            val intent = Intent(context, LiveActivityNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LiveActivityNotificationService::class.java)
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    @Inject lateinit var preferencesManager: EnergyPreferencesManager
    @Inject lateinit var repository: EnergyRepository
    
    private var updateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID, 
                    createLoadingNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, createLoadingNotification())
            }
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                 val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                 nm.notify(NOTIFICATION_ID, createLoadingNotification())
            }
        }
        startPeriodicUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Відображення часу до зміни статусу"
            setShowBadge(false)
            setSound(null, null)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun createLoadingNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle("Завантаження даних...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun startPeriodicUpdates() {
        updateJob = serviceScope.launch {
            while (isActive) {
                try { updateNotification() } catch (e: Exception) { e.printStackTrace() }
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    private suspend fun updateNotification() {
        val savedSelection = preferencesManager.savedSelectionFlow.first() ?: return
        val result = repository.getScheduleWithMessages(savedSelection.cherga, savedSelection.pidcherga)

        result.onSuccess { data ->
            val grouped = ScheduleMapper.getGroupedSchedule(data.schedules)
            val current = ScheduleMapper.getCurrentGroupedStatus(grouped)

            if (current != null) {
                val notification = createLiveActivityNotification(
                    status = current.color,
                    timeSpan = current.span,
                    text = current.displayText,
                    schedules = grouped
                )
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun createLiveActivityNotification(
        status: String,
        timeSpan: String,
        text: String,
        schedules: List<GroupedSchedule>
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val isPowerOn = status.lowercase() != "red"
        val endTimeMs = calculateEndTimeMs(timeSpan)
        val rawEndTimeStr = extractEndTime(timeSpan)
        val endTimeStr = TimeUtils.formatToSystemTime(this, rawEndTimeStr)
        
        val minutesRemaining = (endTimeMs - System.currentTimeMillis()) / 60000
        val titleText = NotificationTextHelper.getStatusTitle(isPowerOn, endTimeStr)
        val detailedMsg = NotificationTextHelper.getDetailedStatus(isPowerOn, endTimeStr, minutesRemaining)
        val easterEgg = NotificationTextHelper.getEasterEgg(isPowerOn)

        if (Build.VERSION.SDK_INT >= 36) {
             val builder = Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(titleText)
                .setContentText(detailedMsg + " | " + easterEgg)
                .setSmallIcon(Icon.createWithResource(this, if (isPowerOn) R.drawable.ic_bolt else R.drawable.ic_home_filled))
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setColorized(false) 
             builder.setWhen(endTimeMs)
             builder.setUsesChronometer(true)
             builder.setChronometerCountDown(true)
             builder.setShowWhen(true)
             val extras = android.os.Bundle()
             extras.putBoolean("android.requestPromotedOngoing", true)
             builder.setExtras(extras)
             buildProgressStyle(schedules)?.let { builder.style = it }
             return builder.build()
        }

        val remoteViews = RemoteViews(packageName, R.layout.notification_live_activity)
        val isWarning = status.lowercase() == "yellow"
        remoteViews.setImageViewResource(R.id.iv_status_icon, if (isPowerOn) R.drawable.ic_bolt else R.drawable.ic_home_filled)
        val statusColor = when {
            isWarning -> Color.parseColor("#FFC107")
            isPowerOn -> Color.parseColor("#4CAF50")
            else -> Color.parseColor("#F44336")
        }
        remoteViews.setInt(R.id.iv_status_icon, "setColorFilter", statusColor)
        remoteViews.setChronometer(R.id.tv_status_timer, endTimeMs, null, true)
        remoteViews.setTextViewText(R.id.tv_status_subtitle, detailedMsg + "\n" + easterEgg)
        remoteViews.setTextViewText(R.id.tv_time_remaining, if (isPowerOn) "СВІТЛО Є" else "OFFLINE")
        
        val pillColor = if (isPowerOn) ContextCompat.getColor(this, R.color.widget_status_positive)
                        else if (isWarning) ContextCompat.getColor(this, R.color.widget_status_warning)
                        else ContextCompat.getColor(this, R.color.widget_status_negative)
        remoteViews.setTextColor(R.id.tv_time_remaining, Color.BLACK)
        remoteViews.setInt(R.id.tv_time_remaining, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(pillColor).defaultColor)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (isPowerOn) R.drawable.ic_bolt else R.drawable.ic_home_filled)
            .setContentTitle(titleText)
            .setContentText(detailedMsg)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            builder.setColorized(true)
            val bgColor = if (isPowerOn) (if (isDarkMode) Color.parseColor("#1B5E20") else Color.parseColor("#F1F8E9"))
                             else (if (isDarkMode) Color.parseColor("#B71C1C") else Color.parseColor("#FFEBEE"))
            builder.setColor(bgColor)
        }
        return builder.build()
    }

    private fun extractEndTime(span: String): String = Regex("""(\d{2}:\d{2})""").findAll(span).lastOrNull()?.value ?: ""

    private fun calculateEndTimeMs(span: String): Long {
        return try {
            val endTimeStr = extractEndTime(span)
            val parts = endTimeStr.split(":")
            val kyivZone = TimeZone.getTimeZone("Europe/Kyiv")
            val calendar = Calendar.getInstance(kyivZone)
            val now = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            calendar.set(Calendar.MINUTE, parts[1].toInt())
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            if (calendar.timeInMillis <= now) calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.timeInMillis
        } catch (e: Exception) { System.currentTimeMillis() }
    }
            
    @RequiresApi(36)
    private fun buildProgressStyle(schedules: List<GroupedSchedule>): Notification.Style? {
        try {
            val kyivZone = TimeZone.getTimeZone("Europe/Kyiv")
            val now = Calendar.getInstance(kyivZone)
            val currentProgress = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val segments = ArrayList<Notification.ProgressStyle.Segment>()
            val points = ArrayList<Notification.ProgressStyle.Point>()
            val sortedSchedules = schedules.sortedBy { 
                val timeStr = Regex("""(\d{2}:\d{2})""").find(it.span)?.value ?: "00:00"
                val p = timeStr.split(":")
                p[0].toInt() * 60 + p[1].toInt()
            }
            for (schedule in sortedSchedules) {
                val duration = schedule.durationHours * 60 + schedule.durationMinutes
                val color = when (schedule.color.lowercase()) {
                    "red" -> Color.parseColor("#F44336")
                    "green", "white" -> Color.parseColor("#4CAF50")
                    "yellow" -> Color.parseColor("#FFC107")
                    else -> Color.GRAY
                }
                segments.add(Notification.ProgressStyle.Segment(duration).setColor(color))
            }
            points.add(Notification.ProgressStyle.Point(currentProgress).setColor(Color.WHITE))
            if (segments.isEmpty()) return null
            val style = Notification.ProgressStyle()
            style.isStyledByProgress = false
            style.progress = currentProgress
            style.progressSegments = segments
            style.progressPoints = points
            style.progressStartIcon = Icon.createWithResource(this, R.drawable.ic_home_filled)
            style.progressEndIcon = Icon.createWithResource(this, R.drawable.ic_bolt)
            return style
        } catch (e: Exception) { return null }
    }
}
