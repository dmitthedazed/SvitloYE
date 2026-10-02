package com.occaecat.ztoeschedule.presentation.ui.home

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import android.provider.CalendarContract
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.data.model.ScheduleStatus
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.TimeUtils
import com.occaecat.ztoeschedule.presentation.ui.addresses.QRAddressData
import com.occaecat.ztoeschedule.presentation.ui.components.ScaleIndication
import com.occaecat.ztoeschedule.presentation.ui.components.ShimmerItem
import com.occaecat.ztoeschedule.presentation.util.ScheduleImageGenerator
import com.occaecat.ztoeschedule.presentation.util.DeepLinkHelper
import com.occaecat.ztoeschedule.presentation.util.QrCodeGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeTab(
    remId: String = "",
    cityId: String = "",
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
    addressId: String = "",
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    iconName: String = "",
    isPrimary: Boolean = false
) {
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()
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
    val allDayAvailableDates = remember(schedules, groupedSchedule) {
        findAllDayAvailableDates(schedules, groupedSchedule)
    }
    val todayDate = remember(currentTimeMs) { formatScheduleDate(currentTimeMs) }
    val isAllDayAvailableToday = remember(allDayAvailableDates, todayDate) {
        todayDate in allDayAvailableDates
    }
    val visibleGroupedByDate = remember(groupedByDate, allDayAvailableDates) {
        groupedByDate.filterKeys { it !in allDayAvailableDates }
    }
    
    // Bottom Sheet State
    var selectedGroupForMenu by remember { mutableStateOf<GroupedSchedule?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // Content scrolls under the (translucent, blurred) top bar, so padding goes into the list
    PullToRefreshBox(
        modifier = modifier
            .fillMaxSize(),
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
            PullToRefreshDefaults.LoadingIndicator(
                state = refreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = contentPadding.calculateTopPadding()),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                color = MaterialTheme.colorScheme.onSurface
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
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading && !isRefreshing && groupedSchedule.isEmpty()) {
                item(contentType = "skeleton") { HomeTabSkeleton() }
            } else {
                item(contentType = "status_card") {
                    CurrentStatusCard(
                        activeGroup = activeGroup, 
                        currentStatus = currentStatus, 
                        groupedSchedule = groupedSchedule,
                        isAllDayAvailableToday = isAllDayAvailableToday,
                        cherga = cherga,
                        pidcherga = pidcherga,
                        onClick = {
                            coroutineScope.launch {
                                val targetDate = activeGroup?.date ?: todayDate
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
                    AddressInfoCard(
                        cityName = cityName, 
                        streetName = streetName, 
                        addressName = addressName, 
                        cherga = cherga, 
                        pidcherga = pidcherga, 
                        groupedSchedule = groupedSchedule,
                        lastUpdateTime = lastUpdateTime,
                        cityId = cityId,
                        streetId = streetId,
                        addressId = addressId,
                        remId = remId,
                        remName = remName,
                        iconName = iconName,
                        isPrimary = isPrimary,
                        modifier = Modifier.animateItem()
                    )
                }

                visibleGroupedByDate.forEach { (date, items) ->
                    // Not sticky: a pinned header would hide behind the translucent top bar
                    item(key = date, contentType = "header") {
                        DayHeader(
                            date = date,
                            todayDate = todayDate,
                            items = items,
                            modifier = Modifier.semantics { heading() }
                        )
                    }
                    item(key = "${date}_content", contentType = "daily_card") {
                        Column(
                            modifier = Modifier.animateItem(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            DayTimelineBar(
                                items = items,
                                nowMs = if (date == todayDate) currentTimeMs else null
                            )
                            items.forEachIndexed { idx, group ->
                                key(group.span) {
                                    ScheduleListItemSimple(
                                        group = group, 
                                        isActive = (group == activeGroup), 
                                        isPast = group.endMs <= currentTimeMs,
                                        address = fullAddress,
                                        highlightTrigger = highlightTrigger,
                                        index = idx + 1,
                                        totalCount = items.size + 1,
                                        onLongClick = { selectedGroupForMenu = group }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedGroupForMenu != null) {
        val group = selectedGroupForMenu!!
        val context = LocalContext.current
        val fullAddress = buildString {
            if (cityName.isNotEmpty()) append("$cityName, ")
            if (streetName.isNotEmpty()) append("$streetName, ")
            append(addressName)
        }
        val dismiss: () -> Unit = {
            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { selectedGroupForMenu = null }
        }

        ModalBottomSheet(
            onDismissRequest = { selectedGroupForMenu = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            IntervalActionsSheet(
                group = group,
                todayDate = todayDate,
                isActive = group == activeGroup,
                onAddToCalendar = { addToCalendar(context, group, fullAddress); dismiss() },
                onCopy = { copyToClipboard(context, group, fullAddress); dismiss() },
                onShare = { shareInterval(context, group, fullAddress); dismiss() }
            )
        }
    }
}

/** Long-press sheet: what this interval is, then what you can do with it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IntervalActionsSheet(
    group: GroupedSchedule,
    todayDate: String,
    isActive: Boolean,
    onAddToCalendar: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val (dayPrimary, daySecondary) = remember(group.date, todayDate) { dayLabels(group.date, todayDate) }
    val (container, content, shape, icon) = when (group.status) {
        ScheduleStatus.Available -> SheetStyle(colorScheme.surfaceContainerHighest, colorScheme.onSurface, MaterialShapes.Sunny, Icons.Default.LightMode)
        ScheduleStatus.Probable -> SheetStyle(colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer, MaterialShapes.Clover4Leaf, Icons.Default.WarningAmber)
        else -> SheetStyle(colorScheme.errorContainer, colorScheme.onErrorContainer, MaterialShapes.Cookie4Sided, Icons.Default.PowerOff)
    }
    val calendarTitle = if (group.status == ScheduleStatus.Available) "Додати в календар" else "Нагадати в календарі"
    val calendarSubtitle = if (group.status == ScheduleStatus.Available) "Подія на час, коли світло є" else "Подія з часом відключення та адресою"

    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        Surface(color = container, contentColor = content, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(56.dp)
                        .background(content, shape.toShape()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = container, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = listOf(dayPrimary, daySecondary).filter { it.isNotEmpty() }.joinToString(", "),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.alpha(0.8f)
                    )
                    Text(
                        text = TimeUtils.formatSpanToSystem(context, group.span),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${group.displayText} · ${group.formattedDuration}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (isActive) {
                    Surface(color = content, contentColor = container, shape = CircleShape) {
                        Text(
                            stringResource(R.string.home_status_now),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        val actions = listOf(
            Triple(Icons.Default.Event, calendarTitle to calendarSubtitle, onAddToCalendar),
            Triple(Icons.Default.Share, "Поділитися" to "Надіслати інтервал з адресою", onShare),
            Triple(Icons.Default.ContentCopy, "Скопіювати" to "Текст для месенджера", onCopy)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            actions.forEachIndexed { index, (actionIcon, labels, action) ->
                com.occaecat.ztoeschedule.presentation.ui.components.SettingsGroupItem(
                    index = index,
                    totalCount = actions.size,
                    headlineContent = { Text(labels.first, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(labels.second) },
                    leadingContent = { com.occaecat.ztoeschedule.presentation.ui.components.StepLeadingIcon(actionIcon) },
                    onClick = action
                )
            }
        }
    }
}

private data class SheetStyle(
    val container: Color,
    val content: Color,
    val shape: androidx.graphics.shapes.RoundedPolygon,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun HomeTabSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ShimmerItem(height = 200.dp, shape = MaterialTheme.shapes.extraLarge)
        ShimmerItem(height = 64.dp, shape = MaterialTheme.shapes.large)
        Spacer(Modifier.height(8.dp))
        ShimmerItem(height = 20.dp, modifier = Modifier.width(120.dp).padding(start = 8.dp), shape = MaterialTheme.shapes.small)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
             repeat(4) { index ->
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    3 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                    else -> RoundedCornerShape(4.dp)
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShimmerItem(32.dp, modifier = Modifier.width(4.dp), shape = CircleShape)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ShimmerItem(18.dp, modifier = Modifier.fillMaxWidth(0.7f), shape = MaterialTheme.shapes.small)
                            ShimmerItem(14.dp, modifier = Modifier.fillMaxWidth(0.4f), shape = MaterialTheme.shapes.small)
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
    isAllDayAvailableToday: Boolean,
    cherga: Int,
    pidcherga: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = activeGroup?.status ?: currentStatus?.status
    val hasElectricity = status == ScheduleStatus.Available
    val isWarning = status == ScheduleStatus.Probable
    val hideLiveTiming = hasElectricity && isAllDayAvailableToday
    
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
    val displayMode = com.occaecat.ztoeschedule.ui.theme.LocalDisplayMode.current
    val cardShape = RoundedCornerShape(28.dp)

    val cardPadding = when (displayMode) {
        com.occaecat.ztoeschedule.data.model.DisplayMode.Compact -> 16.dp
        com.occaecat.ztoeschedule.data.model.DisplayMode.Comfortable -> 20.dp
        com.occaecat.ztoeschedule.data.model.DisplayMode.Spacious -> 24.dp
    }
    val statusIconSize = when (displayMode) {
        com.occaecat.ztoeschedule.data.model.DisplayMode.Compact -> 48.dp
        com.occaecat.ztoeschedule.data.model.DisplayMode.Comfortable -> 64.dp
        com.occaecat.ztoeschedule.data.model.DisplayMode.Spacious -> 80.dp
    }

    val statusShape = when (status) {
        ScheduleStatus.Available -> MaterialShapes.Sunny
        ScheduleStatus.Probable -> MaterialShapes.Clover4Leaf
        else -> MaterialShapes.Cookie4Sided
    }.toShape()

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .semantics {
                liveRegion = LiveRegionMode.Assertive
            },
        shape = cardShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(cardPadding)) {
            val statusIcon = when (status) {
                ScheduleStatus.Available -> Icons.Default.LightMode
                ScheduleStatus.Probable -> Icons.Default.WarningAmber
                else -> Icons.Default.PowerOff
            }
            val statusDescription = when (status) {
                ScheduleStatus.Available -> "Електроенергія є"
                ScheduleStatus.Probable -> "Можливе відключення"
                else -> "Електроенергія відсутня"
            }
            val statusText = activeGroup?.displayText ?: currentStatus?.displayText ?: stringResource(R.string.home_no_data)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(statusIconSize)
                        .background(contentColor, statusShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = statusDescription,
                        tint = containerColor.copy(alpha = 1f),
                        modifier = Modifier.size(statusIconSize * 0.5f)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_status_now),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.alpha(0.8f)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            if (hideLiveTiming) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.home_all_day_available),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.alpha(0.85f)
                )
            } else if (activeGroup != null) {
                val nextGroup = remember(activeGroup, groupedSchedule) {
                    val idx = groupedSchedule.indexOf(activeGroup)
                    if (idx != -1 && idx < groupedSchedule.size - 1) groupedSchedule[idx + 1] else null
                }
                val noOutagesExpected = remember(cherga, pidcherga, hasElectricity, activeGroup, groupedSchedule) {
                    (cherga == 0 && pidcherga == 0) || (hasElectricity && groupedSchedule.none {
                        it.status != ScheduleStatus.Available && it.startMs > activeGroup.startMs
                    })
                }

                Spacer(Modifier.height(20.dp))
                if (noOutagesExpected) {
                    Text(
                        text = stringResource(R.string.home_no_outages_expected),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.alpha(0.85f)
                    )
                } else {
                    val nextChangeTime = if (nextGroup != null) TimeUtils.formatToSystemTime(LocalContext.current, nextGroup.startTime) else "—"
                    LiveProgressBar(
                        activeGroup = activeGroup,
                        contentColor = contentColor,
                        hasElectricity = hasElectricity,
                        nextChangeTime = nextChangeTime
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LiveProgressBar(
    activeGroup: GroupedSchedule,
    contentColor: Color,
    hasElectricity: Boolean,
    nextChangeTime: String
) {
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
    val msRemaining = activeGroup.endMs - nowMs
    val timeRemainingText = when {
        msRemaining < 60_000 -> "<1 хв"
        else -> {
            val rem = msRemaining / 60_000
            ScheduleMapper.formatDuration((rem / 60).toInt(), (rem % 60).toInt())
        }
    }
    val animatedProgress by animateFloatAsState(progress, ProgressIndicatorDefaults.ProgressAnimationSpec, label = "pr")
    
    Column(Modifier.fillMaxWidth()) {
        LinearWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
            color = contentColor,
            trackColor = contentColor.copy(alpha = 0.2f),
            // Default speed (one wavelength per second) feels frantic
            waveSpeed = WavyProgressIndicatorDefaults.LinearDeterminateWavelength / 4
        )
        Spacer(Modifier.height(16.dp))
        Row {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (hasElectricity) "до відключення" else "до увімкнення",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.alpha(0.8f)
                )
                Text(timeRemainingText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (hasElectricity) "відключення" else "увімкнення",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.alpha(0.8f)
                )
                Text(nextChangeTime, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
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
    cityId: String = "",
    streetId: String = "",
    addressId: String = "",
    remId: String = "",
    remName: String = "",
    lastUpdateTime: String = "",
    iconName: String = "",
    isPrimary: Boolean = false
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
    var showQrDialog by remember { mutableStateOf(false) }
    val qrMissingDataText = stringResource(R.string.qr_share_missing_data)
    val qrContent = remember(
        remId,
        cityId,
        streetId,
        addressId,
        cherga,
        pidcherga,
        addressName,
        remName,
        cityName,
        streetName
    ) {
        if (streetId.isNotBlank() && addressId.isNotBlank()) {
            QRAddressData.generateQRContent(
                remId = remId,
                cityId = cityId,
                streetId = streetId,
                addressId = addressId,
                cherga = cherga,
                pidcherga = pidcherga,
                displayName = addressName,
                remName = remName,
                cityName = cityName,
                streetName = streetName,
                addressName = addressName
            )
        } else {
            ""
        }
    }
    val primaryLine = listOf(streetName, addressName).filter { it.isNotBlank() }.joinToString(", ").ifBlank { cityName }
    val secondaryLine = listOfNotNull(
        cityName.takeIf { it.isNotBlank() && primaryLine != cityName },
        lastUpdateTime.takeIf { it.isNotBlank() }?.let { stringResource(R.string.home_last_updated, it) }
    ).joinToString(" · ")
    // Demo presets use placeholder queues (9998/9999) that mean nothing to users
    val showQueue = cherga in 1..99

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.occaecat.ztoeschedule.presentation.ui.components.AddressIconBadge(iconName = iconName, isPrimary = isPrimary, size = 48.dp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = primaryLine,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (secondaryLine.isNotBlank()) {
                    Text(
                        text = secondaryLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            if (showQueue) {
                Spacer(Modifier.width(8.dp))
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer, shape = CircleShape) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("черга", style = MaterialTheme.typography.labelSmall)
                        Text("$cherga.$pidcherga", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(modifier = Modifier.padding(start = 4.dp)) {
                FilledTonalIconButton(onClick = { showShareMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Share, 
                        contentDescription = "Поділитися", 
                        modifier = Modifier.size(20.dp)
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
                                remId = remId,
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
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.qr_show_code)) },
                        leadingIcon = { Icon(Icons.Default.QrCode, null) },
                        onClick = {
                            showShareMenu = false
                            if (qrContent.isBlank()) {
                                android.widget.Toast.makeText(
                                    context,
                                    qrMissingDataText,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                showQrDialog = true
                            }
                        }
                    )
                }
            }
            
        }
    }

    if (showQrDialog) {
        QrCodeDialog(
            content = qrContent,
            addressText = fullAddress,
            onDismiss = { showQrDialog = false }
        )
    }
}

@Composable
private fun QrCodeDialog(
    content: String,
    addressText: String,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val sizePx = with(density) { 220.dp.roundToPx() }
    val qrBitmap = remember(content, sizePx) { QrCodeGenerator.generate(content, sizePx) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.qr_share_title)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (qrBitmap != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.qr_share_content_desc),
                            modifier = Modifier.padding(12.dp).size(220.dp)
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.qr_error_unknown),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (addressText.isNotBlank()) {
                    Text(
                        text = addressText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = stringResource(R.string.qr_share_desc),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.qr_close))
            }
        }
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleListItemSimple(
    group: GroupedSchedule, 
    isActive: Boolean, 
    isPast: Boolean,
    address: String, 
    highlightTrigger: Long,
    index: Int,
    totalCount: Int,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val displayMode = com.occaecat.ztoeschedule.ui.theme.LocalDisplayMode.current
    val itemVerticalPadding = when (displayMode) {
        com.occaecat.ztoeschedule.data.model.DisplayMode.Compact -> 8.dp
        com.occaecat.ztoeschedule.data.model.DisplayMode.Comfortable -> 16.dp
        com.occaecat.ztoeschedule.data.model.DisplayMode.Spacious -> 24.dp
    }
    val circleIconSize = when (displayMode) {
        com.occaecat.ztoeschedule.data.model.DisplayMode.Compact -> 36.dp
        com.occaecat.ztoeschedule.data.model.DisplayMode.Comfortable -> 48.dp
        com.occaecat.ztoeschedule.data.model.DisplayMode.Spacious -> 56.dp
    }
    val innerIconSize = when (displayMode) {
        com.occaecat.ztoeschedule.data.model.DisplayMode.Compact -> 18.dp
        com.occaecat.ztoeschedule.data.model.DisplayMode.Comfortable -> 24.dp
        com.occaecat.ztoeschedule.data.model.DisplayMode.Spacious -> 28.dp
    }
    val timeTextStyle = when (displayMode) {
        com.occaecat.ztoeschedule.data.model.DisplayMode.Compact -> MaterialTheme.typography.bodyMedium
        com.occaecat.ztoeschedule.data.model.DisplayMode.Comfortable -> MaterialTheme.typography.titleMedium
        com.occaecat.ztoeschedule.data.model.DisplayMode.Spacious -> MaterialTheme.typography.titleLarge
    }
    
    val highlightAlpha = remember { Animatable(0f) }
    LaunchedEffect(highlightTrigger) {
        if (highlightTrigger > 0 && isActive) {
            repeat(2) {
                highlightAlpha.animateTo(0.4f, tween(400, easing = LinearOutSlowInEasing))
                highlightAlpha.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
            }
        }
    }
    
    val topRadius by animateDpAsState(
        if (isPressed) 40.dp else if (index == 0) 24.dp else 4.dp,
        label = "tr"
    )
    val bottomRadius by animateDpAsState(
        if (isPressed) 40.dp else if (index == totalCount - 1) 24.dp else 4.dp,
        label = "br"
    )

    val shape = RoundedCornerShape(
        topStart = topRadius, topEnd = topRadius,
        bottomStart = bottomRadius, bottomEnd = bottomRadius
    )
    
    val statusColor = statusColorFor(group.status)
    val onStatusColor = when (group.status) {
        ScheduleStatus.Available -> MaterialTheme.colorScheme.onSurface
        ScheduleStatus.Probable -> MaterialTheme.colorScheme.onTertiary
        else -> MaterialTheme.colorScheme.onError
    }
    val containerColor = if (isActive) {
        androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surfaceContainerHigh, statusColor, if (group.status == ScheduleStatus.Available) 0.35f else 0.16f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = highlightAlpha.value))
            .indication(interactionSource, ScaleIndication)
            .combinedClickable(
                interactionSource = interactionSource, 
                indication = ripple(), 
                onClick = {
                    // Short tap: perform haptic feedback to indicate selection
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onLongClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .testTag("schedule_slot_${group.startTime}"),
        color = containerColor,
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = itemVerticalPadding)
                // Past intervals of today recede so the upcoming ones stand out
                .alpha(if (isPast && !isActive) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(6.dp)
                    .height(circleIconSize - 8.dp)
                    .background(statusColor, CircleShape)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = TimeUtils.formatSpanToSystem(context, group.span),
                    style = timeTextStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (group.status) {
                            ScheduleStatus.Available -> Icons.Default.LightMode
                            ScheduleStatus.Probable -> Icons.Default.WarningAmber
                            else -> Icons.Default.FlashOff
                        },
                        contentDescription = null,
                        modifier = Modifier.size(innerIconSize * 0.7f),
                        tint = if (group.status == ScheduleStatus.Available) MaterialTheme.colorScheme.onSurfaceVariant else statusColor
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = group.displayText, 
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(12.dp))
            if (isActive) {
                Surface(color = statusColor, contentColor = onStatusColor, shape = CircleShape) {
                    Text(
                        text = stringResource(R.string.home_status_now),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            } else {
                Text(
                    text = group.formattedDuration,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * "Light on" is the calm default, so it stays neutral; outages must never be confused with it
 * (primary and error can be near-identical hues in dynamic themes).
 */
@Composable
private fun statusColorFor(status: ScheduleStatus): Color = when (status) {
    ScheduleStatus.Available -> MaterialTheme.colorScheme.outlineVariant
    // A paler outage colour: tertiary can be the same hue as error in some schemes
    ScheduleStatus.Probable -> androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.surfaceContainerHigh, 0.5f)
    else -> MaterialTheme.colorScheme.error
}

private val UA_MONTHS = listOf(
    "січня", "лютого", "березня", "квітня", "травня", "червня",
    "липня", "серпня", "вересня", "жовтня", "листопада", "грудня"
)
private val UA_WEEKDAYS = listOf("Неділя", "Понеділок", "Вівторок", "Середа", "Четвер", "Пʼятниця", "Субота")

/** "Сьогодні" / "Завтра" / weekday, plus "29 вересня". */
private fun dayLabels(date: String, todayDate: String): Pair<String, String> {
    val fmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val cal = Calendar.getInstance()
    val parsed = runCatching { fmt.parse(date) }.getOrNull() ?: return date to ""
    cal.time = parsed
    val secondary = "${cal.get(Calendar.DAY_OF_MONTH)} ${UA_MONTHS[cal.get(Calendar.MONTH)]}"
    val today = runCatching { fmt.parse(todayDate) }.getOrNull()
    val dayDiff = if (today != null) ((parsed.time - today.time) / (24 * 60 * 60 * 1000.0)).let { Math.round(it) } else null
    val primary = when (dayDiff) {
        0L -> "Сьогодні"
        1L -> "Завтра"
        -1L -> "Вчора"
        else -> UA_WEEKDAYS[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }
    return primary to secondary
}

private fun GroupedSchedule.minutes(): Int =
    (durationHours * 60 + durationMinutes).takeIf { it > 0 } ?: ((endMs - startMs) / 60000).toInt().coerceAtLeast(0)

@Composable
private fun DayHeader(
    date: String,
    todayDate: String,
    items: List<GroupedSchedule>,
    modifier: Modifier = Modifier
) {
    val (primary, secondary) = remember(date, todayDate) { dayLabels(date, todayDate) }
    val outageMinutes = remember(items) { items.filter { it.status == ScheduleStatus.Outage }.sumOf { it.minutes() } }
    val probableMinutes = remember(items) { items.filter { it.status == ScheduleStatus.Probable }.sumOf { it.minutes() } }

    Surface(modifier = modifier.fillMaxWidth(), color = Color.Transparent) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                Text(primary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (secondary.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            val (label, container, content) = when {
                outageMinutes > 0 -> Triple(
                    "без світла ${ScheduleMapper.formatDuration(outageMinutes / 60, outageMinutes % 60)}",
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer
                )
                probableMinutes > 0 -> Triple(
                    "можливі ${ScheduleMapper.formatDuration(probableMinutes / 60, probableMinutes % 60)}",
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer
                )
                else -> Triple("без відключень", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Surface(color = container, contentColor = content, shape = CircleShape) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/** Whole day at a glance: coloured segments across 24 h with a "now" marker for today. */
@Composable
private fun DayTimelineBar(items: List<GroupedSchedule>, nowMs: Long?) {
    val colorScheme = MaterialTheme.colorScheme
    val dayStartMs = items.firstOrNull()?.startMs ?: return
    val totalMinutes = items.sumOf { it.minutes() }.coerceAtLeast(1)

    Surface(
        color = colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(20.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items.forEachIndexed { i, group ->
                        val shape = RoundedCornerShape(
                            topStart = if (i == 0) 7.dp else 2.dp, bottomStart = if (i == 0) 7.dp else 2.dp,
                            topEnd = if (i == items.lastIndex) 7.dp else 2.dp, bottomEnd = if (i == items.lastIndex) 7.dp else 2.dp
                        )
                        Box(
                            Modifier
                                .weight(group.minutes().coerceAtLeast(1).toFloat())
                                .fillMaxHeight()
                                .background(statusColorFor(group.status), shape)
                        )
                    }
                }
                if (nowMs != null) {
                    val fraction = ((nowMs - dayStartMs) / 60000f / totalMinutes).coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .offset(x = (maxWidth - 4.dp) * fraction)
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(colorScheme.onSurface, CircleShape)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("00", "06", "12", "18", "24").forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun findAllDayAvailableDates(
    schedules: List<Schedule>,
    groupedSchedule: List<GroupedSchedule>
): Set<String> {
    val datesFromRawSchedule = schedules
        .groupBy { it.date }
        .filterValues { isAllDayAvailable(it) }
        .keys

    val datesFromGroupedSchedule = groupedSchedule
        .filter { it.status == ScheduleStatus.Available && it.startTime == "00:00" && it.durationHours >= 24 }
        .map { it.date }

    return datesFromRawSchedule + datesFromGroupedSchedule
}

private fun isAllDayAvailable(daySchedules: List<Schedule>): Boolean {
    if (daySchedules.isEmpty() || daySchedules.any { it.status != ScheduleStatus.Available }) {
        return false
    }

    val intervals = daySchedules
        .mapNotNull { parseScheduleSpanToMinutes(it.span) }
        .sortedBy { it.first }

    if (intervals.isEmpty()) return false

    var coveredUntil = 0
    intervals.forEach { (start, end) ->
        if (start > coveredUntil) return false
        if (end > coveredUntil) coveredUntil = end
        if (coveredUntil >= MINUTES_PER_DAY) return true
    }

    return coveredUntil >= MINUTES_PER_DAY
}

private fun parseScheduleSpanToMinutes(span: String): Pair<Int, Int>? {
    val parts = span.split("-")
    if (parts.size != 2) return null

    val start = parseClockToMinutes(parts[0].trim()) ?: return null
    val endRaw = parts[1].trim()
    val parsedEnd = parseClockToMinutes(endRaw) ?: return null
    val end = when {
        endRaw == "24:00" -> MINUTES_PER_DAY
        parsedEnd == 0 && start == 0 -> MINUTES_PER_DAY
        parsedEnd <= start -> parsedEnd + MINUTES_PER_DAY
        else -> parsedEnd
    }.coerceAtMost(MINUTES_PER_DAY)

    return start to end
}

private fun parseClockToMinutes(time: String): Int? {
    val parts = time.split(":")
    if (parts.size != 2) return null

    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    if (hours !in 0..24 || minutes !in 0..59) return null
    if (hours == 24 && minutes != 0) return null

    return if (hours == 24) MINUTES_PER_DAY else hours * 60 + minutes
}

private fun formatScheduleDate(timeMs: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Europe/Kyiv")
    }.format(Date(timeMs))
}

private const val MINUTES_PER_DAY = 24 * 60

private fun addToCalendar(context: android.content.Context, group: GroupedSchedule, address: String) {
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
            putExtra(CalendarContract.Events.TITLE, when (group.status) {
                ScheduleStatus.Available -> "Світло є"
                ScheduleStatus.Probable -> "Можливе відключення світла"
                else -> "Відключення світла"
            })
            putExtra(CalendarContract.Events.DESCRIPTION, "${group.displayText} за адресою: $address")
            putExtra(CalendarContract.Events.EVENT_LOCATION, address)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startCal.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endCal.timeInMillis)
            putExtra(CalendarContract.Events.ALL_DAY, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (ex: Exception) { ex.printStackTrace() }
}

private fun intervalText(group: GroupedSchedule, address: String): String =
    "${group.date}, ${group.span} — ${group.displayText} (${group.formattedDuration})\n$address"

private fun copyToClipboard(context: android.content.Context, group: GroupedSchedule, address: String) {
    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Schedule", intervalText(group, address))
    clipboardManager.setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < 33) {
        android.widget.Toast.makeText(context, "Скопійовано", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun shareInterval(context: android.content.Context, group: GroupedSchedule, address: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, intervalText(group, address))
    }
    context.startActivity(Intent.createChooser(intent, "Поділитися"))
}
