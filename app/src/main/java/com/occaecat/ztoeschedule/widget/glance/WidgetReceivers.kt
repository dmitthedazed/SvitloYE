package com.occaecat.ztoeschedule.widget.glance

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Receiver для LightWidget (малий віджет зі статусом)
 */
class LightWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LightWidget()
}

/**
 * Receiver для DetailedScheduleGlanceWidget (детальний розклад)
 */
class DetailedScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DetailedScheduleGlanceWidget()
}

/**
 * Receiver для PowerStatusGlanceWidget (статус живлення)
 */
class PowerStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PowerStatusGlanceWidget()
}
