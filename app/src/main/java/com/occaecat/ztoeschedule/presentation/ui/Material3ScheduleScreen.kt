package com.occaecat.ztoeschedule.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import java.util.Calendar

/**
 * Современный Material 3 экран графика отключений
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3ScheduleScreen(
    selectedAddress: String,
    currentStatus: Schedule?,
    schedules: List<Schedule>,
    groupedSchedule: List<GroupedSchedule>,
    formattedMessage: String,
    cherga: Int,
    pidcherga: Int,
    onRefresh: () -> Unit,
    onChangeAddress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onChangeAddress() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedAddress,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Уведомления */ }) {
                        Icon(Icons.Default.Notifications, "Сповіщення")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Большая статус-карточка
            item {
                currentStatus?.let { status ->
                    StatusCard(status = status, schedules = schedules)
                }
            }

            // Предупреждающее сообщение
            if (formattedMessage.isNotBlank()) {
                item {
                    WarningCard(message = formattedMessage)
                }
            }

            // Кнопки навигации
            item {
                NavigationButtons(cherga = cherga, pidcherga = pidcherga)
            }

            // Заголовок графика
            item {
                Text(
                    text = "Повний графік (згруповано)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Сгруппированный список графика
            items(groupedSchedule) { schedule ->
                GroupedScheduleItemCard(schedule = schedule)
            }
        }
    }
}

/**
 * Большая карточка текущего статуса
 */
@Composable
private fun StatusCard(
    status: Schedule,
    schedules: List<Schedule>
) {
    val isLightOn = status.color.lowercase() in listOf("white", "green")

    // Цвета Material 3
    val containerColor = if (isLightOn) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    val contentColor = if (isLightOn) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    val (progress, timeRemaining) = calculateProgress(status)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Иконка
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isLightOn) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = contentColor
                    )
                }
            }

            // Статус
            Text(
                text = if (isLightOn) "Світло є" else "Відключення",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            // Оставшееся время
            if (timeRemaining.isNotBlank()) {
                OutlinedCard(
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Залишилось у поточному вікні:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = timeRemaining,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Прогресс
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.small),
                    color = contentColor,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = status.span.split("-").firstOrNull() ?: "",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = status.span.split("-").lastOrNull() ?: "",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/**
 * Предупреждающая карточка
 */
@Composable
private fun WarningCard(message: String) {
    val isAttention = message.contains("УВАГА", ignoreCase = true)

    val containerColor = if (isAttention) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isAttention) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isAttention) "Увага! Важлива інформація" else "Інформація",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Кнопки навигации
 */
@Composable
private fun NavigationButtons(
    cherga: Int,
    pidcherga: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // График
        FilledTonalButton(
            onClick = { /* График */ },
            modifier = Modifier
                .weight(1f)
                .height(80.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Графік",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "на сьогодні",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Група
        FilledTonalButton(
            onClick = { /* Группа */ },
            modifier = Modifier
                .weight(1f)
                .height(80.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Група $cherga.$pidcherga",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "Ваша черга",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Элемент сгруппированного графика с вертикальной цветной полосой
 */
@Composable
private fun GroupedScheduleItemCard(
    schedule: GroupedSchedule,
    modifier: Modifier = Modifier
) {
    val indicatorColor = if (schedule.isLightOn) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    val containerColor = if (schedule.isLightOn) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Вертикальная цветная полоса
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(indicatorColor)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Основная информация
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Временной диапазон
                Text(
                    text = "${schedule.startTime} — ${schedule.endTime}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Статус
                Text(
                    text = schedule.displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Продолжительность
            if (schedule.formattedDuration.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = schedule.formattedDuration,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = indicatorColor
                    )

                    Text(
                        text = "${schedule.intervalCount}×30хв",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Вычисляет прогресс и оставшееся время
 */
private fun calculateProgress(currentStatus: Schedule): Pair<Float, String> {
    try {
        val times = currentStatus.span.split("-")
        if (times.size != 2) return Pair(0f, "")

        fun parse(s: String): Int {
            val p = s.trim().split(":")
            return p[0].toInt() * 60 + p[1].toInt()
        }

        val start = parse(times[0])
        var end = parse(times[1])
        if (end <= start) end += 1440

        val now = Calendar.getInstance()
        var current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        if (current < start && end > 1440) current += 1440

        val totalMinutes = (end - start).toFloat()
        val elapsedMinutes = (current - start).toFloat()

        val progress = (elapsedMinutes / totalMinutes).coerceIn(0f, 1f)

        val remainingMinutes = (totalMinutes - elapsedMinutes).toLong()
        val hours = remainingMinutes / 60
        val minutes = remainingMinutes % 60

        val timeRemaining = when {
            hours > 0 -> "$hours год $minutes хв"
            else -> "$minutes хв"
        }

        return Pair(progress, timeRemaining)
    } catch (_: Exception) {
        return Pair(0f, "")
    }
}