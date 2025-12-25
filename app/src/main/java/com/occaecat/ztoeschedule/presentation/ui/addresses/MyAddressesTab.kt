package com.occaecat.ztoeschedule.presentation.ui.addresses

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.occaecat.ztoeschedule.data.model.City
import com.occaecat.ztoeschedule.data.model.Rem
import com.occaecat.ztoeschedule.data.model.SavedAddress
import com.occaecat.ztoeschedule.data.model.Street
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.TimeUtils
import java.util.Calendar
import java.util.Collections
import java.util.TimeZone

/**
 * My Addresses tab with draggable cards for priority management.
 * Locked to Europe/Kyiv timezone for status and progress.
 */
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
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (isAddingNew) {
        var tempSelection by remember { mutableStateOf<TempSelection?>(null) }

        if (tempSelection == null) {
            AddressSelectionFlow(
                remList = remList,
                cityList = cityList,
                streetList = streetList,
                houseNumbers = houseNumbers,
                searchQuery = searchQuery,
                isLoading = isLoading,
                onLoadRem = onLoadRem,
                onLoadCity = onLoadCity,
                onLoadStreet = onLoadStreet,
                onLoadAddress = onLoadAddress,
                onSearchQueryChange = onSearchQueryChange,
                onClearSearch = onClearSearch,
                onCancel = onCancelAdding,
                onComplete = { remId, remName, cityId, cityName, streetId, streetName, addrId, addrName, cherga, pid ->
                    tempSelection = TempSelection(
                        remId ?: "", remName ?: "", cityId ?: "", cityName ?: "",
                        streetId ?: "", streetName ?: "", addrId, addrName, cherga, pid
                    )
                }
            )
        } else {
            AddressCustomizationScreen(
                onComplete = { name, icon ->
                    val s = tempSelection!!
                    onSaveAddress(
                        name, icon, s.remId, s.remName, s.cityId, s.cityName,
                        s.streetId, s.streetName, s.addressId, s.addressName, s.cherga, s.pidcherga
                    )
                },
                onBack = { tempSelection = null }
            )
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = addresses.isEmpty(),
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "addresses_state_transition"
        ) { isEmpty ->
            if (isEmpty) {
                EmptyAddressesView(onStartAdding, modifier = Modifier.fillMaxSize().padding(contentPadding))
            } else {
                DraggableAddressList(
                    addresses = addresses,
                    statuses = addressStatuses,
                    onDelete = onDeleteAddress,
                    onStartAdding = onStartAdding,
                    onUpdateOrder = onUpdateOrder,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding
                )
            }
        }
    }
}

@Composable
private fun DraggableAddressList(
    addresses: List<SavedAddress>,
    statuses: Map<String, GroupedSchedule?>,
    onDelete: (String) -> Unit,
    onStartAdding: () -> Unit,
    onUpdateOrder: (List<SavedAddress>) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var list by remember(addresses) { mutableStateOf(addresses) }
    var addressToDelete by remember { mutableStateOf<SavedAddress?>(null) }
    var showPrimaryChangeDialog by remember { mutableStateOf<SavedAddress?>(null) }
    val initialPrimaryId = remember(addresses) { addresses.firstOrNull()?.id }

    val lazyListState = rememberLazyListState()
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            end = contentPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(list, key = { _, item -> item.id }) { index, address ->
            val isDragging = index == draggedIndex
            val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scale")
            val elevation by animateDpAsState(if (isDragging) 16.dp else 2.dp, label = "elevation")

            Box(modifier = Modifier
                .zIndex(if (isDragging) 1f else 0f)
                .animateItem()
            ) {
                AddressItem(
                    address = address,
                    status = statuses[address.id],
                    isPrimary = index == 0,
                    onDelete = { addressToDelete = address },
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffsetY else 0f
                            scaleX = scale
                            scaleY = scale
                        }
                        .shadow(elevation, shape = MaterialTheme.shapes.extraLarge)
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { 
                                    draggedIndex = index 
                                    dragOffsetY = 0f
                                },
                                onDragEnd = {
                                    if (list.firstOrNull()?.id != initialPrimaryId) {
                                        showPrimaryChangeDialog = list.first()
                                    } else {
                                        onUpdateOrder(list)
                                    }
                                    draggedIndex = -1
                                    dragOffsetY = 0f
                                },
                                onDragCancel = { 
                                    draggedIndex = -1
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffsetY += amount.y
                                    
                                    val currentItem = lazyListState.layoutInfo.visibleItemsInfo.find { it.index == draggedIndex }
                                    val itemHeight = currentItem?.size ?: 500
                                    val threshold = itemHeight / 2f
                                    
                                    if (dragOffsetY > threshold && draggedIndex < list.size - 1) {
                                        val newList = list.toMutableList()
                                        Collections.swap(newList, draggedIndex, draggedIndex + 1)
                                        list = newList
                                        draggedIndex++
                                        dragOffsetY -= itemHeight + 48f
                                    } else if (dragOffsetY < -threshold && draggedIndex > 0) {
                                        val newList = list.toMutableList()
                                        Collections.swap(newList, draggedIndex, draggedIndex - 1)
                                        list = newList
                                        draggedIndex--
                                        dragOffsetY += itemHeight + 48f
                                    }
                                }
                            )
                        }
                )
            }
        }

        item(key = "add_address_item") {
            OutlinedCard(
                onClick = onStartAdding,
                modifier = Modifier.fillMaxWidth().animateItem(),
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Додати нову адресу", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showPrimaryChangeDialog != null) {
        AlertDialog(
            onDismissRequest = { 
                showPrimaryChangeDialog = null
                // Reset local list to original state
                list = addresses
            },
            title = { Text("Змінити основну адресу?") },
            text = { Text("Ви перемістили '${showPrimaryChangeDialog!!.name}' на перше место. Тепер вона буде відображатися як головна.") },
            confirmButton = {
                Button(onClick = {
                    onUpdateOrder(list)
                    showPrimaryChangeDialog = null
                }) {
                    Text("Підтвердити")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPrimaryChangeDialog = null
                    // Reset local list to original state
                    list = addresses
                }) {
                    Text("Скасувати")
                }
            }
        )
    }

    if (addressToDelete != null) {
        AlertDialog(
            onDismissRequest = { addressToDelete = null },
            title = { Text("Видалити адресу?") },
            text = { Text("Ви впевнені, що хочете видалити '${addressToDelete!!.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(addressToDelete!!.id)
                        addressToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(onClick = { addressToDelete = null }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
private fun AddressItem(
    address: SavedAddress,
    status: GroupedSchedule?,
    isPrimary: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = if (isPrimary) {
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else CardDefaults.elevatedCardColors()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = getIconForName(address.iconName),
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = address.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (isPrimary) {
                        Text(
                            text = "Основна локація",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Утримуйте для переміщення",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, "Видалити", tint = if(isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            if (status != null) {
                StatusInfoSection(status, isPrimary)
                Spacer(Modifier.height(16.dp))
            }

            Text(
                text = "${address.cityName}, ${address.streetName}, ${address.addressName}",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = { },
                    label = { Text("Черга ${address.cherga}.${address.pidcherga}") },
                    enabled = false,
                    shape = MaterialTheme.shapes.medium
                )
            }
        }
    }
}

@Composable
private fun StatusInfoSection(
    status: GroupedSchedule,
    isPrimary: Boolean
) {
    val context = LocalContext.current
    val color = when(status.color.lowercase()) {
        "red" -> MaterialTheme.colorScheme.error
        "yellow" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val progress = calculateSimpleProgress(status.span)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(10.dp).clip(CircleShape).background(color)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = status.displayText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            
            val endTimeStr = Regex("""(\d{2}:\d{2})""").findAll(status.span).lastOrNull()?.value ?: ""
            val systemEndTime = if (endTimeStr.isNotEmpty()) TimeUtils.formatToSystemTime(context, endTimeStr) else ""
            
            Text(
                text = "До $systemEndTime",
                style = MaterialTheme.typography.bodyMedium,
                color = if(isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(Modifier.height(12.dp))
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.1f),
            strokeCap = StrokeCap.Round
        )
    }
}

private fun calculateSimpleProgress(span: String): Float {
    return try {
        val timeRegex = Regex("""(\d{2}:\d{2})""")
        val matches = timeRegex.findAll(span).toList()
        if (matches.size < 2) return 0.5f
        
        val startStr = matches[0].value
        val endStr = matches[1].value
        
        fun parse(s: String): Int {
            val p = s.split(":")
            return p[0].toInt() * 60 + p[1].toInt()
        }
        
        val start = parse(startStr)
        var end = parse(endStr)
        val isMultiDay = span.contains("(")
        
        if (isMultiDay || end <= start) {
            if (end <= start) end += 1440
        }
        
        val kyivZone = TimeZone.getTimeZone("Europe/Kyiv")
        val now = Calendar.getInstance(kyivZone)
        var current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        
        if (isMultiDay && current < start) {
            current += 1440
        } else if (!isMultiDay && current < start && end > 1440) {
            current += 1440
        }
        
        ((current - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
    } catch (e: Exception) {
        0.5f
    }
}

@Composable
private fun EmptyAddressesView(
    onStartAdding: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = Icons.Default.LocationOff, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
            Text(text = "Список адрес порожній", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = "Додайте свою першу локацію,\nщоб стежити за світлом", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onStartAdding, shape = MaterialTheme.shapes.medium) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Додати адресу")
            }
        }
    }
}

private fun getIconForName(name: String): ImageVector {
    return when (name) {
        "home" -> Icons.Default.Home
        "apartment" -> Icons.Default.Apartment
        "work" -> Icons.Default.Work
        "school" -> Icons.Default.School
        "star" -> Icons.Default.Star
        else -> Icons.Default.LocationOn
    }
}

private data class TempSelection(val remId: String, val remName: String, val cityId: String, val cityName: String, val streetId: String, val streetName: String, val addressId: String, val addressName: String, val cherga: Int, val pidcherga: Int)