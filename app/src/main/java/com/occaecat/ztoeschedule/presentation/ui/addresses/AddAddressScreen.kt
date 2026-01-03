package com.occaecat.ztoeschedule.presentation.ui.addresses

import androidx.compose.runtime.Composable
import com.occaecat.ztoeschedule.data.model.City
import com.occaecat.ztoeschedule.data.model.Rem
import com.occaecat.ztoeschedule.data.model.Street
import com.occaecat.ztoeschedule.data.repository.ConsumerCategory
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber

@Composable
fun AddAddressScreen(
    remList: List<Rem>,
    cityList: List<City>,
    streetList: List<Street>,
    houseNumbers: List<ParsedHouseNumber>,
    searchQuery: String,
    isLoading: Boolean,
    selectedCategory: ConsumerCategory? = null,
    onLoadRem: () -> Unit,
    onLoadCity: (String) -> Unit,
    onLoadStreet: (String) -> Unit,
    onLoadAddress: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (ConsumerCategory?) -> Unit = {},
    onClearSearch: () -> Unit,
    onSaveAddress: (name: String, icon: String, remId: String, remName: String, cityId: String, cityName: String, streetId: String, streetName: String, addressId: String, addressName: String, cherga: Int, pidcherga: Int) -> Unit,
    onBack: () -> Unit
) {
    AddressPickerDialog(
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
        onDismiss = onBack,
        onComplete = { result ->
            onSaveAddress(
                result.displayName,
                result.iconName,
                result.remId,
                result.remName,
                result.cityId,
                result.cityName,
                result.streetId,
                result.streetName,
                result.addressId,
                result.addressName,
                result.cherga,
                result.pidcherga
            )
            onBack()
        },
        showSheet = true
    )
}