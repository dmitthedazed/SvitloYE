package com.occaecat.ztoeschedule.domain.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.TimeUtils
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.util.ArrayList
import java.util.Calendar
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
    @Inject lateinit var timeProvider: TimeProvider
    
    private var updateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
                 try {
                     val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                     nm.notify(NOTIFICATION_ID, createLoadingNotification())
                 } catch (ex: Exception) { ex.printStackTrace() }
            }
        }

        startPeriodicUpdates()
        return START_STICKY
    }

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
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun startPeriodicUpdates() {
        updateJob?.cancel()
        
        updateJob = serviceScope.launch {
            val ticker = flow {
                while (true) {
                    emit(Unit)
                    delay(UPDATE_INTERVAL_MS)
                }
            }

            preferencesManager.savedSelectionFlow
                .combine(ticker) { selection, _ -> selection }
                .collect { selection ->
                    if (selection != null) {
                        try { updateNotification(selection) } catch (e: Exception) { e.printStackTrace() }
                    }
                }
        }
    }

    private suspend fun updateNotification(selection: com.occaecat.ztoeschedule.data.local.SavedSelection) {
        val result = repository.getCachedScheduleWithMessages(selection.cherga, selection.pidcherga)

        result.onSuccess { data ->
            val grouped = ScheduleMapper.getGroupedSchedule(data.schedules)
            val current = ScheduleMapper.getCurrentGroupedStatus(grouped, timeProvider.now())

            if (current != null) {
                val notification = createLiveActivityNotification(
                    current = current,
                    schedules = grouped,
                    address = selection.addressName
                )
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun createLiveActivityNotification(
        current: GroupedSchedule,
        schedules: List<GroupedSchedule>,
        address: String
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val refreshIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(this, 2, refreshIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val status = current.status
        val isPowerOn = status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.Available
        val isWarning = status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.Probable
        
        val endTimeMs = current.endMs
        val endTimeStr = TimeUtils.formatToSystemTime(this, current.endTime)
        
        val nowMs = timeProvider.now()
        val minutesRemaining = (endTimeMs - nowMs) / 60000
        
        val h = minutesRemaining / 60
        val m = minutesRemaining % 60
        val timeString = if (h > 0) "${h}г ${m}хв" else "${m}хв"
        
        val emoji = if (isPowerOn) "🟢" else if (isWarning) "🟡" else "🔴"
        val statusText = if (isPowerOn) "Світло є" else if (isWarning) "Можливо" else "Відключення"
        
        val smartTitle = "⏳ Ще $timeString • $emoji $statusText"
        val subText = "До $endTimeStr"

        val detailedMsg = NotificationTextHelper.getDetailedStatus(isPowerOn, endTimeStr, minutesRemaining)
        val easterEgg = NotificationTextHelper.getEasterEgg(isPowerOn)

        // --- ANDROID 16 (API 36) LIVE UPDATE ---
        // Using "36" constant. If Build.VERSION_CODES.VANILLA_ICE_CREAM is not yet available in the AGP, we use explicit 36.
        if (Build.VERSION.SDK_INT >= 36) {
             val builder = Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(Icon.createWithResource(this, if (isPowerOn) R.drawable.ic_bolt else R.drawable.ic_home_filled))
                .setContentTitle(smartTitle)
                .setContentText(detailedMsg)
                .setSubText(subText)
                .setOngoing(true)
                .setColorized(false) // Must be FALSE for Promoted
                .setContentIntent(pendingIntent)
                // New API 36 methods
                .setShortCriticalText(timeString) 
                .addExtras(android.os.Bundle().apply {
                    putBoolean("android.requestPromotedOngoing", true)
                })
                .addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_bolt), "Оновити", refreshPendingIntent).build())

             builder.setWhen(endTimeMs)
             builder.setShowWhen(true)
             builder.setUsesChronometer(true)
             builder.setChronometerCountDown(true)

             // Build modern ProgressStyle
             buildProgressStyle(schedules)?.let { builder.style = it }
             
             return builder.build()
        }

        // --- LEGACY STYLE (Android 12-15) ---
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (isPowerOn) R.drawable.ic_bolt else R.drawable.ic_home_filled)
            .setContentTitle(smartTitle)
            .setContentText(detailedMsg)
            .setSubText(subText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$detailedMsg\n📍 $address\n\n$easterEgg"))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_bolt, "Оновити", refreshPendingIntent)
            
        if (Build.VERSION.SDK_INT >= 31) {
            builder.setColorized(true)
            val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val bgColor = if (isPowerOn) (if (isDarkMode) Color.parseColor("#1B5E20") else Color.parseColor("#F1F8E9"))
                             else if (isWarning) (if (isDarkMode) Color.parseColor("#5D4037") else Color.parseColor("#FFF8E1"))
                             else (if (isDarkMode) Color.parseColor("#B71C1C") else Color.parseColor("#FFEBEE"))
            builder.setColor(bgColor)
        }
        
        builder.setWhen(endTimeMs)
        builder.setUsesChronometer(true)
        builder.setChronometerCountDown(true)

        return builder.build()
    }

    @RequiresApi(36)
    private fun buildProgressStyle(schedules: List<GroupedSchedule>): Notification.ProgressStyle? {
        try {
            val style = Notification.ProgressStyle()
            val nowCal = timeProvider.nowCalendar()
            
            // Current progress in minutes from midnight (0..1440)
            val currentProgress = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
            
            // Note: In Kotlin, setters for properties are accessed via direct property assignment
            // e.g. style.progress = ... instead of style.setProgress(...)
            // For Lists, API 36 likely uses setProgressSegments(List) or just property access.
            
            val segments = ArrayList<Notification.ProgressStyle.Segment>()
            val points = ArrayList<Notification.ProgressStyle.Point>()
            
            for (schedule in schedules) {
                val duration = schedule.durationHours * 60 + schedule.durationMinutes
                if (duration <= 0) continue
                
                val color = when (schedule.status) {
                    com.occaecat.ztoeschedule.data.model.ScheduleStatus.Outage -> Color.parseColor("#F44336")
                    com.occaecat.ztoeschedule.data.model.ScheduleStatus.Available -> Color.parseColor("#4CAF50")
                    com.occaecat.ztoeschedule.data.model.ScheduleStatus.Probable -> Color.parseColor("#FFC107")
                    else -> Color.GRAY
                }
                
                segments.add(Notification.ProgressStyle.Segment(duration).setColor(color))
            }
            
            points.add(Notification.ProgressStyle.Point(currentProgress).setColor(Color.WHITE))
            
            style.isStyledByProgress = true 
            style.progress = currentProgress
            style.progressSegments = segments
            style.progressPoints = points
            
            return style
        } catch (e: Exception) { return null }
    }
}
