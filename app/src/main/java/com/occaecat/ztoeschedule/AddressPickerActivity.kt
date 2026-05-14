package com.occaecat.ztoeschedule

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.occaecat.ztoeschedule.presentation.ui.addresses.AddressPickerScreen
import com.occaecat.ztoeschedule.presentation.ui.addresses.AddressPickerResult
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel
import com.occaecat.ztoeschedule.ui.theme.SvitloYeZhytomyrTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddressPickerActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: EnergyScheduleViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // Прелоад РЕМ если пусто
            LaunchedEffect(Unit) {
                if (uiState.remList.isEmpty()) {
                    viewModel.loadRemList()
                }
            }

            SvitloYeZhytomyrTheme(
                themePreference = uiState.colorTheme,
                cornerRadius = uiState.cornerRadius
            ) {
                // Handle back press gracefully
                BackHandler {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
                
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Scaffold(
                        topBar = {}
                    ) { innerPadding ->
                    // Показуємо індикатор завантаження тільки якщо список РЕМ порожній І йде завантаження
                    if (uiState.remList.isEmpty() && uiState.isLoading) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AddressPickerScreen(
                                remList = uiState.remList,
                                cityList = uiState.cityList,
                                streetList = uiState.streetList,
                                houseNumbers = uiState.filteredHouseNumbers,
                                searchQuery = uiState.houseNumberSearchQuery,
                                isLoading = uiState.isLoading,
                                selectedCategory = uiState.selectedCategory,
                                onLoadRem = { viewModel.loadRemList() },
                                onLoadCity = { viewModel.loadCityList(it) },
                                onLoadStreet = { viewModel.loadStreetList(it) },
                                onLoadAddress = { viewModel.loadAddressList(it) },
                                onSearchQueryChange = { viewModel.filterHouseNumbers(it) },
                                onCategorySelected = { viewModel.selectCategory(it) },
                                onClearSearch = { viewModel.clearHouseNumberSearch() },
                                onCancel = { setResult(Activity.RESULT_CANCELED); finish() },
                                onComplete = { result -> finishWithResult(result) },
                                initialRem = null,
                                initialCity = null,
                                initialStreet = null,
                                initialHouse = null,
                                initialName = "",
                                initialIcon = "home",
                                showTopBar = false
                            )
                        }
                    }
                    }
                }
            }
        }
    }

    private fun finishWithResult(result: AddressPickerResult) {
        val intent = Intent().apply {
            putExtra("name", result.displayName)
            putExtra("icon", result.iconName)
            putExtra("remId", result.remId)
            putExtra("remName", result.remName)
            putExtra("cityId", result.cityId)
            putExtra("cityName", result.cityName)
            putExtra("streetId", result.streetId)
            putExtra("streetName", result.streetName)
            putExtra("addressId", result.addressId)
            putExtra("addressName", result.addressName)
            putExtra("cherga", result.cherga)
            putExtra("pidcherga", result.pidcherga)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
