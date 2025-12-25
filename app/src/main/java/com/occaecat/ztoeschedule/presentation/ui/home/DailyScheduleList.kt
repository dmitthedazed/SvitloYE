package com.occaecat.ztoeschedule.presentation.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.domain.GroupedSchedule

@Composable
fun DailyScheduleList(
    groupedSchedule: List<GroupedSchedule>,
    currentStatus: Schedule?,
    modifier: Modifier = Modifier
) {
    if (groupedSchedule.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "Немає графіку на сьогодні",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(groupedSchedule) { index, group ->
                val isFirst = index == 0
                val isLast = index == groupedSchedule.size - 1

                // Check if this group is currently active
                val isActive = currentStatus?.let { status ->
                    status.date == group.date && group.span.contains(status.span.split("-")[0].trim())
                } ?: false

                ScheduleListItem(
                    group = group,
                    isActive = isActive,
                    isFirst = isFirst,
                    isLast = isLast
                )

                if (!isLast) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ScheduleListItem(
    group: GroupedSchedule,
    isActive: Boolean,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    val hasElectricity = group.displayText.contains("Світло є", ignoreCase = true)

    val backgroundColor = if (hasElectricity) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val onAccentGreen = MaterialTheme.colorScheme.onTertiaryContainer
    val onAccentRed = MaterialTheme.colorScheme.onErrorContainer

    val shape = when {
        isFirst && isLast -> MaterialTheme.shapes.large
        isFirst -> androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        isLast -> androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
    }

    Surface(
        shape = shape,
        color = backgroundColor
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = group.displayText,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            },
            supportingContent = {
                Text("${group.span} • ${group.formattedDuration}")
            },
            leadingContent = {
                Surface(
                    modifier = Modifier.size(4.dp, 40.dp),
                    color = if (hasElectricity) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    shape = MaterialTheme.shapes.small
                ) {}
            },
            trailingContent = if (isActive) {
                {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Зараз",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else null,
            colors = ListItemDefaults.colors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                headlineColor = if (hasElectricity) onAccentGreen else onAccentRed,
                supportingColor = if (hasElectricity) onAccentGreen else onAccentRed
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}