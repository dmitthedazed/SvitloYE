package com.occaecat.ztoeschedule.widget

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import com.occaecat.ztoeschedule.widget.glance.LightWidget
import com.occaecat.ztoeschedule.widget.glance.LightWidgetReceiver
import com.occaecat.ztoeschedule.widget.glance.PowerStatusWidget
import com.occaecat.ztoeschedule.widget.glance.PowerStatusWidgetReceiver
import com.occaecat.ztoeschedule.widget.glance.ScheduleWidget
import com.occaecat.ztoeschedule.widget.glance.DetailedScheduleWidgetReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * Redraws every widget from the cache and arranges the next redraw for the moment power changes,
 * so "until 17:00" never outlives 17:00.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val loader: WidgetStateLoader,
    private val timeProvider: TimeProvider
) {
    suspend fun updateAll() {
        if (!hasWidgets()) {
            WorkManager.getInstance(context).cancelUniqueWork(REFRESH_WORK)
            return
        }
        widgets.forEach { runCatching { it.updateAll(context) }.onFailure { e -> Log.w(TAG, "Widget update failed", e) } }
        scheduleNextRefresh((loader.load().state as? WidgetState.Ready)?.nextChangeAt)
    }

    private suspend fun hasWidgets(): Boolean {
        val manager = GlanceAppWidgetManager(context)
        return widgets.any { manager.getGlanceIds(it.javaClass).isNotEmpty() }
    }

    private fun scheduleNextRefresh(atMs: Long?) {
        val workManager = WorkManager.getInstance(context)
        if (atMs == null) {
            // No schedule yet: still look again later rather than freezing until the next sync
            scheduleNextRefresh(timeProvider.now() + NO_DATA_RETRY_MS)
            return
        }
        // A few seconds late so the schedule has really flipped when we read it
        val delay = (atMs - timeProvider.now()).coerceAtLeast(0) + 5_000L
        workManager.enqueueUniqueWork(
            REFRESH_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
        )
    }

    private companion object {
        const val TAG = "WidgetUpdater"
        const val REFRESH_WORK = "widget_refresh_at_change"
        const val NO_DATA_RETRY_MS = 60 * 60_000L
        val widgets: List<GlanceAppWidget> = listOf(LightWidget(), PowerStatusWidget(), ScheduleWidget())
    }
}

@HiltWorker
class WidgetRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val updater: WidgetUpdater
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        updater.updateAll()
        return Result.success()
    }
}

/**
 * Pushes generated previews to the widget picker (Android 15+). The system rate-limits this to
 * roughly two calls an hour, so previews are re-published only when [PREVIEW_VERSION] changes.
 * Older Android versions use the static `previewLayout` / `previewImage` from the provider XML.
 */
object WidgetPreviews {
    /** Bump whenever widget visuals or the sample data change. */
    private const val PREVIEW_VERSION = 3
    private const val PREFS = "widget_previews"

    private val receivers: List<KClass<out GlanceAppWidgetReceiver>> = listOf(
        LightWidgetReceiver::class,
        PowerStatusWidgetReceiver::class,
        DetailedScheduleWidgetReceiver::class
    )

    suspend fun publishIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val manager = GlanceAppWidgetManager(context)
        for (receiver in receivers) {
            val key = "version_${receiver.simpleName}"
            if (prefs.getInt(key, 0) == PREVIEW_VERSION) continue
            val result = runCatching { manager.setWidgetPreviews(receiver) }
                .onFailure { Log.w("WidgetPreviews", "Failed to publish ${receiver.simpleName}", it) }
                .getOrNull()
            // Rate-limited or failed: stop and try again on a later launch
            if (result != GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) return
            prefs.edit().putInt(key, PREVIEW_VERSION).apply()
        }
    }
}
