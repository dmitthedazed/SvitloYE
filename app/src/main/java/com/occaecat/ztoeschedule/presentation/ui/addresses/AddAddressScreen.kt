package com.occaecat.ztoeschedule.presentation.ui.addresses

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.occaecat.ztoeschedule.data.model.*
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAddressScreen(
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
    onSaveAddress: (name: String, icon: String, remId: String, remName: String, cityId: String, cityName: String, streetId: String, streetName: String, addressId: String, addressName: String, cherga: Int, pidcherga: Int) -> Unit,
    onBack: () -> Unit
) {
    var tempSelection by remember { mutableStateOf<TempSelection?>(null) }

    AnimatedContent(
        targetState = tempSelection != null,
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
            }
        },
        label = "add_address_transition"
    ) { isCustomizing ->
        if (!isCustomizing) {
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
                onCancel = onBack,
                onComplete = { rI: String?, rN: String?, cI: String?, cN: String?, sI: String?, sN: String?, aI: String, aN: String, c: Int, p: Int ->
                    tempSelection = TempSelection(rI ?: "", rN ?: "", cI ?: "", cN ?: "", sI ?: "", sN ?: "", aI, aN, c, p)
                }
            )
        } else {
            AddressCustomizationScreen(
                onComplete = { name: String, icon: String ->
                    val s = tempSelection!!
                    onSaveAddress(name, icon, s.remId, s.remName, s.cityId, s.cityName, s.streetId, s.streetName, s.addressId, s.addressName, s.cherga, s.pidcherga)
                },
                onBack = { tempSelection = null }
            )
        }
    }
}

private data class TempSelection(val remId: String, val remName: String, val cityId: String, val cityName: String, val streetId: String, val streetName: String, val addressId: String, val addressName: String, val cherga: Int, val pidcherga: Int)
