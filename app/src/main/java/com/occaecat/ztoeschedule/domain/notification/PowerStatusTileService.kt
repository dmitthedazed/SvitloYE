package com.occaecat.ztoeschedule.domain.notification

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.occaecat.ztoeschedule.domain.time.TimeProvider

@AndroidEntryPoint
class PowerStatusTileService : TileService() {

    @Inject
    lateinit var repository: EnergyRepository

    @Inject
    lateinit var timeProvider: TimeProvider

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (Build.VERSION.SDK_INT >= 34) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun updateTile() {
        val tile = qsTile ?: return

        serviceScope.launch {
            try {
                // Get primary address
                val addresses = repository.getSavedAddresses().sortedBy { it.priority }
                val primaryAddress = addresses.firstOrNull()

                if (primaryAddress == null) {
                    updateTileState(tile, Tile.STATE_INACTIVE, "Немає адреси", "Налаштуйте")
                    return@launch
                }

                // Get cached schedule
                val result = repository.getSchedule(primaryAddress.cherga, primaryAddress.pidcherga)
                if (result.isFailure) {
                    updateTileState(tile, Tile.STATE_INACTIVE, "Невідомо", primaryAddress.name)
                    return@launch
                }

                val schedules = result.getOrThrow()
                val grouped = ScheduleMapper.getGroupedSchedule(schedules)
                val currentStatus = ScheduleMapper.getCurrentGroupedStatus(grouped, timeProvider.now())

                if (currentStatus != null) {
                    val isLightOn = currentStatus.status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.AVAILABLE
                    val state = if (isLightOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    val label = if (isLightOn) "Світло є" else "Світла немає"
                    updateTileState(tile, state, label, primaryAddress.name)
                } else {
                    updateTileState(tile, Tile.STATE_INACTIVE, "Оновіть", primaryAddress.name)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                updateTileState(tile, Tile.STATE_INACTIVE, "Помилка", "СвітлоЄ?")
            }
        }
    }

    private fun updateTileState(tile: Tile, state: Int, label: String, subtitle: String?) {
        tile.state = state
        tile.label = label
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            tile.subtitle = subtitle
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_bolt)
        tile.updateTile()
    }
}
