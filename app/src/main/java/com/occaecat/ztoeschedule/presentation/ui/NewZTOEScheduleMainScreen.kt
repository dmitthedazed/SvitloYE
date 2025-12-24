package com.occaecat.ztoeschedule.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.occaecat.ztoeschedule.data.model.*
import com.occaecat.ztoeschedule.presentation.ui.selection.*
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModelFactory
import kotlinx.coroutines.delay

/**
 * Главный экран приложения с новым дизайном
 */
@Composable
fun NewZTOEScheduleMainScreen() {
    val context = LocalContext.current
    val viewModel: EnergyScheduleViewModel = viewModel(
        factory = EnergyScheduleViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()

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

    // Main content
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = "Помилка: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
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
                        AddressSelectionScreen(
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

