package com.occaecat.ztoeschedule.domain.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.TimeUtils
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification style options.
 *
 * Different styles for different Android versions with progressively richer features:
 * - SIMPLE: Standard notification (all versions)
 * - LIVE_ACTIVITY: Rich live notification with progress (Android 12/API 31+)
 * - PROMOTED: Live Update notification with status chip (Android 15/API 35+)
 */
enum class StatusNotificationStyle {
    SIMPLE,           // Standard Android notification (all versions)
    LIVE_ACTIVITY,    // Rich/live notification with chronometer (API 31+)
    PROMOTED          // Live Update (Promoted Ongoing) notification (API 35+)
}

/**
 * Factory for creating notification objects.
 *
 * Responsibilities:
 * - Create alert notifications (high priority, sound/vibration)
 * - Create status notifications in different styles
 * - Build rich notification content with progress and status icons
 * - Use NotificationTextHelper for consistent message formatting
 * - Handle API-level differences gracefully
 *
 * Note: Does NOT post notifications - just creates them.
 * SmartNotificationManager/PowerStatusService handles posting.
 */
@Singleton
class NotificationFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) {
    companion object {
        private const val TAG = "NotificationFactory"
    }

    /**
     * Create a high-priority alert notification about power status change.
     *
     * Features:
     * - Uses CHANNEL_ALERTS_ID (high priority with sound/vibration)
     * - Includes big text style for full message display
     * - Color-coded by status (red=outage, green=available)
     * - Auto-cancels when tapped
     * - Opens MainActivity on tap
     *
     * @param title Alert title (includes emoji and action text)
     * @param message Alert message (address and status)
     * @param isOutage true for power outage, false for power available
     */
    fun createAlert(
        title: String,
        message: String,
        isOutage: Boolean
    ): Notification {
        Log.d(TAG, "createAlert: $title")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val color = if (isOutage) Color.RED else Color.GREEN

        return NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(color)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(false)
            .build()
    }

    /**
     * Create a status notification in appropriate style for current Android version.
     *
     * Automatically selects style based on:
     * 1. Android API level
     * 2. Requested style
     * 3. Available resources
     *
     * @param current Current power status
     * @param schedules All schedules for progress calculation
     * @param address Address being monitored
     * @param style Preferred style (SIMPLE/LIVE_ACTIVITY/PROMOTED)
     */
    fun createStatus(
        current: GroupedSchedule,
        schedules: List<GroupedSchedule>,
        address: String,
        style: StatusNotificationStyle = StatusNotificationStyle.SIMPLE
    ): Notification {
        Log.d(TAG, "createStatus for $address (style=$style, status=${current.status})")

        // Automatically downgrade style if not supported
        val actualStyle = when {
            style == StatusNotificationStyle.PROMOTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM -> {
                StatusNotificationStyle.PROMOTED
            }
            style == StatusNotificationStyle.LIVE_ACTIVITY && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                StatusNotificationStyle.LIVE_ACTIVITY
            }
            else -> {
                if (style != StatusNotificationStyle.SIMPLE) {
                    Log.d(TAG, "Downgrading from $style to SIMPLE (API level ${Build.VERSION.SDK_INT})")
                }
                StatusNotificationStyle.SIMPLE
            }
        }

        return when (actualStyle) {
            StatusNotificationStyle.PROMOTED -> createPromotedNotification(current, address)
            StatusNotificationStyle.LIVE_ACTIVITY -> createLiveActivityNotification(current, address)
            StatusNotificationStyle.SIMPLE -> createSimpleStatusNotification(current, address)
        }
    }

    /**
     * Create simple status notification (works on all Android versions).
     *
     * Features:
     * - Static content with progress bar
     * - Refresh action
     * - Openable to MainActivity
     * - Low priority (silent)
     */
    private fun createSimpleStatusNotification(
        current: GroupedSchedule,
        address: String
    ): Notification {
        Log.d(TAG, "createSimpleStatusNotification")

        val pendingIntent = getPendingIntentToApp()
        val refreshPendingIntent = getRefreshPendingIntent()

        val (title, message, progress) = buildStatusContent(current, address)

        return NotificationCompat.Builder(context, NotificationHelper.CHANNEL_STATUS_ID)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_bolt, "Оновити", refreshPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Create live activity notification with chronometer countdown (Android 31+).
     *
     * Features:
     * - Chronometer showing countdown to next status change
     * - Rich status-aware icon
     * - Colorized background
     * - Progress indication via chronometer
     * - Refresh action
     */
    private fun createLiveActivityNotification(
        current: GroupedSchedule,
        address: String
    ): Notification {
        Log.d(TAG, "createLiveActivityNotification")

        val pendingIntent = getPendingIntentToApp()
        val refreshPendingIntent = getRefreshPendingIntent()

        val (title, message, progress) = buildStatusContent(current, address)
        val isPowerOn = current.status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.Available
        val isWarning = current.status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.Probable

        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_STATUS_ID)
            .setSmallIcon(if (isPowerOn) R.drawable.ic_bolt else R.drawable.ic_home_filled)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_bolt, "Оновити", refreshPendingIntent)
            .setColorized(true)

        // Set appropriate color based on status
        val colorRes = when {
            isPowerOn -> R.color.widget_power_on
            isWarning -> android.R.color.holo_orange_light
            else -> R.color.widget_power_off
        }
        builder.setColor(context.getColor(colorRes))

        // Chronometer countdown to next change
        builder.setWhen(current.endMs)
        builder.setUsesChronometer(true)
        builder.setChronometerCountDown(true)

        return builder.build()
    }

    /**
     * Create promoted Live Update notification (Android 15+/API 35+).
     *
     * Features:
     * - Uses Promoted Ongoing Notification API
     * - Persistent display at top of notification shade
     * - Status chip with critical information
     * - Expanded by default, uncollapsible
     * - Requires POST_PROMOTED_NOTIFICATIONS permission
     *
     * Requirements:
     * - Must use Standard/BigText/Progress style (no RemoteViews)
     * - Must be ongoing (FLAG_ONGOING_EVENT)
     * - Must have contentTitle
     * - Must request promotion via setRequestPromotedOngoing
     * - Channel must NOT be IMPORTANCE_MIN
     */
    private fun createPromotedNotification(
        current: GroupedSchedule,
        address: String
    ): Notification {
        Log.d(TAG, "createPromotedNotification (Live Update)")

        val pendingIntent = getPendingIntentToApp()
        val refreshPendingIntent = getRefreshPendingIntent()

        val (title, message, progress) = buildStatusContent(current, address)
        val isPowerOn = current.status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.Available
        val isWarning = current.status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.Probable

        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_STATUS_ID)
            .setSmallIcon(if (isPowerOn) R.drawable.ic_bolt else R.drawable.ic_home_filled)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Required for Live Update
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_bolt, "Оновити", refreshPendingIntent)
            .setOnlyAlertOnce(true)

        // Request promotion (Live Update) - API 35+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            builder.setRequestPromotedOngoing(true)
            Log.d(TAG, "Requested Live Update promotion")
        }

        // Set status chip text (critical info display) - shows in collapsed chip
        val timeRemaining = TimeUtils.formatDuration(current.endMs - timeProvider.now())
        builder.setShortCriticalText(timeRemaining)
        
        // Set when time for automatic countdown in status chip
        builder.setWhen(current.endMs)
        builder.setShowWhen(true)
        builder.setUsesChronometer(true)
        builder.setChronometerCountDown(true)

        // Don't colorize (requirement: must NOT setColorized to TRUE)
        // But we can set the accent color for icon/actions
        val colorRes = when {
            isPowerOn -> R.color.widget_power_on
            isWarning -> android.R.color.holo_orange_light
            else -> R.color.widget_power_off
        }
        builder.setColor(context.getColor(colorRes))

        return builder.build()
    }

    /**
     * Build notification content: title, message, and progress percentage.
     *
     * Uses NotificationTextHelper for consistent message formatting.
     *
     * Title format: "🟢 Світло є • До HH:MM"
     * Message format: "Залишилось: XXг XXхв\n📍 Address Name"
     * Progress: 0-100 percentage of current period duration
     */
    private fun buildStatusContent(
        current: GroupedSchedule,
        address: String
    ): Triple<String, String, Int> {
        val isPowerOn = current.status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.Available
        val isWarning = current.status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.Probable

        // Use TimeUtils for consistent time formatting
        val endTime = TimeUtils.formatToSystemTime(context, current.endTime)

        // Build title with icon and time
        val titleIcon = when {
            isPowerOn -> "🟢"
            isWarning -> "🟡"
            else -> "🔴"
        }
        val statusTitle = when {
            isPowerOn -> "Світло є"
            isWarning -> "Можливо"
            else -> "Відключення"
        }
        val title = "$titleIcon $statusTitle • До $endTime"

        // Calculate remaining time
        val nowMs = timeProvider.now()
        val remainingMs = current.endMs - nowMs
        val remainingMinutes = if (remainingMs > 0) remainingMs / 60000 else 0
        val hours = remainingMinutes / 60
        val minutes = remainingMinutes % 60
        val remainingText = if (hours > 0) "${hours}год ${minutes}хв" else "${minutes}хв"

        // Calculate progress percentage
        val durationMs = current.endMs - current.startMs
        val progress = if (durationMs > 0) {
            ((nowMs - current.startMs).toFloat() / durationMs.toFloat() * 100)
                .toInt()
                .coerceIn(0, 100)
        } else 0

        // Build final message
        val message = "Залишилось: $remainingText\n📍 $address"

        return Triple(title, message, progress)
    }

    /**
     * Get PendingIntent to open MainActivity.
     */
    private fun getPendingIntentToApp(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Get PendingIntent for refresh action in notification.
     */
    private fun getRefreshPendingIntent(): PendingIntent {
        val refreshIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_REFRESH
        }
        return PendingIntent.getBroadcast(
            context, 1, refreshIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Check if the app can post Live Update (Promoted) notifications.
     *
     * This checks:
     * 1. Android version (API 35+)
     * 2. User permission settings (canPostPromotedNotifications)
     *
     * @return true if Live Updates can be posted
     */
    fun canPostLiveUpdates(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            Log.d(TAG, "Live Updates not supported: API ${Build.VERSION.SDK_INT} < 35")
            return false
        }

        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) 
                as? android.app.NotificationManager
            val canPost = notificationManager?.canPostPromotedNotifications() ?: false
            Log.d(TAG, "Can post Live Updates: $canPost")
            canPost
        } catch (e: Exception) {
            Log.w(TAG, "Error checking Live Update permission", e)
            false
        }
    }

    /**
     * Get the best supported notification style for current device/settings.
     *
     * Priority:
     * 1. PROMOTED (if API 35+ and user enabled Live Updates)
     * 2. LIVE_ACTIVITY (if API 31+)
     * 3. SIMPLE (fallback for all versions)
     */
    fun getBestSupportedStyle(): StatusNotificationStyle {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && canPostLiveUpdates() -> {
                Log.d(TAG, "Using PROMOTED style (Live Update)")
                StatusNotificationStyle.PROMOTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                Log.d(TAG, "Using LIVE_ACTIVITY style")
                StatusNotificationStyle.LIVE_ACTIVITY
            }
            else -> {
                Log.d(TAG, "Using SIMPLE style")
                StatusNotificationStyle.SIMPLE
            }
        }
    }
}