package com.occaecat.ztoeschedule.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import com.occaecat.ztoeschedule.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** Fires a planned alert when its alarm goes off, re-checking the rules at that moment. */
@AndroidEntryPoint
class AlertReceiver : BroadcastReceiver() {

    @Inject lateinit var preferences: EnergyPreferencesManager
    @Inject lateinit var renderer: NotificationRenderer
    @Inject lateinit var notifier: Notifier
    @Inject lateinit var timeProvider: TimeProvider
    @Inject lateinit var controller: NotificationController
    @Inject lateinit var widgetUpdater: WidgetUpdater

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALERT) return
        val alert = intent.toAlert() ?: return
        val addressName = intent.getStringExtra(EXTRA_ADDRESS).orEmpty()

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = preferences.notificationSettingsFlow.first()
                val now = timeProvider.now()
                // The alarm was planned at the last sync; the schedule may have changed since
                if (!controller.isStillPlanned(alert)) {
                    NotificationSync.syncNow(context, fetch = false)
                    return@launch
                }
                // Quiet hours follow the user's own clock: they sleep in local time
                val local = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
                val delivery = AlertRules.delivery(
                    settings, alert.kind, alert.isOutage, alert.atMs, now,
                    minuteOfDay = local.hour * 60 + local.minute
                )
                if (delivery != Delivery.Drop) {
                    if (alert.kind == AlertKind.Change) notifier.cancel(NotificationIds.WARNING)
                    notifier.post(
                        if (alert.kind == AlertKind.Warning) NotificationIds.WARNING else NotificationIds.CHANGE,
                        renderer.alert(alert, addressName, silent = delivery == Delivery.Silent, nowMs = now)
                    )
                }
                if (alert.kind == AlertKind.Change) {
                    StatusNotificationService.refresh()
                    if (settings.statusNotification) StatusNotificationService.start(context)
                    requestTileUpdate(context)
                    widgetUpdater.updateAll()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_ALERT = "com.occaecat.ztoeschedule.ACTION_POWER_ALERT"
        private const val EXTRA_KIND = "kind"
        private const val EXTRA_AT = "at"
        private const val EXTRA_EVENT = "event"
        private const val EXTRA_FROM = "from"
        private const val EXTRA_TO = "to"
        private const val EXTRA_UNTIL = "until"
        private const val EXTRA_ADDRESS = "address"

        fun intent(context: Context, alert: PlannedAlert, addressName: String): Intent =
            Intent(context, AlertReceiver::class.java)
                .setAction(ACTION_ALERT)
                .putExtra(EXTRA_KIND, alert.kind.name)
                .putExtra(EXTRA_AT, alert.atMs)
                .putExtra(EXTRA_EVENT, alert.eventMs)
                .putExtra(EXTRA_FROM, alert.from.name)
                .putExtra(EXTRA_TO, alert.to.name)
                .putExtra(EXTRA_UNTIL, alert.untilMs ?: -1L)
                .putExtra(EXTRA_ADDRESS, addressName)

        private fun Intent.toAlert(): PlannedAlert? = runCatching {
            PlannedAlert(
                kind = AlertKind.valueOf(getStringExtra(EXTRA_KIND)!!),
                atMs = getLongExtra(EXTRA_AT, 0L),
                eventMs = getLongExtra(EXTRA_EVENT, 0L),
                from = PowerState.valueOf(getStringExtra(EXTRA_FROM)!!),
                to = PowerState.valueOf(getStringExtra(EXTRA_TO)!!),
                untilMs = getLongExtra(EXTRA_UNTIL, -1L).takeIf { it > 0 }
            )
        }.getOrNull()
    }
}
