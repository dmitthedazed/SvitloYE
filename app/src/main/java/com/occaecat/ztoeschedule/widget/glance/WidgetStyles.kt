package com.occaecat.ztoeschedule.widget.glance

import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.unit.ColorProvider
import com.occaecat.ztoeschedule.data.model.ScheduleStatus

/**
 * Small helper for sharing status-based colors between widgets.
 */
data class WidgetPalette(
    val container: ColorProvider,
    val content: ColorProvider,
    val muted: ColorProvider,
    val accent: ColorProvider
)

@Composable
fun paletteForStatus(status: ScheduleStatus?): WidgetPalette {
    val colors = GlanceTheme.colors
    return when (status) {
        ScheduleStatus.Available -> WidgetPalette(
            container = colors.primaryContainer,
            content = colors.onPrimaryContainer,
            muted = colors.onPrimaryContainer,
            accent = colors.primary
        )
        ScheduleStatus.Probable -> WidgetPalette(
            container = colors.tertiaryContainer,
            content = colors.onTertiaryContainer,
            muted = colors.onTertiaryContainer,
            accent = colors.tertiary
        )
        ScheduleStatus.Outage -> WidgetPalette(
            container = colors.errorContainer,
            content = colors.onErrorContainer,
            muted = colors.onErrorContainer,
            accent = colors.error
        )
        else -> WidgetPalette(
            container = colors.surface,
            content = colors.onSurface,
            muted = colors.onSurfaceVariant,
            accent = colors.primary
        )
    }
}
