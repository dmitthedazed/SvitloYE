package com.occaecat.ztoeschedule.presentation.ui.addresses

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.onFocusChanged
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.*
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.TimeUtils
import com.occaecat.ztoeschedule.presentation.ui.home.HomeTab
import com.occaecat.ztoeschedule.presentation.ui.components.ShimmerItem
import com.occaecat.ztoeschedule.presentation.ui.components.ScaleIndication
import com.occaecat.ztoeschedule.presentation.ui.components.StepGutter
import com.occaecat.ztoeschedule.presentation.ui.components.AddressIconBadge
import com.occaecat.ztoeschedule.presentation.ui.components.StepHeroIcon
import com.occaecat.ztoeschedule.presentation.ui.components.StepPrimaryButton
import java.util.Collections
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.LayoutDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAddressesTab(
    addresses: List<SavedAddress>, addressStatuses: Map<String, GroupedSchedule?>, isAddingNew: Boolean,
    remList: List<Rem>, cityList: List<City>, streetList: List<Street>, houseNumbers: List<ParsedHouseNumber>,
    searchQuery: String, isLoading: Boolean, useWideLayout: Boolean = false,
    inspectedScheduleList: List<Schedule> = emptyList(), inspectedGroupedSchedule: List<GroupedSchedule> = emptyList(),
    isInspectingLoading: Boolean = false, onStartAdding: () -> Unit, onCancelAdding: () -> Unit,
    onLoadRem: () -> Unit, onLoadCity: (String) -> Unit, onLoadStreet: (String) -> Unit,
    onLoadAddress: (String) -> Unit, onSearchQueryChange: (String) -> Unit, onClearSearch: () -> Unit,
    onSaveAddress: (name: String, icon: String, remId: String, remName: String, cityId: String, cityName: String, streetId: String, streetName: String, addressId: String, addressName: String, cherga: Int, pidcherga: Int) -> Unit,
    onDeleteAddress: (String) -> Unit, onUpdateOrder: (List<SavedAddress>) -> Unit,
    onRefreshAddress: (Int, Int) -> Unit = { _, _ -> }, onInspectAddress: (SavedAddress) -> Unit = {},
    modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var selectedId by rememberSaveable { mutableStateOf(addresses.firstOrNull()?.id) }
    LaunchedEffect(addresses) { if (selectedId == null && addresses.isNotEmpty()) selectedId = addresses.first().id }
    val selectedAddr = remember(selectedId, addresses) { addresses.find { it.id == selectedId } }
    LaunchedEffect(selectedId, useWideLayout) { if (useWideLayout && selectedAddr != null) onInspectAddress(selectedAddr) }

    Row(modifier = modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            Crossfade(targetState = when { isLoading && addresses.isEmpty() -> "l"; addresses.isEmpty() -> "e"; else -> "c" }, label = "f") { state ->
                when (state) {
                    "l" -> AddressesSkeleton(contentPadding)
                    "e" -> EmptyAddressesView(onStartAdding, contentPadding)
                    else -> DraggableAddressList(addresses, addressStatuses, if (useWideLayout) selectedId else null, onDeleteAddress, onStartAdding, onUpdateOrder, { if (useWideLayout) selectedId = it.id else onInspectAddress(it) }, Modifier.fillMaxSize(), contentPadding)
                }
            }
        }
        if (useWideLayout) {
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Box(Modifier.weight(1.5f)) {
                if (selectedAddr != null) {
                    if (isInspectingLoading && inspectedGroupedSchedule.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    else HomeTab(
                        remId = selectedAddr.remId,
                        cityId = selectedAddr.cityId,
                        remName = selectedAddr.remName,
                        cityName = selectedAddr.cityName,
                        streetName = selectedAddr.streetName,
                        addressName = selectedAddr.addressName,
                        cherga = selectedAddr.cherga,
                        pidcherga = selectedAddr.pidcherga,
                        iconName = selectedAddr.iconName,
                        isPrimary = selectedAddr.id == addresses.firstOrNull()?.id,
                        currentStatus = null,
                        schedules = inspectedScheduleList,
                        groupedSchedule = inspectedGroupedSchedule,
                        onRefresh = { onInspectAddress(selectedAddr) },
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPadding,
                        lastUpdateTime = "",
                        isOffline = false,
                        isLoading = isInspectingLoading,
                        streetId = selectedAddr.streetId,
                        addressId = selectedAddr.addressId
                    )
                } else Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Оберіть адресу зліва", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun AddressesSkeleton(cp: PaddingValues, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(cp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) { ShimmerItem(120.dp, shape = MaterialTheme.shapes.extraLarge) }
        Spacer(Modifier.height(8.dp)); ShimmerItem(64.dp, shape = MaterialTheme.shapes.medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraggableAddressList(addrs: List<SavedAddress>, statuses: Map<String, GroupedSchedule?>, selectedId: String?, onDelete: (String) -> Unit, onAdd: () -> Unit, onUpdate: (List<SavedAddress>) -> Unit, onSelect: (SavedAddress) -> Unit, modifier: Modifier = Modifier, cp: PaddingValues = PaddingValues(0.dp)) {
    var list by remember(addrs) { mutableStateOf(addrs) }
    var dId by rememberSaveable { mutableStateOf<String?>(null) }
    val dAddr = remember(dId, addrs) { addrs.find { it.id == dId } }
    var pId by rememberSaveable { mutableStateOf<String?>(null) }
    val pDialog = remember(pId, addrs) { addrs.find { it.id == pId } }
    val initPId = remember(addrs) { addrs.firstOrNull()?.id }
    val listState = rememberLazyListState()
    var dragIdx by rememberSaveable { mutableIntStateOf(-1) }
    var dragOff by rememberSaveable { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val nowMs = rememberAdaptiveNowMs(statuses)

    LazyColumn(
        state = listState, 
        userScrollEnabled = listState.canScrollForward || listState.canScrollBackward,
        modifier = modifier.fillMaxSize().testTag("address_list"), 
        contentPadding = PaddingValues(
            start = cp.calculateStartPadding(LayoutDirection.Ltr) + 16.dp, 
            top = cp.calculateTopPadding() + 16.dp, 
            end = cp.calculateEndPadding(LayoutDirection.Ltr) + 16.dp, 
            // Room for the "Додати" FAB above the navigation
            bottom = cp.calculateBottomPadding() + 88.dp
        ), 
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(list, key = { _, item -> item.id }) { idx, addr ->
            val isD = idx == dragIdx; val isS = addr.id == selectedId
            val scale by animateFloatAsState(if (isD) 1.05f else 1f, spring(Spring.DampingRatioLowBouncy), label = "s")
            val alpha by animateFloatAsState(if (isD) 0.8f else 1f, label = "a")
            val dState = rememberSwipeToDismissBoxState(confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { dId = addr.id; false } else false })
            var showMenu by remember { mutableStateOf(false) }
            Box(Modifier.zIndex(if (isD) 1f else 0f).animateItem()) {
                SwipeToDismissBox(state = dState, enableDismissFromStartToEnd = false, backgroundContent = {
                    val p = if (dState.dismissDirection == SwipeToDismissBoxValue.EndToStart) dState.progress else 0f
                    val c = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.errorContainer, (p * 2f).coerceAtMost(1f))
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(c).padding(horizontal = 24.dp), contentAlignment = Alignment.CenterEnd) {
                        Icon(Icons.Default.Delete, "Видалити", tint = if (p > 0.25f) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }) {
                    AddressItem(
                        a = addr, 
                        s = statuses[addr.id], 
                        nowMs = nowMs,
                        isP = (idx == 0), 
                        isSel = isS, 
                        index = idx,
                        totalCount = list.size,
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect(addr) },
                        modifier = Modifier.graphicsLayer { translationY = if (isD) dragOff else 0f; scaleX = scale; scaleY = scale; this.alpha = alpha }
                            .pointerInput(addr.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); dragIdx = idx; dragOff = 0f },
                                    onDragEnd = { if (list.firstOrNull()?.id != initPId) pId = list.first().id else onUpdate(list); dragIdx = -1 },
                                    onDragCancel = { dragIdx = -1 },
                                    onDrag = { change, amount ->
                                        change.consume(); dragOff += amount.y
                                        val h = listState.layoutInfo.visibleItemsInfo.find { it.key == addr.id }?.size ?: 0
                                        if (h > 0 && Math.abs(dragOff) > h / 2f) {
                                            val dir = if (dragOff > 0) 1 else -1; val target = dragIdx + dir
                                            if (target in list.indices) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); val newList = list.toMutableList(); Collections.swap(newList, dragIdx, target); list = newList; dragIdx = target; dragOff -= dir * h }
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) { awaitPointerEventScope { while (true) { val e = awaitPointerEvent(); if (e.type == PointerEventType.Release && e.buttons.isSecondaryPressed) showMenu = true } } }
                    )
                    DropdownMenu(showMenu, { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Зробити головною") }, onClick = { showMenu = false; if (idx != 0) { val nl = list.toMutableList(); val i = nl.removeAt(idx); nl.add(0, i); onUpdate(nl) } }, leadingIcon = { Icon(Icons.Default.Star, null) })
                        DropdownMenuItem(text = { Text("Видалити") }, onClick = { showMenu = false; dId = addr.id }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                    }
                }
            }
        }
        item(key = "gesture_hint") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .animateItem(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.TouchApp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Утримуйте картку, щоб змінити порядок.\nПроведіть вліво, щоб видалити.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
    if (pDialog != null) AlertDialog(onDismissRequest = { pId = null; list = addrs }, icon = { Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary) }, title = { Text("Змінити головну адресу?") }, text = { Text("Ви вибрали ${pDialog.name} як основну.") }, confirmButton = { Button(onClick = { onUpdate(list); pId = null }) { Text("Так") } }, dismissButton = { TextButton(onClick = { pId = null; list = addrs }) { Text("Скасувати") } })
    if (dAddr != null) AlertDialog(onDismissRequest = { dId = null }, icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) }, title = { Text("Видалити адресу?") }, text = { Text("Ви впевнені, що хочете видалити ${dAddr.name}?") }, confirmButton = { TextButton(onClick = { onDelete(dAddr.id); dId = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Видалити") } }, dismissButton = { TextButton(onClick = { dId = null }) { Text("Залишити") } })
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddressItem(
    a: SavedAddress, 
    s: GroupedSchedule?, 
    nowMs: Long, 
    isP: Boolean, 
    isSel: Boolean, 
    index: Int,
    totalCount: Int,
    onClick: () -> Unit, 
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(if (isPressed) 40.dp else 28.dp, label = "corner")

    // Main address is highlighted; selection (wide layout) uses the secondary role
    val containerColor = when {
        isSel -> colorScheme.secondaryContainer
        isP -> colorScheme.primaryContainer
        else -> colorScheme.surfaceContainerHigh
    }
    val onContainerColor = when {
        isSel -> colorScheme.onSecondaryContainer
        isP -> colorScheme.onPrimaryContainer
        else -> colorScheme.onSurface
    }
    val addressLine = listOf(a.streetName, a.addressName).filter { it.isNotBlank() }.joinToString(", ")

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .semantics { onClick(label = "проглянути", action = { onClick(); true }) },
        shape = RoundedCornerShape(corner),
        color = containerColor,
        contentColor = onContainerColor,
        border = if (isSel) BorderStroke(2.dp, colorScheme.primary) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AddressIconBadge(iconName = a.iconName, isPrimary = isP)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = a.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isP) {
                            Spacer(Modifier.width(8.dp))
                            Surface(color = colorScheme.primary, contentColor = colorScheme.onPrimary, shape = CircleShape) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp))
                                    Text("Головна", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Text(
                        text = addressLine.ifBlank { a.cityName },
                        style = MaterialTheme.typography.bodyMedium,
                        color = onContainerColor.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (addressLine.isNotBlank()) {
                        Text(
                            text = a.cityName,
                            style = MaterialTheme.typography.bodySmall,
                            color = onContainerColor.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (a.cherga in 1..99) { // demo presets use placeholder queues
                    Spacer(Modifier.width(12.dp))
                    Surface(color = onContainerColor.copy(alpha = 0.1f), shape = CircleShape) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("черга", style = MaterialTheme.typography.labelSmall, color = onContainerColor.copy(alpha = 0.7f))
                            Text("${a.cherga}.${a.pidcherga}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (s != null) { 
                Spacer(Modifier.height(16.dp))
                StatusInfoSection(s, onContainerColor, nowMs) 
            }
        }
    }
}

@Composable
private fun StatusInfoSection(s: GroupedSchedule, contentColor: Color, nowMs: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // Status color logic - ensured to be high contrast on card
    val statusColor = when(s.status) { 
        ScheduleStatus.Outage -> colorScheme.error
        ScheduleStatus.Probable -> colorScheme.tertiary
        else -> colorScheme.primary 
    }
    val onStatusColor = when(s.status) {
        ScheduleStatus.Outage -> colorScheme.onError
        ScheduleStatus.Probable -> colorScheme.onTertiary
        else -> colorScheme.onPrimary
    }
    
    val animatedStatusColor by animateColorAsState(statusColor, label = "sc")
    val progress = remember(s, nowMs) { 
        val d = s.endMs - s.startMs
        if (d <= 0) 0f else ((nowMs - s.startMs).toFloat() / d.toFloat()).coerceIn(0f, 1f) 
    }
    val animatedProgress by animateFloatAsState(progress, label = "p")
    val hideLiveTiming = s.status == ScheduleStatus.Available &&
        s.startTime == "00:00" &&
        s.endMs - s.startMs >= FULL_DAY_MS
    val statusText = if (hideLiveTiming) stringResource(R.string.address_status_light_stays_on) else s.displayText
    val statusIcon = when (s.status) {
        ScheduleStatus.Available -> Icons.Default.CheckCircle
        ScheduleStatus.Probable -> Icons.Default.WarningAmber
        else -> Icons.Default.FlashOff
    }
    
    Surface(
        modifier = modifier.fillMaxWidth().testTag("status_info_section"),
        shape = RoundedCornerShape(20.dp),
        color = animatedStatusColor.copy(alpha = 0.14f)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = animatedStatusColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = onStatusColor
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!hideLiveTiming) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = contentColor.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = contentColor
                            )
                            Text(
                                text = "До ${TimeUtils.formatToSystemTime(context, s.endTime)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            if (!hideLiveTiming) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = animatedStatusColor,
                    trackColor = contentColor.copy(alpha = 0.12f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

private const val FULL_DAY_MS = 24 * 60 * 60 * 1000L

@Composable
private fun rememberNowMs(tickMs: Long): Long {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(tickMs) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(tickMs)
        }
    }
    return nowMs
}

@Composable
private fun rememberAdaptiveNowMs(
    statuses: Map<String, GroupedSchedule?>,
    fastThresholdMs: Long = 60_000L,
    fastTickMs: Long = 1_000L,
    slowTickMs: Long = 60_000L
): Long {
    val tickMs by remember(statuses) {
        derivedStateOf {
            val now = System.currentTimeMillis()
            val minRemaining = statuses.values
                .filterNotNull()
                .map { it.endMs - now }
                .filter { it > 0 }
                .minOrNull()
            if (minRemaining != null && minRemaining <= fastThresholdMs) fastTickMs else slowTickMs
        }
    }
    return rememberNowMs(tickMs)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyAddressesView(onAdd: () -> Unit, contentPadding: PaddingValues) {
    com.occaecat.ztoeschedule.presentation.ui.components.EmptyStatePage(
        icon = Icons.Default.AddLocationAlt,
        shape = MaterialShapes.Clover4Leaf.toShape(),
        title = "Ще немає адрес",
        body = "Додайте дім, роботу чи рідних, щоб стежити за графіком для кожної адреси",
        contentPadding = contentPadding,
        actionText = "Додати адресу",
        actionIcon = Icons.Default.Add,
        onAction = onAdd,
        chips = listOf(
            com.occaecat.ztoeschedule.presentation.ui.components.EmptyStateChip(Icons.Default.Edit, "Вручну"),
            com.occaecat.ztoeschedule.presentation.ui.components.EmptyStateChip(Icons.Default.MyLocation, "GPS"),
            com.occaecat.ztoeschedule.presentation.ui.components.EmptyStateChip(Icons.Default.QrCodeScanner, "QR-код")
        )
    )
}
