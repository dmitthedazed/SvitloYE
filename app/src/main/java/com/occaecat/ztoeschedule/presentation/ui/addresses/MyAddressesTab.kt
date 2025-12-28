package com.occaecat.ztoeschedule.presentation.ui.addresses

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.onFocusChanged
import com.occaecat.ztoeschedule.data.model.*
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.TimeUtils
import com.occaecat.ztoeschedule.presentation.ui.home.HomeTab
import com.occaecat.ztoeschedule.ui.theme.OctagonShape
import com.occaecat.ztoeschedule.presentation.ui.components.ShimmerItem
import com.occaecat.ztoeschedule.presentation.ui.components.ScaleIndication
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
                    "e" -> EmptyAddressesView(onStartAdding, Modifier.fillMaxSize().padding(contentPadding))
                    else -> DraggableAddressList(addresses, addressStatuses, if (useWideLayout) selectedId else null, onDeleteAddress, onStartAdding, onUpdateOrder, { if (useWideLayout) selectedId = it.id else onInspectAddress(it) }, Modifier.fillMaxSize(), contentPadding)
                }
            }
        }
        if (useWideLayout) {
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Box(Modifier.weight(1.5f)) {
                if (selectedAddr != null) {
                    if (isInspectingLoading && inspectedGroupedSchedule.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    else HomeTab(selectedAddr.remName, selectedAddr.cityName, selectedAddr.streetName, selectedAddr.addressName, selectedAddr.cherga, selectedAddr.pidcherga, null, inspectedScheduleList, inspectedGroupedSchedule, { onInspectAddress(selectedAddr) }, Modifier.fillMaxSize(), contentPadding, "", false, isInspectingLoading)
                } else Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Оберіть адресу зліва", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun AddressesSkeleton(cp: PaddingValues, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(cp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) { ShimmerItem(120.dp, shape = MaterialTheme.shapes.extraLarge) }
        Spacer(Modifier.height(8.dp)); ShimmerItem(64.dp, shape = OctagonShape)
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

    LazyColumn(
        state = listState, 
        modifier = modifier.fillMaxSize().testTag("address_list"), 
        contentPadding = PaddingValues(
            start = cp.calculateStartPadding(LayoutDirection.Ltr) + 16.dp, 
            top = cp.calculateTopPadding() + 16.dp, 
            end = cp.calculateEndPadding(LayoutDirection.Ltr) + 16.dp, 
            bottom = cp.calculateBottomPadding() + 80.dp
        ), 
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(list, key = { _, item -> item.id }) { idx, addr ->
            val isD = idx == dragIdx; val isS = addr.id == selectedId
            val scale by animateFloatAsState(if (isD) 1.05f else 1f, spring(Spring.DampingRatioLowBouncy), label = "s")
            val alpha by animateFloatAsState(if (isD) 0.8f else 1f, label = "a")
            val dState = rememberSwipeToDismissBoxState(confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { dId = addr.id; false } else false })
            var showMenu by remember { mutableStateOf(false) }
            Box(Modifier.zIndex(if (isD) 1f else 0f).animateItem()) {
                SwipeToDismissBox(state = dState, enableDismissFromStartToEnd = false, backgroundContent = {
                    val p = dState.progress
                    val c = androidx.compose.ui.graphics.lerp(Color.LightGray.copy(alpha = 0.2f), MaterialTheme.colorScheme.errorContainer, if (dState.dismissDirection == SwipeToDismissBoxValue.EndToStart) p else 0f)
                    Box(Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge).background(c).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                        Icon(Icons.Default.Delete, null, tint = if (p > 0.5f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }) {
                    AddressItem(
                        a = addr, 
                        s = statuses[addr.id], 
                        isP = (idx == 0), 
                        isSel = isS, 
                        onDelete = { dId = addr.id }, 
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect(addr) },
                        modifier = Modifier.graphicsLayer { translationY = if (isD) dragOff else 0f; scaleX = scale; scaleY = scale; this.alpha = alpha }.shadow(if (isD) 16.dp else 0.dp, MaterialTheme.shapes.extraLarge)
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
        item { Spacer(Modifier.height(80.dp)) }
    }
    if (pDialog != null) AlertDialog(onDismissRequest = { pId = null; list = addrs }, icon = { Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary) }, title = { Text("Змінити головну адресу?") }, text = { Text("Ви вибрали ${pDialog.name} як основну.") }, confirmButton = { Button(onClick = { onUpdate(list); pId = null }) { Text("Так") } }, dismissButton = { TextButton(onClick = { pId = null; list = addrs }) { Text("Скасувати") } })
    if (dAddr != null) AlertDialog(onDismissRequest = { dId = null }, icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) }, title = { Text("Видалити адресу?") }, text = { Text("Ви впевнені, що хочете видалити ${dAddr.name}?") }, confirmButton = { TextButton(onClick = { onDelete(dAddr.id); dId = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Видалити") } }, dismissButton = { TextButton(onClick = { dId = null }) { Text("Залишити") } })
}

@Composable
private fun AddressItem(a: SavedAddress, s: GroupedSchedule?, isP: Boolean, isSel: Boolean, onDelete: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var isF by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    val t = updateTransition(targetState = isSel to isP, label = "t")
    
    val containerColor by t.animateColor(label = "c") { state ->
        when {
            state.first -> colorScheme.secondaryContainer
            state.second -> colorScheme.primaryContainer
            else -> colorScheme.surfaceContainerLow
        }
    }
    
    val onContainerColor by t.animateColor(label = "oc") { state ->
        when {
            state.first -> colorScheme.onSecondaryContainer
            state.second -> colorScheme.onPrimaryContainer
            else -> colorScheme.onSurface
        }
    }

    val elevation by t.animateDp(label = "e") { state -> if (state.first) 8.dp else 2.dp }
    val interactionSource = remember { MutableInteractionSource() }
    
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .indication(interactionSource, ScaleIndication)
            .onFocusChanged { isF = it.isFocused }
            .border(2.dp, if (isF) colorScheme.primary else Color.Transparent, MaterialTheme.shapes.extraLarge)
            .semantics { onClick(label = "проглянути", action = { onClick(); true }) },
        shape = MaterialTheme.shapes.extraLarge, 
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = onContainerColor
        ), 
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
        interactionSource = interactionSource
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp), 
                    shape = OctagonShape, 
                    color = if(isP) colorScheme.primary else colorScheme.surfaceVariant
                ) { 
                    Box(contentAlignment = Alignment.Center) { 
                        Icon(
                            imageVector = getIconForName(a.iconName), 
                            contentDescription = null, 
                            modifier = Modifier.padding(10.dp), 
                            tint = if(isP) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                        ) 
                    } 
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { 
                    Text(
                        text = a.name, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = onContainerColor
                    ) 
                    Text(
                        text = "${a.cityName}, ${a.streetName}", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = onContainerColor.copy(alpha = 0.8f)
                    ) 
                }
                IconButton(
                    onClick = onDelete, 
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), 
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if(isP || isSel) onContainerColor else colorScheme.error
                    )
                ) { 
                    Icon(Icons.Default.DeleteOutline, "Видалити ${a.name}") 
                }
            }
            if (s != null) { 
                Spacer(Modifier.height(12.dp))
                StatusInfoSection(s, onContainerColor) 
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.spacedBy(8.dp), 
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) { 
                SuggestionChip(
                    onClick = {}, 
                    label = { Text("${a.cherga}.${a.pidcherga}") }, 
                    shape = CircleShape, 
                    border = null, 
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = colorScheme.surfaceVariant,
                        labelColor = colorScheme.onSurfaceVariant
                    )
                )
                if (isP) { 
                    AssistChip(
                        onClick = {},
                        label = { 
                            Text(
                                text = "Головна", 
                                style = MaterialTheme.typography.labelSmall, 
                                fontWeight = FontWeight.Bold
                            ) 
                        },
                        shape = CircleShape,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = colorScheme.primary,
                            labelColor = colorScheme.onPrimary
                        ),
                        border = null
                    )
                } 
            }
        }
    }
}

@Composable
private fun StatusInfoSection(s: GroupedSchedule, contentColor: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // Status color logic - ensured to be high contrast on card
    val statusColor = when(s.status) { 
        ScheduleStatus.Outage -> colorScheme.error
        ScheduleStatus.Probable -> colorScheme.tertiary
        else -> colorScheme.primary 
    }
    
    val animatedStatusColor by animateColorAsState(statusColor, label = "sc")
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { nowMs = System.currentTimeMillis(); delay(10000) } }
    
    val progress = remember(s, nowMs) { 
        val d = s.endMs - s.startMs
        if (d <= 0) 0f else ((nowMs - s.startMs).toFloat() / d.toFloat()).coerceIn(0f, 1f) 
    }
    val animatedProgress by animateFloatAsState(progress, label = "p")
    
    Column(modifier = modifier.fillMaxWidth().testTag("status_info_section")) {
        Row(verticalAlignment = Alignment.CenterVertically) { 
            Box(
                Modifier
                    .size(8.dp)
                    .clip(OctagonShape)
                    .background(animatedStatusColor)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = s.displayText, 
                style = MaterialTheme.typography.labelLarge, 
                color = contentColor,
                fontWeight = FontWeight.Bold, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis, 
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "До ${TimeUtils.formatToSystemTime(context, s.endTime)}", 
                style = MaterialTheme.typography.bodySmall, 
                color = contentColor.copy(alpha = 0.7f),
                maxLines = 1
            ) 
        }
        Spacer(Modifier.height(8.dp))
        // High contrast progress bar
        LinearProgressIndicator(
            progress = { animatedProgress }, 
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape), 
            color = animatedStatusColor, 
            trackColor = contentColor.copy(alpha = 0.15f), // High contrast track
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun EmptyAddressesView(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.testTag("empty_addresses_view"), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) { Icon(Icons.Default.LocationOff, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)); Text("Список адрес порожній", style = MaterialTheme.typography.titleMedium); Button(onClick = onAdd, shape = OctagonShape) { Text("Додати адресу") } } }
}

private fun getIconForName(name: String) = when (name) { "home" -> Icons.Default.Home; "apartment" -> Icons.Default.Apartment; "work" -> Icons.Default.Work; "school" -> Icons.Default.School; "star" -> Icons.Default.Star; else -> Icons.Default.LocationOn }