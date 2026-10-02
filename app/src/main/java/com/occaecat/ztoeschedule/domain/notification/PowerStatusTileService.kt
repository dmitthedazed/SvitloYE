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
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick Settings tile service that displays current power status.
 *
 * Displays, from the cached schedule of the primary address:
 * - STATE_ACTIVE (tile ON) if power is currently available
 * - STATE_INACTIVE (tile OFF) for an outage or a possible outage, each with its own label
 * - STATE_UNAVAILABLE when the schedule doesn't cover the current moment
 * - Subtitle showing primary address name
 *
 * Updates:
 * - onStartListening() - when tile becomes visible
 * - requestTileUpdate() after notification sync and status changes
 *
 * Threading:
 * - Cache reads run on IO dispatcher
 */
@AndroidEntryPoint
class PowerStatusTileService : TileService() {

    companion object {
        private const val TAG = "PowerStatusTileService"
    }

    @Inject
    lateinit var controller: NotificationController

    @Inject
    lateinit var timeProvider: TimeProvider

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var updateJob: Job? = null

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
        // The tile is no longer visible: don't touch it after this point
        updateJob?.cancel()
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
        val tile = qsTile ?: return

        // The shade is opened often: read the local cache only, syncing is NotificationController's job
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            try {
                val address = controller.primaryAddress()
                if (address == null) {
                    updateTileState(tile, Tile.STATE_INACTIVE, "Немає адреси", "Налаштуйте")
                    return@launch
                }
                val run = controller.cachedTimeline(address)?.currentRun(timeProvider.now())
                when (run?.state) {
                    PowerState.On -> updateTileState(tile, Tile.STATE_ACTIVE, "Світло є ✅", address.name)
                    PowerState.Off -> updateTileState(tile, Tile.STATE_INACTIVE, "Світла немає 🔴", address.name)
                    PowerState.Maybe -> updateTileState(tile, Tile.STATE_INACTIVE, "Можливе відключення 🟡", address.name)
                    null -> updateTileState(tile, Tile.STATE_UNAVAILABLE, "Невідомо", address.name)
                }
            } catch (e: CancellationException) {
                throw e
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
