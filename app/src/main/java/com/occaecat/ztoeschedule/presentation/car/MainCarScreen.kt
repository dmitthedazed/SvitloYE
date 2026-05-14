package com.occaecat.ztoeschedule.presentation.car

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.SavedAddress
import com.occaecat.ztoeschedule.data.model.ScheduleStatus
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.TimeUtils
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import kotlinx.coroutines.launch

class MainCarScreen(
    carContext: CarContext,
    private val repository: EnergyRepository,
    private val timeProvider: TimeProvider
) : Screen(carContext) {

    private var isLoading = true
    private var addresses: List<SavedAddress> = emptyList()
    private var statusMap: Map<String, String> = emptyMap()
    private var statusColors: Map<String, Int> = emptyMap()

    init {
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                addresses = repository.getSavedAddresses().sortedBy { it.priority }
                val newStatusMap = mutableMapOf<String, String>()
                val newColorMap = mutableMapOf<String, Int>()
                val now = timeProvider.now()

                addresses.forEach { address ->
                    val scheduleResult = repository.getSchedule(address.cherga, address.pidcherga)
                    if (scheduleResult.isSuccess) {
                        val schedule = scheduleResult.getOrThrow()
                        val grouped = ScheduleMapper.getGroupedSchedule(schedule)
                        val currentStatus = ScheduleMapper.getCurrentGroupedStatus(grouped, now)

                        if (currentStatus != null) {
                            val statusText = currentStatus.displayText
                            val timeText = carContext.getString(R.string.car_until, TimeUtils.formatToSystemTime(carContext, currentStatus.endTime))
                            newStatusMap[address.id] = "$statusText • $timeText"
                            
                            val color = when (currentStatus.status) {
                                ScheduleStatus.Available -> Color.GREEN
                                ScheduleStatus.Probable -> Color.YELLOW
                                else -> Color.RED
                            }
                            newColorMap[address.id] = color
                        } else {
                            newStatusMap[address.id] = carContext.getString(R.string.car_status_unknown)
                            newColorMap[address.id] = Color.GRAY
                        }
                    } else {
                        newStatusMap[address.id] = carContext.getString(R.string.car_error_loading)
                        newColorMap[address.id] = Color.DKGRAY
                    }
                }
                statusMap = newStatusMap
                statusColors = newColorMap
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
                invalidate() // Refresh the screen
            }
        }
    }

    override fun onGetTemplate(): Template {
        // If loading, show a loading list. IMPORTANT: ItemList cannot be empty without a message.
        if (isLoading) {
            return ListTemplate.Builder()
                .setHeader(
                    Header.Builder()
                        .setTitle(carContext.getString(R.string.app_name))
                        .setStartHeaderAction(Action.APP_ICON)
                        .build()
                )
                .setLoading(true)
                .setSingleList(
                    ItemList.Builder()
                        .setNoItemsMessage(carContext.getString(R.string.car_loading))
                        .build()
                )
                .build()
        }

        val itemListBuilder = ItemList.Builder()

        if (addresses.isEmpty()) {
            itemListBuilder.setNoItemsMessage(carContext.getString(R.string.car_no_addresses_desc))
        } else {
            addresses.forEach { address ->
                val statusText = statusMap[address.id] ?: carContext.getString(R.string.car_status_unknown)
                val color = statusColors[address.id] ?: Color.GRAY
                
                val rowBuilder = Row.Builder()
                    .setTitle(address.name)
                    .addText(statusText)
                    .setImage(createStatusIcon(color))
                    .setBrowsable(false) // Not clickable for now

                itemListBuilder.addItem(rowBuilder.build())
            }
        }

        return ListTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle(carContext.getString(R.string.app_name))
                    .setStartHeaderAction(Action.APP_ICON)
                    .addEndHeaderAction(
                        Action.Builder()
                            .setTitle(carContext.getString(R.string.car_action_refresh))
                            .setOnClickListener {
                                isLoading = true
                                invalidate()
                                loadData()
                            }
                            .build()
                    )
                    .build()
            )
            .setSingleList(itemListBuilder.build())
            .build()
    }

    private fun createStatusIcon(color: Int): CarIcon {
        val size = 48
        val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = color
        paint.isAntiAlias = true
        // Draw a full circle
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
    }
}
