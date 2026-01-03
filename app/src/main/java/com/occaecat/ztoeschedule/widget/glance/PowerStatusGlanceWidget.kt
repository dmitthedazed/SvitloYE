package com.occaecat.ztoeschedule.widget.glance

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.ScheduleStatus
import com.occaecat.ztoeschedule.widget.data.WidgetData
import com.occaecat.ztoeschedule.widget.data.WidgetDataProvider
import com.occaecat.ztoeschedule.widget.glance.WidgetPalette
import com.occaecat.ztoeschedule.widget.glance.paletteForStatus
import com.occaecat.ztoeschedule.widget.glance.SvitloYeGlanceTheme
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PowerStatusWidgetEntryPoint {
    fun widgetDataProvider(): WidgetDataProvider
}

/**
 * Компактний віджет статусу живлення з MD3 дизайном через Glance
 */
class PowerStatusGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 100.dp),  // 1x1 - мінімальний
            DpSize(200.dp, 100.dp),  // 4x1 - горизонтальний
            DpSize(200.dp, 200.dp)   // 2x2 - квадрат з деталями
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext ?: context
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            PowerStatusWidgetEntryPoint::class.java
        )
        val dataProvider = entryPoint.widgetDataProvider()
        
        val widgetData = try {
            dataProvider.getWidgetData()
        } catch (e: Exception) {
            WidgetData.Error("Помилка", null)
        }

        provideContent {
            SvitloYeGlanceTheme {
                WidgetContent(widgetData, context)
            }
        }
    }

    @Composable
    private fun WidgetContent(data: WidgetData, context: Context) {
        val size = LocalSize.current
        val palette = paletteForStatus((data as? WidgetData.Loaded)?.currentStatus?.status)
        
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(palette.container)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (data) {
                is WidgetData.NotConfigured -> NotConfiguredView(palette)
                is WidgetData.Error -> ErrorView(data.message, palette)
                is WidgetData.Loaded -> {
                    when {
                        size.height >= 180.dp && size.width >= 180.dp -> LargeView(data, palette)
                        size.width >= 180.dp -> HorizontalView(data, palette)
                        else -> MinimalView(data, palette)
                    }
                }
            }
        }
    }

    @Composable
    private fun NotConfiguredView(palette: WidgetPalette) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_bolt),
                contentDescription = "Налаштувати",
                modifier = GlanceModifier.size(36.dp),
                colorFilter = ColorFilter.tint(palette.content)
            )
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = "Налаштувати",
                style = TextStyle(
                    color = palette.content,
                    fontSize = 12.sp
                )
            )
        }
    }

    @Composable
    private fun ErrorView(message: String, palette: WidgetPalette) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_home_filled),
                contentDescription = "Помилка",
                modifier = GlanceModifier.size(32.dp),
                colorFilter = ColorFilter.tint(palette.content)
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = message,
                style = TextStyle(
                    color = palette.content,
                    fontSize = 11.sp
                ),
                maxLines = 2
            )
        }
    }

    @Composable
    private fun MinimalView(data: WidgetData.Loaded, palette: WidgetPalette) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // Велика іконка статусу
            val status = data.currentStatus?.status
            StatusIcon(status, 48.dp, palette)
            
            Spacer(GlanceModifier.height(8.dp))
            
            // Короткий статус
            val statusText = when (status) {
                ScheduleStatus.Available -> "Є"
                ScheduleStatus.Outage -> "Немає"
                ScheduleStatus.Probable -> "Можливо"
                else -> "?"
            }
            
            Text(
                text = statusText,
                style = TextStyle(
                    color = palette.content,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
            
            // Час до зміни
            data.nextStatus?.let { next ->
                val timeStr = next.span.split("-")[0].trim()
                
                Text(
                    text = timeStr,
                    style = TextStyle(
                        color = palette.muted,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }

    @Composable
    private fun HorizontalView(data: WidgetData.Loaded, palette: WidgetPalette) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            // Іконка
            val status = data.currentStatus?.status
            StatusIcon(status, 56.dp, palette)
            
            Spacer(GlanceModifier.width(16.dp))
            
            Column {
                // Статус
                val statusText = when (status) {
                    ScheduleStatus.Available -> "Світло є"
                    ScheduleStatus.Outage -> "Відключення"
                    ScheduleStatus.Probable -> "Можливе відкл."
                    else -> "Оновлення..."
                }
                
                Text(
                    text = statusText,
                    style = TextStyle(
                        color = palette.content,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
                
                Spacer(GlanceModifier.height(4.dp))
                
                // Час до наступної зміни
                data.nextStatus?.let { next ->
                    val nextText = when (next.status) {
                        ScheduleStatus.Available -> "Увімкнення"
                        ScheduleStatus.Outage -> "Відключення"
                        else -> "Зміна"
                    }
                    val timeStr = next.span.split("-")[0].trim()
                    
                    Text(
                        text = "$nextText о $timeStr",
                        style = TextStyle(
                            color = palette.muted,
                            fontSize = 12.sp
                        )
                    )
                }
                
                // Адреса
                Text(
                    text = data.addressName,
                    style = TextStyle(
                        color = palette.muted,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }

    @Composable
    private fun LargeView(data: WidgetData.Loaded, palette: WidgetPalette) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            // Велика іконка статусу
            val status = data.currentStatus?.status
            StatusIcon(status, 72.dp, palette)
            
            Spacer(GlanceModifier.height(12.dp))
            
            // Статус текст
            val statusText = when (status) {
                ScheduleStatus.Available -> "Світло є"
                ScheduleStatus.Outage -> "Відключення"
                ScheduleStatus.Probable -> "Можливе відключення"
                else -> "Оновлення..."
            }
            
            Text(
                text = statusText,
                style = TextStyle(
                    color = palette.content,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            
            Spacer(GlanceModifier.height(8.dp))
            
            // Розділювач
            Box(
                modifier = GlanceModifier
                    .width(60.dp)
                    .height(2.dp)
                    .cornerRadius(1.dp)
                        .background(palette.muted)
            ) {}
            
            Spacer(GlanceModifier.height(12.dp))
            
            // Наступна подія
            data.nextStatus?.let { next ->
                val nextText = when (next.status) {
                    ScheduleStatus.Available -> "Увімкнення"
                    ScheduleStatus.Outage -> "Відключення"
                    ScheduleStatus.Probable -> "Можлива зміна"
                    else -> "Зміна"
                }
                val timeStr = next.span.split("-")[0].trim()
                
                Text(
                    text = nextText,
                    style = TextStyle(
                        color = palette.muted,
                        fontSize = 12.sp
                    )
                )
                
                Spacer(GlanceModifier.height(4.dp))
                
                Text(
                    text = "о $timeStr",
                    style = TextStyle(
                        color = palette.content,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                )
            }
            
            Spacer(GlanceModifier.height(12.dp))
            
            // Адреса
            Text(
                text = data.addressName,
                style = TextStyle(
                    color = palette.muted,
                    fontSize = 11.sp
                ),
                maxLines = 2
            )
            
            // Черга
            Text(
                text = "Черга ${data.cherga}.${data.pidcherga}",
                style = TextStyle(
                    color = palette.muted,
                    fontSize = 10.sp
                )
            )
        }
    }

    @Composable
    private fun StatusIcon(status: ScheduleStatus?, size: androidx.compose.ui.unit.Dp, palette: WidgetPalette) {
        val iconRes = when (status) {
            ScheduleStatus.Available -> R.drawable.ic_bolt
            else -> R.drawable.ic_home_filled
        }
        
        val tint = palette.content
        
        val contentDescription = when (status) {
            ScheduleStatus.Outage -> "Відключення електроенергії"
            ScheduleStatus.Available -> "Електроенергія є"
            ScheduleStatus.Probable -> "Ймовірне відключення"
            else -> "Статус невідомий"
        }
        
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(size),
            colorFilter = ColorFilter.tint(tint)
        )
    }
}
