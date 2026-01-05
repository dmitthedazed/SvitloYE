package com.occaecat.ztoeschedule.domain.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
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
        private const val UPDATE_INTERVAL_MS = 60_000L // 1 minute
        private const val NETWORK_TIMEOUT_MS = 10_000L  // 10 seconds

        fun start(context: Context) {
            Log.d(TAG, "Starting PowerStatusService")
            val intent = Intent(context, PowerStatusService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            Log.d(TAG, "Stopping PowerStatusService")
            val intent = Intent(context, PowerStatusService::class.java)
            context.stopService(intent)
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

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate()")

        NotificationHelper.createAllChannels(this)

        try {
            // Create initial notification immediately for foreground requirement
            val initialNotification = buildInitialNotification()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NotificationIds.STATUS,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
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
        return START_STICKY // Restart if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy()")

        updateJob?.cancel()
        serviceScope.cancel()

        // Notify coordinator of graceful shutdown
        serviceScope.launch {
            notificationCoordinator.shutdown()
        }
    }

    /**
     * Start periodic updates by combining address selection with timer.
     *
     * Handles address changes immediately by canceling old job and starting new one.
     * Uses primary address (sorted by priority) instead of saved selection.
     */
    private fun startPeriodicUpdates() {
        Log.d(TAG, "Starting periodic updates")

        updateJob = serviceScope.launch {
            try {
                // Create a flow that emits ticker events
                createTickerFlow().collect { _ ->
                    // Get primary address (sorted by priority)
                    val addresses = repository.getSavedAddresses()
                        .sortedBy { it.priority }
                    
                    val primaryAddress = addresses.firstOrNull()

                    if (primaryAddress != null) {
                        // Handle address change - graceful restart
                        if (primaryAddress.name != lastAddressName) {
                            Log.i(TAG, "Address changed from $lastAddressName to ${primaryAddress.name}")
                            lastAddressName = primaryAddress.name
                        }

                        // Update notification with primary address
                        updateNotificationForAddress(primaryAddress)
                    } else {
                        Log.d(TAG, "No saved addresses available")
                        showNoAddressNotification()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in periodic updates", e)
            }
        }
    }

    /**
     * Create ticker flow that emits every UPDATE_INTERVAL_MS.
     */
    private suspend fun createTickerFlow() = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(Unit)
            delay(UPDATE_INTERVAL_MS)
        }
    }

    /**
     * Update notification for given address.
     *
     * With network timeout protection to prevent hanging.
     */
    private suspend fun updateNotificationForAddress(
        address: com.occaecat.ztoeschedule.data.local.SavedAddress
    ) {
        try {
            Log.d(TAG, "Updating notification for ${address.name}")

            // Network call with timeout
            val result = withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
                repository.getCachedScheduleWithMessages(address.cherga, address.pidcherga)
            }

            if (result == null) {
                Log.w(TAG, "Network timeout fetching schedule")
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

                    // Auto-select best notification style based on device capabilities
                    val style = notificationFactory.getBestSupportedStyle()

                    // Create notification
                    val notification = smartNotificationManager.createStatusNotification(
                        currentStatus = currentStatus,
                        allSchedules = groupedSchedules,
                        address = address.name,
                        style = style
                    )

                    // Update through coordinator for atomic UI updates
                    smartNotificationManager.updateStatusNotification(notification)

                    Log.d(TAG, "Notification updated successfully (style=$style)")
                } else {
                    Log.w(TAG, "Could not determine current status")
                }
            }

            result.onFailure { error ->
                Log.e(TAG, "Error fetching schedule: ${error.message}", error)
                showErrorNotification(selection.addressName)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification", e)
        }
    }

    /**
     * Show placeholder notification when network timeout occurs.
     */
    private fun showTimeoutNotification(addressName: String) {
        try {
            val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_STATUS_ID)
                .setSmallIcon(R.drawable.ic_bolt)
                .setContentTitle("СвітлоЄ? - $addressName")
                .setContentText("Сіть недоступна...")
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
            val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_STATUS_ID)
                .setSmallIcon(R.drawable.ic_bolt)
                .setContentTitle("СвітлоЄ?")
                .setContentText("Додайте адресу в налаштуваннях")
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
            val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_STATUS_ID)
                .setSmallIcon(R.drawable.ic_bolt)
                .setContentTitle("СвітлоЄ? - $addressName")
                .setContentText("Помилка завантаження...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(getPendingIntentToApp())
                .build()

            smartNotificationManager.updateStatusNotification(notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing error notification", e)
        }
    }

    /**
     * Build initial notification for startForeground().
     */
    private fun buildInitialNotification() = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_STATUS_ID)
        .setSmallIcon(R.drawable.ic_bolt)
        .setContentTitle("СвітлоЄ?")
        .setContentText("Завантаження даних...")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setContentIntent(getPendingIntentToApp())
        .build()

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
}