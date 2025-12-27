package com.occaecat.ztoeschedule.presentation.ui.addresses

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.occaecat.ztoeschedule.data.model.*
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.TimeUtils
import com.occaecat.ztoeschedule.presentation.ui.home.HomeTab
import com.occaecat.ztoeschedule.ui.theme.OctagonShape
import java.util.Collections
import com.occaecat.ztoeschedule.presentation.ui.components.ShimmerItem
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAddressesTab(
    addresses: List<SavedAddress>,
    addressStatuses: Map<String, GroupedSchedule?>,
    isAddingNew: Boolean,
    remList: List<Rem>,
    cityList: List<City>,
    streetList: List<Street>,
    houseNumbers: List<ParsedHouseNumber>,
    searchQuery: String,
    isLoading: Boolean,
    useWideLayout: Boolean = false,
    inspectedScheduleList: List<Schedule> = emptyList(),
    inspectedGroupedSchedule: List<GroupedSchedule> = emptyList(),
    isInspectingLoading: Boolean = false,
    onStartAdding: () -> Unit,
    onCancelAdding: () -> Unit,
    onLoadRem: () -> Unit,
    onLoadCity: (String) -> Unit,
    onLoadStreet: (String) -> Unit,
    onLoadAddress: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSaveAddress: (name: String, icon: String, remId: String, remName: String, cityId: String, cityName: String, streetId: String, streetName: String, addressId: String, addressName: String, cherga: Int, pidcherga: Int) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onUpdateOrder: (List<SavedAddress>) -> Unit,
    onRefreshAddress: (Int, Int) -> Unit = { _, _ -> },
    onInspectAddress: (SavedAddress) -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    // Persist selection across rotation
    var selectedAddressId by rememberSaveable { mutableStateOf(addresses.firstOrNull()?.id) }
    
    // Update selection if list changes and nothing is selected, but don't override user choice
    LaunchedEffect(addresses) {
        if (selectedAddressId == null && addresses.isNotEmpty()) {
            selectedAddressId = addresses.first().id
        }
    }

    val selectedAddress = remember(selectedAddressId, addresses) { addresses.find { it.id == selectedAddressId } }

    LaunchedEffect(selectedAddressId, useWideLayout) {
        if (useWideLayout && selectedAddress != null) onInspectAddress(selectedAddress)
    }

    Row(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading && addresses.isEmpty()) {
                AddressesSkeleton(contentPadding)
            } else if (addresses.isEmpty()) {
                EmptyAddressesView(onStartAdding, Modifier.fillMaxSize().padding(contentPadding))
            } else {
                DraggableAddressList(
                    addresses = addresses, statuses = addressStatuses,
                    selectedId = if (useWideLayout) selectedAddressId else null,
                    onDelete = onDeleteAddress, onStartAdding = onStartAdding, onUpdateOrder = onUpdateOrder,
                    onSelect = { if (useWideLayout) selectedAddressId = it.id else onInspectAddress(it) },
                    modifier = Modifier.fillMaxSize(), contentPadding = contentPadding
                )
            }
        }

        if (useWideLayout) {
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Box(modifier = Modifier.weight(1.5f)) {
                if (selectedAddress != null) {
                    if (isInspectingLoading && inspectedGroupedSchedule.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    } else {
                        HomeTab(selectedAddress.remName, selectedAddress.cityName, selectedAddress.streetName, selectedAddress.addressName, selectedAddress.cherga, selectedAddress.pidcherga, null, inspectedScheduleList, inspectedGroupedSchedule, { onInspectAddress(selectedAddress) }, Modifier.fillMaxSize(), contentPadding, "", false, isInspectingLoading)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Оберіть адресу зліва", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun AddressesSkeleton(contentPadding: PaddingValues) {
    val ld = LocalLayoutDirection.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = contentPadding.calculateStartPadding(ld) + 16.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                end = contentPadding.calculateEndPadding(ld) + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            ShimmerItem(height = 120.dp, shape = MaterialTheme.shapes.extraLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerItem(height = 64.dp, shape = OctagonShape)
    }
}

@Composable
private fun DraggableAddressList(
    addresses: List<SavedAddress>,
    statuses: Map<String, GroupedSchedule?>,
    selectedId: String?,
    onDelete: (String) -> Unit,
    onStartAdding: () -> Unit,
    onUpdateOrder: (List<SavedAddress>) -> Unit,
    onSelect: (SavedAddress) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var list by remember(addresses) { mutableStateOf(addresses) }
    
    // We only need to save the ID of the address to delete to survive rotation
    var addressToDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val addressToDelete = remember(addressToDeleteId, addresses) { addresses.find { it.id == addressToDeleteId } }
    
    // Similarly for primary change dialog
    var showPrimaryChangeId by rememberSaveable { mutableStateOf<String?>(null) }
    val showPrimaryChangeDialog = remember(showPrimaryChangeId, addresses) { addresses.find { it.id == showPrimaryChangeId } }

    val initialPrimaryId = remember(addresses) { addresses.firstOrNull()?.id }
    val lazyListState = rememberLazyListState()
    var draggedIndex by rememberSaveable { mutableIntStateOf(-1) }
    var dragOffsetY by rememberSaveable { mutableFloatStateOf(0f) }
    val ld = LocalLayoutDirection.current
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        state = lazyListState, modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = contentPadding.calculateStartPadding(ld) + 16.dp, top = contentPadding.calculateTopPadding() + 16.dp, end = contentPadding.calculateEndPadding(ld) + 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(list, key = { _, item -> item.id }) { index, address ->
            val isDragging = index == draggedIndex
            val isSelected = address.id == selectedId
            val scale by animateFloatAsState(
                targetValue = if (isDragging) 1.05f else 1f, 
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "s"
            )
            
            Box(modifier = Modifier.zIndex(if (isDragging) 1f else 0f).animateItem()) {
                AddressItem(
                    address, statuses[address.id], index == 0, isSelected, { addressToDeleteId = address.id }, { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(address) 
                    },
                    Modifier.graphicsLayer { translationY = if (isDragging) dragOffsetY else 0f; scaleX = scale; scaleY = scale }
                        .shadow(if (isDragging) 12.dp else 0.dp, MaterialTheme.shapes.extraLarge)
                        .pointerInput(address.id) { // Use address.id as key
                            detectDragGesturesAfterLongPress(
                                onDragStart = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggedIndex = index; dragOffsetY = 0f 
                                },
                                onDragEnd = { 
                                    if (list.firstOrNull()?.id != initialPrimaryId) showPrimaryChangeId = list.first().id else onUpdateOrder(list)
                                    draggedIndex = -1 
                                },
                                onDragCancel = { draggedIndex = -1 },
                                onDrag = { change, amount ->
                                    change.consume(); dragOffsetY += amount.y
                                    val itemHeight = lazyListState.layoutInfo.visibleItemsInfo.find { it.index == draggedIndex }?.size ?: 500
                                    if (kotlin.math.abs(dragOffsetY) > itemHeight / 2f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (dragOffsetY > 0 && draggedIndex < list.size - 1) {
                                            val newList = list.toMutableList(); Collections.swap(newList, draggedIndex, draggedIndex + 1)
                                            list = newList; draggedIndex++; dragOffsetY -= itemHeight
                                        } else if (dragOffsetY < 0 && draggedIndex > 0) {
                                            val newList = list.toMutableList(); Collections.swap(newList, draggedIndex, draggedIndex - 1)
                                            list = newList; draggedIndex--; dragOffsetY += itemHeight
                                        }
                                    }
                                }
                            )
                        }
                )
            }
        }
        item {
            Button(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStartAdding() 
                }, 
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(top = 8.dp), 
                shape = OctagonShape, // Use 2025 Octagon Shape
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            ) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(12.dp)); Text("Додати нову адресу", fontWeight = FontWeight.Bold)
            }
        }
    }
    if (showPrimaryChangeDialog != null) AlertDialog(onDismissRequest = { showPrimaryChangeId = null; list = addresses }, title = { Text("Змінити головну?") }, confirmButton = { Button(onClick = { onUpdateOrder(list); showPrimaryChangeId = null }) { Text("Так") } })
    if (addressToDelete != null) AlertDialog(onDismissRequest = { addressToDeleteId = null }, title = { Text("Видалити?") }, confirmButton = { TextButton(onClick = { onDelete(addressToDelete!!.id); addressToDeleteId = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Так") } })
}

@Composable
private fun AddressItem(address: SavedAddress, status: GroupedSchedule?, isPrimary: Boolean, isSelected: Boolean, onDelete: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(
        onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
        colors = when {
            isSelected -> CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            isPrimary -> CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            else -> CardDefaults.elevatedCardColors()
        }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = OctagonShape, color = if(isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(44.dp)) {
                    Icon(getIconForName(address.iconName), null, Modifier.padding(10.dp), if(isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(address.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${address.cityName}, ${address.streetName}", style = MaterialTheme.typography.bodySmall, color = if(isPrimary) MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, null, tint = if(isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error) }
            }
            if (status != null) {
                Spacer(Modifier.height(12.dp))
                StatusInfoSection(status)
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = {}, 
                    label = { Text("${address.cherga}.${address.pidcherga}") }, 
                    shape = CircleShape, 
                    border = null, 
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = (if(isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.surfaceVariant).copy(0.1f)
                    )
                )
                
                if (isPrimary) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Головна", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusInfoSection(status: GroupedSchedule) {
    val context = LocalContext.current
    val color = when(status.status) { 
        com.occaecat.ztoeschedule.data.model.ScheduleStatus.OUTAGE -> MaterialTheme.colorScheme.error 
        com.occaecat.ztoeschedule.data.model.ScheduleStatus.PROBABLE -> MaterialTheme.colorScheme.tertiary 
        else -> MaterialTheme.colorScheme.primary 
    }
    
    // Ticker to force recomposition for progress bar
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { // Use Unit to keep the loop running independently of data changes
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(10000) // Update every 10s is enough for address list
        }
    }
    
    val progress = remember(status, nowMs) {
        val durationMs = status.endMs - status.startMs
        if (durationMs <= 0) 0f
        else ((nowMs - status.startMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(OctagonShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(
                text = status.displayText, 
                style = MaterialTheme.typography.labelLarge, 
                color = color, 
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "До ${TimeUtils.formatToSystemTime(context, status.endTime)}", 
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = color, trackColor = color.copy(0.1f), strokeCap = StrokeCap.Round)
    }
}

@Composable
private fun EmptyAddressesView(onStartAdding: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.LocationOff, null, Modifier.size(64.dp), MaterialTheme.colorScheme.secondary.copy(0.5f))
            Text("Список адрес порожній", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onStartAdding, shape = OctagonShape) { Text("Додати адресу") }
        }
    }
}

private fun getIconForName(name: String) = when (name) { "home" -> Icons.Default.Home; "apartment" -> Icons.Default.Apartment; "work" -> Icons.Default.Work; "school" -> Icons.Default.School; "star" -> Icons.Default.Star; else -> Icons.Default.LocationOn }
