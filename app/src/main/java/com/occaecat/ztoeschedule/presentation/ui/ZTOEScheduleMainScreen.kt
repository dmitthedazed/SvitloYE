package com.occaecat.ztoeschedule.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.occaecat.ztoeschedule.data.model.*
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModelFactory
import kotlinx.coroutines.delay

/**
 * Main screen for ZTOE Schedule App
 * Implements full selection chain: REM → City → Street → Address → Schedule
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZTOEScheduleMainScreen() {
    val context = LocalContext.current
    val viewModel: EnergyScheduleViewModel = viewModel(
        factory = EnergyScheduleViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()

    // Show welcome screen if onboarding not completed
    if (!uiState.onboardingCompleted) {
        WelcomeScreen(
            onComplete = {
                viewModel.completeOnboarding()
            }
        )
        return
    }

    // Track navigation state
    var currentStep by remember { mutableStateOf(SelectionStep.REM) }
    var selectedRem by remember { mutableStateOf<Rem?>(null) }
    var selectedCity by remember { mutableStateOf<City?>(null) }
    var selectedStreet by remember { mutableStateOf<Street?>(null) }
    var selectedAddress by remember { mutableStateOf<Address?>(null) }

    // Auto-navigate to schedule if saved selection exists
    LaunchedEffect(uiState.hasSavedSelection, uiState.scheduleList) {
        if (uiState.hasSavedSelection && uiState.scheduleList.isNotEmpty()) {
            currentStep = SelectionStep.SCHEDULE
        }
    }

    // Auto-refresh current status every minute
    LaunchedEffect(selectedAddress) {
        if (selectedAddress != null) {
            while (true) {
                delay(60_000) // 1 minute
                viewModel.refreshCurrentStatus()
            }
        }
    }

    // Load initial data
    LaunchedEffect(Unit) {
        if (uiState.remList.isEmpty() && !uiState.isLoading) {
            viewModel.loadRemList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentStep) {
                            SelectionStep.REM -> "Оберіть РЕМ"
                            SelectionStep.CITY -> "Оберіть місто"
                            SelectionStep.STREET -> "Оберіть вулицю"
                            SelectionStep.ADDRESS -> "Оберіть адресу"
                            SelectionStep.SCHEDULE -> "Графік відключень"
                        }
                    )
                },
                navigationIcon = {
                    if (currentStep != SelectionStep.REM) {
                        IconButton(onClick = {
                            currentStep = when (currentStep) {
                                SelectionStep.CITY -> {
                                    selectedCity = null
                                    SelectionStep.REM
                                }
                                SelectionStep.STREET -> {
                                    selectedStreet = null
                                    SelectionStep.CITY
                                }
                                SelectionStep.ADDRESS -> {
                                    selectedAddress = null
                                    SelectionStep.STREET
                                }
                                SelectionStep.SCHEDULE -> {
                                    SelectionStep.ADDRESS
                                }
                                else -> SelectionStep.REM
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                        }
                    }
                },
                actions = {
                    if (currentStep == SelectionStep.SCHEDULE) {
                        // Change address button
                        IconButton(onClick = {
                            currentStep = SelectionStep.REM
                            selectedRem = null
                            selectedCity = null
                            selectedStreet = null
                            selectedAddress = null
                        }) {
                            Icon(Icons.Default.Home, "Змінити адресу")
                        }
                        // Refresh button
                        IconButton(onClick = {
                            selectedAddress?.let {
                                viewModel.loadScheduleWithMessages(it.cherga, it.pidcherga)
                            }
                        }) {
                            Icon(Icons.Default.Refresh, "Оновити")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                uiState.isLoading -> {
                    LoadingScreen()
                }
                uiState.error != null -> {
                    ErrorScreen(
                        error = uiState.error ?: "Невідома помилка",
                        onDismiss = { viewModel.clearError() }
                    )
                }
                else -> {
                    when (currentStep) {
                        SelectionStep.REM -> {
                            RemSelectionScreen(
                                rems = uiState.remList,
                                onRemSelected = { rem ->
                                    selectedRem = rem
                                    viewModel.loadCityList(rem.id)
                                    currentStep = SelectionStep.CITY
                                }
                            )
                        }
                        SelectionStep.CITY -> {
                            CitySelectionScreen(
                                cities = uiState.cityList,
                                onCitySelected = { city ->
                                    selectedCity = city
                                    viewModel.loadStreetList(city.id)
                                    currentStep = SelectionStep.STREET
                                }
                            )
                        }
                        SelectionStep.STREET -> {
                            StreetSelectionScreen(
                                streets = uiState.streetList,
                                onStreetSelected = { street ->
                                    selectedStreet = street
                                    viewModel.loadAddressList(street.id)
                                    currentStep = SelectionStep.ADDRESS
                                }
                            )
                        }
                        SelectionStep.ADDRESS -> {
                            AddressGridScreen(
                                houseNumbers = uiState.filteredHouseNumbers,
                                searchQuery = uiState.houseNumberSearchQuery,
                                onSearchQueryChange = { query ->
                                    viewModel.filterHouseNumbers(query)
                                },
                                onClearSearch = {
                                    viewModel.clearHouseNumberSearch()
                                },
                                onHouseNumberSelected = { parsedHouse ->
                                    // Create address object
                                    selectedAddress = Address(
                                        id = parsedHouse.originalAddressId,
                                        name = parsedHouse.houseNumber,
                                        cherga = parsedHouse.cherga,
                                        pidcherga = parsedHouse.pidcherga
                                    )

                                    // Save complete selection chain
                                    viewModel.saveSelection(
                                        remId = selectedRem?.id,
                                        remName = selectedRem?.name,
                                        cityId = selectedCity?.id,
                                        cityName = selectedCity?.name,
                                        streetId = selectedStreet?.id,
                                        streetName = selectedStreet?.name,
                                        addressId = parsedHouse.originalAddressId,
                                        addressName = parsedHouse.houseNumber,
                                        cherga = parsedHouse.cherga,
                                        pidcherga = parsedHouse.pidcherga
                                    )

                                    // Load schedule
                                    viewModel.loadScheduleWithMessages(parsedHouse.cherga, parsedHouse.pidcherga)
                                    currentStep = SelectionStep.SCHEDULE
                                }
                            )
                        }
                        SelectionStep.SCHEDULE -> {
                            Material3ScheduleScreen(
                                selectedAddress = selectedAddress?.name ?: "Не вибрано",
                                currentStatus = uiState.currentStatus,
                                schedules = uiState.scheduleList,
                                groupedSchedule = uiState.groupedSchedule,
                                formattedMessage = uiState.formattedMessage,
                                cherga = selectedAddress?.cherga ?: 0,
                                pidcherga = selectedAddress?.pidcherga ?: 0,
                                onRefresh = {
                                    selectedAddress?.let {
                                        viewModel.loadScheduleWithMessages(it.cherga, it.pidcherga)
                                    }
                                },
                                onChangeAddress = {
                                    currentStep = SelectionStep.REM
                                    selectedRem = null
                                    selectedCity = null
                                    selectedStreet = null
                                    selectedAddress = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Завантаження...")
        }
    }
}

@Composable
private fun ErrorScreen(error: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Помилка",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss) {
                    Text("ОК")
                }
            }
        }
    }
}

@Composable
private fun RemSelectionScreen(
    rems: List<Rem>,
    onRemSelected: (Rem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(rems) { rem ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onRemSelected(rem) }
            ) {
                Text(
                    text = rem.name,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun CitySelectionScreen(
    cities: List<City>,
    onCitySelected: (City) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cities) { city ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCitySelected(city) }
            ) {
                Text(
                    text = city.name,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun StreetSelectionScreen(
    streets: List<Street>,
    onStreetSelected: (Street) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(streets) { street ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onStreetSelected(street) }
            ) {
                Text(
                    text = street.name,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

/**
 * Legacy address selection screen - replaced by AddressGridScreen
 * Kept for reference or backward compatibility if needed
 */
@Deprecated("Use AddressGridScreen instead for better UX with parsed house numbers")
@Composable
private fun AddressSelectionScreen(
    addresses: List<Address>,
    onAddressSelected: (Address) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(addresses) { address ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onAddressSelected(address) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = address.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Черга: ${address.cherga}, Підчерга: ${address.pidcherga}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleDisplayScreen(
    schedules: List<Schedule>,
    formattedMessage: String,
    currentStatus: Schedule?,
    selectedAddress: Address?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Selected address info
        selectedAddress?.let { address ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Обрана адреса",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = address.name)
                        Text(
                            text = "Черга ${address.cherga}, Підчерга ${address.pidcherga}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Current status
        currentStatus?.let { status ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Поточний статус",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = status.displayText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⏰ ${status.span}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "📅 ${status.date}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Info messages using NoticeCard
        if (formattedMessage.isNotBlank()) {
            item {
                NoticeCard(
                    message = formattedMessage,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // Schedule list
        if (schedules.isNotEmpty()) {
            item {
                Text(
                    text = "📋 Повний графік",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(schedules) { schedule ->
                ScheduleItemCard(schedule, isActive = schedule == currentStatus)
            }
        }

        // Empty state
        if (schedules.isEmpty() && formattedMessage.isBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Немає даних про графік",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleItemCard(schedule: Schedule, isActive: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isActive) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isActive) {
                        Text(
                            text = "▶ ",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = schedule.displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = schedule.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = schedule.span,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

enum class SelectionStep {
    REM, CITY, STREET, ADDRESS, SCHEDULE
}

