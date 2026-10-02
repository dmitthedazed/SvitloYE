package com.occaecat.ztoeschedule.domain.notification

import android.content.Context
import android.util.Log
import com.occaecat.ztoeschedule.AppForegroundState
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.model.SavedAddress
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.debug.MockScheduleProvider
import com.occaecat.ztoeschedule.domain.time.ScheduleZone
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import com.occaecat.ztoeschedule.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single entry point that brings notifications in line with the current schedule and settings:
 * reschedules alert alarms, reports schedule updates and nudges the status notification.
 *
 * Notifications follow the primary address (lowest priority value).
 */
@Singleton
class NotificationController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: EnergyRepository,
    private val preferences: EnergyPreferencesManager,
    private val alarmScheduler: AlarmScheduler,
    private val renderer: NotificationRenderer,
    private val notifier: Notifier,
    private val timeProvider: TimeProvider,
    private val widgetUpdater: WidgetUpdater
) {
    private val mutex = Mutex()

    suspend fun primaryAddress(): SavedAddress? = repository.getSavedAddresses().minByOrNull { it.priority }

    /**
     * Whether [alert] still matches the cached schedule of the primary address.
     * Demo schedules are generated on the fly, so they are always trusted.
     */
    suspend fun isStillPlanned(alert: PlannedAlert): Boolean {
        val address = primaryAddress() ?: return false
        if (isMock(address)) return true
        val timeline = cachedTimeline(address) ?: return false
        return timeline.transitions().any { (before, after) ->
            before.state == alert.from && after.state == alert.to &&
                kotlin.math.abs(after.startMs - alert.eventMs) <= 60_000L
        }
    }

    private fun isMock(address: SavedAddress) =
        MockScheduleProvider.isDemoLocation(address.cherga, address.pidcherga) ||
            MockScheduleProvider.isPreviewLocation(address.cherga, address.pidcherga)

    /** Loads the primary address timeline from the local cache. */
    suspend fun cachedTimeline(address: SavedAddress): PowerTimeline? =
        repository.getCachedScheduleWithMessages(address.cherga, address.pidcherga).getOrNull()
            ?.let { PowerTimeline.from(ScheduleMapper.getGroupedSchedule(it.schedules)) }

    /**
     * @param fetch try the network first (falls back to cache when offline).
     * @return false if there was no schedule to work with.
     */
    suspend fun sync(fetch: Boolean): Boolean = mutex.withLock {
        syncLocked(fetch).also { widgetUpdater.updateAll() }
    }

    private suspend fun syncLocked(fetch: Boolean): Boolean {
        val settings = preferences.notificationSettingsFlow.first()
        val address = primaryAddress()
        if (address == null) {
            alarmScheduler.cancelAll()
            StatusNotificationService.refresh()
            return false
        }

        val data = (if (fetch) repository.getScheduleWithMessages(address.cherga, address.pidcherga).getOrNull() else null)
            ?: repository.getCachedScheduleWithMessages(address.cherga, address.pidcherga).getOrNull()
        if (data == null) {
            Log.w(TAG, "No schedule for ${address.name}, keeping existing alarms")
            return false
        }

        val timeline = PowerTimeline.from(ScheduleMapper.getGroupedSchedule(data.schedules))
        val now = timeProvider.now()
        if (timeline.runs.isEmpty()) {
            // Nothing to plan from; an empty answer must not wipe alarms that may still be right
            Log.w(TAG, "Empty schedule for ${address.name}, keeping existing alarms")
        } else {
            alarmScheduler.replace(AlertPlanner.plan(timeline, now, settings), address.name)
            if (!isMock(address)) reportScheduleChange(address, timeline, now)
        }

        StatusNotificationService.refresh()
        requestTileUpdate(context)
        return true
    }

    private suspend fun reportScheduleChange(address: SavedAddress, timeline: PowerTimeline, now: Long) {
        // Schedule days are Kyiv days
        val zone = ScheduleZone.zoneId
        val key = "${address.cherga}/${address.pidcherga}"
        val snapshot = ScheduleChangeDetector.snapshot(key, timeline)
        // One snapshot per queue, so switching the primary address back and forth keeps its baseline
        val stored = ScheduleChangeDetector.unpackAll(preferences.scheduleSnapshotFlow.first())
        val previous = stored[key]
        if (snapshot == previous) return
        preferences.saveScheduleSnapshot(ScheduleChangeDetector.packAll(stored - key + (key to snapshot)))

        val change = ScheduleChangeDetector.detect(previous, snapshot, now, zone) ?: return

        val settings = preferences.notificationSettingsFlow.first()
        val local = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
        val delivery = AlertRules.scheduleChangeDelivery(settings, local.hour * 60 + local.minute)
        if (delivery == Delivery.Drop) return
        // The app may be open on another tab or address: still report it, just without sound
        val silent = delivery == Delivery.Silent || AppForegroundState.isForeground
        notifier.post(
            NotificationIds.SCHEDULE,
            renderer.scheduleChange(change, address.name, silent = silent, zone = zone)
        )
    }

    private companion object {
        const val TAG = "NotificationController"
    }
}
