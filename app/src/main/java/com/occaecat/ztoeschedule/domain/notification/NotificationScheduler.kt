package com.occaecat.ztoeschedule.domain.notification

import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.service.quicksettings.TileService
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

/**
 * Manages scheduling of power monitoring notifications using WorkManager.
 *
 * Handles:
 * - Periodic checks every 15 minutes with exponential backoff
 * - Immediate on-demand checks triggered by user or system
 * - Graceful cancellation
 * - Network connectivity constraint
 *
 * All operations are thread-safe and handle errors gracefully.
 */
object NotificationScheduler {

    private const val WORK_NAME = "power_monitor_work"
    private const val IMMEDIATE_WORK_NAME = "power_monitor_immediate"
    private const val IMMEDIATE_DEBOUNCE_MS = 2_000L
    private const val TAG = "NotificationScheduler"
    @Volatile
    private var lastImmediateCheckMs: Long = 0L

    /**
     * Schedule periodic power monitoring task.
     *
     * Runs every 6 hours to:
     * 1. Refresh schedule data from API
     * 2. Reschedule alarms for next 24 hours
     * 3. Update status notification
     * 
     * This ensures alarms are always scheduled even if user doesn't open the app.
     * 
     * @param context Application context
     */
    fun schedulePowerMonitoring(context: Context) {
        Log.d(TAG, "schedulePowerMonitoring() called")

        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            // Run every 6 hours with 1 hour flex window
            val workRequest = PeriodicWorkRequestBuilder<PowerMonitorWorker>(
                6, TimeUnit.HOURS,
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )

            Log.i(TAG, "✓ Periodic power monitoring scheduled (every 6 hours)")

        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling power monitoring", e)
        }
    }

    /**
     * Cancel periodic power monitoring.
     *
     * @param context Application context
     */
    fun cancelPowerMonitoring(context: Context) {
        Log.d(TAG, "cancelPowerMonitoring() called")

        try {
            WorkManager.getInstance(context)
                .cancelUniqueWork(WORK_NAME)

            Log.i(TAG, "Power monitoring cancelled")

        } catch (e: Exception) {
            Log.e(TAG, "Error canceling power monitoring", e)
        }
    }

    /**
     * Check if power monitoring is currently scheduled.
     *
     * @param context Application context
     * @return true if periodic work is enqueued or running
     */
    fun isPowerMonitoringScheduled(context: Context): Boolean {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WORK_NAME)
                .get()

            val isScheduled = workInfos.any { workInfo ->
                workInfo.state == WorkInfo.State.ENQUEUED ||
                workInfo.state == WorkInfo.State.RUNNING
            }

            Log.d(TAG, "isPowerMonitoringScheduled() = $isScheduled")
            isScheduled

        } catch (e: Exception) {
            Log.e(TAG, "Error checking if power monitoring scheduled", e)
            false
        }
    }

    /**
     * Trigger immediate power check on-demand.
     *
     * Used when:
     * - User taps refresh button
     * - Address selection changes
     * - Manual refresh from settings
     *
     * @param context Application context
     */
    fun runImmediateCheck(context: Context) {
        Log.d(TAG, "runImmediateCheck() called")

        try {
            val now = System.currentTimeMillis()
            if (now - lastImmediateCheckMs < IMMEDIATE_DEBOUNCE_MS) {
                Log.d(TAG, "Immediate check debounced")
                return
            }
            lastImmediateCheckMs = now

            val workRequest = OneTimeWorkRequestBuilder<PowerMonitorWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    IMMEDIATE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

            Log.i(TAG, "Immediate power check enqueued")

            // Try to update tile as well
            updateTile(context)

        } catch (e: Exception) {
            Log.e(TAG, "Error triggering immediate check", e)
        }
    }

    /**
     * Request Quick Settings tile to refresh its display.
     *
     * Called after status changes to immediately update tile.
     */
    private fun updateTile(context: Context) {
        try {
            TileService.requestListeningState(
                context,
                ComponentName(context, PowerStatusTileService::class.java)
            )
            Log.d(TAG, "QS tile update requested")
        } catch (e: Exception) {
            Log.w(TAG, "Error updating QS tile", e)
        }
    }

    private fun hasInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

