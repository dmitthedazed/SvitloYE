package com.occaecat.ztoeschedule.presentation.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import kotlinx.coroutines.delay

/**
 * Home tab - shows current address, status, and schedule
 */
@Composable
fun HomeTab(
    remName: String,
    cityName: String,
    streetName: String,
    addressName: String,
    cherga: Int,
    pidcherga: Int,
    currentStatus: Schedule?,
    schedules: List<Schedule>,
    groupedSchedule: List<GroupedSchedule>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-refresh every minute
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000) // 1 minute
            onRefresh()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp) // Content padding only
    ) {
        // Address info card
        AddressInfoCard(
            remName = remName,
            cityName = cityName,
            streetName = streetName,
            addressName = addressName,
            cherga = cherga,
            pidcherga = pidcherga
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Current status card
        CurrentStatusCard(
            currentStatus = currentStatus,
            schedules = schedules
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Daily schedule
        Text(
            text = "📋 Графік на сьогодні",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Schedule list - rendered directly in scrollable column without spacing
        groupedSchedule.forEachIndexed { index, group ->
            val isFirst = index == 0
            val isLast = index == groupedSchedule.lastIndex

            // Check if this group represents the current status
            val isActive = currentStatus?.let { status ->
                // Compare by display text (most reliable)
                val statusType = if (status.displayText.contains("Світло є", ignoreCase = true)) "on" else "off"
                val groupType = if (group.displayText.contains("Світло є", ignoreCase = true)) "on" else "off"

                // Check if types match
                if (statusType != groupType) return@let false

                // Parse times to minutes for comparison
                val statusStartMinutes = try {
                    parseTimeToMinutes(status.span.split("-")[0].trim())
                } catch (e: Exception) { -1 }

                val groupStartMinutes = try {
                    parseTimeToMinutes(group.span.split("-")[0].trim())
                } catch (e: Exception) { -1 }

                val groupEndMinutes = try {
                    parseTimeToMinutes(group.span.split("-")[1].trim())
                } catch (e: Exception) { -1 }

                // Check if status time falls within group's range
                if (groupEndMinutes >= groupStartMinutes) {
                    statusStartMinutes >= groupStartMinutes && statusStartMinutes < groupEndMinutes
                } else {
                    // Handle overnight spans
                    statusStartMinutes >= groupStartMinutes || statusStartMinutes < groupEndMinutes
                }
            } ?: false

            ScheduleListItemSimple(
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

@Composable
private fun AddressInfoCard(
    remName: String,
    cityName: String,
    streetName: String,
    addressName: String,
    cherga: Int,
    pidcherga: Int
) {
    // Simple address line with icons
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Location icon for address
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))

        // Format: "Місто, Вулиця, Будинок"
        val fullAddress = buildString {
            if (cityName.isNotEmpty()) append("$cityName, ")
            if (streetName.isNotEmpty()) append("$streetName, ")
            append(addressName)
        }

        Text(
            text = fullAddress,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Queue info with icon
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$cherga.$pidcherga",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CurrentStatusCard(
    currentStatus: Schedule?,
    schedules: List<Schedule>
) {
    val hasElectricity = currentStatus?.displayText?.contains("Світло є", ignoreCase = true) == true

    // Vibrant accent colors
    val accentGreen = androidx.compose.ui.graphics.Color(0xFF4CAF50) // Material Green 500
    val accentRed = androidx.compose.ui.graphics.Color(0xFFF44336)   // Material Red 500
    val onAccentGreen = androidx.compose.ui.graphics.Color(0xFFFFFFFF) // White text
    val onAccentRed = androidx.compose.ui.graphics.Color(0xFFFFFFFF)   // White text

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge, // Rounded corners
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (hasElectricity) accentGreen else accentRed
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Icon(
                imageVector = if (hasElectricity) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (hasElectricity) onAccentGreen else onAccentRed
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status text
            Text(
                text = currentStatus?.displayText ?: "Немає даних",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (hasElectricity) onAccentGreen else onAccentRed
            )

            if (currentStatus != null && schedules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                // Show next change time instead of current interval
                val now = java.util.Calendar.getInstance()
                val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
                val nextStatus = findNextStatus(schedules, currentMinutes)
                val nextChangeTime = nextStatus?.span?.split("-")?.get(0)?.trim() ?: "—"

                Text(
                    text = if (hasElectricity)
                        "⏰ Відключення о $nextChangeTime"
                    else
                        "⏰ Увімкнення о $nextChangeTime",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (hasElectricity)
                        onAccentGreen.copy(alpha = 0.9f)
                    else
                        onAccentRed.copy(alpha = 0.9f)
                )
            }

            // Progress bar with animation
            if (currentStatus != null && schedules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                val progress = calculateProgress(currentStatus, schedules)
                val timeRemaining = calculateTimeRemaining(currentStatus, schedules)

                // Animate progress changes smoothly
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    label = "progress"
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp), // Thicker progress bar
                        color = if (hasElectricity) onAccentGreen else onAccentRed,
                        trackColor = if (hasElectricity)
                            onAccentGreen.copy(alpha = 0.3f)
                        else
                            onAccentRed.copy(alpha = 0.3f),
                        strokeCap = StrokeCap.Round // Rounded ends
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (hasElectricity)
                            "До відключення: $timeRemaining"
                        else
                            "До увімкнення: $timeRemaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasElectricity)
                            onAccentGreen.copy(alpha = 0.9f)
                        else
                            onAccentRed.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}


private fun calculateProgress(currentStatus: Schedule, schedules: List<Schedule>): Float {
    if (schedules.isEmpty()) return 0f

    val now = java.util.Calendar.getInstance()
    val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)

    try {
        // Find next status change
        val nextStatus = findNextStatus(schedules, currentMinutes)
        if (nextStatus == null) return 0f

        // Get current status start time
        val currentStart = parseTimeToMinutes(currentStatus.span.split("-")[0].trim())

        // Get next status start time (which is when current status ends)
        val nextStart = parseTimeToMinutes(nextStatus.span.split("-")[0].trim())

        // Calculate total duration until next change
        val totalDuration = if (nextStart >= currentStart) {
            nextStart - currentStart
        } else {
            (24 * 60 - currentStart) + nextStart
        }

        // Calculate elapsed time
        val elapsed = if (currentMinutes >= currentStart) {
            currentMinutes - currentStart
        } else {
            (24 * 60 - currentStart) + currentMinutes
        }

        return (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
    } catch (_: Exception) {
        return 0f
    }
}

private fun calculateTimeRemaining(@Suppress("UNUSED_PARAMETER") currentStatus: Schedule, schedules: List<Schedule>): String {
    if (schedules.isEmpty()) return "—"

    val now = java.util.Calendar.getInstance()
    val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)

    try {
        // Find next status (when current ends)
        val nextStatus = findNextStatus(schedules, currentMinutes)
        if (nextStatus == null) return "—"

        // Get next status start time
        val nextStart = parseTimeToMinutes(nextStatus.span.split("-")[0].trim())

        // Calculate remaining time
        val remaining = if (nextStart >= currentMinutes) {
            nextStart - currentMinutes
        } else {
            (24 * 60 - currentMinutes) + nextStart
        }

        val hours = remaining / 60
        val minutes = remaining % 60

        return if (hours > 0) {
            "${hours}г ${minutes}хв"
        } else {
            "${minutes}хв"
        }
    } catch (_: Exception) {
        return "—"
    }
}

private fun findNextStatus(schedules: List<Schedule>, currentMinutes: Int): Schedule? {
    if (schedules.isEmpty()) return null

    // Sort schedules by start time
    val sortedSchedules = schedules.sortedBy {
        parseTimeToMinutes(it.span.split("-")[0].trim())
    }

    // Find current status
    val currentStatus = sortedSchedules.firstOrNull { schedule ->
        val span = schedule.span.split("-")
        val startTime = parseTimeToMinutes(span[0].trim())
        val endTime = parseTimeToMinutes(span[1].trim())

        if (endTime >= startTime) {
            currentMinutes >= startTime && currentMinutes < endTime
        } else {
            currentMinutes >= startTime || currentMinutes < endTime
        }
    }

    if (currentStatus == null) return sortedSchedules.firstOrNull()

    // Get current status type (electricity on/off)
    val currentHasElectricity = currentStatus.displayText.contains("Світло є", ignoreCase = true)

    // Find next status with DIFFERENT type
    val currentIndex = sortedSchedules.indexOf(currentStatus)

    // Search forward from current position
    for (i in currentIndex + 1 until sortedSchedules.size) {
        val nextHasElectricity = sortedSchedules[i].displayText.contains("Світло є", ignoreCase = true)
        if (nextHasElectricity != currentHasElectricity) {
            return sortedSchedules[i]
        }
    }

    // If not found, search from beginning (next day)
    for (schedule in sortedSchedules) {
        val nextHasElectricity = schedule.displayText.contains("Світло є", ignoreCase = true)
        if (nextHasElectricity != currentHasElectricity) {
            return schedule
        }
    }

    return null
}

private fun parseTimeToMinutes(time: String): Int {
    val parts = time.split(":")
    return parts[0].toInt() * 60 + parts[1].toInt()
}

@Composable
private fun ScheduleListItemSimple(
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
        isFirst -> androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp
        )
        isLast -> androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp
        )
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
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    "${group.span} • ${group.formattedDuration}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                Surface(
                    modifier = Modifier.size(4.dp, 40.dp),
                    color = if (hasElectricity) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
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
