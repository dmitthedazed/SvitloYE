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
import com.occaecat.ztoeschedule.presentation.util.DeepLinkHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.BorderStroke


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
    isLoading: Boolean = false,
    streetId: String = "",
    addressId: String = ""
) {
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var highlightTrigger by remember { mutableLongStateOf(0L) }
    
    // Smart time update - triggers exactly when current status ends
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(groupedSchedule) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTimeMs = now
            
            // Find current status and calculate time until it ends
            val current = ScheduleMapper.getCurrentGroupedStatus(groupedSchedule, now)
            val delayMs = if (current != null && current.endMs > now) {
                // Wait until current status ends, then update immediately
                (current.endMs - now).coerceAtLeast(1000L)
            } else {
                // Fallback: check every 10 seconds
                10000L
            }
            
            delay(delayMs)
        }
    }
    
    val activeGroup = remember(groupedSchedule, currentStatus, currentTimeMs) {
        ScheduleMapper.getCurrentGroupedStatus(groupedSchedule, currentTimeMs) ?: currentStatus?.let { s ->
            groupedSchedule.find { it.date == s.date && it.span.contains(s.span.split("-")[0]) }
        }
    }
    val groupedByDate = remember(groupedSchedule) { groupedSchedule.groupBy { it.date } }

    PullToRefreshBox(
        modifier = modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
        state = refreshState,
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefresh()
            coroutineScope.launch {
                delay(1200)
                isRefreshing = false
            }
        },
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
                top = 16.dp, 
                end = 16.dp, 
                bottom = contentPadding.calculateBottomPadding() + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                Text(stringResource(R.string.error_offline_banner), style = MaterialTheme.typography.bodyMedium)
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
                                val targetDate = activeGroup?.date ?: SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                                val sortedDates = groupedByDate.keys.toList().sortedBy { it.split(".").reversed().joinToString("") }
                                val dateIndex = sortedDates.indexOf(targetDate)
                                
                                if (dateIndex != -1) {
                                    val baseOffset = if (isOffline) 3 else 2
                                    val targetIdx = baseOffset + (dateIndex * 2) + 1
                                    listState.animateScrollToItem(targetIdx)
                                    highlightTrigger = System.currentTimeMillis()
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
                        AddressInfoCard(
                            cityName = cityName, 
                            streetName = streetName, 
                            addressName = addressName, 
                            cherga = cherga, 
                            pidcherga = pidcherga, 
                            groupedSchedule = groupedSchedule,
                            streetId = streetId,
                            addressId = addressId,
                            remName = remName
                        )
                        if (lastUpdateTime.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.home_last_updated, lastUpdateTime), 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
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
                        val radius = com.occaecat.ztoeschedule.ui.theme.LocalCornerRadius.current
                        val vPadding = remember(radius) { if (radius > 32) (radius - 32).toFloat() / 3f else 0f }
                        
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().animateItem(), 
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(modifier = Modifier.padding(vertical = vPadding.dp)) {
                                items.forEachIndexed { idx, group ->
                                    key(group.span) {
                                        ScheduleListItemSimple(
                                            group = group, 
                                            isActive = (group == activeGroup), 
                                            address = fullAddress,
                                            highlightTrigger = highlightTrigger
                                        )
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
}

@Composable
private fun HomeTabSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ShimmerItem(height = 200.dp, shape = MaterialTheme.shapes.extraLarge)
        ShimmerItem(height = 64.dp, shape = MaterialTheme.shapes.large)
        Spacer(Modifier.height(8.dp))
        ShimmerItem(height = 20.dp, modifier = Modifier.width(120.dp).padding(start = 8.dp), shape = MaterialTheme.shapes.small)
        ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShimmerItem(32.dp, modifier = Modifier.width(4.dp), shape = CircleShape)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ShimmerItem(18.dp, modifier = Modifier.fillMaxWidth(0.7f), shape = MaterialTheme.shapes.small)
                            ShimmerItem(14.dp, modifier = Modifier.fillMaxWidth(0.4f), shape = MaterialTheme.shapes.small)
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
    val radius = com.occaecat.ztoeschedule.ui.theme.LocalCornerRadius.current
    
    val adaptivePadding = remember(radius) {
        val base = 16f
        val extra = if (radius > 16) (radius - 16).toFloat() / 2f else 0f
        (base + extra).dp
    }

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
            },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(adaptivePadding), 
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
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                    Spacer(Modifier.height(24.dp))
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
            if (ms <= 0) {
                // Status ended - show 0 seconds
                context.resources.getQuantityString(R.plurals.second, 0, 0)
            } else if (ms < 60000) {
                // Less than 1 minute - show seconds
                val seconds = (ms / 1000).toInt().coerceAtLeast(1)
                context.resources.getQuantityString(R.plurals.second, seconds, seconds)
            } else {
                // 1 minute or more - show hours and minutes
                val rem = ms / 60000
                val h = (rem / 60).toInt()
                val m = (rem % 60).toInt()
                
                val res = context.resources
                val hStr = if (h > 0) res.getQuantityString(R.plurals.hour, h, h) + " " else ""
                val mStr = res.getQuantityString(R.plurals.minute, m, m)
                hStr + mStr
            }
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
    modifier: Modifier = Modifier,
    streetId: String = "",
    addressId: String = "",
    remName: String = ""
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val fullAddress = buildString {
        if (cityName.isNotEmpty()) append("$cityName, ")
        if (streetName.isNotEmpty()) append("$streetName, ")
        append(addressName)
    }
    var showShareMenu by remember { mutableStateOf(false) }
    val radius = com.occaecat.ztoeschedule.ui.theme.LocalCornerRadius.current
    val hPadding = remember(radius) {
        val base = 12.0
        val extra = if (radius > 20) (radius - 20).toDouble() / 2.5 else 0.0
        (base + extra).dp
    }
    
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant, 
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = hPadding, vertical = 8.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn, 
                contentDescription = null, 
                modifier = Modifier.size(24.dp), 
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fullAddress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee()
                                )            }

            Box(modifier = Modifier.padding(start = 4.dp)) {
                IconButton(
                    onClick = { showShareMenu = true }, 
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share, 
                        contentDescription = null, 
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                DropdownMenu(
                    expanded = showShareMenu,
                    onDismissRequest = { showShareMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Поділитися графіком (фото)") },
                        leadingIcon = { Icon(Icons.Default.Image, null) },
                        onClick = {
                            showShareMenu = false
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
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Копіювати текст") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                        onClick = {
                            showShareMenu = false
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
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Поділитися посиланням") },
                        leadingIcon = { Icon(Icons.Default.Link, null) },
                        onClick = {
                            showShareMenu = false
                            // Create a temporary SavedAddress object with real IDs for sharing
                            val address = com.occaecat.ztoeschedule.data.model.SavedAddress(
                                id = "",
                                name = addressName,
                                iconName = "",
                                priority = 0,
                                remId = "",
                                remName = remName,
                                cityId = "",
                                cityName = cityName,
                                streetId = streetId,
                                streetName = streetName,
                                addressId = addressId,
                                addressName = addressName,
                                cherga = cherga,
                                pidcherga = pidcherga
                            )
                            DeepLinkHelper.shareLink(context, address)
                        }
                    )
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
private fun ScheduleListItemSimple(
    group: GroupedSchedule, 
    isActive: Boolean, 
    address: String, 
    highlightTrigger: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    
    val highlightAlpha = remember { Animatable(0f) }
    LaunchedEffect(highlightTrigger) {
        if (highlightTrigger > 0 && isActive) {
            repeat(2) {
                highlightAlpha.animateTo(0.4f, tween(400, easing = LinearOutSlowInEasing))
                highlightAlpha.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
            }
        }
    }
    
    val statusColor = when (group.status) {
        ScheduleStatus.Available -> MaterialTheme.colorScheme.primary
        ScheduleStatus.Probable -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    
    val containerColor = if (isActive) {
        statusColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    
    val shape = MaterialTheme.shapes.medium

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = highlightAlpha.value))
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
            .testTag("schedule_slot_${group.startTime}"),
        color = containerColor,
        shape = shape,
        border = if (isActive) BorderStroke(1.dp, statusColor.copy(alpha = 0.38f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon with background
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (group.status) {
                            ScheduleStatus.Available -> Icons.Default.LightMode
                            ScheduleStatus.Probable -> Icons.Default.WarningAmber
                            else -> Icons.Default.FlashOff
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = statusColor
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Time and Status Text
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = TimeUtils.formatSpanToSystem(context, group.span), 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                    
                    // Duration badge
                    val badgeContentColor = when (group.status) {
                        ScheduleStatus.Available -> MaterialTheme.colorScheme.onPrimary
                        ScheduleStatus.Probable -> MaterialTheme.colorScheme.onTertiary
                        else -> MaterialTheme.colorScheme.onError
                    }
                    
                    Surface(
                        color = statusColor,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = group.formattedDuration,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = badgeContentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = group.displayText, 
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            // Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.38f))
                    ) {
                        Text(
                            text = stringResource(R.string.home_status_now),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                
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
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Event, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}