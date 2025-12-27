package com.occaecat.ztoeschedule.presentation.ui.home

import android.content.Intent
import android.provider.CalendarContract
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.TimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

import com.occaecat.ztoeschedule.presentation.ui.components.ShimmerItem
import com.occaecat.ztoeschedule.presentation.util.ScheduleImageGenerator
import androidx.compose.ui.graphics.Color
import android.os.Parcelable
import androidx.compose.runtime.saveable.rememberSaveable

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
    lastUpdateTime: String = "",
    isOffline: Boolean = false,
    isLoading: Boolean = false
) {
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Identify the active grouped block. 
    // We use current time only for initial identification, avoiding constant recomposition.
    val activeGroup = remember(groupedSchedule, currentStatus) {
        val nowMs = System.currentTimeMillis()
        ScheduleMapper.getCurrentGroupedStatus(groupedSchedule, nowMs)
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
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter // Center the content column
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp) // Standard M3 max width for readability
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading && !isRefreshing) {
                HomeTabSkeleton()
            } else {
                if (isOffline) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.error_offline_banner),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // 1. Current Status Card (Highest Priority)
                CurrentStatusCard(
                    activeGroup = activeGroup,
                    currentStatus = currentStatus,
                    groupedSchedule = groupedSchedule
                )

                // 2. Address Card and Update Time
                val fullAddressString = buildString {
                    if (cityName.isNotEmpty()) append("$cityName, ")
                    if (streetName.isNotEmpty()) append("$streetName, ")
                    append(addressName)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AddressInfoCard(
                        cityName = cityName,
                        streetName = streetName,
                        addressName = addressName,
                        cherga = cherga,
                        pidcherga = pidcherga,
                        groupedSchedule = groupedSchedule
                    )
                    
                    if (lastUpdateTime.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.home_last_updated, lastUpdateTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // 4. Detailed Schedule List
                groupedByDate.forEach { (date, items) ->
                    key(date) {
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
                                    key(group.span) {
                                        val isActive = group == activeGroup
                                        
                                        ScheduleListItemSimple(
                                            group = group,
                                            isActive = isActive,
                                            address = fullAddressString
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
        }
    }
}

@Composable
private fun HomeTabSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Status Card Skeleton
        ShimmerItem(height = 200.dp, shape = MaterialTheme.shapes.extraLarge)
        
        // Address Info Skeleton
        ShimmerItem(height = 64.dp, shape = MaterialTheme.shapes.large)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Schedule List Header Skeleton
        ShimmerItem(height = 20.dp, modifier = Modifier.width(120.dp).padding(start = 8.dp))
        
        // Schedule Items Skeleton
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShimmerItem(height = 32.dp, modifier = Modifier.width(4.dp), shape = CircleShape)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ShimmerItem(height = 18.dp, modifier = Modifier.fillMaxWidth(0.7f))
                            ShimmerItem(height = 14.dp, modifier = Modifier.fillMaxWidth(0.4f))
                        }
                    }
                    if (it < 3) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
private fun CurrentStatusCard(
    activeGroup: GroupedSchedule?,
    currentStatus: Schedule?,
    groupedSchedule: List<GroupedSchedule>
) {
    val context = LocalContext.current
    val status = activeGroup?.status ?: currentStatus?.status
    
    val hasElectricity = status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.AVAILABLE
    val isWarning = status == com.occaecat.ztoeschedule.data.model.ScheduleStatus.PROBABLE

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
            val statusIcon = when (status) {
                com.occaecat.ztoeschedule.data.model.ScheduleStatus.AVAILABLE -> Icons.Default.CheckCircle
                com.occaecat.ztoeschedule.data.model.ScheduleStatus.PROBABLE -> Icons.Default.Warning
                else -> Icons.Default.PowerOff
            }
            
            val statusDescription = when (status) {
                com.occaecat.ztoeschedule.data.model.ScheduleStatus.AVAILABLE -> stringResource(R.string.status_available)
                com.occaecat.ztoeschedule.data.model.ScheduleStatus.PROBABLE -> stringResource(R.string.status_probable)
                else -> stringResource(R.string.status_outage)
            }

            Icon(
                imageVector = statusIcon,
                contentDescription = statusDescription,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = activeGroup?.displayText ?: currentStatus?.displayText ?: stringResource(R.string.home_no_data),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

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
                        text = if (hasElectricity) stringResource(R.string.home_next_outage, nextChangeTime) 
                               else stringResource(R.string.home_next_restore, nextChangeTime),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.alpha(0.8f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    LiveProgressBar(
                        activeGroup = activeGroup,
                        contentColor = contentColor,
                        hasElectricity = hasElectricity
                    )
                }
            }
        }
    }
}

/**
 * Isolated progress bar component that handles its own time updates.
 * This prevents the entire CurrentStatusCard from recomposing every second.
 */
@Composable
private fun LiveProgressBar(
    activeGroup: GroupedSchedule,
    contentColor: Color,
    hasElectricity: Boolean
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // Local ticker only for this component
    LaunchedEffect(activeGroup) {
        while (true) {
            delay(1000)
            nowMs = System.currentTimeMillis()
        }
    }

    val progress = remember(activeGroup, nowMs) {
        calculateProgressAbsolute(activeGroup, nowMs)
    }
    
    val timeRemaining = remember(activeGroup, nowMs) {
        calculateTimeRemainingAbsolute(activeGroup, nowMs)
    }

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
            text = if (hasElectricity) stringResource(R.string.home_time_to_outage, timeRemaining) 
                   else stringResource(R.string.home_time_to_restore, timeRemaining),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

private fun calculateProgressAbsolute(group: GroupedSchedule, nowMs: Long): Float {
    val durationMs = group.endMs - group.startMs
    if (durationMs <= 0) return 0f
    
    val elapsedMs = nowMs - group.startMs
    return (elapsedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

private fun calculateTimeRemainingAbsolute(group: GroupedSchedule, nowMs: Long): String {
    val remainingMs = group.endMs - nowMs
    if (remainingMs <= 0) return "0хв"
    
    val remainingMinutes = remainingMs / 60000
    val hours = remainingMinutes / 60
    val mins = remainingMinutes % 60
    
    return if (hours > 0) "${hours}г ${mins}хв" else "${mins}хв"
}

@Composable
private fun AddressInfoCard(
    cityName: String,
    streetName: String,
    addressName: String,
    cherga: Int,
    pidcherga: Int,
    groupedSchedule: List<GroupedSchedule>
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fullAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = {
                    scope.launch {
                        val uri = ScheduleImageGenerator.generateAndShare(
                            context,
                            fullAddress,
                            "$cherga.$pidcherga",
                            groupedSchedule
                        )
                        if (uri != null) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri as Parcelable)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Поділитися графіком"))
                        }
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Поділитися зображенням",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = {
                    val text = formatScheduleForSharing(fullAddress, cherga, pidcherga, groupedSchedule)
                    clipboardManager.setText(AnnotatedString(text))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Копіювати графік",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

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

private fun formatScheduleForSharing(
    address: String,
    cherga: Int,
    pidcherga: Int,
    schedules: List<GroupedSchedule>
): String = buildString {
    appendLine("📍 $address")
    appendLine("⚡ Черга: $cherga.$pidcherga")
    appendLine()
    
    val groupedByDate = schedules.groupBy { it.date }
    groupedByDate.forEach { (date, items) ->
        appendLine("🗓 $date:")
        items.forEach { item ->
            val icon = when (item.status) {
                com.occaecat.ztoeschedule.data.model.ScheduleStatus.AVAILABLE -> "🟢"
                com.occaecat.ztoeschedule.data.model.ScheduleStatus.PROBABLE -> "🟡"
                else -> "🔴"
            }
            appendLine("$icon ${item.span} - ${item.displayText}")
        }
        appendLine()
    }
    append("Сгенеровано додатком СвітлоЄ? Житомир")
}

@Composable
private fun ScheduleListItemSimple(
    group: GroupedSchedule,
    isActive: Boolean,
    address: String
) {
    val context = LocalContext.current
    val systemSpan = TimeUtils.formatSpanToSystem(context, group.span)
    
    ListItem(
        headlineContent = {
            Text(text = group.displayText, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, style = if (isActive) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
            Text(text = "$systemSpan • ${group.formattedDuration}", style = MaterialTheme.typography.bodySmall)
        },
        leadingContent = {
            val indicatorColor = when (group.status) {
                com.occaecat.ztoeschedule.data.model.ScheduleStatus.AVAILABLE -> MaterialTheme.colorScheme.primary
                com.occaecat.ztoeschedule.data.model.ScheduleStatus.PROBABLE -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            Surface(modifier = Modifier.size(4.dp, 32.dp), color = indicatorColor, shape = CircleShape) {}
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (group.status != com.occaecat.ztoeschedule.data.model.ScheduleStatus.AVAILABLE) {
                    IconButton(
                        onClick = { addOutageToCalendar(context, group, address) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Додати в календар",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                
                if (isActive) {
                    Spacer(Modifier.width(4.dp))
                    SuggestionChip(
                        onClick = { },
                        label = { Text(stringResource(R.string.home_status_now)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer, labelColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        border = null
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}

private fun addOutageToCalendar(context: android.content.Context, group: GroupedSchedule, address: String) {
    try {
        val kyivZone = TimeZone.getTimeZone("Europe/Kyiv")
        val dateParts = group.date.split(".")
        
        val startParts = group.startTime.split(":")
        val startCal = Calendar.getInstance(kyivZone).apply {
            set(dateParts[2].toInt(), dateParts[1].toInt() - 1, dateParts[0].toInt(), startParts[0].toInt(), startParts[1].toInt(), 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endParts = group.endTime.split(":")
        val endCal = Calendar.getInstance(kyivZone).apply {
            set(dateParts[2].toInt(), dateParts[1].toInt() - 1, dateParts[0].toInt(), endParts[0].toInt(), endParts[1].toInt(), 0)
            set(Calendar.MILLISECOND, 0)
            if (group.endTime == "00:00" || timeInMillis <= startCal.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Відключення світла 🔴")
            putExtra(CalendarContract.Events.DESCRIPTION, "Заплановане відключення за адресою: $address")
            putExtra(CalendarContract.Events.EVENT_LOCATION, address)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startCal.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endCal.timeInMillis)
            putExtra(CalendarContract.Events.ALL_DAY, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}