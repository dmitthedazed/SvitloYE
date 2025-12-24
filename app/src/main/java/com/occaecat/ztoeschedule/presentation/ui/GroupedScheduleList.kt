package com.occaecat.ztoeschedule.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.domain.GroupedSchedule

/**
 * Компонент для отображения сгруппированного графика отключений
 */
@Composable
fun GroupedScheduleList(
    groupedSchedule: List<GroupedSchedule>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(groupedSchedule) { schedule ->
            GroupedScheduleItem(schedule = schedule)
        }
    }
}

/**
 * Элемент сгруппированного графика
 */
@Composable
private fun GroupedScheduleItem(
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
                        text = "${schedule.intervalCount} інтервалів",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Альтернативный вариант с круглым индикатором
 */
@Composable
fun GroupedScheduleItemWithCircle(
    schedule: GroupedSchedule,
    modifier: Modifier = Modifier
) {
    val indicatorColor = if (schedule.isLightOn) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    OutlinedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Круглый индикатор
            Surface(
                modifier = Modifier.size(16.dp),
                shape = CircleShape,
                color = indicatorColor
            ) {}

            Spacer(modifier = Modifier.width(16.dp))

            // Временной диапазон и статус
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = schedule.startTime,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " — ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = schedule.endTime,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = schedule.displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (schedule.formattedDuration.isNotEmpty()) {
                        Text(
                            text = " • ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = schedule.formattedDuration,
                            style = MaterialTheme.typography.bodyMedium,
                            color = indicatorColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Компактный вариант для вложенных списков
 */
@Composable
fun CompactGroupedScheduleItem(
    schedule: GroupedSchedule,
    modifier: Modifier = Modifier
) {
    val indicatorColor = if (schedule.isLightOn) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Маленький индикатор
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Время
        Text(
            text = "${schedule.startTime} — ${schedule.endTime}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Продолжительность
        if (schedule.formattedDuration.isNotEmpty()) {
            Text(
                text = schedule.formattedDuration,
                style = MaterialTheme.typography.bodySmall,
                color = indicatorColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

