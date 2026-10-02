package com.occaecat.ztoeschedule.domain.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps AlarmManager in sync with the planned alerts.
 *
 * Alarms live in fixed slots (one request code per slot), so replacing the whole plan is just
 * "overwrite the first N slots, cancel the rest" with no bookkeeping to persist.
 */
@Singleton
class AlarmScheduler @Inject constructor(@param:ApplicationContext private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    @Synchronized
    fun replace(alerts: List<PlannedAlert>, addressName: String) {
        cancelLegacyAlarms()
        val planned = alerts.take(AlertPlanner.MAX_ALERTS)
        planned.forEachIndexed { slot, alert -> schedule(slot, alert, addressName) }
        for (slot in planned.size until AlertPlanner.MAX_ALERTS) cancel(slot)
        Log.i(TAG, "Scheduled ${planned.size} alerts for $addressName (exact=${canScheduleExact()})")
    }

    @Synchronized
    fun cancelAll() {
        for (slot in 0 until AlertPlanner.MAX_ALERTS) cancel(slot)
    }

    private fun schedule(slot: Int, alert: PlannedAlert, addressName: String) {
        val intent = AlertReceiver.intent(context, alert, addressName)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE_BASE + slot, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alert.atMs, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alert.atMs, pending)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm denied, falling back to inexact", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alert.atMs, pending)
        }
    }

    private fun cancel(slot: Int) {
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE_BASE + slot,
            Intent(context, AlertReceiver::class.java).setAction(AlertReceiver.ACTION_ALERT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        ) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }

    /** One-time cleanup of alarms set by the old StatusChangeAlarmReceiver-based system. */
    private fun cancelLegacyAlarms() {
        val prefs = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        if (prefs.all.isEmpty()) return
        val legacyIntent = Intent().setClassName(context, "$LEGACY_PACKAGE.StatusChangeAlarmReceiver")
        prefs.all.values.filterIsInstance<String>()
            .flatMap { it.split(',') }
            .mapNotNull { it.toIntOrNull() }
            .forEach { code ->
                PendingIntent.getBroadcast(context, code, legacyIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
                    ?.let { alarmManager.cancel(it); it.cancel() }
            }
        prefs.edit().clear().apply()
    }

    private companion object {
        const val TAG = "AlarmScheduler"
        const val REQUEST_CODE_BASE = 20_000
        const val LEGACY_PREFS = "scheduled_alarms"
        const val LEGACY_PACKAGE = "com.occaecat.ztoeschedule.domain.notification"
    }
}
