package com.occaecat.ztoeschedule.domain.notification

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import com.occaecat.ztoeschedule.domain.notification.NotificationCoordinator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Background worker that monitors power schedules periodically.
 *
 * Responsibilities (simplified via NotificationCoordinator):
 * - Check each saved address for upcoming status changes
 * - Detect schedule hash changes
 * - Delegate alert sending to NotificationCoordinator
 * - Delegate UI updates to NotificationCoordinator
 *
 * Execution:
 * - Runs every 15 minutes with 5 minute flex window
 * - Requires network connectivity
 * - Uses exponential backoff on failure (max 3 retries)
 *
 * Coordination:
 * - PowerMonitorWorker handles schedule checking
 * - NotificationCoordinator handles deduplication and UI updates
 * - No direct widget/tile updates from Worker
 */
@HiltWorker
class PowerMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferencesManager: EnergyPreferencesManager,
    private val repository: EnergyRepository,
    private val notificationCoordinator: NotificationCoordinator,
    private val timeProvider: TimeProvider
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "PowerMonitorWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker execution started (attempt $runAttemptCount)")

        return try {
            val addresses = repository.getSavedAddresses()
                .sortedBy { it.priority }

            if (addresses.isEmpty()) {
                Log.i(TAG, "No saved addresses, nothing to check")
                return Result.success()
            }

            // Only check primary address (first by priority)
            val primaryAddress = addresses.first()
            Log.d(TAG, "Checking primary address: ${primaryAddress.name} (priority: ${primaryAddress.priority})")

            val notificationsEnabled = preferencesManager.notificationsEnabledFlow.first()

            var hadChanges = false

            // Check only primary address
            try {
                val result = repository.getScheduleWithMessages(primaryAddress.cherga, primaryAddress.pidcherga)

                if (result.isSuccess) {
                    val data = result.getOrThrow()
                    val groupedSchedules = ScheduleMapper.getGroupedSchedule(data.schedules)

                    Log.d(TAG, "Schedule for ${primaryAddress.name}: ${groupedSchedules.size} groups")

                    // 1. Check for schedule hash change
                    if (data.schedules.isNotEmpty()) {
                        val currentHash = data.schedules.hashCode().toString()
                        val lastHash = preferencesManager.lastScheduleHashFlow.first()

                        if (lastHash != null && lastHash != currentHash) {
                            Log.i(TAG, "Schedule hash changed for ${primaryAddress.name}")
                            preferencesManager.saveLastScheduleHash(currentHash)
                            hadChanges = true
                        } else if (lastHash == null) {
                            preferencesManager.saveLastScheduleHash(currentHash)
                        }
                    }

                    // 2. Check for status changes RIGHT NOW (no advance warning)
                    if (notificationsEnabled && groupedSchedules.isNotEmpty()) {
                        val now = timeProvider.now()
                        val currentStatus = ScheduleMapper.getCurrentGroupedStatus(groupedSchedules, now)

                        if (currentStatus != null) {
                            // Check if current status just started (within last 16 min, worker runs every 15 min)
                            val statusJustChanged = (now - currentStatus.startMs) < (16 * 60 * 1000)
                            
                            if (statusJustChanged) {
                                Log.i(TAG, "✓ Status just changed for ${primaryAddress.name}: now ${currentStatus.status}")
                                
                                // Find previous schedule entry
                                val previousSchedule = groupedSchedules.firstOrNull { 
                                    it.endMs == currentStatus.startMs 
                                }
                                
                                if (previousSchedule != null && previousSchedule.status != currentStatus.status) {
                                    notificationCoordinator.notifyStatusChange(
                                        primaryAddress,
                                        previousSchedule,
                                        currentStatus
                                    )
                                }
                            }

                            // Update status notification and UI
                            notificationCoordinator.updateAllUI(
                                primaryAddress,
                                currentStatus,
                                groupedSchedules
                            )
                        }
                    }

                } else {
                    Log.w(TAG, "Failed to fetch schedule for ${primaryAddress.name}: ${result.exceptionOrNull()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing primary address ${primaryAddress.name}", e)
            }

            Log.d(TAG, "Worker execution completed successfully. Changes detected: $hadChanges")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Worker execution failed", e)

            // Exponential backoff with max 3 retries
            return if (runAttemptCount < 3) {
                Log.w(TAG, "Retrying (attempt ${runAttemptCount + 1}/3)")
                Result.retry()
            } else {
                Log.e(TAG, "Max retries exceeded, giving up")
                Result.failure()
            }
        }
    }
}