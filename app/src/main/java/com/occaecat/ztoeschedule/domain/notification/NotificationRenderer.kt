package com.occaecat.ztoeschedule.domain.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/** The only place that builds notifications. Does not post them (see [Notifier]). */
@Singleton
class NotificationRenderer @Inject constructor(@param:ApplicationContext private val context: Context) {

    // ---------- Alerts ----------

    fun alert(alert: PlannedAlert, addressName: String, silent: Boolean, nowMs: Long): Notification {
        val title = when (alert.kind) {
            AlertKind.Warning -> {
                // From the actual fire time: a late alarm must not promise "in 15 min" when 5 are left
                val minutes = ((alert.eventMs - nowMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
                if (alert.to == PowerState.Maybe) context.getString(R.string.notif_warning_maybe_title, minutes)
                else context.getString(R.string.notif_warning_off_title, minutes)
            }
            AlertKind.Change -> when (alert.to) {
                PowerState.Off -> context.getString(R.string.notif_change_off_title)
                PowerState.On -> context.getString(R.string.notif_change_on_title)
                PowerState.Maybe ->
                    if (alert.isOutage) context.getString(R.string.notif_change_maybe_title)
                    else context.getString(R.string.notif_change_maybe_on_title)
            }
        }
        val until = alert.untilMs?.let(::time)
        val text = when {
            alert.kind == AlertKind.Warning && until != null ->
                context.getString(R.string.notif_alert_at_until, addressName, time(alert.eventMs), until)
            alert.kind == AlertKind.Warning -> context.getString(R.string.notif_alert_at, addressName, time(alert.eventMs))
            until != null -> context.getString(R.string.notif_alert_until, addressName, until)
            else -> addressName
        }

        return NotificationCompat.Builder(context, NotificationChannels.ALERTS)
            .setSmallIcon(R.drawable.ic_bolt)
            .setColor(colorFor(alert.to))
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(if (alert.kind == AlertKind.Warning) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(alert.eventMs)
            .setShowWhen(true)
            .setSilent(silent)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            // A warning is useless once the outage starts; a change alert once the next one comes
            .setTimeoutAfter(
                if (alert.kind == AlertKind.Warning) (alert.eventMs - nowMs).coerceAtLeast(60_000L)
                else alert.untilMs?.let { (it - nowMs).coerceAtLeast(60_000L) } ?: 0L
            )
            .build()
    }

    fun scheduleChange(change: ScheduleChange, addressName: String, silent: Boolean, zone: ZoneId): Notification {
        val today = Instant.now().atZone(zone).toLocalDate()
        val dateFormat = DateTimeFormatter.ofPattern("dd.MM")
        val daysLabel = change.days.joinToString(", ") { day ->
            when (day) {
                today -> context.getString(R.string.notif_schedule_today)
                today.plusDays(1) -> context.getString(R.string.notif_schedule_tomorrow)
                else -> day.format(dateFormat)
            }
        }
        val multiDay = change.days.size > 1
        val outagesText = if (change.outages.isEmpty()) {
            context.getString(R.string.notif_schedule_no_outages)
        } else {
            context.getString(R.string.notif_schedule_outages, change.outages.joinToString(", ") { run ->
                val prefix = if (multiDay) Instant.ofEpochMilli(run.startMs).atZone(zone).toLocalDate().format(dateFormat) + " " else ""
                val mark = if (run.state == PowerState.Maybe) "?" else ""
                "$prefix${time(run.startMs)}–${time(run.endMs)}$mark"
            })
        }
        val text = "$outagesText\n${context.getString(R.string.notif_message_address, addressName)}"

        return NotificationCompat.Builder(context, NotificationChannels.SCHEDULE)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle(context.getString(R.string.notif_schedule_title, daysLabel))
            .setContentText(outagesText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setSilent(silent)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
    }

    // ---------- Persistent status ----------

    fun status(addressName: String, timeline: PowerTimeline, nowMs: Long): Notification {
        val current = timeline.currentRun(nowMs)
            ?: return placeholder(addressName, context.getString(R.string.notif_no_schedule))
        val next = timeline.nextRun(current)

        if (current.state == PowerState.On && next == null) {
            return statusBuilder(NotificationChannels.STATUS)
                .setColor(colorFor(PowerState.On))
                .setContentTitle(context.getString(R.string.notif_status_light_stays_on))
                .setContentText(context.getString(R.string.notif_message_address, addressName))
                .setShowWhen(false)
                .build()
        }

        val (icon, label) = when (current.state) {
            PowerState.On -> "🟢" to context.getString(R.string.notif_status_power_on)
            PowerState.Off -> "🔴" to context.getString(R.string.notif_status_outage)
            PowerState.Maybe -> "🟡" to context.getString(R.string.notif_status_probable)
            null -> "⚪" to context.getString(R.string.notif_status_unknown)
        }
        val endKnown = next != null
        val title = if (endKnown) context.getString(R.string.notif_title_format, icon, label, time(current.endMs)) else "$icon $label"
        val text = if (endKnown) context.getString(R.string.notif_message_format, remaining(current.endMs - nowMs), addressName)
        else context.getString(R.string.notif_message_address, addressName)
        val nextText = next?.state?.let { state ->
            when (state) {
                PowerState.On -> context.getString(R.string.notif_next_on, time(next.startMs))
                PowerState.Off -> context.getString(R.string.notif_next_off, time(next.startMs))
                PowerState.Maybe -> context.getString(R.string.notif_next_maybe, time(next.startMs))
            }
        }

        val promoted = canPostPromoted()
        val builder = statusBuilder(if (promoted) NotificationChannels.LIVE_UPDATE else NotificationChannels.STATUS)
            .setColor(colorFor(current.state))
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(nextText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(listOfNotNull(text, nextText).joinToString("\n")))

        if (endKnown) {
            builder.setWhen(current.endMs).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
            val totalMin = ((current.endMs - current.startMs) / 60_000L).toInt().coerceAtLeast(1)
            val elapsedMin = ((nowMs - current.startMs) / 60_000L).toInt().coerceIn(0, totalMin)
            if (promoted) {
                builder.setStyle(
                    NotificationCompat.ProgressStyle()
                        .setProgressSegments(listOf(NotificationCompat.ProgressStyle.Segment(totalMin).setColor(colorFor(current.state))))
                        .setProgress(elapsedMin)
                )
                builder.setShortCriticalText(shortRemaining(current.endMs - nowMs))
            } else {
                builder.setProgress(totalMin, elapsedMin, false)
            }
        } else {
            builder.setShowWhen(false)
        }
        if (promoted) builder.setRequestPromotedOngoing(true)
        return builder.build()
    }

    fun placeholder(title: String, text: String): Notification =
        statusBuilder(NotificationChannels.STATUS)
            .setContentTitle(title)
            .setContentText(text)
            .setShowWhen(false)
            .build()

    fun canPostPromoted(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
            context.getSystemService(NotificationManager::class.java).canPostPromotedNotifications()

    private fun statusBuilder(channel: String) = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_bolt)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setContentIntent(openAppIntent())
        .setDeleteIntent(actionIntent(NotificationActionReceiver.ACTION_DISMISSED, 2))
        .addAction(R.drawable.ic_bolt, context.getString(R.string.notif_action_refresh), actionIntent(NotificationActionReceiver.ACTION_REFRESH, 1))

    // ---------- Helpers ----------

    private fun colorFor(state: PowerState?): Int = context.getColor(
        when (state) {
            PowerState.On -> R.color.widget_power_on
            PowerState.Off -> R.color.widget_power_off
            PowerState.Maybe, null -> R.color.widget_power_probable
        }
    )

    private fun time(ms: Long): String = DateFormat.getTimeFormat(context).format(Date(ms))

    private fun remaining(ms: Long): String {
        val minutes = (ms.coerceAtLeast(0) + 59_999) / 60_000
        return if (minutes >= 60) context.getString(R.string.notif_remaining_hm, (minutes / 60).toInt(), (minutes % 60).toInt())
        else context.getString(R.string.notif_remaining_m, minutes.toInt())
    }

    /** Status bar chip text; Android shows at most ~7 characters. */
    private fun shortRemaining(ms: Long): String {
        val minutes = (ms.coerceAtLeast(0) + 59_999) / 60_000
        return if (minutes >= 60) "%d:%02d".format(minutes / 60, minutes % 60) else "${minutes}хв"
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun actionIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context, requestCode,
        Intent(context, NotificationActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
