package com.occaecat.ztoeschedule.domain.notification

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick Settings tile service that displays current power status.
 *
 * Displays:
 * - STATE_ACTIVE (tile ON) if power is currently available
 * - STATE_INACTIVE (tile OFF) if power is off or unknown
 * - Label showing "Світло є" or "Світла немає"
 * - Subtitle showing primary address name
 *
 * Updates:
 * - onStartListening() - when tile becomes visible
 * - onClick() from NotificationScheduler.updateTile()
 *
 * Threading:
 * - Network operations run on IO dispatcher
 * - Tile updates run on main thread
 */
@AndroidEntryPoint
class PowerStatusTileService : TileService() {

    companion object {
        private const val TAG = "PowerStatusTileService"
    }

    @Inject
    lateinit var repository: EnergyRepository

    @Inject
    lateinit var timeProvider: TimeProvider

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "onStartListening()")
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        Log.d(TAG, "onClick()")

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(pendingIntent)
            } else {
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching activity", e)
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        Log.d(TAG, "onStopListening()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
        serviceScope.cancel()
    }

    /**
     * Update tile display with current power status.
     *
     * Fetches primary address schedule and determines if power is available.
     */
    private fun updateTile() {
        Log.d(TAG, "updateTile()")

        val tile = qsTile
        if (tile == null) {
            Log.w(TAG, "QS tile not available")
            return
        }

        serviceScope.launch {
            try {
                // Get primary address (sorted by priority)
                val addresses = repository.getSavedAddresses()
                    .sortedBy { it.priority }

                val primaryAddress = addresses.firstOrNull()

                if (primaryAddress == null) {
                    Log.w(TAG, "No saved addresses")
                    updateTileState(tile, Tile.STATE_INACTIVE, "Немає адреси", "Налаштуйте")
                    return@launch
                }

                Log.d(TAG, "Fetching schedule for ${primaryAddress.name}")

                // Fetch current schedule
                val result = repository.getSchedule(primaryAddress.cherga, primaryAddress.pidcherga)

                if (result.isFailure) {
                    Log.w(TAG, "Failed to fetch schedule: ${result.exceptionOrNull()}")
                    updateTileState(tile, Tile.STATE_INACTIVE, "Невідомо", primaryAddress.name)
                    return@launch
                }

                val schedules = result.getOrThrow()
                val grouped = ScheduleMapper.getGroupedSchedule(schedules)
                val currentStatus = ScheduleMapper.getCurrentGroupedStatus(grouped, timeProvider.now())

                if (currentStatus != null) {
                    val isLightOn = currentStatus.status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.Available
                    val state = if (isLightOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    val label = if (isLightOn) "Світло є ✅" else "Світла немає 🔴"

                    Log.d(TAG, "Tile updated: state=$state, label=$label, address=${primaryAddress.name}")
                    updateTileState(tile, state, label, primaryAddress.name)
                } else {
                    Log.w(TAG, "Could not determine current status")
                    updateTileState(tile, Tile.STATE_INACTIVE, "Оновіть", primaryAddress.name)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error updating tile", e)
                updateTileState(tile, Tile.STATE_INACTIVE, "Помилка", "СвітлоЄ?")
            }
        }
    }

    /**
     * Update tile UI on main thread.
     *
     * @param tile QS tile to update
     * @param state STATE_ACTIVE or STATE_INACTIVE
     * @param label Main label text
     * @param subtitle Subtitle text (Android Q+)
     */
    private fun updateTileState(tile: Tile, state: Int, label: String, subtitle: String?) {
        tile.state = state
        tile.label = label

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = subtitle ?: "СвітлоЄ?"
        }

        tile.icon = Icon.createWithResource(this, R.drawable.ic_bolt)

        try {
            tile.updateTile()
        } catch (e: Exception) {
            Log.e(TAG, "Error calling updateTile()", e)
        }
    }
}
