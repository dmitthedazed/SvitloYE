package com.occaecat.ztoeschedule.domain.notification

import android.content.Context
import android.util.Log
import androidx.work.*
import com.occaecat.ztoeschedule.BuildConfig
import java.util.concurrent.TimeUnit

/**
 * Manages scheduling of power monitoring notifications using WorkManager
 */
object NotificationScheduler {

    private const val WORK_NAME = "power_monitor_work"
    private const val TAG = "NotificationScheduler"

    /**
     * Schedule periodic power monitoring
     * Runs every 15 minutes to check for upcoming power changes
     */
    fun schedulePowerMonitoring(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<PowerMonitorWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // Flex interval
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
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
                workRequest
            )

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Power monitoring scheduled")
        }
    }

    /**
     * Cancel power monitoring
     */
    fun cancelPowerMonitoring(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(WORK_NAME)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Power monitoring cancelled")
        }
    }

    /**
     * Check if power monitoring is scheduled
     */
    fun isPowerMonitoringScheduled(context: Context): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WORK_NAME)
            .get()

        return workInfos.any {
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }
    }

    /**
     * Force immediate check (for testing)
     */
    fun runImmediateCheck(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<PowerMonitorWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context)
            .enqueue(workRequest)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Immediate power check triggered")
        }
    }
}

