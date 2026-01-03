package com.occaecat.ztoeschedule.presentation.ui.addresses

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Stable
import kotlinx.coroutines.launch
import com.occaecat.ztoeschedule.data.model.City
import com.occaecat.ztoeschedule.data.model.Rem
import com.occaecat.ztoeschedule.data.model.Street
import com.occaecat.ztoeschedule.data.repository.ConsumerCategory
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber
import com.occaecat.ztoeschedule.presentation.ui.onboarding.CitySelectionContent
import com.occaecat.ztoeschedule.presentation.ui.onboarding.HouseSelectionContent
import com.occaecat.ztoeschedule.presentation.ui.onboarding.RemSelectionContent
import com.occaecat.ztoeschedule.presentation.ui.onboarding.StreetSelectionContent
import com.occaecat.ztoeschedule.presentation.ui.addresses.availableIcons

/**
 * Unified address picker used by onboarding and the Add Address flow.
 * Presents REM -> City -> Street -> House -> Customize steps with shared logic.
 */
@Stable
data class AddressPickerResult(
    val remId: String,
    val remName: String,
    val cityId: String,
    val cityName: String,
    val streetId: String,
    val streetName: String,
    val addressId: String,
    val addressName: String,
    val cherga: Int,
    val pidcherga: Int,
    val displayName: String,
    val iconName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressPickerDialog(
    remList: List<Rem>,
    cityList: List<City>,
    streetList: List<Street>,
    houseNumbers: List<ParsedHouseNumber>,
    searchQuery: String,
    isLoading: Boolean,
    selectedCategory: ConsumerCategory?,
    onLoadRem: () -> Unit,
    onLoadCity: (String) -> Unit,
    onLoadStreet: (String) -> Unit,
    onLoadAddress: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (ConsumerCategory?) -> Unit,
    onClearSearch: () -> Unit,
    onDismiss: () -> Unit,
    onComplete: (AddressPickerResult) -> Unit,
    showSheet: Boolean = true,
    initialRem: Rem? = null,
    initialCity: City? = null,
    initialStreet: Street? = null,
    initialHouse: ParsedHouseNumber? = null,
    initialName: String = "",
    initialIcon: String = "home"
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
        ) {
        UnifiedAddressPicker(
            remList = remList,
            cityList = cityList,
            streetList = streetList,
            houseNumbers = houseNumbers,
            searchQuery = searchQuery,
            isLoading = isLoading,
            selectedCategory = selectedCategory,
            onLoadRem = onLoadRem,
            onLoadCity = onLoadCity,
            onLoadStreet = onLoadStreet,
            onLoadAddress = onLoadAddress,
            onSearchQueryChange = onSearchQueryChange,
            onCategorySelected = onCategorySelected,
            onClearSearch = onClearSearch,
            onCancel = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            },
            onComplete = { result ->
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onComplete(result)
                }
            },
            initialRem = initialRem,
            initialCity = initialCity,
            initialStreet = initialStreet,
            initialHouse = initialHouse,
            initialName = initialName,
            initialIcon = initialIcon,
            showTopBar = false,
            contentPadding = PaddingValues(16.dp)
        )
        }
    }
}

@Composable
fun AddressPickerScreen(
    remList: List<Rem>,
    cityList: List<City>,
    streetList: List<Street>,
    houseNumbers: List<ParsedHouseNumber>,
    searchQuery: String,
    isLoading: Boolean,
    selectedCategory: ConsumerCategory?,
    onLoadRem: () -> Unit,
    onLoadCity: (String) -> Unit,
    onLoadStreet: (String) -> Unit,
    onLoadAddress: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (ConsumerCategory?) -> Unit,
    onClearSearch: () -> Unit,
    onCancel: () -> Unit,
    onComplete: (AddressPickerResult) -> Unit,
    onGoBack: (() -> Unit)? = null,
    onStepChanged: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    initialRem: Rem? = null,
    initialCity: City? = null,
    initialStreet: Street? = null,
    initialHouse: ParsedHouseNumber? = null,
    initialName: String = "",
    initialIcon: String = "home"
) {
    UnifiedAddressPicker(
        remList = remList,
        cityList = cityList,
        streetList = streetList,
        houseNumbers = houseNumbers,
        searchQuery = searchQuery,
        isLoading = isLoading,
        selectedCategory = selectedCategory,
        onLoadRem = onLoadRem,
        onLoadCity = onLoadCity,
        onLoadStreet = onLoadStreet,
        onLoadAddress = onLoadAddress,
        onSearchQueryChange = onSearchQueryChange,
        onCategorySelected = onCategorySelected,
        onClearSearch = onClearSearch,
        onCancel = onCancel,
        onComplete = onComplete,
        onGoBack = onGoBack,
        onStepChanged = onStepChanged,
        initialRem = initialRem,
        initialCity = initialCity,
        initialStreet = initialStreet,
        initialHouse = initialHouse,
        initialName = initialName,
        initialIcon = initialIcon,
        modifier = modifier,
        showTopBar = true,
        contentPadding = PaddingValues(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnifiedAddressPicker(
    remList: List<Rem>,
    cityList: List<City>,
    streetList: List<Street>,
    houseNumbers: List<ParsedHouseNumber>,
    searchQuery: String,
    isLoading: Boolean,
    selectedCategory: ConsumerCategory?,
    onLoadRem: () -> Unit,
    onLoadCity: (String) -> Unit,
    onLoadStreet: (String) -> Unit,
    onLoadAddress: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (ConsumerCategory?) -> Unit,
    onClearSearch: () -> Unit,
    onCancel: () -> Unit,
    onComplete: (AddressPickerResult) -> Unit,
    onGoBack: (() -> Unit)? = null,
    onStepChanged: ((Int) -> Unit)? = null,
    initialRem: Rem?,
    initialCity: City?,
    initialStreet: Street?,
    initialHouse: ParsedHouseNumber?,
    initialName: String,
    initialIcon: String,
    modifier: Modifier = Modifier,
    showTopBar: Boolean,
    contentPadding: PaddingValues
) {
    var step by remember { mutableIntStateOf(if (initialHouse != null) 5 else 1) }
    var selectedRem by remember { mutableStateOf(initialRem) }
    var selectedCity by remember { mutableStateOf(initialCity) }
    var selectedStreet by remember { mutableStateOf(initialStreet) }
    var selectedHouse by remember { mutableStateOf(initialHouse) }
    var name by remember { mutableStateOf(initialName) }
    var icon by remember { mutableStateOf(initialIcon) }
    var userEditedName by remember { mutableStateOf(initialName.isNotBlank()) }

    // Notify about step changes
    LaunchedEffect(step) {
        onStepChanged?.invoke(step)
    }

    // Handle system back button
    BackHandler(enabled = step > 1) {
        step--
    }

    LaunchedEffect(selectedRem) { if (selectedRem != null && cityList.isEmpty()) onLoadCity(selectedRem!!.id) }
    LaunchedEffect(selectedCity) { if (selectedCity != null && streetList.isEmpty()) onLoadStreet(selectedCity!!.id) }
    LaunchedEffect(selectedStreet) { if (selectedStreet != null && houseNumbers.isEmpty()) onLoadAddress(selectedStreet!!.id) }
    LaunchedEffect(Unit) { if (remList.isEmpty()) onLoadRem() }

    Scaffold(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f),
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Нова адреса", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (step > 1) {
                                step--
                            } else {
                                onCancel()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Filled.Close, contentDescription = "Закрити")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StepHeader(step = step, total = 5)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }
                    },
                    label = "unified_address_step"
                ) { targetStep ->
                    when (targetStep) {
                        1 -> RemSelectionContent(
                            remList = remList,
                            isLoading = isLoading,
                            onLoadRem = onLoadRem,
                            onRemSelected = {
                                selectedRem = it
                                selectedCity = null
                                selectedStreet = null
                                selectedHouse = null
                                onLoadCity(it.id)
                                step = 2
                            },
                            selectedRem = selectedRem
                        )
                        2 -> CitySelectionContent(
                            cityList = cityList,
                            isLoading = isLoading,
                            remName = selectedRem?.name ?: "",
                            selectedCity = selectedCity,
                            onCitySelected = {
                                selectedCity = it
                                selectedStreet = null
                                selectedHouse = null
                                onLoadStreet(it.id)
                                step = 3
                            }
                        )
                        3 -> StreetSelectionContent(
                            streetList = streetList,
                            isLoading = isLoading,
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            onClearSearch = onClearSearch,
                            onStreetSelected = {
                                selectedStreet = it
                                selectedHouse = null
                                onClearSearch()  // Clear house search when selecting new street
                                onLoadAddress(it.id)
                                step = 4
                            },
                            selectedStreet = selectedStreet
                        )
                        4 -> HouseSelectionContent(
                            houseNumbers = houseNumbers,
                            isLoading = isLoading,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            onSearchQueryChange = onSearchQueryChange,
                            onCategorySelected = onCategorySelected,
                            onHouseSelected = {
                                selectedHouse = it
                                // Set default name to icon label if user hasn't edited name
                                if (!userEditedName) {
                                    val iconLabel = availableIcons.find { iconItem -> iconItem.name == icon }?.label ?: "Дім"
                                    name = iconLabel
                                }
                                step = 5
                            },
                            selectedHouse = selectedHouse
                        )
                        5 -> CustomizationStep(
                            name = name,
                            icon = icon,
                            onNameChange = { 
                                name = it
                                userEditedName = true
                            },
                            onIconChange = { 
                                icon = it
                                // Update name to icon label if user hasn't manually edited it
                                if (!userEditedName) {
                                    val iconLabel = availableIcons.find { iconItem -> iconItem.name == it }?.label ?: "Дім"
                                    name = iconLabel
                                }
                            }
                        )
                    }
                }
            }

            ActionRow(
                step = step,
                canGoBack = step > 1,
                onBack = { if (step > 1) step-- else onCancel() },
                onCancel = onCancel,
                onContinue = {
                    if (step < 5) {
                        step++
                    } else if (selectedRem != null && selectedCity != null && selectedStreet != null && selectedHouse != null) {
                        onComplete(
                            AddressPickerResult(
                                remId = selectedRem!!.id,
                                remName = selectedRem!!.name,
                                cityId = selectedCity!!.id,
                                cityName = selectedCity!!.name,
                                streetId = selectedStreet!!.id,
                                streetName = selectedStreet!!.name,
                                addressId = selectedHouse!!.originalAddressId,
                                addressName = selectedHouse!!.houseNumber,
                                cherga = selectedHouse!!.cherga,
                                pidcherga = selectedHouse!!.pidcherga,
                                displayName = name.ifBlank { buildAddressLabel(selectedCity, selectedStreet, selectedHouse) },
                                iconName = icon
                            )
                        )
                    }
                },
                canContinue = when (step) {
                    1 -> selectedRem != null
                    2 -> selectedCity != null
                    3 -> selectedStreet != null
                    4 -> selectedHouse != null
                    else -> selectedRem != null && selectedCity != null && selectedStreet != null && selectedHouse != null && name.isNotBlank()
                },
                isLast = step == 5
            )
        }
    }
}

@Composable
private fun StepHeader(step: Int, total: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Крок $step з $total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text("Вибір адреси", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
            LinearProgressIndicator(
                progress = { step.toFloat() / total },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomizationStep(
    name: String,
    icon: String,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Назва локації") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            supportingText = { Text("Введіть назву для цієї адреси") }
        )

        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        Text("Іконка", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            availableIcons.forEach { item ->
                val selected = icon == item.name
                AssistChip(
                    onClick = { onIconChange(item.name) },
                    label = { Text(item.label) },
                    leadingIcon = { Icon(item.icon, contentDescription = null) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }
        }
    }
}

@Composable
private fun ActionRow(step: Int, canGoBack: Boolean, onBack: () -> Unit, onCancel: () -> Unit, onContinue: () -> Unit, canContinue: Boolean, isLast: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = if (canGoBack) onBack else onCancel,
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(if (canGoBack) "Назад" else "Скасувати")
        }
        Button(
            onClick = onContinue,
            enabled = canContinue,
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(if (isLast) "Готово" else "Далі")
        }
    }
}

private fun buildAddressLabel(city: City?, street: Street?, house: ParsedHouseNumber?): String {
    val parts = listOfNotNull(city?.name, street?.name, house?.houseNumber)
    return parts.joinToString(", ")
}
