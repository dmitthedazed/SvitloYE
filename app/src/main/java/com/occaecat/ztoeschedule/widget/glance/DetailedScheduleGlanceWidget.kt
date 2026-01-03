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
import com.occaecat.ztoeschedule.domain.GroupedSchedule
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
interface DetailedScheduleWidgetEntryPoint {
    fun widgetDataProvider(): WidgetDataProvider
}

/**
 * Детальний віджет розкладу з MD3 дизайном через Glance
 */
class DetailedScheduleGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(200.dp, 100.dp),  // 4x2
            DpSize(300.dp, 150.dp),  // Larger
            DpSize(300.dp, 200.dp)   // Full details
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext ?: context
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            DetailedScheduleWidgetEntryPoint::class.java
        )
        val dataProvider = entryPoint.widgetDataProvider()
        
        // Отримуємо актуальні дані
        val widgetData = try {
            dataProvider.getWidgetData()
        } catch (e: Exception) {
            WidgetData.Error("Помилка завантаження", null)
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
                .padding(16.dp)
        ) {
            when (data) {
                is WidgetData.NotConfigured -> NotConfiguredContent(palette)
                is WidgetData.Error -> ErrorContent(data.message, palette)
                is WidgetData.Loaded -> {
                    when {
                        size.height >= 180.dp -> DetailedContent(data, palette)
                        size.height >= 120.dp -> MediumContent(data, palette)
                        else -> CompactContent(data, palette)
                    }
                }
            }
        }
    }

    @Composable
    private fun NotConfiguredContent(palette: WidgetPalette) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_bolt),
                contentDescription = "СвітлоЄ?",
                modifier = GlanceModifier.size(48.dp),
                colorFilter = ColorFilter.tint(palette.content)
            )
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = "Налаштуйте віджет",
                style = TextStyle(
                    color = palette.content,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    @Composable
    private fun ErrorContent(message: String, palette: WidgetPalette) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_home_filled),
                contentDescription = "Помилка",
                modifier = GlanceModifier.size(40.dp),
                colorFilter = ColorFilter.tint(palette.content)
            )
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = message,
                style = TextStyle(
                    color = palette.content,
                    fontSize = 12.sp
                )
            )
        }
    }

    @Composable
    private fun StatusIcon(status: com.occaecat.ztoeschedule.data.model.Schedule?, size: androidx.compose.ui.unit.Dp, palette: WidgetPalette) {
        val iconRes = when (status?.status) {
            ScheduleStatus.Available -> R.drawable.ic_bolt
            else -> R.drawable.ic_home_filled
        }

        val contentDescription = when (status?.status) {
            ScheduleStatus.Outage -> "Відключення електроенергії"
            ScheduleStatus.Available -> "Електроенергія є"
            ScheduleStatus.Probable -> "Ймовірне відключення"
            else -> "Статус невідомий"
        }

        Image(
            provider = ImageProvider(iconRes),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(size),
            colorFilter = ColorFilter.tint(palette.content)
        )
    }

    @Composable
    private fun CompactContent(data: WidgetData.Loaded, palette: WidgetPalette) {
        // ... (StatusIcon now takes GroupedSchedule?) No, WidgetData.Loaded holds currentStatus as Schedule? in WidgetDataProvider.
        // Wait, WidgetDataProvider was updated to return GroupedSchedule for currentStatus?
        // Let's check WidgetDataProvider.kt content again.
        // WidgetData.Loaded(val currentStatus: Schedule?, ...) -> No, it was NOT updated to GroupedSchedule for currentStatus.
        // It WAS updated for 'schedules': val schedules: List<GroupedSchedule>
        
        // But currentStatus is Schedule? in WidgetData.Loaded (from previous read of WidgetDataProvider.kt).
        // Let's assume currentStatus is Schedule? and schedules is List<GroupedSchedule>.
        
        // StatusIcon uses Schedule?. So that's fine.
        
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // Статус іконка
            StatusIcon(data.currentStatus, 32.dp, palette)
            
            Spacer(GlanceModifier.width(12.dp))
            
            Column(modifier = GlanceModifier.defaultWeight()) {
                // Назва адреси
                Text(
                    text = data.addressName,
                    style = TextStyle(
                        color = palette.content,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1
                )
                
                // Поточний статус
                val statusText = when (data.currentStatus?.status) {
                    ScheduleStatus.Available -> "Є світло"
                    ScheduleStatus.Outage -> "Відключення"
                    ScheduleStatus.Probable -> "Можливе відкл."
                    else -> "Оновлення..."
                }
                
                Text(
                    text = statusText,
                    style = TextStyle(
                        color = palette.muted,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }

    @Composable
    private fun MediumContent(data: WidgetData.Loaded, palette: WidgetPalette) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Заголовок
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                StatusIcon(data.currentStatus, 32.dp, palette)
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = data.addressName,
                        style = TextStyle(
                            color = palette.content,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "${data.cherga}.${data.pidcherga}",
                        style = TextStyle(
                            color = palette.muted,
                            fontSize = 12.sp
                        )
                    )
                }
            }
            
            Spacer(GlanceModifier.height(12.dp))
            
            // Список найближчих змін (до 3)
            data.schedules.take(3).forEach { schedule ->
                ScheduleItem(schedule, isCompact = true, palette = palette)
                Spacer(GlanceModifier.height(4.dp))
            }
        }
    }

    @Composable
    private fun DetailedContent(data: WidgetData.Loaded, palette: WidgetPalette) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Заголовок
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                StatusIcon(data.currentStatus, 40.dp, palette)
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = data.addressName,
                        style = TextStyle(
                            color = palette.content,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "Черга ${data.cherga}.${data.pidcherga}",
                        style = TextStyle(
                            color = palette.muted,
                            fontSize = 12.sp
                        )
                    )
                }
            }
            
            Spacer(GlanceModifier.height(16.dp))
            
            // Розділювач
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.muted)
            ) {}
            
            Spacer(GlanceModifier.height(12.dp))
            
            // Детальний список змін (до 5)
            data.schedules.take(5).forEach { schedule ->
                ScheduleItem(schedule, isCompact = false, palette = palette)
                Spacer(GlanceModifier.height(8.dp))
            }
        }
    }

    @Composable
    private fun ScheduleItem(schedule: GroupedSchedule, isCompact: Boolean, palette: WidgetPalette) { // Changed to GroupedSchedule
        
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // Індикатор статусу
            val indicatorColor = when (schedule.status) {
                ScheduleStatus.Available -> GlanceTheme.colors.primary
                ScheduleStatus.Outage -> GlanceTheme.colors.error
                ScheduleStatus.Probable -> GlanceTheme.colors.tertiary
                else -> palette.muted
            }
            
            Box(
                modifier = GlanceModifier
                    .size(if (isCompact) 6.dp else 8.dp)
                    .cornerRadius(if (isCompact) 3.dp else 4.dp)
                    .background(indicatorColor)
            ) {}
            
            Spacer(GlanceModifier.width(8.dp))
            
            // Час
            Text(
                text = schedule.startTime, // Use String property
                style = TextStyle(
                    color = palette.content,
                    fontSize = if (isCompact) 12.sp else 14.sp
                )
            )
            
            if (!isCompact) {
                Spacer(GlanceModifier.width(8.dp))
                
                // Статус текст
                val statusText = when (schedule.status) {
                    ScheduleStatus.Available -> "Світло є"
                    ScheduleStatus.Outage -> "Відключення"
                    ScheduleStatus.Probable -> "Можливе"
                    else -> ""
                }
                
                Text(
                    text = statusText,
                    style = TextStyle(
                        color = palette.muted,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
