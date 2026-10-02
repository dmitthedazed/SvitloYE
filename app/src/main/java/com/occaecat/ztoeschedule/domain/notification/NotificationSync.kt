package com.occaecat.ztoeschedule.domain.notification

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OutOfQuotaPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.occaecat.ztoeschedule.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** WorkManager plumbing around [NotificationController.sync]. */
object NotificationSync {
    private const val PERIODIC_WORK = "notification_sync_periodic"
    private const val ONE_TIME_WORK = "notification_sync_now"
    private val legacyWork = listOf("power_monitor_work", "power_monitor_immediate")

    /** Hourly fetch: keeps alarms fresh and catches schedule updates while the app is closed. */
    fun schedulePeriodic(context: Context) {
        val workManager = WorkManager.getInstance(context)
        legacyWork.forEach(workManager::cancelUniqueWork)
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<NotificationSyncWorker>(1, TimeUnit.HOURS, 15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        )
    }

    /** Stops all background syncing (after the user wiped their data). */
    fun cancelAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(PERIODIC_WORK)
        workManager.cancelUniqueWork(ONE_TIME_WORK)
    }

    /**
     * Re-sync soon. Use `fetch = false` after settings or address changes: the cached schedule
     * is enough to replan, and it works offline.
     *
     * @param urgent the user explicitly asked for it (a refresh button): run expedited so the
     * result shows up right away instead of whenever background limits allow.
     */
    fun syncNow(context: Context, fetch: Boolean = true, urgent: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<NotificationSyncWorker>()
            .setInputData(workDataOf(NotificationSyncWorker.KEY_FETCH to fetch))
        if (urgent) request.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_TIME_WORK, ExistingWorkPolicy.REPLACE, request.build())
    }
}

@HiltWorker
class NotificationSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val controller: NotificationController,
    private val renderer: NotificationRenderer
) : CoroutineWorker(context, params) {

    /** Only used for expedited runs on Android 11 and older, which run as a foreground service. */
    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        NotificationIds.SYNC,
        renderer.placeholder(
            applicationContext.getString(R.string.app_name),
            applicationContext.getString(R.string.notif_loading_data)
        )
    )

    override suspend fun doWork(): Result {
        controller.sync(fetch = inputData.getBoolean(KEY_FETCH, true))
        return Result.success()
    }

    companion object {
        const val KEY_FETCH = "fetch"
    }
}

fun requestTileUpdate(context: Context) {
    runCatching {
        TileService.requestListeningState(context, ComponentName(context, PowerStatusTileService::class.java))
    }
}
