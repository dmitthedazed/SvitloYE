package com.occaecat.ztoeschedule.widget.glance

import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.notification.PowerRun
import com.occaecat.ztoeschedule.domain.notification.PowerState
import com.occaecat.ztoeschedule.ui.theme.DarkColorScheme
import com.occaecat.ztoeschedule.ui.theme.LightColorScheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val BrandColors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme)

/** Material You colours when the app uses them, otherwise the app's amber brand scheme. */
@Composable
fun SvitloWidgetTheme(dynamicColor: Boolean, content: @Composable () -> Unit) {
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    GlanceTheme(colors = if (useDynamic) GlanceTheme.colors else BrandColors, content = content)
}

/** MD3 type scale roles used by the widgets. */
object WidgetType {
    fun titleLarge(color: ColorProvider) = TextStyle(color = color, fontSize = 22.sp, fontWeight = FontWeight.Medium)
    fun titleMedium(color: ColorProvider) = TextStyle(color = color, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    fun body(color: ColorProvider, size: TextUnit = 14.sp) = TextStyle(color = color, fontSize = size)
    fun label(color: ColorProvider, size: TextUnit = 12.sp) =
        TextStyle(color = color, fontSize = size, fontWeight = FontWeight.Medium)
}

/**
 * How a power state looks. Light on is the calm default (neutral surface, brand badge), an outage
 * takes over the whole widget, and a possible outage is a softer error accent, as in the app.
 */
data class StatusStyle(
    val background: ColorProvider,
    val onBackground: ColorProvider,
    val onBackgroundVariant: ColorProvider,
    val badge: ColorProvider,
    val onBadge: ColorProvider,
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int
)

@Composable
fun statusStyle(state: PowerState?): StatusStyle {
    val c = GlanceTheme.colors
    return when (state) {
        PowerState.On -> StatusStyle(
            c.widgetBackground, c.onSurface, c.onSurfaceVariant,
            c.primaryContainer, c.onPrimaryContainer, R.drawable.ic_bolt, statusLabel(state)
        )
        PowerState.Off -> StatusStyle(
            c.errorContainer, c.onErrorContainer, c.onErrorContainer,
            c.error, c.onError, R.drawable.ic_widget_power_off, statusLabel(state)
        )
        PowerState.Maybe -> StatusStyle(
            c.widgetBackground, c.onSurface, c.onSurfaceVariant,
            c.errorContainer, c.onErrorContainer, R.drawable.ic_widget_power_maybe, statusLabel(state)
        )
        null -> StatusStyle(
            c.widgetBackground, c.onSurface, c.onSurfaceVariant,
            c.surfaceVariant, c.onSurfaceVariant, R.drawable.ic_widget_no_schedule, statusLabel(state)
        )
    }
}

/** Colour of a run in the day strip and the upcoming list. */
@Composable
fun runColor(state: PowerState?): ColorProvider {
    val c = GlanceTheme.colors
    return when (state) {
        PowerState.On -> c.secondaryContainer
        PowerState.Off -> c.error
        PowerState.Maybe -> c.errorContainer
        null -> c.surfaceVariant
    }
}

@StringRes
fun statusLabel(state: PowerState?): Int = when (state) {
    PowerState.On -> R.string.widget_state_on
    PowerState.Off -> R.string.widget_state_off
    PowerState.Maybe -> R.string.widget_state_maybe
    null -> R.string.widget_state_unknown
}

@StringRes
fun runLabel(state: PowerState?): Int = when (state) {
    PowerState.On -> R.string.widget_run_on
    PowerState.Off -> R.string.widget_run_off
    PowerState.Maybe -> R.string.widget_run_maybe
    null -> R.string.widget_state_unknown
}

/** "Відключення о 17:00", or null when the schedule does not say what comes next. */
fun nextChangeText(context: Context, next: PowerRun?, now: Long): String? {
    next ?: return null
    val res = when (next.state) {
        PowerState.On -> R.string.widget_next_on
        PowerState.Off -> R.string.widget_next_off
        PowerState.Maybe -> R.string.widget_next_maybe
        null -> return null
    }
    return context.getString(res, formatTime(context, next.startMs, now))
}

private val HOURS_MINUTES = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_MONTH = DateTimeFormatter.ofPattern("dd.MM")

/**
 * "17:00" today, "завтра 06:00", or "03.10 06:00". The coming midnight reads "24:00" when it
 * ends something ([isEnd]) and "завтра 00:00" when it starts something.
 */
fun formatTime(context: Context, ms: Long, now: Long, isEnd: Boolean = false): String {
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(ms).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val tomorrow = today.plusDays(1)
    val hm = time.format(HOURS_MINUTES)
    return when {
        time.toLocalDate() == today -> hm
        isEnd && time.toLocalDate() == tomorrow && time.toLocalTime().toSecondOfDay() == 0 -> "24:00"
        time.toLocalDate() == tomorrow -> context.getString(R.string.widget_tomorrow_at, hm)
        else -> "${time.format(DAY_MONTH)} $hm"
    }
}

fun formatSpan(context: Context, run: PowerRun, now: Long): String =
    "${formatTime(context, run.startMs, now)} – ${formatTime(context, run.endMs, now, isEnd = true)}"

fun formatDuration(run: PowerRun): String {
    val minutes = ((run.endMs - run.startMs) / 60_000L).toInt()
    return ScheduleMapper.formatDuration(minutes / 60, minutes % 60)
}

/** Root container with the launcher's corner radius, so widgets match the home screen. */
@Composable
fun WidgetSurface(
    background: ColorProvider,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
    padding: Dp = 16.dp,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .background(background)
            .clickable(onClick)
            .padding(padding),
        contentAlignment = contentAlignment,
        content = content
    )
}

/** Status icon in a tonal circle. */
@Composable
fun StatusBadge(style: StatusStyle, size: Dp, contentDescription: String? = null) {
    Box(
        modifier = GlanceModifier.size(size).cornerRadius(size / 2).background(style.badge),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(style.icon),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(size * 0.55f),
            colorFilter = ColorFilter.tint(style.onBadge)
        )
    }
}

/** Shared empty/error states: no address picked yet, or nothing cached for it. */
@Composable
fun WidgetMessage(@DrawableRes icon: Int, title: String, body: String?, compact: Boolean) {
    val colors = GlanceTheme.colors
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier.size(40.dp).cornerRadius(20.dp).background(colors.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = null,
                modifier = GlanceModifier.size(22.dp),
                colorFilter = ColorFilter.tint(colors.onSecondaryContainer)
            )
        }
        if (!compact) {
            Text(
                text = title,
                style = WidgetType.titleMedium(colors.onSurface),
                maxLines = 1,
                modifier = GlanceModifier.padding(top = 8.dp)
            )
            if (body != null) {
                Text(text = body, style = WidgetType.body(colors.onSurfaceVariant, 12.sp), maxLines = 2)
            }
        }
    }
}

@Composable
fun stringRes(@StringRes id: Int, vararg args: Any): String = LocalContext.current.getString(id, *args)
