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
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur as glassBlur
import com.kyant.backdrop.effects.lens as glassLens
import com.kyant.backdrop.effects.vibrancy as glassVibrancy


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
            PullToRefreshDefaults.LoadingIndicator(
                state = refreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
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
                top = 16.dp, 
                end = 16.dp, 
                bottom = contentPadding.calculateBottomPadding() + 80.dp
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
                            cityId = cityId,
                            streetId = streetId,
                            addressId = addressId,
                            remId = remId,
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

                visibleGroupedByDate.forEach { (date, items) ->
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
                        Column(
                            modifier = Modifier.animateItem(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items.forEachIndexed { idx, group ->
                                key(group.span) {
                                    ScheduleListItemSimple(
                                        group = group, 
                                        isActive = (group == activeGroup), 
                                        address = fullAddress,
                                        highlightTrigger = highlightTrigger,
                                        index = idx,
                                        totalCount = items.size,
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

        ModalBottomSheet(
            onDismissRequest = { selectedGroupForMenu = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "${group.date} • ${TimeUtils.formatSpanToSystem(context, group.span)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
                
                ListItem(
                    headlineContent = { Text("Додати в календар") },
                    leadingContent = { Icon(Icons.Default.Event, null) },
                    modifier = Modifier.clickable {
                        addToCalendar(context, group, fullAddress)
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { selectedGroupForMenu = null }
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Скопіювати") },
                    leadingContent = { Icon(Icons.Default.ContentCopy, null) },
                    modifier = Modifier.clickable {
                        copyToClipboard(context, group)
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { selectedGroupForMenu = null }
                    }
                )
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
    val radius = com.occaecat.ztoeschedule.ui.theme.LocalCornerRadius.current
    val displayMode = com.occaecat.ztoeschedule.ui.theme.LocalDisplayMode.current
    val glassBackdrop = com.occaecat.ztoeschedule.ui.theme.LocalGlassBackdrop.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val glassBlurPx = with(density) { 20.dp.toPx() }
    val glassLensHeightPx = with(density) { 10.dp.toPx() }
    val glassLensAmountPx = with(density) { 20.dp.toPx() }
    val cardShape = MaterialTheme.shapes.extraLarge
    val glassModifier = if (glassBackdrop != null) {
        Modifier.drawBackdrop(
            backdrop = glassBackdrop,
            shape = { cardShape },
            effects = {
                glassVibrancy()
                glassBlur(glassBlurPx)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    glassLens(glassLensHeightPx, glassLensAmountPx, true)
                }
            }
        )
    } else Modifier

    val adaptivePadding = remember(radius, displayMode) {
        val base = when (displayMode) {
            com.occaecat.ztoeschedule.data.model.DisplayMode.Compact -> 12f
            com.occaecat.ztoeschedule.data.model.DisplayMode.Comfortable -> 16f
            com.occaecat.ztoeschedule.data.model.DisplayMode.Spacious -> 24f
        }
        val extra = if (radius > 16) (radius - 16).toFloat() / 2f else 0f
        (base + extra).dp
    }
    val statusIconSize = when (displayMode) {
        com.occaecat.ztoeschedule.data.model.DisplayMode.Compact -> 48.dp
        com.occaecat.ztoeschedule.data.model.DisplayMode.Comfortable -> 64.dp
        com.occaecat.ztoeschedule.data.model.DisplayMode.Spacious -> 80.dp
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
            .then(glassModifier)
            .semantics {
                liveRegion = LiveRegionMode.Assertive
            },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (glassBackdrop != null) containerColor.copy(alpha = 0.25f) else containerColor,
            contentColor = contentColor
        )
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
            val statusDescription = when (status) {
                ScheduleStatus.Available -> "Електроенергія є"
                ScheduleStatus.Probable -> "Можливе відключення"
                else -> "Електроенергія відсутня"
            }
            val statusText = activeGroup?.displayText ?: currentStatus?.displayText ?: stringResource(R.string.home_no_data)

            if (hideLiveTiming) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = statusDescription,
                        modifier = Modifier.size(statusIconSize)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.home_all_day_available),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.alpha(0.8f)
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = statusDescription,
                    modifier = Modifier.size(statusIconSize).graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                )
                Spacer(Modifier.height(if (displayMode == com.occaecat.ztoeschedule.data.model.DisplayMode.Compact) 8.dp else 16.dp))
                Text(
                    text = statusText,
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

                        val noOutagesExpected = remember(cherga, pidcherga, hasElectricity, activeGroup, groupedSchedule) {
                            (cherga == 0 && pidcherga == 0) || (hasElectricity && groupedSchedule.none {
                                it.status != ScheduleStatus.Available && it.startMs > activeGroup.startMs
                            })
                        }

                        if (noOutagesExpected) {
                            Text(
                                text = stringResource(R.string.home_no_outages_expected),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.alpha(0.8f)
                            )
                        } else {
                            val nextChangeTime = if (nextGroup != null) TimeUtils.formatToSystemTime(LocalContext.current, nextGroup.startTime) else "—"
                            Text(
                                text = if (hasElectricity) stringResource(R.string.home_next_outage, nextChangeTime) else stringResource(R.string.home_next_restore, nextChangeTime),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.alpha(0.8f)
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                        LiveProgressBar(activeGroup, contentColor, hasElectricity)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LiveProgressBar(activeGroup: GroupedSchedule, contentColor: Color, hasElectricity: Boolean) {
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
        msRemaining <= 0 -> pluralStringResource(R.plurals.second, 0, 0)
        msRemaining < 60000 -> {
            val seconds = (msRemaining / 1000).toInt().coerceAtLeast(1)
            pluralStringResource(R.plurals.second, seconds, seconds)
        }
        else -> {
            val rem = msRemaining / 60000
            val h = (rem / 60).toInt()
            val m = (rem % 60).toInt()
            val hStr = if (h > 0) pluralStringResource(R.plurals.hour, h, h) + " " else ""
            val mStr = pluralStringResource(R.plurals.minute, m, m)
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
    modifier: Modifier = Modifier,
    cityId: String = "",
    streetId: String = "",
    addressId: String = "",
    remId: String = "",
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
                contentDescription = "Адреса", 
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
                )
            }

            Box(modifier = Modifier.padding(start = 4.dp)) {
                IconButton(
                    onClick = { showShareMenu = true }, 
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share, 
                        contentDescription = "Поділитися", 
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
    
    val statusColor = when (group.status) {
        ScheduleStatus.Available -> MaterialTheme.colorScheme.primary
        ScheduleStatus.Probable -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    
    val containerColor = if (isActive) {
        statusColor.copy(alpha = 0.12f)
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
        shape = shape,
        border = if (isActive) BorderStroke(1.dp, statusColor.copy(alpha = 0.38f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = itemVerticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon with background
            Surface(
                modifier = Modifier.size(circleIconSize),
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
                        contentDescription = when (group.status) {
                            ScheduleStatus.Available -> "Електроенергія є"
                            ScheduleStatus.Probable -> "Можливе відключення"
                            else -> "Електроенергія відсутня"
                        },
                        modifier = Modifier.size(innerIconSize),
                        tint = statusColor
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Time and Status Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = TimeUtils.formatSpanToSystem(context, group.span),
                    style = timeTextStyle,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = group.displayText, 
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
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
}

private fun copyToClipboard(context: android.content.Context, group: GroupedSchedule) {
    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Schedule", "${group.date}: ${group.span} - ${group.displayText}")
    clipboardManager.setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < 33) {
        android.widget.Toast.makeText(context, "Скопійовано", android.widget.Toast.LENGTH_SHORT).show()
    }
}
