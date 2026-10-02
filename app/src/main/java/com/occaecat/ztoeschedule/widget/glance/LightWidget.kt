package com.occaecat.ztoeschedule.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.widget.WidgetSnapshot
import com.occaecat.ztoeschedule.widget.WidgetState
import com.occaecat.ztoeschedule.widget.WidgetStateLoader

/** The light bulb: the whole tile glows in the status colour, with the time of the next change. */
class LightWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, LARGE))
    override val previewSizeMode = SizeMode.Responsive(setOf(SMALL, LARGE))

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
            val context = LocalContext.current
            val state = snapshot.state
            val ready = state as? WidgetState.Ready
            val style = statusStyle(ready?.state)
            val large = LocalSize.current.width >= LARGE.width && LocalSize.current.height >= LARGE.height

            val icon = when (state) {
                WidgetState.NoAddress -> R.drawable.ic_widget_add_location
                else -> style.icon
            }
            val caption = when (state) {
                WidgetState.NoAddress -> context.getString(R.string.widget_no_address_title)
                is WidgetState.NoData -> context.getString(R.string.widget_no_data_title)
                is WidgetState.Ready -> state.next?.let {
                    context.getString(R.string.widget_until, formatTime(context, it.startMs, state.now, isEnd = true))
                } ?: context.getString(style.label)
            }
            val description = when (state) {
                is WidgetState.Ready -> listOfNotNull(
                    context.getString(style.label),
                    nextChangeText(context, state.next, state.now)
                ).joinToString(". ")
                else -> caption
            }

            WidgetSurface(
                background = style.badge,
                onClick = actionStartActivity<MainActivity>(),
                modifier = GlanceModifier.semantics { contentDescription = description },
                padding = if (large) 12.dp else 4.dp,
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        provider = ImageProvider(icon),
                        contentDescription = null,
                        modifier = GlanceModifier.size(if (large) 40.dp else 28.dp),
                        colorFilter = ColorFilter.tint(style.onBadge)
                    )
                    if (large) {
                        Text(
                            text = caption,
                            style = WidgetType.label(style.onBadge),
                            maxLines = 1,
                            modifier = GlanceModifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }

    private companion object {
        val SMALL = DpSize(48.dp, 48.dp)
        val LARGE = DpSize(96.dp, 96.dp)
    }
}
