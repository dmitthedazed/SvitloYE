package com.occaecat.ztoeschedule.domain.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.TimeUtils
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Foreground service that displays persistent notification with current power status.
 * Uses NTP time via TimeProvider.
 * Reacts immediately to address changes.
 */
@AndroidEntryPoint
class StatusNotificationService : Service() {

    companion object {
        private const val CHANNEL_ID = "power_status_channel"
        private const val CHANNEL_NAME = "СвітлоЄ? Статус"
        private const val NOTIFICATION_ID = 2001
        private const val UPDATE_INTERVAL_MS = 60_000L // 1 minute

        fun start(context: Context) {
            val intent = Intent(context, StatusNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, StatusNotificationService::class.java)
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
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, createNotification())
            }
            stopSelf()
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
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = "Постійне відображення наявності світла"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_STATUS_ID)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle("Завантаження статусу...")
            .setContentText("Оновлення інформації")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun startPeriodicUpdates() {
        updateJob = serviceScope.launch {
            // Ticker flow that emits every minute
            val ticker = flow {
                while (true) {
                    emit(Unit)
                    delay(UPDATE_INTERVAL_MS)
                }
            }

            // Combine ticker and address preferences.
            // This ensures we update when:
            // 1. Time passes (every minute)
            // 2. User changes address (immediately)
            preferencesManager.savedSelectionFlow
                .combine(ticker) { selection, _ -> selection }
                .collect { selection ->
                    if (selection != null && selection.cherga != 0) {
                        try { updateNotification(selection) } catch (e: Exception) {}
                    }
                }
        }
    }

    private suspend fun updateNotification(selection: com.occaecat.ztoeschedule.data.local.SavedSelection) {
        val result = repository.getCachedScheduleWithMessages(selection.cherga, selection.pidcherga)
        result.onSuccess { data ->
            val groupedSchedules = ScheduleMapper.getGroupedSchedule(data.schedules)
            val currentStatus = ScheduleMapper.getCurrentGroupedStatus(groupedSchedules, timeProvider.now())

            if (currentStatus != null) {
                val notification = createStatusNotification(
                    current = currentStatus,
                    address = selection.addressName
                )
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun createStatusNotification(
        current: GroupedSchedule,
        address: String
    ): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        
        val refreshIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(this, 1, refreshIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val isPowerOn = current.status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.AVAILABLE
        val isWarning = current.status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.PROBABLE
        
        val endTime = TimeUtils.formatToSystemTime(this, current.endTime)
        
        // Emoji Title
        val titleIcon = if (isPowerOn) "🟢" else if (isWarning) "🟡" else "🔴"
        val statusTitle = if (isPowerOn) "Світло є" else if (isWarning) "Можливо" else "Відключення"
        val title = "$titleIcon $statusTitle • До $endTime"
        
        // Calculate remaining time using NTP
        val nowMs = timeProvider.now()
        val remainingMs = current.endMs - nowMs
        val remainingMinutes = if (remainingMs > 0) remainingMs / 60000 else 0
        
        val h = remainingMinutes / 60
        val m = remainingMinutes % 60
        val remainingText = if (h > 0) "${h}год ${m}хв" else "${m}хв"
        
        // Calculate progress
        val durationMs = current.endMs - current.startMs
        val progress = if (durationMs > 0) {
            ((nowMs - current.startMs).toFloat() / durationMs.toFloat() * 100).toInt().coerceIn(0, 100)
        } else 0
        
        val icon = if (isPowerOn) android.R.drawable.presence_online else android.R.drawable.presence_busy
        val message = "Залишилось: $remainingText\n📍 $address"

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_STATUS_ID)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle(title)
            .setContentText("Залишилось: $remainingText")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_bolt, "Оновити", refreshPendingIntent) // Refresh Action
            .build()
    }
}