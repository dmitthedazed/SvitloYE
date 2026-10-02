package com.occaecat.ztoeschedule.domain.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Foreground service that keeps the persistent status notification up to date.
 *
 * It only reads the local cache; fetching and alarms belong to [NotificationController].
 * Redraws once a minute (progress, remaining time), exactly at period boundaries,
 * and whenever [refresh] is called.
 */
@AndroidEntryPoint
class StatusNotificationService : Service() {

    @Inject lateinit var preferences: EnergyPreferencesManager
    @Inject lateinit var controller: NotificationController
    @Inject lateinit var renderer: NotificationRenderer
    @Inject lateinit var notifier: Notifier
    @Inject lateinit var timeProvider: TimeProvider

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        try {
            ServiceCompat.startForeground(
                this,
                NotificationIds.STATUS,
                renderer.placeholder(getString(R.string.app_name), getString(R.string.notif_loading_data)),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Cannot start foreground", e)
            stopSelf()
            return
        }

        scope.launch {
            preferences.statusNotificationEnabledFlow.filter { !it }.first()
            stopSelf()
        }
        scope.launch {
            while (true) {
                val nextRedrawMs = render()
                withTimeoutOrNull(nextRedrawMs) { refreshRequests.receive() }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Posts the current status and returns how long to wait before the next redraw. */
    private suspend fun render(): Long {
        val now = timeProvider.now()
        val address = controller.primaryAddress()
        val timeline = address?.let { controller.cachedTimeline(it) }
        val notification = when {
            address == null -> renderer.placeholder(getString(R.string.app_name), getString(R.string.notif_add_address))
            timeline == null -> renderer.placeholder(address.name, getString(R.string.notif_no_schedule))
            else -> renderer.status(address.name, timeline, now)
        }
        notifier.post(NotificationIds.STATUS, notification)

        val untilNextMinute = MINUTE_MS - now % MINUTE_MS
        val untilBoundary = timeline?.currentRun(now)?.let { it.endMs - now } ?: Long.MAX_VALUE
        return minOf(untilNextMinute, untilBoundary).coerceAtLeast(1_000L)
    }

    companion object {
        private const val TAG = "StatusNotificationService"
        private const val MINUTE_MS = 60_000L

        private val refreshRequests = Channel<Unit>(Channel.CONFLATED)

        /** Redraw as soon as possible (picked up by the running service, if any). */
        fun refresh() {
            refreshRequests.trySend(Unit)
        }

        /** Android may refuse to start a foreground service from the background; that's fine here. */
        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, StatusNotificationService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Status service start refused", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StatusNotificationService::class.java))
        }
    }
}
