package com.occaecat.ztoeschedule.domain.notification

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
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
    private const val TAG = "NotificationScheduler"

    /**
     * Schedule periodic power monitoring task.
     *
     * DEPRECATED: Now using AlarmManager for exact scheduled notifications.
     * This method is kept for backward compatibility but does nothing.
     * 
     * @param context Application context
     */
    fun schedulePowerMonitoring(context: Context) {
        Log.d(TAG, "schedulePowerMonitoring() called - SKIPPED (using AlarmManager instead)")
        // DISABLED: Now using ScheduledAlarmManager for exact status change notifications
        // The periodic worker is no longer needed as we schedule alarms at exact times
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
            val workRequest = OneTimeWorkRequestBuilder<PowerMonitorWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueue(workRequest)

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
}

