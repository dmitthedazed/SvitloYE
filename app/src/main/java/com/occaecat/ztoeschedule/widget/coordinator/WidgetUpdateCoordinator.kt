package com.occaecat.ztoeschedule.widget.coordinator

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.occaecat.ztoeschedule.data.model.ScheduleStatus
import com.occaecat.ztoeschedule.widget.data.WidgetData
import com.occaecat.ztoeschedule.widget.data.WidgetDataProvider
import com.occaecat.ztoeschedule.widget.glance.LightWidget
import com.occaecat.ztoeschedule.widget.glance.PowerStatusGlanceWidget
import com.occaecat.ztoeschedule.widget.glance.DetailedScheduleGlanceWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Координатор оновлень віджетів.
 * Централізує логіку оновлення всіх типів віджетів через єдину точку входу.
 */
@Singleton
class WidgetUpdateCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataProvider: WidgetDataProvider
) {
    
    /**
     * Оновлює всі віджети в системі
     */
    suspend fun updateAllWidgets() {
        withContext(Dispatchers.IO) {
            try {
                // Отримуємо актуальні дані
                val widgetData = dataProvider.getWidgetData()
                
                // Оновлюємо всі типи віджетів
                updateLightWidgets(widgetData)
                updatePowerStatusWidgets()
                updateDetailedScheduleWidgets()
                
            } catch (e: Exception) {
                android.util.Log.e("WidgetUpdateCoordinator", "Error updating widgets", e)
            }
        }
    }
    
    /**
     * Оновлює віджети LightWidget
     */
    private suspend fun updateLightWidgets(data: WidgetData) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(LightWidget::class.java)
        
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs ->
                when (data) {
                    is WidgetData.Loaded -> {
                        // Визначаємо статус
                        val status = when (data.currentStatus?.status) {
                            ScheduleStatus.Outage -> "outage"
                            ScheduleStatus.Available -> "available"
                            ScheduleStatus.Probable -> "probable"
                            else -> "unknown"
                        }
                        
                        // Форматуємо наступну подію: беремо початок інтервалу зі span
                        val nextEvent = data.nextStatus?.span
                            ?.split("-")
                            ?.firstOrNull()
                            ?.trim()
                            ?: "--:--"
                        
                        // Зберігаємо в preferences
                        prefs[LightWidget.KEY_STATUS] = status
                        prefs[LightWidget.KEY_NEXT_EVENT] = nextEvent
                        prefs[LightWidget.KEY_ADDRESS] = data.addressName
                        prefs[LightWidget.KEY_UPDATED] = System.currentTimeMillis()
                    }
                    
                    is WidgetData.Error -> {
                        prefs[LightWidget.KEY_STATUS] = "unknown"
                        prefs[LightWidget.KEY_NEXT_EVENT] = data.message
                        data.addressName?.let { prefs[LightWidget.KEY_ADDRESS] = it }
                    }
                    
                    is WidgetData.NotConfigured -> {
                        prefs[LightWidget.KEY_STATUS] = "unknown"
                        prefs[LightWidget.KEY_NEXT_EVENT] = "Налаштуйте адресу"
                    }
                }
            }
        }
        
        // Оновлюємо UI всіх віджетів
        LightWidget().updateAll(context)
    }

    private suspend fun updatePowerStatusWidgets() {
        PowerStatusGlanceWidget().updateAll(context)
    }

    private suspend fun updateDetailedScheduleWidgets() {
        DetailedScheduleGlanceWidget().updateAll(context)
    }
    
    /**
     * Оновлює конкретний віджет за його ID
     */
    suspend fun updateWidget(glanceId: androidx.glance.GlanceId) {
        withContext(Dispatchers.IO) {
            try {
                val widgetData = dataProvider.getWidgetData()
                updateAppWidgetState(context, glanceId) { prefs ->
                    // Логіка така сама як у updateLightWidgets
                    when (widgetData) {
                        is WidgetData.Loaded -> {
                            val status = when (widgetData.currentStatus?.status) {
                                ScheduleStatus.Outage -> "outage"
                                ScheduleStatus.Available -> "available"
                                ScheduleStatus.Probable -> "probable"
                                else -> "unknown"
                            }
                            prefs[LightWidget.KEY_STATUS] = status
                            prefs[LightWidget.KEY_ADDRESS] = widgetData.addressName
                        }
                        else -> {
                            prefs[LightWidget.KEY_STATUS] = "unknown"
                        }
                    }
                }
                LightWidget().update(context, glanceId)
            } catch (e: Exception) {
                android.util.Log.e("WidgetUpdateCoordinator", "Error updating widget", e)
            }
        }
    }
}
