package com.occaecat.ztoeschedule.widget.glance

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import com.occaecat.ztoeschedule.domain.notification.NotificationSync
import com.occaecat.ztoeschedule.widget.WidgetSnapshot
import com.occaecat.ztoeschedule.widget.WidgetStateLoader
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

// Receiver class names are referenced by widgets already placed on home screens: do not rename.

/** 1×1 light bulb. */
class LightWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LightWidget()
}

/** 2×2 power status. */
class PowerStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PowerStatusWidget()
}

/** 4×2 day schedule. */
class DetailedScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetStateLoader(): WidgetStateLoader
}

internal suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot =
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        .widgetStateLoader()
        .load()

/** Fetches a fresh schedule; the sync redraws every widget when it finishes. */
class RefreshWidgetsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        NotificationSync.syncNow(context, fetch = true, urgent = true)
    }
}
