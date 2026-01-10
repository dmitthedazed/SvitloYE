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
 * - PROMOTED: Live Update notification with status chip (Android 16/API 36+)
 */
enum class StatusNotificationStyle {
    SIMPLE,           // Standard Android notification (all versions)
    LIVE_ACTIVITY,    // Rich/live notification with chronometer (API 31+)
    PROMOTED          // Live Update (Promoted Ongoing) notification (API 36+)
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
        private const val PROMOTED_MIN_SDK = 36
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
        Log.i(TAG, ">>> createStatus for $address (requested style=$style, status=${current.status})")

        // Automatically downgrade style if not supported
        val actualStyle = when {
            style == StatusNotificationStyle.PROMOTED && Build.VERSION.SDK_INT >= PROMOTED_MIN_SDK -> {
                StatusNotificationStyle.PROMOTED
            }
            style == StatusNotificationStyle.LIVE_ACTIVITY && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                StatusNotificationStyle.LIVE_ACTIVITY
            }
            else -> {
                if (style != StatusNotificationStyle.SIMPLE) {
                    Log.w(TAG, "⚠ Downgrading from $style to SIMPLE (API level ${Build.VERSION.SDK_INT})")
                }
                StatusNotificationStyle.SIMPLE
            }
        }
        
        Log.i(TAG, "Using actualStyle=$actualStyle for API ${Build.VERSION.SDK_INT}")

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
        Log.i(TAG, ">>> createSimpleStatusNotification for $address")

        val pendingIntent = getPendingIntentToApp()
        val refreshPendingIntent = getRefreshPendingIntent()

        val (title, message, progress) = buildStatusContent(current, address)

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_STATUS_ID)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_bolt, context.getString(R.string.notif_action_refresh), refreshPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        
        Log.i(TAG, "<<< SIMPLE notification created: progress=$progress")
        return notification
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
        Log.i(TAG, ">>> createLiveActivityNotification for $address")

        val pendingIntent = getPendingIntentToApp()
        val refreshPendingIntent = getRefreshPendingIntent()

        val (title, message, progress) = buildStatusContent(current, address)
        val isPowerOn = current.status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.Available
        val isWarning = current.status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.Probable

        // Set appropriate color based on status
        val colorRes = when {
            isPowerOn -> R.color.widget_power_on
            isWarning -> android.R.color.holo_orange_light
            else -> R.color.widget_power_off
        }
        val color = context.getColor(colorRes)

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_STATUS_ID)
            .setSmallIcon(if (isPowerOn) R.drawable.ic_bolt else R.drawable.ic_home_filled)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_bolt, context.getString(R.string.notif_action_refresh), refreshPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            // Color for icon/actions (NOT colorized background - forbidden for Live Updates)
            .setColor(color)
            // Chronometer countdown to next change
            .setWhen(current.endMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .build()
        
        Log.i(TAG, "<<< LIVE_ACTIVITY notification created: colorized=true, when=${current.endMs}, color=$color")
        return notification
    }

    /**
     * Create promoted Live Update notification (Android 16+/API 36+).
     *
     * Features:
     * - Uses Promoted Ongoing Notification API with ProgressStyle
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
    @Suppress("NewApi")
    private fun createPromotedNotification(
        current: GroupedSchedule,
        address: String
    ): Notification {
        Log.i(TAG, ">>> createPromotedNotification (Live Update) for $address")

        val pendingIntent = getPendingIntentToApp()
        val refreshPendingIntent = getRefreshPendingIntent()

        val (title, message, progress) = buildStatusContent(current, address)
        val isPowerOn = current.status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.Available
        val isWarning = current.status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.Probable

        // Use LIVE_UPDATE channel for API 36+ (requires IMPORTANCE_DEFAULT or higher)
        val channelId = NotificationHelper.CHANNEL_LIVE_UPDATE_ID
        Log.i(TAG, "Using channel: $channelId")

        // Calculate remaining time for progress
        val remainingMs = current.endMs - timeProvider.now()
        val totalDurationMs = current.endMs - current.startMs
        val progressPercent = if (totalDurationMs > 0) {
            ((totalDurationMs - remainingMs) * 100 / totalDurationMs).coerceIn(0, 100).toInt()
        } else 0
        val totalMinutes = (totalDurationMs / 60_000L).coerceAtLeast(1L)
        val elapsedMinutes = ((totalDurationMs - remainingMs) / 60_000L).coerceIn(0L, totalMinutes)

        // Status text for chip - max 6 chars per Android requirement
        val statusChipText = when {
            isPowerOn -> "ON" // Power is on
            isWarning -> "WARN" // Probable outage
            else -> "OFF" // Power is off
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (isPowerOn) R.drawable.ic_bolt else R.drawable.ic_home_filled)
            .setContentTitle(title) // Required for Live Update
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true) // Required for Live Update
            .setRequestPromotedOngoing(true) // Request promotion to Live Update
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_bolt, context.getString(R.string.notif_action_refresh), refreshPendingIntent)
            .setOnlyAlertOnce(true)
            .setColorized(false) // Must NOT be colorized for Live Update
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setDeleteIntent(getDeleteIntent()) // Required for Promoted cleanup
            .setShortCriticalText(statusChipText) // Status chip - max 6 chars
            .setWhen(current.endMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            // Minute-resolution progress so long periods still advance steadily.
            .setProgress(totalMinutes.toInt(), elapsedMinutes.toInt(), false)

        // Try to use ProgressStyle if available (Android 15+)
        try {
            val progressStyle = NotificationCompat.ProgressStyle()
                .setProgress(progressPercent)
            builder.setStyle(progressStyle)
            Log.i(TAG, "✓ Using ProgressStyle with progress=$progressPercent%")
        } catch (e: Exception) {
            // Fallback to BigTextStyle if ProgressStyle not available
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
            Log.w(TAG, "ProgressStyle not available, using BigTextStyle: ${e.message}")
        }

        // Set accent color for icon/actions (not colorized background)
        val colorRes = when {
            isPowerOn -> R.color.widget_power_on
            isWarning -> android.R.color.holo_orange_light
            else -> R.color.widget_power_off
        }
        builder.setColor(context.getColor(colorRes))

        val notification = builder.build()
        
        // Diagnostic logging - check if notification can be promoted
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val canPost = notificationManager.canPostPromotedNotifications()
            val hasPromotable = notification.hasPromotableCharacteristics()
            Log.i(TAG, "🔍 canPostPromotedNotifications=$canPost, hasPromotableCharacteristics=$hasPromotable")
        } catch (e: Exception) {
            Log.w(TAG, "Could not check promotion status: ${e.message}")
        }
        
        Log.i(TAG, "<<< PROMOTED notification created: channel=$channelId, progress=$progressPercent%, chipText='$statusChipText'")
        return notification
    }

    /**
     * Build notification content: title, message, and progress percentage.
     *
     * Uses NotificationTextHelper for consistent message formatting.
     *
     * Title format: "🟢 Power ON • Until HH:MM"
     * Message format: "Remaining: XXh XXm\n📍 Address Name"
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
            isPowerOn -> context.getString(R.string.notif_status_power_on)
            isWarning -> context.getString(R.string.notif_status_probable)
            else -> context.getString(R.string.notif_status_outage)
        }
        val title = context.getString(R.string.notif_title_format, titleIcon, statusTitle, endTime)

        // Calculate remaining time
        val nowMs = timeProvider.now()
        val remainingMs = current.endMs - nowMs
        val remainingMinutes = if (remainingMs > 0) remainingMs / 60000 else 0
        val hours = remainingMinutes / 60
        val minutes = remainingMinutes % 60
        val remainingText = if (hours > 0) {
            context.getString(R.string.notif_remaining_hm, hours.toInt(), minutes.toInt())
        } else {
            context.getString(R.string.notif_remaining_m, minutes.toInt())
        }

        // Calculate progress percentage
        val durationMs = current.endMs - current.startMs
        val progress = if (durationMs > 0) {
            ((nowMs - current.startMs).toFloat() / durationMs.toFloat() * 100)
                .toInt()
                .coerceIn(0, 100)
        } else 0

        // Build final message
        val message = context.getString(R.string.notif_message_format, remainingText, address)

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
     * Get PendingIntent for notification deletion (swipe dismiss).
     * Required for Promoted/Live Update notifications cleanup.
     */
    private fun getDeleteIntent(): PendingIntent {
        val deleteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISSED
        }
        return PendingIntent.getBroadcast(
            context, 2, deleteIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Check if the app can post Live Update (Promoted) notifications.
     *
     * This checks:
     * 1. Android version (API 36+)
     * 2. User permission settings (canPostPromotedNotifications)
     *
     * @return true if Live Updates can be posted
     */
    fun canPostLiveUpdates(): Boolean {
        if (Build.VERSION.SDK_INT < PROMOTED_MIN_SDK) {
            Log.d(TAG, "Live Updates not supported: API ${Build.VERSION.SDK_INT} < $PROMOTED_MIN_SDK")
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
     * 1. PROMOTED (if API 36+ and user enabled Live Updates)
     * 2. LIVE_ACTIVITY (if API 31+)
     * 3. SIMPLE (fallback for all versions)
     */
    fun getBestSupportedStyle(): StatusNotificationStyle {
        Log.i(TAG, ">>> getBestSupportedStyle: API level = ${Build.VERSION.SDK_INT}")
        
        val style = when {
            Build.VERSION.SDK_INT >= PROMOTED_MIN_SDK && canPostPromotedNotifications() -> {
                // Android 16+ supports Live Update (Promoted) notifications
                // Only use if user has granted permission
                StatusNotificationStyle.PROMOTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Android 12-14 use rich chronometer style
                // Also fallback for API 36+ if promoted permission denied
                StatusNotificationStyle.LIVE_ACTIVITY
            }
            else -> {
                // Android < 12 use simple style
                StatusNotificationStyle.SIMPLE
            }
        }
        
        Log.i(TAG, "<<< getBestSupportedStyle: returning $style")
        return style
    }
    
    /**
     * Check if app can post promoted (Live Update) notifications.
     * Returns true if user has enabled the permission in Settings.
     */
    @Suppress("NewApi")
    private fun canPostPromotedNotifications(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= PROMOTED_MIN_SDK) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val canPost = notificationManager.canPostPromotedNotifications()
                Log.d(TAG, "canPostPromotedNotifications = $canPost")
                canPost
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking promoted notification permission: ${e.message}")
            false
        }
    }
}
