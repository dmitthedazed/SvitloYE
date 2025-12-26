package com.occaecat.ztoeschedule.presentation.ui.addresses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.data.model.City
import com.occaecat.ztoeschedule.data.model.Rem
import com.occaecat.ztoeschedule.data.model.Street
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber
import com.occaecat.ztoeschedule.presentation.ui.SelectionListItem
import com.occaecat.ztoeschedule.presentation.ui.onboarding.CitySelectionPage
import com.occaecat.ztoeschedule.presentation.ui.onboarding.HouseNumberSelectionPage
import com.occaecat.ztoeschedule.presentation.ui.onboarding.RemSelectionPage
import com.occaecat.ztoeschedule.presentation.ui.onboarding.StreetSelectionPage

import androidx.compose.animation.*

/**
 * A more integrated address selection flow for adding new locations within the app.
 * It doesn't have the "Welcome" screens and fits better into a sub-navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSelectionFlow(
    remList: List<Rem>,
    cityList: List<City>,
    streetList: List<Street>,
    houseNumbers: List<ParsedHouseNumber>,
    searchQuery: String,
    isLoading: Boolean,
    onLoadRem: () -> Unit,
    onLoadCity: (String) -> Unit,
    onLoadStreet: (String) -> Unit,
    onLoadAddress: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onCancel: () -> Unit,
    onComplete: (
        remId: String?,
        remName: String?,
        cityId: String?,
        cityName: String?,
        streetId: String?,
        streetName: String?,
        addressId: String,
        addressName: String,
        cherga: Int,
        pidcherga: Int
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var selectedRem by remember { mutableStateOf<Rem?>(null) }
    var selectedCity by remember { mutableStateOf<City?>(null) }
    var selectedStreet by remember { mutableStateOf<Street?>(null) }

    LaunchedEffect(Unit) {
        if (remList.isEmpty()) onLoadRem()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (step) {
                            1 -> "Оберіть РЕМ"
                            2 -> "Оберіть місто"
                            3 -> "Оберіть вулицю"
                            4 -> "Оберіть будинок"
                            else -> "Нова адреса"
                        }
                    )
                },
                navigationIcon = {
                    if (step > 1) {
                        IconButton(onClick = { step-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                        }
                    } else {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, "Закрити")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) { 
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "address_step_transition"
            ) { targetStep ->
                when (targetStep) {
                    1 -> RemSelectionPage(
                        rems = remList,
                        isLoading = isLoading,
                        onRemSelected = {
                            selectedRem = it
                            onLoadCity(it.id)
                            step = 2
                        }
                    )
                    2 -> CitySelectionPage(
                        cities = cityList,
                        isLoading = isLoading,
                        onCitySelected = {
                            selectedCity = it
                            onLoadStreet(it.id)
                            step = 3
                        }
                    )
                    3 -> StreetSelectionPage(
                        streets = streetList,
                        isLoading = isLoading,
                        onStreetSelected = {
                            selectedStreet = it
                            onLoadAddress(it.id)
                            step = 4
                        }
                    )
                    4 -> HouseNumberSelectionPage(
                        houseNumbers = houseNumbers,
                        searchQuery = searchQuery,
                        isLoading = isLoading,
                        onSearchQueryChange = onSearchQueryChange,
                        onClearSearch = onClearSearch,
                        onHouseSelected = {
                            onComplete(
                                selectedRem?.id, selectedRem?.name,
                                selectedCity?.id, selectedCity?.name,
                                selectedStreet?.id, selectedStreet?.name,
                                it.originalAddressId, it.houseNumber, it.cherga, it.pidcherga
                            )
                        }
                    )
                }
            }
        }
    }
}
