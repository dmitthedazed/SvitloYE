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
import com.occaecat.ztoeschedule.presentation.ui.components.ExpressiveHeroBadge
import com.occaecat.ztoeschedule.presentation.ui.components.GroupedInfoRow
import com.occaecat.ztoeschedule.presentation.ui.components.StepGutter
import com.occaecat.ztoeschedule.presentation.ui.components.StepHeroIcon
import com.occaecat.ztoeschedule.presentation.ui.components.StepHeroPage
import com.occaecat.ztoeschedule.presentation.ui.components.StepLeadingIcon
import com.occaecat.ztoeschedule.presentation.ui.components.StepListHeader
import com.occaecat.ztoeschedule.presentation.ui.components.StepPrimaryButton
import com.occaecat.ztoeschedule.presentation.ui.components.StepSecondaryButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
            // The picker draws its own header with back / close actions
            dragHandle = { BottomSheetDefaults.DragHandle(modifier = Modifier.padding(top = 12.dp, bottom = 0.dp)) },
            modifier = Modifier.statusBarsPadding()
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
            showBackButton = false,
            inSheet = true,
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
    showBackButton: Boolean = true,
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
        showBackButton = showBackButton,
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
    showBackButton: Boolean = true,
    contentPadding: PaddingValues,
    skipConfirmation: Boolean,
    inSheet: Boolean = false
) {
    val analytics = com.occaecat.ztoeschedule.analytics.rememberAnalytics()
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
        if (step < 0) {
            // GPS / QR sub-screens return to method selection
            step = 0
        } else if (step > 1) {
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
    BackHandler(enabled = step != 0) {
        navigateBack()
    }

    LaunchedEffect(selectedRem) { if (selectedRem != null && cityList.isEmpty()) onLoadCity(selectedRem!!.id) }
    LaunchedEffect(selectedCity) { if (selectedCity != null && streetList.isEmpty()) onLoadStreet(selectedCity!!.id) }
    LaunchedEffect(selectedStreet) { if (selectedStreet != null && houseNumbers.isEmpty()) onLoadAddress(selectedStreet!!.id) }
    LaunchedEffect(Unit) { if (remList.isEmpty()) onLoadRem() }

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        // A sheet already handles the system bars; applying them again left a gap at the top
        contentWindowInsets = if (inSheet) WindowInsets(0) else WindowInsets.safeDrawing,
        containerColor = if (inSheet) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            if (inSheet) {
                SheetHeader(
                    step = step,
                    onBack = { navigateBack() },
                    onClose = onCancel
                )
            } else if (showTopBar) {
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
                .consumeWindowInsets(padding)
        ) {
            val motionScheme = MaterialTheme.motionScheme
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val slideSpec = motionScheme.defaultSpatialSpec<IntOffset>()
                        val fadeSpec = motionScheme.defaultEffectsSpec<Float>()

                        // GPS (-1) and QR (-2) sit at the same depth as manual selection (1),
                        // so compare navigation depth rather than raw step numbers
                        fun depth(step: Int) = if (step < 0) 1 else step
                        if (depth(targetState) >= depth(initialState)) {
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
                    // Actions belong to the page so they slide with it instead of resizing the content area
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            when (targetStep) {
                                0 -> SelectionMethodContent(
                                    onManualSelection = { step = 1 },
                                    onAutoSelection = { step = -1 }, // Auto-location step
                                    onQRCodeScan = { step = -2 } // QR scan step
                                )
                                -1 -> AutoLocationScreen(
                                    remList = remList,
                                    onRemSelected = { rem ->
                                        analytics.logAutoLocationAttempt(success = true)
                                        selectedRem = rem
                                        selectedCity = null
                                        selectedStreet = null
                                        selectedHouse = null
                                        onLoadCity(rem.id)
                                        step = 2
                                    },
                                    onManualSelection = { analytics.logAutoLocationAttempt(success = false); step = 1 },
                                    onDismiss = { step = 0 }
                                )
                                -2 -> QRScannerScreen(
                                    onResult = { result ->
                                        when (result) {
                                            is QRScanResult.Success -> {
                                                analytics.logQRScanAttempt(success = true)
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
                                                analytics.logQRScanAttempt(success = false)
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
                                    subtitle = "Район електромереж, що обслуговує вашу адресу",
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
                                    subtitle = selectedRem?.name?.let { "$it РЕМ" },
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
                                    subtitle = selectedCity?.name,
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
                                    subtitle = listOfNotNull(selectedCity?.name, selectedStreet?.name).joinToString(", ").ifBlank { null },
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
                                    addressLabel = buildAddressLabel(selectedCity, selectedStreet, selectedHouse),
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

                        // Steps 1-4 advance on tap, so only offer an explicit action where there is one to take
                        val hasPrimaryAction = targetStep >= 5
                        if (hasPrimaryAction || (targetStep in 1..4 && showBackButton)) {
                            ActionRow(
                                canGoBack = targetStep > 1,
                                showBack = showBackButton,
                                showContinue = hasPrimaryAction,
                                onBack = { navigateBack() },
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
                                canContinue = when (targetStep) {
                                    0 -> true
                                    1 -> selectedRem != null
                                    2 -> selectedCity != null
                                    3 -> selectedStreet != null
                                    4 -> selectedHouse != null
                                    5 -> true // Confirmation is always valid if we got here
                                    else -> selectedRem != null && selectedCity != null && selectedStreet != null && selectedHouse != null && name.isNotBlank()
                                },
                                isLast = targetStep == 6,
                                modifier = Modifier
                                    .padding(horizontal = StepGutter, vertical = 16.dp)
                                    .navigationBarsPadding()
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Header for the sheet variant: back, title with the current step, close and step progress. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SheetHeader(
    step: Int,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val (stepTitle, stepIndex) = when (step) {
        -1 -> "Пошук за GPS" to 1
        -2 -> "Сканування QR" to 1
        1 -> "Район" to 1
        2 -> "Населений пункт" to 2
        3 -> "Вулиця" to 3
        4 -> "Будинок" to 4
        5 -> "Перевірка" to 5
        6 -> "Назва" to 6
        else -> "Спосіб додавання" to 0
    }
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = stepIndex / 6f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "sheet_step_progress"
    )

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Keep the slot so the title doesn't jump when the back button appears
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = step != 0,
                    enter = fadeIn() + androidx.compose.animation.scaleIn(),
                    exit = fadeOut() + androidx.compose.animation.scaleOut()
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Нова адреса",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                AnimatedContent(
                    targetState = stepTitle,
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(90)) },
                    label = "sheet_step_title"
                ) { title ->
                    Text(
                        text = if (stepIndex > 0) "Крок $stepIndex з 6 · $title" else title,
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            FilledTonalIconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Закрити")
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .height(4.dp),
            color = colorScheme.primary,
            trackColor = colorScheme.surfaceContainerHighest,
            gapSize = 4.dp,
            drawStopIndicator = {}
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConfirmationStep(
    remName: String,
    cityName: String,
    streetName: String,
    addressName: String,
    cherga: Int,
    pidcherga: Int
) {
    val colorScheme = MaterialTheme.colorScheme
    val rows = listOf(
        Triple(Icons.Default.Business, "РЕМ", remName),
        Triple(Icons.Default.LocationCity, "Населений пункт", cityName),
        Triple(Icons.Default.Signpost, "Вулиця", streetName),
        Triple(Icons.Default.Home, "Будинок", addressName)
    )

    StepHeroPage(
        hero = {
            // The queue is the one thing users need from this step, so it becomes the hero
            if (cherga <= 0) {
                StepHeroIcon(
                    icon = Icons.Default.Home,
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    containerColor = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer,
                    rotate = true
                )
            } else ExpressiveHeroBadge(
                shape = MaterialShapes.Cookie9Sided.toShape(),
                containerColor = colorScheme.primaryContainer,
                size = 160.dp,
                rotate = true
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "черга",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$cherga.$pidcherga",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        title = "Перевірте дані",
        subtitle = if (cherga <= 0) {
            "Черга для цієї адреси поки не визначена — графік з'явиться, щойно вона буде відома"
        } else {
            "Графік відключень підбиратиметься для цієї адреси"
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            rows.forEachIndexed { index, (icon, label, value) ->
                GroupedInfoRow(
                    index = index,
                    count = rows.size,
                    icon = icon,
                    overline = label,
                    title = value.ifBlank { "—" }
                )
            }
        }
    }
}

@Composable
private fun CustomizationStep(
    name: String,
    icon: String,
    addressLabel: String,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val selectedIcon = availableIcons.find { it.name == icon } ?: availableIcons.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StepListHeader(
            title = "Назвіть адресу",
            subtitle = "Так вона відображатиметься в додатку"
        )

        Column(
            modifier = Modifier.padding(horizontal = StepGutter),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live preview of how the address will look in lists
            ListItem(
                headlineContent = {
                    Text(name.ifBlank { selectedIcon.label }, fontWeight = FontWeight.SemiBold)
                },
                supportingContent = { Text(addressLabel) },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(colorScheme.primary, MaterialShapes.Cookie6Sided.toShape()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(selectedIcon.icon, contentDescription = null, tint = colorScheme.onPrimary)
                    }
                },
                colors = ListItemDefaults.colors(containerColor = colorScheme.surfaceContainerHigh),
                modifier = Modifier.clip(RoundedCornerShape(24.dp))
            )

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Назва локації") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Text(
                text = "Іконка",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )

            // Fixed-size grid inside a scrolling column, so lay rows out manually
            availableIcons.chunked(4).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { item ->
                        IconChoice(
                            item = item,
                            selected = icon == item.name,
                            onClick = { onIconChange(item.name) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun IconChoice(
    item: IconItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val corner by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (selected) 40.dp else 20.dp,
        label = "icon_choice_corner"
    )
    val container by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) colorScheme.primary else colorScheme.surfaceContainerHigh,
        label = "icon_choice_container"
    )
    val content = if (selected) colorScheme.onPrimary else colorScheme.onSurface

    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(corner),
        color = container
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(item.icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = content,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ActionRow(
    canGoBack: Boolean, 
    showBack: Boolean,
    showContinue: Boolean,
    onBack: () -> Unit, 
    onCancel: () -> Unit, 
    onContinue: () -> Unit, 
    canContinue: Boolean, 
    isLast: Boolean, 
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showBack) {
            StepSecondaryButton(
                text = if (canGoBack) "Назад" else "Скасувати",
                onClick = if (canGoBack) onBack else onCancel,
                modifier = Modifier.weight(1f)
            )
        }
        if (showContinue) {
            StepPrimaryButton(
                text = if (isLast) "Готово" else "Далі",
                icon = if (isLast) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                onClick = onContinue,
                enabled = canContinue,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionMethodContent(
    onManualSelection: () -> Unit,
    onAutoSelection: () -> Unit,
    onQRCodeScan: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val items = listOf(
        Triple("Вибрати вручну", "РЕМ → Місто → Вулиця → Будинок", Icons.Filled.Edit) to onManualSelection,
        Triple("Вибрати автоматично", "Визначити найближчий РЕМ за GPS", Icons.Filled.LocationOn) to onAutoSelection,
        Triple("Відсканувати QR-код", "Швидке додавання з QR-коду", Icons.Filled.QrCodeScanner) to onQRCodeScan
    )

    StepHeroPage(
        hero = {
            StepHeroIcon(
                icon = Icons.Filled.Home,
                shape = MaterialShapes.Clover4Leaf.toShape(),
                containerColor = colorScheme.secondaryContainer,
                contentColor = colorScheme.onSecondaryContainer
            )
        },
        title = "Додайте адресу",
        subtitle = "Оберіть зручний спосіб знайти ваш будинок"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEachIndexed { index, (data, action) ->
                val (title, subtitle, icon) = data
                SettingsGroupItem(
                    index = index,
                    totalCount = items.size,
                    headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(subtitle) },
                    leadingContent = {
                        StepLeadingIcon(
                            icon = icon,
                            containerColor = colorScheme.primaryContainer,
                            contentColor = colorScheme.onPrimaryContainer
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant
                        )
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

