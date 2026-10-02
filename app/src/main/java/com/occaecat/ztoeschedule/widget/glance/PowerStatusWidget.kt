package com.occaecat.ztoeschedule.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.widget.WidgetSnapshot
import com.occaecat.ztoeschedule.widget.WidgetState
import com.occaecat.ztoeschedule.widget.WidgetStateLoader

/** Is there power right now, and when does that change. */
class PowerStatusWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, SQUARE, WIDE))
    override val previewSizeMode = SizeMode.Responsive(setOf(SQUARE, WIDE))

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
            val size = LocalSize.current
            val compact = size.height < SQUARE.height
            when (val state = snapshot.state) {
                WidgetState.NoAddress -> MessageSurface(
                    R.drawable.ic_widget_add_location,
                    stringRes(R.string.widget_no_address_title),
                    stringRes(R.string.widget_no_address_body),
                    compact
                )
                is WidgetState.NoData -> MessageSurface(
                    R.drawable.ic_widget_no_schedule,
                    stringRes(R.string.widget_no_data_title),
                    stringRes(R.string.widget_no_data_body),
                    compact
                )
                is WidgetState.Ready -> when {
                    size.width >= WIDE.width && size.height < WIDE_TALL_HEIGHT -> Wide(state)
                    size.width >= SQUARE.width && size.height >= SQUARE.height -> Square(state)
                    else -> Small(state)
                }
            }
        }
    }

    @Composable
    private fun MessageSurface(icon: Int, title: String, body: String, compact: Boolean) {
        WidgetSurface(
            background = statusStyle(null).background,
            onClick = actionStartActivity<MainActivity>(),
            modifier = GlanceModifier.semantics { contentDescription = "$title. $body" }
        ) {
            WidgetMessage(icon, title, body, compact)
        }
    }

    /** Badge on top, big status, next change, address at the bottom. */
    @Composable
    private fun Square(state: WidgetState.Ready) {
        val context = LocalContext.current
        val style = statusStyle(state.state)
        val next = nextChangeText(context, state.next, state.now)
        StatusSurface(state, style, next) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                StatusBadge(style, 44.dp)
                Spacer(GlanceModifier.defaultWeight())
                Text(context.getString(style.label), style = WidgetType.titleLarge(style.onBackground), maxLines = 2)
                if (next != null) {
                    Text(next, style = WidgetType.body(style.onBackgroundVariant), maxLines = 1)
                }
                Text(
                    text = addressLine(context, state),
                    style = WidgetType.label(style.onBackgroundVariant, 11.sp),
                    maxLines = 1,
                    modifier = GlanceModifier.padding(top = 6.dp)
                )
            }
        }
    }

    /** One line: badge, status and next change side by side. */
    @Composable
    private fun Wide(state: WidgetState.Ready) {
        val context = LocalContext.current
        val style = statusStyle(state.state)
        val next = nextChangeText(context, state.next, state.now)
        StatusSurface(state, style, next) {
            Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(style, 48.dp)
                Column(modifier = GlanceModifier.defaultWeight().padding(start = 14.dp)) {
                    Text(context.getString(style.label), style = WidgetType.titleLarge(style.onBackground), maxLines = 1)
                    Text(
                        text = next ?: addressLine(context, state),
                        style = WidgetType.body(style.onBackgroundVariant),
                        maxLines = 1
                    )
                    if (next != null) {
                        Text(addressLine(context, state), style = WidgetType.label(style.onBackgroundVariant, 11.sp), maxLines = 1)
                    }
                }
            }
        }
    }

    /** Resized below 2×2: badge and the time of the next change. */
    @Composable
    private fun Small(state: WidgetState.Ready) {
        val context = LocalContext.current
        val style = statusStyle(state.state)
        val next = nextChangeText(context, state.next, state.now)
        StatusSurface(state, style, next, padding = 8.dp, alignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StatusBadge(style, 36.dp)
                Text(
                    text = state.next?.let { context.getString(R.string.widget_until, formatTime(context, it.startMs, state.now, isEnd = true)) }
                        ?: context.getString(style.label),
                    style = WidgetType.label(style.onBackground),
                    maxLines = 1,
                    modifier = GlanceModifier.padding(top = 4.dp)
                )
            }
        }
    }

    @Composable
    private fun StatusSurface(
        state: WidgetState.Ready,
        style: StatusStyle,
        next: String?,
        padding: Dp = 16.dp,
        alignment: Alignment = Alignment.TopStart,
        content: @Composable () -> Unit
    ) {
        val context = LocalContext.current
        val description = listOfNotNull(context.getString(style.label), next, state.addressName).joinToString(". ")
        WidgetSurface(
            background = style.background,
            onClick = actionStartActivity<MainActivity>(),
            modifier = GlanceModifier.semantics { contentDescription = description },
            padding = padding,
            contentAlignment = alignment,
            content = content
        )
    }

    private fun addressLine(context: Context, state: WidgetState.Ready) =
        "${state.addressName} · ${context.getString(R.string.widget_queue, state.queue)}"

    private companion object {
        val SMALL = DpSize(72.dp, 72.dp)
        val SQUARE = DpSize(120.dp, 120.dp)
        val WIDE = DpSize(220.dp, 90.dp)
        val WIDE_TALL_HEIGHT = 150.dp
    }
}
