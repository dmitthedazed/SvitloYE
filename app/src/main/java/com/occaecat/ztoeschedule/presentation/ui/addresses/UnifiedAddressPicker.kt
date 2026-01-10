package com.occaecat.ztoeschedule.presentation.ui.addresses

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.School
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.occaecat.ztoeschedule.presentation.ui.components.SettingsGroupItem

/**
 * Icon item for customization step
 */
private data class IconItem(
    val name: String,
    val label: String,
    val icon: ImageVector
)

/**
 * Available icons for address customization
 */
private val availableIcons = listOf(
    IconItem("home", "Дім", Icons.Default.Home),
    IconItem("work", "Робота", Icons.Default.Work),
    IconItem("person", "Рідні", Icons.Default.Person),
    IconItem("favorite", "Улюблене", Icons.Default.Favorite),
    IconItem("star", "Важливе", Icons.Default.Star),
    IconItem("place", "Місце", Icons.Default.Place),
    IconItem("store", "Магазин", Icons.Default.Store),
    IconItem("school", "Школа", Icons.Default.School)
)

/**
 * Unified address picker used by the Add Address flow.
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
    initialIcon: String = "home",
    skipConfirmation: Boolean = false
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
            contentPadding = PaddingValues(16.dp),
            skipConfirmation = skipConfirmation
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
    initialIcon: String = "home",
    startAtManual: Boolean = false,
    showTopBar: Boolean = true,
    skipConfirmation: Boolean = false
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
        startAtManual = startAtManual,
        modifier = modifier,
        showTopBar = showTopBar,
        contentPadding = PaddingValues(16.dp),
        skipConfirmation = skipConfirmation
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
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
    startAtManual: Boolean = false,
    modifier: Modifier = Modifier,
    showTopBar: Boolean,
    contentPadding: PaddingValues,
    skipConfirmation: Boolean
) {
    var step by remember { 
        mutableIntStateOf(
            if (initialHouse != null) 5 
            else if (startAtManual) 1 
            else 0
        ) 
    }
    var selectedRem by remember { mutableStateOf(initialRem) }
    var selectedCity by remember { mutableStateOf(initialCity) }
    var selectedStreet by remember { mutableStateOf(initialStreet) }
    var selectedHouse by remember { mutableStateOf(initialHouse) }
    var name by remember { mutableStateOf(initialName) }
    var icon by remember { mutableStateOf(initialIcon) }
    var userEditedName by remember { mutableStateOf(initialName.isNotBlank()) }
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Notify about step changes
    LaunchedEffect(step) {
        onStepChanged?.invoke(step)
    }

    // Handle back navigation logic
    val navigateBack = {
        if (step > 1) {
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
                LargeFlexibleTopAppBar(
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
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    scrollBehavior = null // Pinned behavior
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val slideSpec = tween<IntOffset>(400, easing = FastOutSlowInEasing)
                        val fadeSpec = tween<Float>(200)

                        if (targetState > initialState) {
                            (slideInHorizontally(slideSpec) { it / 3 } + fadeIn(fadeSpec))
                                .togetherWith(slideOutHorizontally(slideSpec) { -it / 3 } + fadeOut(fadeSpec))
                        } else {
                            (slideInHorizontally(slideSpec) { -it / 3 } + fadeIn(fadeSpec))
                                .togetherWith(slideOutHorizontally(slideSpec) { it / 3 } + fadeOut(fadeSpec))
                        }
                    },
                    label = "unified_address_step",
                    modifier = Modifier.fillMaxSize()
                ) { targetStep ->
                    when (targetStep) {
                        0 -> SelectionMethodContent(
                            onManualSelection = { step = 1 },
                            onAutoSelection = { step = -1 }, // Auto-location step
                            onQRCodeScan = { step = -2 } // QR scan step
                        )
                        -1 -> AutoLocationScreen(
                            remList = remList,
                            onRemSelected = { rem ->
                                selectedRem = rem
                                selectedCity = null
                                selectedStreet = null
                                selectedHouse = null
                                onLoadCity(rem.id)
                                step = 2
                            },
                            onManualSelection = { step = 1 },
                            onDismiss = { step = 0 }
                        )
                        -2 -> QRScannerScreen(
                            onResult = { result ->
                                when (result) {
                                    is QRScanResult.Success -> {
                                        // QR code contains full address data - go to customization
                                        val data = result.data
                                        if (data.isFullData()) {
                                            val houseName = data.houseName?.takeIf { it.isNotBlank() }
                                            val resolvedAddressName = data.addressName?.takeIf { it.isNotBlank() } ?: houseName ?: ""
                                            val resolvedDisplayName = data.displayName?.takeIf { it.isNotBlank() } ?: resolvedAddressName
                                            
                                            // Populate state objects
                                            selectedRem = Rem(id = data.remId!!, name = data.remName ?: "")
                                            selectedCity = City(id = data.cityId!!, name = data.cityName ?: "", remId = data.remId)
                                            selectedStreet = Street(id = data.streetId, name = data.streetName ?: "", cityId = data.cityId)
                                            selectedHouse = ParsedHouseNumber(
                                                houseNumber = resolvedAddressName,
                                                cherga = data.cherga!!,
                                                pidcherga = data.pidcherga!!,
                                                originalAddressId = data.addressId,
                                                category = ConsumerCategory.OTHER // Default as we don't know
                                            )
                                            
                                            name = resolvedDisplayName
                                            icon = "home"
                                            
                                            // Go to customization step
                                            step = 5
                                        } else {
                                            val houseName = data.preferredHouseName()
                                            val intent = Intent(context, com.occaecat.ztoeschedule.InspectActivity::class.java).apply {
                                                putExtra("streetId", data.streetId)
                                                putExtra("addressId", data.addressId)
                                                putExtra("houseName", houseName ?: "")
                                            }
                                            context.startActivity(intent)
                                            onCancel()
                                        }
                                    }
                                    is QRScanResult.Error -> {
                                        // Show error and return to method selection
                                        step = 0
                                    }
                                    is QRScanResult.Cancelled -> {
                                        step = 0
                                    }
                                }
                            },
                            onDismiss = { step = 0 }
                        )
                        1 -> RemSelectionPage(
                            rems = remList,
                            isLoading = isLoading,
                            onRemSelected = { rem ->
                                selectedRem = rem
                                selectedCity = null
                                selectedStreet = null
                                selectedHouse = null
                                onLoadCity(rem.id)
                                step = 2
                            }
                        )
                        2 -> CitySelectionPage(
                            cities = cityList,
                            isLoading = isLoading,
                            onCitySelected = { city ->
                                selectedCity = city
                                selectedStreet = null
                                selectedHouse = null
                                onLoadStreet(city.id)
                                step = 3
                            }
                        )
                        3 -> StreetSelectionPage(
                            streets = streetList,
                            isLoading = isLoading,
                            onStreetSelected = { street ->
                                selectedStreet = street
                                selectedHouse = null
                                onClearSearch()
                                onLoadAddress(street.id)
                                step = 4
                            }
                        )
                        4 -> HouseNumberSelectionPage(
                            houseNumbers = houseNumbers,
                            searchQuery = searchQuery,
                            isLoading = isLoading,
                            selectedCategory = selectedCategory,
                            onSearchQueryChange = onSearchQueryChange,
                            onCategorySelected = onCategorySelected,
                            onClearSearch = onClearSearch,
                            onHouseSelected = { house ->
                                selectedHouse = house
                                val defaultName = availableIcons.find { iconItem -> iconItem.name == icon }?.label ?: "Дім"
                                val finalName = if (userEditedName) name else defaultName
                                name = finalName

                                if (skipConfirmation) {
                                    // IMMEDIATE COMPLETION
                                    if (selectedRem != null && selectedCity != null && selectedStreet != null) {
                                        onComplete(
                                            AddressPickerResult(
                                                remId = selectedRem!!.id,
                                                remName = selectedRem!!.name,
                                                cityId = selectedCity!!.id,
                                                cityName = selectedCity!!.name,
                                                streetId = selectedStreet!!.id,
                                                streetName = selectedStreet!!.name,
                                                addressId = house.originalAddressId,
                                                addressName = house.houseNumber,
                                                cherga = house.cherga,
                                                pidcherga = house.pidcherga,
                                                displayName = finalName.ifBlank { buildAddressLabel(selectedCity, selectedStreet, house) },
                                                iconName = icon
                                            )
                                        )
                                    }
                                } else {
                                    // ORIGINAL FLOW
                                    if (!userEditedName) {
                                        name = defaultName
                                    }
                                    step = 5
                                }
                            }
                        )
                        5 -> ConfirmationStep(
                            remName = selectedRem?.name ?: "",
                            cityName = selectedCity?.name ?: "",
                            streetName = selectedStreet?.name ?: "",
                            addressName = selectedHouse?.houseNumber ?: "",
                            cherga = selectedHouse?.cherga ?: 0,
                            pidcherga = selectedHouse?.pidcherga ?: 0
                        )
                        6 -> CustomizationStep(
                            name = name,
                            icon = icon,
                            onNameChange = { newName -> 
                                name = newName
                                userEditedName = true
                            },
                            onIconChange = { newIcon -> 
                                icon = newIcon
                                if (!userEditedName) {
                                    val iconLabel = availableIcons.find { iconItem -> iconItem.name == newIcon }?.label ?: "Дім"
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
                        if (step < 6) {
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
                        5 -> true // Confirmation is always valid if we got here
                        else -> selectedRem != null && selectedCity != null && selectedStreet != null && selectedHouse != null && name.isNotBlank()
                    },
                    isLast = step == 6,
                    modifier = Modifier
                        .padding(24.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }
}

@Composable
private fun ConfirmationStep(
    remName: String,
    cityName: String,
    streetName: String,
    addressName: String,
    cherga: Int,
    pidcherga: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Перевірте дані",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(Modifier.height(32.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SummaryItem(Icons.Default.Business, "РЕМ", remName)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SummaryItem(Icons.Default.LocationCity, "Місто", cityName)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SummaryItem(Icons.Default.Signpost, "Вулиця", streetName)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SummaryItem(Icons.Default.Home, "Будинок", addressName)
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Row(
                Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Черга",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "$cherga.$pidcherga",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
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
            
            Spacer(modifier = Modifier.height(12.dp))

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
                            labelColor = if (step == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            leadingIconContentColor = if (step == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
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
                                labelColor = if (step == 2) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                leadingIconContentColor = if (step == 2) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
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
                                labelColor = if (step == 3) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                leadingIconContentColor = if (step == 3) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = if (step == 3) null else AssistChipDefaults.assistChipBorder(true)
                        )
                    }
                }
            }
        }
    }
}

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
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Назва локації") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape =  RoundedCornerShape(16.dp),
            supportingText = { Text("Введіть назву для цієї адреси") },
            colors = OutlinedTextFieldDefaults.colors()
        )

        Text(
            text = "Оберіть іконку",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4), // Increased columns for more compact look
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(availableIcons) { item ->
                val isSelected = icon == item.name
                val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                
                Surface(
                    onClick = { onIconChange(item.name) },
                    modifier = Modifier.aspectRatio(1f),
                    shape = RoundedCornerShape(24.dp),
                    color = color,
                    tonalElevation = if (isSelected) 8.dp else 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = contentColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = contentColor,
                            maxLines = 1,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    step: Int, 
    canGoBack: Boolean, 
    onBack: () -> Unit, 
    onCancel: () -> Unit, 
    onContinue: () -> Unit, 
    canContinue: Boolean, 
    isLast: Boolean, 
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = if (canGoBack) onBack else onCancel,
            modifier = Modifier
                .weight(1f)
                .height(64.dp), // Height 64dp
            shape = RoundedCornerShape(32.dp), // Radius 32dp
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Text(
                if (canGoBack) "Назад" else "Скасувати",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Button(
            onClick = onContinue,
            enabled = canContinue,
            modifier = Modifier
                .weight(1f)
                .height(64.dp), // Height 64dp
            shape = RoundedCornerShape(32.dp), // Radius 32dp
            colors = ButtonDefaults.buttonColors(
                // Use default Primary color
            )
        ) {
            Text(
                if (isLast) "Готово" else "Далі", 
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                if (isLast) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
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
            text = "Додавання адреси",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val items = listOf(
                Triple("Вибрати вручну", "РЕМ → Місто → Вулиця → Будинок", Icons.Filled.Edit) to onManualSelection,
                Triple("Вибрати автоматично", "Визначити найближчий РЕМ за GPS", Icons.Filled.LocationOn) to onAutoSelection,
                Triple("Відсканувати QR-код", "Швидке додавання з QR-коду", Icons.Filled.QrCodeScanner) to onQRCodeScan
            )

            items.forEachIndexed { index, (data, action) ->
                val (title, subtitle, icon) = data
                
                SettingsGroupItem(
                    index = index,
                    totalCount = items.size,
                    headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(subtitle) },
                    leadingContent = {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                            }
                        }
                    },
                    onClick = action
                )
            }
        }
    }
}

private fun buildAddressLabel(city: City?, street: Street?, house: ParsedHouseNumber?): String {
    val parts = listOfNotNull(city?.name, street?.name, house?.houseNumber)
    return parts.joinToString(", ")
}

