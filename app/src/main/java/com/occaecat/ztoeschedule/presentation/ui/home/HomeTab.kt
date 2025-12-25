package com.occaecat.ztoeschedule.presentation.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.TimeUtils
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Home tab - shows current status, address, and schedule.
 * Optimized for Kyiv timezone and grouped progress bars.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    lastUpdateTime: String = ""
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Ticker to force recomposition for time-sensitive UI (updates every second)
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowMs = System.currentTimeMillis()
        }
    }

    // Identify the active grouped block based on CURRENT TIME
    // We re-calculate this whenever 'groupedSchedule' changes or 'nowMs' updates (effectively every second)
    val activeGroup = remember(groupedSchedule, nowMs) {
        ScheduleMapper.getCurrentGroupedStatus(groupedSchedule)
            ?: currentStatus?.let { status ->
                groupedSchedule.find { it.date == status.date && it.span.contains(status.span.split("-")[0]) }
            }
    }

    val groupedByDate = remember(groupedSchedule) {
        groupedSchedule.groupBy { it.date }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefresh()
            coroutineScope.launch {
                delay(1000)
                isRefreshing = false
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Current Status Card (Highest Priority)
            CurrentStatusCard(
                activeGroup = activeGroup,
                currentStatus = currentStatus,
                groupedSchedule = groupedSchedule,
                nowMs = nowMs
            )

            // 2. Address Card and Update Time
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AddressInfoCard(
                    cityName = cityName,
                    streetName = streetName,
                    addressName = addressName,
                    cherga = cherga,
                    pidcherga = pidcherga
                )
                
                if (lastUpdateTime.isNotEmpty()) {
                    Text(
                        text = "Оновлено: $lastUpdateTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // 3. Detailed Schedule List
            groupedByDate.forEach { (date, items) ->
                Text(
                    text = "🗓 $date",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column {
                        items.forEachIndexed { index, group ->
                            val isActive = group == activeGroup
                            
                            ScheduleListItemSimple(
                                group = group,
                                isActive = isActive
                            )

                            if (index < items.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentStatusCard(
    activeGroup: GroupedSchedule?,
    currentStatus: Schedule?,
    groupedSchedule: List<GroupedSchedule>,
    nowMs: Long
) {
    val context = LocalContext.current
    
    // Determine visuals based on active group or fallback to raw status
    val hasElectricity = activeGroup?.isLightOn ?: (currentStatus?.displayText?.contains("Світло є", ignoreCase = true) == true)
    val isWarning = activeGroup?.color?.lowercase() == "yellow" || currentStatus?.color?.lowercase() == "yellow"

    val containerColor = when {
        isWarning -> MaterialTheme.colorScheme.tertiaryContainer 
        hasElectricity -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    
    val contentColor = when {
        isWarning -> MaterialTheme.colorScheme.onTertiaryContainer
        hasElectricity -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (hasElectricity) Icons.Default.CheckCircle else Icons.Default.PowerOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = activeGroup?.displayText ?: currentStatus?.displayText ?: "Немає даних",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Progress and Timer Section (Always visible if we have data)
            if (activeGroup != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(8.dp))

                    val nextGroup = remember(activeGroup, groupedSchedule) {
                        val idx = groupedSchedule.indexOf(activeGroup)
                        if (idx != -1 && idx < groupedSchedule.size - 1) groupedSchedule[idx + 1] else null
                    }
                    
                    val rawNextTime = nextGroup?.startTime ?: "—"
                    val nextChangeTime = if (rawNextTime != "—") TimeUtils.formatToSystemTime(context, rawNextTime) else "—"

                    Text(
                        text = if (hasElectricity) "⏰ Відключення о $nextChangeTime" else "⏰ Увімкнення о $nextChangeTime",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.alpha(0.8f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    val progress = calculateProgressAbsolute(activeGroup, nowMs)
                    val timeRemaining = calculateTimeRemainingAbsolute(activeGroup, nowMs)

                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                        label = "progress"
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth().height(12.dp),
                            color = contentColor,
                            trackColor = contentColor.copy(alpha = 0.2f),
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (hasElectricity) "До відключення: $timeRemaining" else "До увімкнення: $timeRemaining",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

private fun calculateProgressAbsolute(group: GroupedSchedule, nowMs: Long): Float {
    return try {
        val kyivZone = TimeZone.getTimeZone("Europe/Kyiv")
        val startCal = Calendar.getInstance(kyivZone)
        val dateParts = group.date.split(".")
        val timeParts = group.startTime.split(":")
        startCal.set(dateParts[2].toInt(), dateParts[1].toInt() - 1, dateParts[0].toInt(), timeParts[0].toInt(), timeParts[1].toInt(), 0)
        startCal.set(Calendar.MILLISECOND, 0)
        
        val startMs = startCal.timeInMillis
        val durationMs = (group.durationHours * 60 + group.durationMinutes) * 60 * 1000L
        if (durationMs <= 0) return 0f
        
        val elapsedMs = nowMs - startMs
        (elapsedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } catch (e: Exception) { 0f }
}

private fun calculateTimeRemainingAbsolute(group: GroupedSchedule, nowMs: Long): String {
    return try {
        val kyivZone = TimeZone.getTimeZone("Europe/Kyiv")
        val startCal = Calendar.getInstance(kyivZone)
        val dateParts = group.date.split(".")
        val timeParts = group.startTime.split(":")
        startCal.set(dateParts[2].toInt(), dateParts[1].toInt() - 1, dateParts[0].toInt(), timeParts[0].toInt(), timeParts[1].toInt(), 0)
        startCal.set(Calendar.MILLISECOND, 0)
        
        val startMs = startCal.timeInMillis
        val durationMs = (group.durationHours * 60 + group.durationMinutes) * 60 * 1000L
        val endMs = startMs + durationMs
        
        val remainingMs = endMs - nowMs
        if (remainingMs <= 0) return "0хв"
        
        val remainingMinutes = remainingMs / 60000
        val hours = remainingMinutes / 60
        val mins = remainingMinutes % 60
        
        if (hours > 0) "${hours}г ${mins}хв" else "${mins}хв"
    } catch (e: Exception) { "—" }
}

@Composable
private fun AddressInfoCard(
    cityName: String,
    streetName: String,
    addressName: String,
    cherga: Int,
    pidcherga: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))

            val fullAddress = buildString {
                if (cityName.isNotEmpty()) append("$cityName, ")
                if (streetName.isNotEmpty()) append("$streetName, ")
                append(addressName)
            }

            Text(
                text = fullAddress,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = "$cherga.$pidcherga",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun ScheduleListItemSimple(
    group: GroupedSchedule,
    isActive: Boolean
) {
    val context = LocalContext.current
    val hasElectricity = group.isLightOn
    val systemSpan = TimeUtils.formatSpanToSystem(context, group.span)
    
    ListItem(
        headlineContent = {
            Text(text = group.displayText, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, style = if (isActive) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
            Text(text = "$systemSpan • ${group.formattedDuration}", style = MaterialTheme.typography.bodySmall)
        },
        leadingContent = {
            Surface(modifier = Modifier.size(4.dp, 32.dp), color = if (hasElectricity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, shape = CircleShape) {}
        },
        trailingContent = if (isActive) {
            {
                SuggestionChip(
                    onClick = { },
                    label = { Text("Зараз") },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer, labelColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    border = null
                )
            }
        } else null,
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}