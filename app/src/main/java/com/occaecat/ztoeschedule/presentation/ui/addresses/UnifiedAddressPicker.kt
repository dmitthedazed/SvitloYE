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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.ui.graphics.Color
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
    var step by remember { mutableIntStateOf(if (initialHouse != null) 5 else 0) }
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

    // Handle back navigation logic
    val navigateBack = {
        if (step > 0) {
            val prevStep = step - 1
            // Clear selection based on where we are going back TO
            when (prevStep) {
                0 -> selectedRem = null // Going back to method selection clears REM
                1 -> selectedCity = null // Going back to REM clears City
                2 -> selectedStreet = null // Going back to City clears Street
                3 -> selectedHouse = null // Going back to Street clears House
            }
            // Clear subsequent selections recursively (cascade)
            if (prevStep < 4) selectedHouse = null
            if (prevStep < 3) selectedStreet = null
            if (prevStep < 2) selectedCity = null
            if (prevStep < 1) selectedRem = null
            
            step = prevStep
        } else {
            onCancel()
        }
    }

    // Handle system back button
    BackHandler(enabled = step > 0) {
        navigateBack()
    }

    LaunchedEffect(selectedRem) { if (selectedRem != null && cityList.isEmpty()) onLoadCity(selectedRem!!.id) }
    LaunchedEffect(selectedCity) { if (selectedCity != null && streetList.isEmpty()) onLoadStreet(selectedCity!!.id) }
    LaunchedEffect(selectedStreet) { if (selectedStreet != null && houseNumbers.isEmpty()) onLoadAddress(selectedStreet!!.id) }
    LaunchedEffect(Unit) { if (remList.isEmpty()) onLoadRem() }

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { 
                        Text(
                            text = "Нова адреса", 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold 
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Filled.Close, contentDescription = "Закрити")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (step > 0) {
                StepHeader(
                    step = step,
                    selectedRem = selectedRem,
                    selectedCity = selectedCity,
                    selectedStreet = selectedStreet,
                    onJumpToStep = { targetStep ->
                        if (targetStep < step) {
                            // Logic similar to back navigation but jumping
                            when (targetStep) {
                                1 -> { selectedCity = null; selectedStreet = null; selectedHouse = null }
                                2 -> { selectedStreet = null; selectedHouse = null }
                                3 -> { selectedHouse = null }
                            }
                            step = targetStep
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    label = "unified_address_step",
                    modifier = Modifier.fillMaxSize()
                ) { targetStep ->
                    when (targetStep) {
                        0 -> SelectionMethodContent(
                            onManualSelection = { step = 1 },
                            onAutoSelection = { /* TODO: implement auto selection */ },
                            onQRCodeScan = { /* TODO: implement QR code scanner */ }
                        )
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
                                onClearSearch()
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
                                if (!userEditedName) {
                                    val iconLabel = availableIcons.find { iconItem -> iconItem.name == it }?.label ?: "Дім"
                                    name = iconLabel
                                }
                            }
                        )
                    }
                }
            }

            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
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
                        0 -> true
                        1 -> selectedRem != null
                        2 -> selectedCity != null
                        3 -> selectedStreet != null
                        4 -> selectedHouse != null
                        else -> selectedRem != null && selectedCity != null && selectedStreet != null && selectedHouse != null && name.isNotBlank()
                    },
                    isLast = step == 5,
                    modifier = Modifier
                        .padding(16.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }
}

@Composable
private fun StepHeader(
    step: Int,
    selectedRem: Rem?,
    selectedCity: City?,
    selectedStreet: Street?,
    onJumpToStep: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Title based on current step
            val title = when (step) {
                1 -> "Оберіть район (РЕМ)"
                2 -> "Оберіть населений пункт"
                3 -> "Оберіть вулицю"
                4 -> "Оберіть будинок"
                else -> "Налаштування"
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Breadcrumbs (Scrollable Row)
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    AssistChip(
                        onClick = { onJumpToStep(1) },
                        label = { Text(selectedRem?.name ?: "РЕМ") },
                        leadingIcon = { Icon(Icons.Default.Business, null, Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (step == 1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            labelColor = if (step == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = if (step == 1) null else AssistChipDefaults.assistChipBorder(true)
                    )
                }

                if (step > 1) {
                    item {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    item {
                        AssistChip(
                            onClick = { onJumpToStep(2) },
                            label = { Text(selectedCity?.name ?: "Місто") },
                            leadingIcon = { Icon(Icons.Default.LocationCity, null, Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (step == 2) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                labelColor = if (step == 2) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = if (step == 2) null else AssistChipDefaults.assistChipBorder(true)
                        )
                    }
                }

                if (step > 2) {
                    item {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    item {
                        AssistChip(
                            onClick = { onJumpToStep(3) },
                            label = { Text(selectedStreet?.name ?: "Вулиця") },
                            leadingIcon = { Icon(Icons.Default.Signpost, null, Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (step == 3) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                labelColor = if (step == 3) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = if (step == 3) null else AssistChipDefaults.assistChipBorder(true)
                        )
                    }
                }
            }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
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
private fun ActionRow(step: Int, canGoBack: Boolean, onBack: () -> Unit, onCancel: () -> Unit, onContinue: () -> Unit, canContinue: Boolean, isLast: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
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

@Composable
private fun SelectionMethodContent(
    onManualSelection: () -> Unit,
    onAutoSelection: () -> Unit,
    onQRCodeScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Як ви хочете додати адресу?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Виберіть зручний для вас спосіб",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Manual selection button
        ElevatedButton(
            onClick = onManualSelection,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Вибрати вручну",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "РЕМ → Місто → Вулиця → Будинок",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Auto selection button
        ElevatedButton(
            onClick = onAutoSelection,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Вибрати автоматично",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Визначити за GPS координатами",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // QR code scan button
        ElevatedButton(
            onClick = onQRCodeScan,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Відсканувати QR-код",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Швидке додавання з коду",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun buildAddressLabel(city: City?, street: Street?, house: ParsedHouseNumber?): String {
    val parts = listOfNotNull(city?.name, street?.name, house?.houseNumber)
    return parts.joinToString(", ")
}
