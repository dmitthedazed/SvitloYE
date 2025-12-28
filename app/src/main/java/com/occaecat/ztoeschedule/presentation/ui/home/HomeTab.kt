package com.occaecat.ztoeschedule.presentation.ui.home

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import android.provider.CalendarContract
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.data.model.ScheduleStatus
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.TimeUtils
import com.occaecat.ztoeschedule.presentation.ui.components.ScaleIndication
import com.occaecat.ztoeschedule.presentation.ui.components.ShimmerItem
import com.occaecat.ztoeschedule.presentation.util.ScheduleImageGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val refreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(is24Hour = true)
    
    val activeGroup = remember(groupedSchedule, currentStatus) {
        val nowMs = System.currentTimeMillis()
        ScheduleMapper.getCurrentGroupedStatus(groupedSchedule, nowMs) ?: currentStatus?.let { s ->
            groupedSchedule.find { it.date == s.date && it.span.contains(s.span.split("-")[0]) }
        }
    }
    val groupedByDate = remember(groupedSchedule) { groupedSchedule.groupBy { it.date } }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { 
            isRefreshing = true
            onRefresh()
            coroutineScope.launch {
                delay(1200)
                isRefreshing = false
            }
        },
        state = refreshState,
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
        indicator = { 
            PullToRefreshDefaults.Indicator(
                state = refreshState, 
                isRefreshing = isRefreshing, 
                modifier = Modifier.align(Alignment.TopCenter), 
                containerColor = MaterialTheme.colorScheme.primaryContainer, 
                color = MaterialTheme.colorScheme.onPrimaryContainer
            ) 
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, 
                top = contentPadding.calculateTopPadding() + 16.dp, 
                end = 16.dp, 
                bottom = contentPadding.calculateBottomPadding() + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading && !isRefreshing) {
                item(contentType = "skeleton") { HomeTabSkeleton() }
            } else {
                if (isOffline) {
                    item(contentType = "banner") {
                        Card(
                            modifier = Modifier.fillMaxWidth().animateItem(), 
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer, 
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ), 
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp), 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CloudOff, null)
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(R.string.error_offline_banner), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                item(contentType = "status_card") {
                    CurrentStatusCard(
                        activeGroup = activeGroup, 
                        currentStatus = currentStatus, 
                        groupedSchedule = groupedSchedule, 
                        onClick = {
                            coroutineScope.launch {
                                val today = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                                val keys = groupedByDate.keys.toList().sortedBy { it.split(".").reversed().joinToString("") }
                                val idx = keys.indexOf(today)
                                if (idx != -1) {
                                    listState.animateScrollToItem(idx * 2 + 2)
                                }
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }

                val fullAddress = buildString {
                    if (cityName.isNotEmpty()) append("$cityName, ")
                    if (streetName.isNotEmpty()) append("$streetName, ")
                    append(addressName)
                }

                item(contentType = "address_card") {
                    Column(modifier = Modifier.animateItem(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AddressInfoCard(cityName, streetName, addressName, cherga, pidcherga, groupedSchedule, { showTimePicker = true })
                        if (lastUpdateTime.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.home_last_updated, lastUpdateTime), 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                groupedByDate.forEach { (date, items) ->
                    stickyHeader(key = date, contentType = "header") {
                        Surface(
                            modifier = Modifier.fillMaxWidth().semantics { heading() }, 
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "🗓 $date", 
                                style = MaterialTheme.typography.titleSmall, 
                                color = MaterialTheme.colorScheme.primary, 
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                            )
                        }
                    }
                    item(key = "${date}_content", contentType = "daily_card") {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().animateItem(), 
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column {
                                items.forEachIndexed { idx, group ->
                                    key(group.span) {
                                        ScheduleListItemSimple(group, (group == activeGroup), fullAddress)
                                        if (idx < items.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp), 
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false }, 
            onConfirm = { showTimePicker = false }
        ) {
            TimeInput(state = timePickerState)
        }
    }
}

@Composable
private fun TimePickerDialog(onDismiss: () -> Unit, onConfirm: () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
        confirmButton = { TextButton(onClick = onConfirm) { Text("OK") } },
        title = { Text("Оберіть час", style = MaterialTheme.typography.titleMedium) },
        text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() } }
    )
}

@Composable
private fun HomeTabSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ShimmerItem(height = 200.dp, shape = MaterialTheme.shapes.extraLarge)
        ShimmerItem(height = 64.dp, shape = MaterialTheme.shapes.large)
        Spacer(Modifier.height(8.dp))
        ShimmerItem(height = 20.dp, modifier = Modifier.width(120.dp).padding(start = 8.dp))
        ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShimmerItem(32.dp, modifier = Modifier.width(4.dp), shape = CircleShape)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ShimmerItem(18.dp, modifier = Modifier.fillMaxWidth(0.7f))
                            ShimmerItem(14.dp, modifier = Modifier.fillMaxWidth(0.4f))
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
    groupedSchedule: List<GroupedSchedule>, 
    onClick: () -> Unit, 
    modifier: Modifier = Modifier
) {
    val status = activeGroup?.status ?: currentStatus?.status
    val hasElectricity = status == ScheduleStatus.Available
    val isWarning = status == ScheduleStatus.Probable
    
    val containerColorByState = when {
        isWarning -> MaterialTheme.colorScheme.tertiaryContainer
        hasElectricity -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColorByState = when {
        isWarning -> MaterialTheme.colorScheme.onTertiaryContainer
        hasElectricity -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }
    
    val containerColor by animateColorAsState(containerColorByState, label = "c")
    val contentColor by animateColorAsState(contentColorByState, label = "ct")
    val infiniteTransition = rememberInfiniteTransition(label = "p")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, 
        targetValue = 1.05f, 
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), 
        label = "s"
    )
    
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .semantics { 
                liveRegion = LiveRegionMode.Assertive 
            }
            .testTag("current_status_card"),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val statusIcon = when (status) {
                ScheduleStatus.Available -> Icons.Default.CheckCircle
                ScheduleStatus.Probable -> Icons.Default.Warning
                else -> Icons.Default.PowerOff
            }
            Icon(
                imageVector = statusIcon, 
                contentDescription = null, 
                modifier = Modifier.size(64.dp).graphicsLayer { 
                    scaleX = pulseScale
                    scaleY = pulseScale 
                }
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = activeGroup?.displayText ?: currentStatus?.displayText ?: stringResource(R.string.home_no_data), 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Bold
            )
            if (activeGroup != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(8.dp))
                    val nextGroup = remember(activeGroup, groupedSchedule) {
                        val idx = groupedSchedule.indexOf(activeGroup)
                        if (idx != -1 && idx < groupedSchedule.size - 1) groupedSchedule[idx + 1] else null
                    }
                    val nextChangeTime = if (nextGroup != null) TimeUtils.formatToSystemTime(LocalContext.current, nextGroup.startTime) else "—"
                    Text(
                        text = if (hasElectricity) stringResource(R.string.home_next_outage, nextChangeTime) else stringResource(R.string.home_next_restore, nextChangeTime), 
                        style = MaterialTheme.typography.bodyLarge, 
                        modifier = Modifier.alpha(0.8f)
                    )
                    Spacer(Modifier.height(20.dp))
                    LiveProgressBar(activeGroup, contentColor, hasElectricity)
                }
            }
        }
    }
}

@Composable
private fun LiveProgressBar(activeGroup: GroupedSchedule, contentColor: Color, hasElectricity: Boolean) {
    val context = LocalContext.current
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeGroup) {
        while (true) {
            delay(1000)
            nowMs = System.currentTimeMillis()
        }
    }
    val progress = remember(activeGroup, nowMs) {
        val dur = activeGroup.endMs - activeGroup.startMs
        if (dur <= 0) 0f else ((nowMs - activeGroup.startMs).toFloat() / dur.toFloat()).coerceIn(0f, 1f)
    }
    val timeRemainingText by remember(activeGroup, nowMs) {
        derivedStateOf {
            val ms = activeGroup.endMs - nowMs
            val rem = if (ms > 0) ms / 60000 else 0
            val h = (rem / 60).toInt()
            val m = (rem % 60).toInt()
            
            val res = context.resources
            val hStr = if (h > 0) res.getQuantityString(R.plurals.hour, h, h) + " " else ""
            val mStr = res.getQuantityString(R.plurals.minute, m, m)
            hStr + mStr
        }
    }
    val animatedProgress by animateFloatAsState(progress, ProgressIndicatorDefaults.ProgressAnimationSpec, label = "pr")
    Column(Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { animatedProgress }, 
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape), 
            color = contentColor, 
            trackColor = contentColor.copy(alpha = 0.2f), 
            strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (hasElectricity) stringResource(R.string.home_time_to_outage, timeRemainingText) else stringResource(R.string.home_time_to_restore, timeRemainingText), 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Medium, 
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressInfoCard(
    cityName: String,
    streetName: String,
    addressName: String,
    cherga: Int,
    pidcherga: Int,
    groupedSchedule: List<GroupedSchedule>,
    onJumpToTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fullAddress = buildString {
        if (cityName.isNotEmpty()) append("$cityName, ")
        if (streetName.isNotEmpty()) append("$streetName, ")
        append(addressName)
    }
    
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), 
        shape = MaterialTheme.shapes.large,
        modifier = modifier.animateContentSize().testTag("address_info_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("Перейти до часу") } },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = onJumpToTime, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            
            Spacer(Modifier.width(4.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fullAddress, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onSurface, 
                    maxLines = 1, 
                    modifier = Modifier.basicMarquee()
                )
            }
            
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("Поділитися") } },
                state = rememberTooltipState()
            ) {
                FilledTonalIconButton(
                    onClick = {
                        scope.launch {
                            val uri = ScheduleImageGenerator.generateAndShare(context, fullAddress, "$cherga.$pidcherga", groupedSchedule)
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri as Parcelable)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Поділитися"))
                            }
                        }
                    }, 
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(Modifier.width(8.dp))
            
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("Копіювати") } },
                state = rememberTooltipState()
            ) {
                val clipboardManager = LocalClipboardManager.current
                FilledTonalIconButton(
                    onClick = {
                        val text = buildString {
                            appendLine("📍 $fullAddress")
                            appendLine("⚡ Черга: $cherga.$pidcherga")
                            appendLine()
                            groupedSchedule.groupBy { it.date }.forEach { (date, items) ->
                                appendLine("🗓 $date:")
                                items.forEach { item ->
                                    val icon = when (item.status) {
                                        ScheduleStatus.Available -> "🟢"
                                        ScheduleStatus.Probable -> "🟡"
                                        else -> "🔴"
                                    }
                                    appendLine("$icon ${item.span} - ${item.displayText}")
                                }
                                appendLine()
                            }
                            append("Сгенеровано додатком СвітлоЄ? Житомир")
                        }
                        clipboardManager.setText(AnnotatedString(text))
                        if (Build.VERSION.SDK_INT < 33) {
                            android.widget.Toast.makeText(context, "Скопійовано", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }, 
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(Modifier.width(8.dp))
            
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleListItemSimple(group: GroupedSchedule, isActive: Boolean, address: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    
    ListItem(
        headlineContent = { 
            Text(
                text = group.displayText, 
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, 
                style = if (isActive) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
            ) 
        },
        supportingContent = { 
            Text(
                text = "${TimeUtils.formatSpanToSystem(context, group.span)} • ${group.formattedDuration}", 
                style = MaterialTheme.typography.bodySmall
            ) 
        },
        leadingContent = { 
            val color = when (group.status) {
                ScheduleStatus.Available -> MaterialTheme.colorScheme.primary
                ScheduleStatus.Probable -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            Surface(modifier = Modifier.size(4.dp, 32.dp), color = color, shape = CircleShape) {} 
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (group.status != ScheduleStatus.Available) {
                    IconButton(
                        onClick = { 
                            try {
                                val kyiv = TimeZone.getTimeZone("Europe/Kyiv")
                                val d = group.date.split(".")
                                val s = group.startTime.split(":")
                                val startCal = Calendar.getInstance(kyiv).apply {
                                    set(d[2].toInt(), d[1].toInt() - 1, d[0].toInt(), s[0].toInt(), s[1].toInt(), 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val e = group.endTime.split(":")
                                val endCal = Calendar.getInstance(kyiv).apply {
                                    set(d[2].toInt(), d[1].toInt() - 1, d[0].toInt(), e[0].toInt(), e[1].toInt(), 0)
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
                            } catch (ex: Exception) { ex.printStackTrace() }
                        }, 
                        modifier = Modifier.size(32.dp)
                    ) { 
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) 
                    }
                }
                if (isActive) {
                    Spacer(Modifier.width(4.dp))
                    SuggestionChip(
                        onClick = {}, 
                        label = { Text(stringResource(R.string.home_status_now)) }, 
                        border = null, 
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer, 
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .indication(interactionSource, ScaleIndication)
            .combinedClickable(
                interactionSource = interactionSource, 
                indication = ripple(), 
                onClick = {}, 
                onLongClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    clipboardManager.setText(AnnotatedString("${group.date}: ${group.span} - ${group.displayText}"))
                    if (Build.VERSION.SDK_INT < 33) {
                        android.widget.Toast.makeText(context, "Скопійовано", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
            .testTag("schedule_slot_${group.startTime}")
    )
}
