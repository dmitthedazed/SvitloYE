package com.occaecat.ztoeschedule.domain.notification

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

object NotificationHelper {
    fun startNotificationService(context: Context) {
        val intent = Intent(context, StatusNotificationService::class.java)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            // If we can't start foreground service, use WorkManager instead
            scheduleNotificationWork(context)
        }
    }

    private fun scheduleNotificationWork(context: Context) {
        // Implement WorkManager periodic task as fallback
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<NotificationWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        ).build()

        androidx.work.WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "status_notification",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
    }
}

