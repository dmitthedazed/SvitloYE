package com.occaecat.ztoeschedule.domain.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.network.RetrofitClient
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.TimeUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.TimeZone

/**
 * Foreground service that displays persistent notification with current power status.
 * Uses Europe/Kyiv timezone for accurate tracking.
 */
class StatusNotificationService : Service() {

    companion object {
        private const val CHANNEL_ID = "power_status_channel"
        private const val CHANNEL_NAME = "Статус Електропостачання"
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
    private lateinit var preferencesManager: EnergyPreferencesManager
    private lateinit var repository: EnergyRepository
    private var updateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        preferencesManager = EnergyPreferencesManager(applicationContext)
        val addressStorage = com.occaecat.ztoeschedule.data.local.AddressStorage(applicationContext)
        repository = EnergyRepository(RetrofitClient.apiService, preferencesManager, addressStorage)

        createNotificationChannel()
        try {
            startForeground(NOTIFICATION_ID, createNotification())
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
            description = "Постійне відображення статусу електропостачання"
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
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Завантаження статусу...")
            .setContentText("Оновлення інформації")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startPeriodicUpdates() {
        updateJob = serviceScope.launch {
            while (isActive) {
                try { updateNotification() } catch (e: Exception) {}
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    private suspend fun updateNotification() {
        val savedSelection = preferencesManager.savedSelectionFlow.first() ?: return
        if (savedSelection.cherga == 0) return

        val result = repository.getScheduleWithMessages(savedSelection.cherga, savedSelection.pidcherga)
        result.onSuccess { data ->
            val groupedSchedules = ScheduleMapper.getGroupedSchedule(data.schedules)
            val currentStatus = getCurrentGroupedStatus(groupedSchedules)

            if (currentStatus != null) {
                val notification = createStatusNotification(
                    isPowerOn = currentStatus.isLightOn,
                    timeSpan = currentStatus.span,
                    address = savedSelection.addressName,
                    statusText = currentStatus.displayText
                )
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
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
        return try { parts[0].toInt() * 60 + parts[1].toInt() } catch (e: Exception) { null }
    }

    private fun createStatusNotification(
        isPowerOn: Boolean,
        timeSpan: String,
        address: String,
        statusText: String
    ): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val icon = if (isPowerOn) android.R.drawable.presence_online else android.R.drawable.presence_busy
        val title = if (isPowerOn) "🟢 $statusText" else "🔴 $statusText"
        
        val rawEndTime = Regex("""(\d{2}:\d{2})""").findAll(timeSpan).lastOrNull()?.value ?: ""
        val endTime = TimeUtils.formatToSystemTime(this, rawEndTime)
        val message = "До $endTime • $address"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
