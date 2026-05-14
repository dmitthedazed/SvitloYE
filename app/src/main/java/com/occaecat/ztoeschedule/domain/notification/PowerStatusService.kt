package com.occaecat.ztoeschedule.domain.notification

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.occaecat.ztoeschedule.AppForegroundState
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.TimeUtils
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Foreground service that displays persistent notification with current power status.
 *
 * Responsibilities:
 * - Display persistent STATUS notification showing current power state
 * - Update periodically (every ~60 seconds)
 * - React immediately to address changes
 * - Gracefully restart when address changes
 * - Use NotificationCoordinator for coordinated UI updates
 *
 * Architecture:
 * - Uses StateFlow combining address selection + ticker for updates
 * - Delegates actual updates to NotificationCoordinator
 * - Properly manages coroutine scope lifecycle
 * - Validates permissions before starting foreground
 *
 * Lifecycle:
 * onCreate() → startForeground() → startPeriodicUpdates() → [periodic updates] → onDestroy()
 */
@AndroidEntryPoint
class PowerStatusService : Service() {

    companion object {
        private const val TAG = "PowerStatusService"
        private const val UPDATE_INTERVAL_LONG_MS = 5 * 60_000L // 5 minutes
        private const val UPDATE_INTERVAL_MEDIUM_MS = 60_000L // 1 minute
        private const val UPDATE_INTERVAL_SHORT_MS = 1_000L // 1 second
        private const val PROMOTION_WINDOW_MS = 15 * 60_000L // 15 minutes
        private const val CRITICAL_WINDOW_MS = 60_000L // 1 minute
        private const val NETWORK_TIMEOUT_MS = 10_000L  // 10 seconds
        private const val PROMOTED_MIN_SDK = 36
        private const val ACTION_IMMEDIATE_REFRESH = "com.occaecat.ztoeschedule.ACTION_IMMEDIATE_REFRESH"

        fun start(context: Context) {
            Log.d(TAG, "Starting PowerStatusService")
            val intent = Intent(context, PowerStatusService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            Log.d(TAG, "Stopping PowerStatusService")
            val intent = Intent(context, PowerStatusService::class.java)
            context.stopService(intent)
        }

        fun requestImmediateRefresh(context: Context) {
            Log.d(TAG, "Requesting immediate status refresh")
            val intent = Intent(context, PowerStatusService::class.java).apply {
                action = ACTION_IMMEDIATE_REFRESH
            }
            context.startForegroundService(intent)
        }
    }

    // Proper lifecycle-managed coroutine scope
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Inject
    lateinit var preferencesManager: EnergyPreferencesManager

    @Inject
    lateinit var repository: EnergyRepository

    @Inject
    lateinit var timeProvider: TimeProvider

    @Inject
    lateinit var smartNotificationManager: SmartNotificationManager

    @Inject
    lateinit var notificationCoordinator: NotificationCoordinator

    @Inject
    lateinit var notificationFactory: NotificationFactory

    private var updateJob: Job? = null
    private var lastAddressName: String? = null
    @Volatile
    private var latestStatusEndMs: Long? = null
    private var latestAddressId: String? = null
    private var latestGroupedSchedules: List<GroupedSchedule> = emptyList()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate()")

        NotificationHelper.createAllChannels(this)

        try {
            // Create initial notification immediately for foreground requirement
            val initialNotification = buildInitialNotification()

            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    NotificationIds.STATUS,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NotificationIds.STATUS,
                    initialNotification,
                    0 // No specific type needed for this purpose on API 29-33
                )
            } else {
                startForeground(NotificationIds.STATUS, initialNotification)
            }

            Log.d(TAG, "Foreground service started successfully")

            // Now start periodic updates
            startPeriodicUpdates()

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting foreground service - missing POST_NOTIFICATIONS permission", e)
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
            try {
                // Fallback without service type
                val fallbackNotification = buildInitialNotification()
                startForeground(NotificationIds.STATUS, fallbackNotification)
                startPeriodicUpdates()
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback also failed, stopping service", e2)
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand()")
        if (intent?.action == ACTION_IMMEDIATE_REFRESH) {
            serviceScope.launch {
                try {
                    performUpdate()
                } catch (e: Exception) {
                    Log.e(TAG, "Immediate refresh failed", e)
                }
            }
        }
        return START_STICKY // Restart if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy()")

        updateJob?.cancel()
        
        // Notify coordinator of graceful shutdown BEFORE canceling scope
        // Using runBlocking because onDestroy is called synchronously
        try {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                notificationCoordinator.shutdown()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown: ${e.message}")
        }
        
        serviceScope.cancel()
    }

    /**
     * Start periodic updates by combining address selection with timer.
     *
     * Handles address changes immediately by canceling old job and starting new one.
     * Uses primary address (sorted by priority) instead of saved selection.
     * 
     * First update happens IMMEDIATELY, then uses adaptive cadence.
     */
    private fun startPeriodicUpdates() {
        Log.d(TAG, "Starting periodic updates")

        updateJob = serviceScope.launch {
            try {
                // IMMEDIATE FIRST UPDATE - no delay
                Log.i(TAG, "🚀 Performing immediate initial update")
                performUpdate()
                
                // Then start periodic updates
                createTickerFlow().collect { intervalMs ->
                    val useCacheOnly = intervalMs == UPDATE_INTERVAL_SHORT_MS
                    performUpdate(useCacheOnly = useCacheOnly)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in periodic updates", e)
            }
        }
    }
    
    /**
     * Perform a single notification update with primary address.
     */
    private suspend fun performUpdate(useCacheOnly: Boolean = false) {
        // Get primary address (sorted by priority)
        val addresses = repository.getSavedAddresses()
            .sortedBy { it.priority }
        
        val primaryAddress = addresses.firstOrNull()

        if (primaryAddress != null) {
            // Handle address change - graceful restart
            if (primaryAddress.name != lastAddressName) {
                Log.i(TAG, "Address changed from $lastAddressName to ${primaryAddress.name}")
                lastAddressName = primaryAddress.name
                notificationCoordinator.clearAlertHistory()
            }

            // Update notification with primary address
            if (useCacheOnly && canUseCachedStatus(primaryAddress)) {
                updateNotificationFromCache(primaryAddress)
            } else {
                updateNotificationForAddress(primaryAddress)
            }
        } else {
            Log.d(TAG, "No saved addresses available")
            latestStatusEndMs = null
            clearCachedStatus()
            showNoAddressNotification()
        }
    }

    /**
     * Create ticker flow with adaptive cadence based on time to next status change.
     * Delay happens BEFORE emit (since first emit is handled manually).
     */
    private suspend fun createTickerFlow() = kotlinx.coroutines.flow.flow {
        while (true) {
            val (delayMs, intervalMs) = calculateNextUpdateSchedule()
            delay(delayMs)
            emit(intervalMs)
        }
    }

    /**
     * Update notification for given address.
     *
     * With network timeout protection to prevent hanging.
     */
    private suspend fun updateNotificationForAddress(
        address: com.occaecat.ztoeschedule.data.model.SavedAddress
    ) {
        try {
            Log.d(TAG, "Updating notification for ${address.name}")

            // Network call with timeout
            val result = withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
                repository.getCachedScheduleWithMessages(address.cherga, address.pidcherga)
            }

            if (result == null) {
                Log.w(TAG, "Network timeout fetching schedule")
                latestStatusEndMs = null
                clearCachedStatus()
                showTimeoutNotification(address.name)
                return
            }

            result.onSuccess { data ->
                val groupedSchedules = ScheduleMapper.getGroupedSchedule(data.schedules)
                val currentStatus = ScheduleMapper.getCurrentGroupedStatus(
                    groupedSchedules,
                    timeProvider.now()
                )

                if (currentStatus != null) {
                    Log.d(TAG, "Current status: ${currentStatus.status}")
                    latestStatusEndMs = currentStatus.endMs
                    latestAddressId = address.id
                    latestGroupedSchedules = groupedSchedules

                    // Auto-select best notification style based on device capabilities
                    val style = notificationFactory.getBestSupportedStyle()
                    Log.i(TAG, "📱 Selected notification style: $style (API ${Build.VERSION.SDK_INT})")

                    // Create notification
                    val notification = smartNotificationManager.createStatusNotification(
                        currentStatus = currentStatus,
                        allSchedules = groupedSchedules,
                        address = address.name,
                        style = style
                    )

                    // Update through coordinator for atomic UI updates
                    smartNotificationManager.updateStatusNotification(notification)

                    Log.i(TAG, "✓ Notification updated successfully (style=$style)")
                } else {
                    Log.w(TAG, "Could not determine current status")
                    latestStatusEndMs = null
                    clearCachedStatus()
                }
            }

            result.onFailure { error ->
                Log.e(TAG, "Error fetching schedule: ${error.message}", error)
                latestStatusEndMs = null
                clearCachedStatus()
                showErrorNotification(address.name)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification", e)
            latestStatusEndMs = null
            clearCachedStatus()
        }
    }

    /**
     * Show placeholder notification when network timeout occurs.
     */
    private fun showTimeoutNotification(addressName: String) {
        try {
            latestStatusEndMs = null
            val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_STATUS_ID)
                .setSmallIcon(R.drawable.ic_bolt)
                .setContentTitle(getString(R.string.notif_title_with_address, addressName))
                .setContentText(getString(R.string.notif_network_unavailable))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(getPendingIntentToApp())
                .build()

            smartNotificationManager.updateStatusNotification(notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing timeout notification", e)
        }
    }

    /**
     * Show placeholder notification when no addresses are saved.
     */
    private fun showNoAddressNotification() {
        try {
            latestStatusEndMs = null
            val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_STATUS_ID)
                .setSmallIcon(R.drawable.ic_bolt)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notif_add_address))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(getPendingIntentToApp())
                .build()

            smartNotificationManager.updateStatusNotification(notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing no address notification", e)
        }
    }

    /**
     * Show placeholder notification when error occurs.
     */
    private fun showErrorNotification(addressName: String) {
        try {
            latestStatusEndMs = null
            val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_STATUS_ID)
                .setSmallIcon(R.drawable.ic_bolt)
                .setContentTitle(getString(R.string.notif_title_with_address, addressName))
                .setContentText(getString(R.string.notif_loading_error))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(getPendingIntentToApp())
                .build()

            smartNotificationManager.updateStatusNotification(notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing error notification", e)
        }
    }

    private fun canUseCachedStatus(address: com.occaecat.ztoeschedule.data.model.SavedAddress): Boolean {
        return latestAddressId == address.id && latestGroupedSchedules.isNotEmpty()
    }

    private suspend fun updateNotificationFromCache(address: com.occaecat.ztoeschedule.data.model.SavedAddress) {
        if (!canUseCachedStatus(address)) {
            updateNotificationForAddress(address)
            return
        }

        val currentStatus = ScheduleMapper.getCurrentGroupedStatus(
            latestGroupedSchedules,
            timeProvider.now()
        )
        if (currentStatus == null) {
            clearCachedStatus()
            updateNotificationForAddress(address)
            return
        }

        latestStatusEndMs = currentStatus.endMs
        val style = notificationFactory.getBestSupportedStyle()
        val notification = smartNotificationManager.createStatusNotification(
            currentStatus = currentStatus,
            allSchedules = latestGroupedSchedules,
            address = address.name,
            style = style
        )
        smartNotificationManager.updateStatusNotification(notification)
    }

    private fun clearCachedStatus() {
        latestStatusEndMs = null
        latestAddressId = null
        latestGroupedSchedules = emptyList()
    }

    /**
     * Build initial notification for startForeground() with PROMOTED style.
     * Uses best available notification style immediately.
     */
    private fun buildInitialNotification(): Notification {
        Log.i(TAG, ">>> buildInitialNotification")
        
        // Get best style immediately
        val style = notificationFactory.getBestSupportedStyle()
        Log.i(TAG, "📱 Initial notification style: $style (API ${Build.VERSION.SDK_INT})")
        
        // Use LIVE_UPDATE channel for PROMOTED style (API 36+)
        val channelId = if (style == StatusNotificationStyle.PROMOTED && Build.VERSION.SDK_INT >= PROMOTED_MIN_SDK) {
            NotificationHelper.CHANNEL_LIVE_UPDATE_ID
        } else {
            NotificationHelper.CHANNEL_STATUS_ID
        }
        Log.i(TAG, "Using channel: $channelId")
        
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_loading_data))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Raised for visibility
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(getPendingIntentToApp())
            .setOnlyAlertOnce(true)
        
        // Apply PROMOTED style features if available (API 36+ ONLY)
        if (style == StatusNotificationStyle.PROMOTED && Build.VERSION.SDK_INT >= PROMOTED_MIN_SDK) {
            try {
                builder.setRequestPromotedOngoing(true)
                builder.setShortCriticalText("...")
                builder.setUsesChronometer(false)
                Log.i(TAG, "✓ Applied PROMOTED style to initial notification (API ${Build.VERSION.SDK_INT})")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to apply PROMOTED style: ${e.message}")
            }
        } else if (style == StatusNotificationStyle.LIVE_ACTIVITY) {
            // For API 31-34, use chronometer
            builder.setUsesChronometer(true)
            builder.setColorized(true)
            builder.setColor(getColor(R.color.widget_power_on))
            Log.i(TAG, "✓ Applied LIVE_ACTIVITY style to initial notification (API ${Build.VERSION.SDK_INT})")
        }
        
        val notification = builder.build()
        Log.i(TAG, "<<< Initial notification built: channel=$channelId, style=$style")
        return notification
    }

    /**
     * Get PendingIntent to launch MainActivity.
     */
    private fun getPendingIntentToApp() = android.app.PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        },
        android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun calculateNextUpdateSchedule(): Pair<Long, Long> {
        val now = timeProvider.now()
        val endMs = latestStatusEndMs
        val remainingMs = endMs?.let { it - now } ?: Long.MAX_VALUE

        val intervalMs = when {
            remainingMs <= CRITICAL_WINDOW_MS -> UPDATE_INTERVAL_SHORT_MS
            remainingMs <= PROMOTION_WINDOW_MS -> UPDATE_INTERVAL_MEDIUM_MS
            else -> UPDATE_INTERVAL_LONG_MS
        }

        val effectiveIntervalMs = if (intervalMs == UPDATE_INTERVAL_SHORT_MS && AppForegroundState.isForeground) {
            UPDATE_INTERVAL_MEDIUM_MS
        } else {
            intervalMs
        }

        val aligned = alignDelay(now, effectiveIntervalMs)
        val capped = if (remainingMs > 0 && remainingMs != Long.MAX_VALUE) {
            minOf(aligned, remainingMs)
        } else {
            aligned
        }
        return Pair(capped.coerceAtLeast(1L), effectiveIntervalMs)
    }

    private fun alignDelay(nowMs: Long, intervalMs: Long): Long {
        val nextTick = ((nowMs / intervalMs) + 1) * intervalMs
        return (nextTick - nowMs).coerceAtLeast(1L)
    }
}
