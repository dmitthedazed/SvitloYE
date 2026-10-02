package com.occaecat.ztoeschedule.widget.glance

import android.content.Context
import com.occaecat.ztoeschedule.domain.notification.PowerState
import androidx.glance.layout.ContentScale
import androidx.glance.Image
import androidx.compose.ui.graphics.toArgb
import android.graphics.RectF
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.domain.notification.PowerRun
import com.occaecat.ztoeschedule.widget.WidgetSnapshot
import com.occaecat.ztoeschedule.widget.WidgetState
import com.occaecat.ztoeschedule.widget.WidgetStateLoader

/** Today at a glance: current status, a 24-hour strip and the upcoming changes. */
class ScheduleWidget : GlanceAppWidget() {

    // Exact: the day strip is laid out in dp from the real widget width
    override val sizeMode = SizeMode.Exact
    // Typical 4×2 and 4×3 cells; the picker scales the preview to its slot
    override val previewSizeMode = SizeMode.Responsive(setOf(DpSize(320.dp, 180.dp), DpSize(320.dp, 280.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadWidgetSnapshot(context)
        provideContent { Content(snapshot) }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val snapshot = WidgetSnapshot(WidgetStateLoader.previewState(), dynamicColor = true)
        provideContent { Content(snapshot) }
    }

    @Composable
    private fun Content(snapshot: WidgetSnapshot) {
        SvitloWidgetTheme(snapshot.dynamicColor) {
            val state = snapshot.state
            val description = (state as? WidgetState.Ready)?.let { describe(LocalContext.current, it) }
            WidgetSurface(
                background = GlanceTheme.colors.widgetBackground,
                onClick = actionStartActivity<MainActivity>(),
                modifier = if (description != null) GlanceModifier.semantics { contentDescription = description } else GlanceModifier,
                padding = PADDING
            ) {
                val compact = LocalSize.current.height < MEDIUM.height
                when (state) {
                    WidgetState.NoAddress -> WidgetMessage(
                        R.drawable.ic_widget_add_location,
                        stringRes(R.string.widget_no_address_title),
                        stringRes(R.string.widget_no_address_body),
                        compact = false
                    )
                    is WidgetState.NoData -> WidgetMessage(
                        R.drawable.ic_widget_no_schedule,
                        stringRes(R.string.widget_no_data_title),
                        stringRes(R.string.widget_no_data_body),
                        compact = false
                    )
                    is WidgetState.Ready -> Ready(state, compact)
                }
            }
        }
    }

    @Composable
    private fun Ready(state: WidgetState.Ready, compact: Boolean) {
        val size = LocalSize.current
        val stripWidth = size.width - PADDING * 2
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Header(state)
            Spacer(GlanceModifier.height(if (compact) 8.dp else 14.dp))
            DayStrip(state, stripWidth)
            if (!compact) {
                HourLabels()
                // Header 40 + gap 14 + strip 20 + labels 14 + "Далі" 26; each row ~26
                val rows = ((size.height - PADDING * 2 - 114.dp) / ROW_HEIGHT).toInt().coerceIn(0, 4)
                val upcoming = state.upcoming.filter { it.startMs > state.now }.take(rows)
                if (upcoming.isNotEmpty()) {
                    Text(
                        text = stringRes(R.string.widget_upcoming),
                        style = WidgetType.label(GlanceTheme.colors.onSurfaceVariant, 11.sp),
                        modifier = GlanceModifier.padding(top = 10.dp, bottom = 2.dp)
                    )
                    upcoming.forEach { RunRow(it, state.now) }
                }
            }
        }
    }

    @Composable
    private fun Header(state: WidgetState.Ready) {
        val context = LocalContext.current
        val colors = GlanceTheme.colors
        val style = statusStyle(state.state)
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(style, 40.dp)
            Column(modifier = GlanceModifier.defaultWeight().padding(start = 12.dp)) {
                Text(context.getString(style.label), style = WidgetType.titleMedium(colors.onSurface), maxLines = 1)
                Text(
                    text = listOfNotNull(
                        nextChangeText(context, state.next, state.now),
                        state.addressName
                    ).joinToString(" · "),
                    style = WidgetType.body(colors.onSurfaceVariant, 12.sp),
                    maxLines = 1
                )
            }
            CircleIconButton(
                imageProvider = ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = context.getString(R.string.widget_refresh),
                onClick = actionRunCallback<RefreshWidgetsAction>(),
                backgroundColor = null,
                contentColor = colors.onSurfaceVariant
            )
        }
    }

    /**
     * Today's runs as proportional pills with a "now" marker, drawn into a bitmap that stretches
     * to the real width: the picker and launchers render wider than the size bucket says, and a
     * bitmap keeps the strip aligned with the hour labels either way.
     */
    @Composable
    private fun DayStrip(state: WidgetState.Ready, width: Dp) {
        val context = LocalContext.current
        val density = context.resources.displayMetrics.density
        val palette = mapOf(
            null to runColor(null),
            PowerState.On to runColor(PowerState.On),
            PowerState.Off to runColor(PowerState.Off),
            PowerState.Maybe to runColor(PowerState.Maybe)
        ).mapValues { it.value.getColor(context).toArgb() }
        val marker = GlanceTheme.colors.onSurface.getColor(context).toArgb()
        val bitmap = dayStripBitmap(
            state = state,
            widthPx = (width.value * density).toInt().coerceAtLeast(1),
            density = density,
            colorOf = { palette.getValue(it) },
            markerColor = marker
        )
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxWidth().height(MARKER_HEIGHT)
        )
    }

    private fun dayStripBitmap(
        state: WidgetState.Ready,
        widthPx: Int,
        density: Float,
        colorOf: (PowerState?) -> Int,
        markerColor: Int
    ): Bitmap {
        val heightPx = (MARKER_HEIGHT.value * density).toInt()
        val stripPx = STRIP_HEIGHT.value * density
        val gapPx = density
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dayLength = (state.dayEnd - state.dayStart).toFloat()
        fun x(ms: Long) = widthPx * ((ms.coerceIn(state.dayStart, state.dayEnd) - state.dayStart) / dayLength)
        val top = (heightPx - stripPx) / 2

        fun pill(state: PowerState?, startMs: Long, endMs: Long) {
            val left = x(startMs) + gapPx
            val right = x(endMs) - gapPx
            if (right <= left) return
            paint.color = colorOf(state)
            canvas.drawRoundRect(RectF(left, top, right, top + stripPx), stripPx / 2, stripPx / 2, paint)
        }

        var cursor = state.dayStart
        for (run in state.today) {
            if (run.startMs > cursor) pill(null, cursor, run.startMs)
            pill(run.state, maxOf(run.startMs, cursor), run.endMs)
            cursor = maxOf(cursor, run.endMs)
        }
        if (cursor < state.dayEnd) pill(null, cursor, state.dayEnd)

        val markerPx = MARKER_WIDTH.value * density
        val markerX = x(state.now).coerceIn(markerPx / 2, widthPx - markerPx / 2)
        paint.color = markerColor
        canvas.drawRoundRect(
            RectF(markerX - markerPx / 2, 0f, markerX + markerPx / 2, heightPx.toFloat()),
            markerPx / 2, markerPx / 2, paint
        )
        return bitmap
    }

    @Composable
    private fun HourLabels() {
        val style = WidgetType.label(GlanceTheme.colors.onSurfaceVariant, 10.sp)
        Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp)) {
            listOf("00", "06", "12", "18", "24").forEachIndexed { index, hour ->
                if (index > 0) Spacer(GlanceModifier.defaultWeight())
                Text(hour, style = style)
            }
        }
    }

    @Composable
    private fun RunRow(run: PowerRun, now: Long) {
        val context = LocalContext.current
        val colors = GlanceTheme.colors
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier.size(10.dp).cornerRadius(5.dp).background(runColor(run.state))
            ) {}
            Text(
                text = formatSpan(context, run, now),
                style = WidgetType.label(colors.onSurface, 13.sp),
                maxLines = 1,
                modifier = GlanceModifier.padding(start = 10.dp)
            )
            Text(
                text = context.getString(runLabel(run.state)),
                style = WidgetType.body(colors.onSurfaceVariant, 13.sp),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight().padding(start = 8.dp)
            )
            Text(formatDuration(run), style = WidgetType.body(colors.onSurfaceVariant, 12.sp), maxLines = 1)
        }
    }

    private fun describe(context: Context, state: WidgetState.Ready): String = buildList {
        add(context.getString(statusLabel(state.state)))
        nextChangeText(context, state.next, state.now)?.let(::add)
        add(state.addressName)
        state.upcoming.filter { it.startMs > state.now }.take(4).forEach {
            add("${context.getString(runLabel(it.state))} ${formatSpan(context, it, state.now)}")
        }
    }.joinToString(". ")

    private companion object {
        val MEDIUM = DpSize(250.dp, 150.dp)
        val PADDING = 16.dp
        val STRIP_HEIGHT = 12.dp
        val MARKER_HEIGHT = 20.dp
        val MARKER_WIDTH = 4.dp
        val ROW_HEIGHT = 26.dp
    }
}
