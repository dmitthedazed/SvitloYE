package com.occaecat.ztoeschedule.presentation.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.occaecat.ztoeschedule.presentation.ui.addresses.MyAddressesTab
import com.occaecat.ztoeschedule.presentation.ui.home.HomeTab
import com.occaecat.ztoeschedule.presentation.ui.notifications.NotificationsTab
import com.occaecat.ztoeschedule.presentation.ui.onboarding.OnboardingFlow
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsTab
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModelFactory

/**
 * New main screen with bottom navigation
 * Four tabs: Home, Notifications, My Addresses, Settings
 */
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val viewModel: EnergyScheduleViewModel = viewModel(
        factory = EnergyScheduleViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()

    // Show onboarding if not completed
    if (!uiState.onboardingCompleted || !uiState.hasSavedSelection) {
        OnboardingFlow(
            remList = uiState.remList,
            cityList = uiState.cityList,
            streetList = uiState.streetList,
            houseNumbers = uiState.filteredHouseNumbers,
            searchQuery = uiState.houseNumberSearchQuery,
            isLoading = uiState.isLoading,
            onLoadRem = { viewModel.loadRemList() },
            onLoadCity = { remId: String -> viewModel.loadCityList(remId) },
            onLoadStreet = { cityId: String -> viewModel.loadStreetList(cityId) },
            onLoadAddress = { streetId: String -> viewModel.loadAddressList(streetId) },
            onSearchQueryChange = { query: String -> viewModel.filterHouseNumbers(query) },
            onClearSearch = { viewModel.clearHouseNumberSearch() },
            onComplete = { remId: String?, remName: String?, cityId: String?, cityName: String?,
                          streetId: String?, streetName: String?, addressId: String,
                          addressName: String, cherga: Int, pidcherga: Int ->
                // Save selection
                viewModel.saveSelection(
                    remId = remId,
                    remName = remName,
                    cityId = cityId,
                    cityName = cityName,
                    streetId = streetId,
                    streetName = streetName,
                    addressId = addressId,
                    addressName = addressName,
                    cherga = cherga,
                    pidcherga = pidcherga
                )
                // Load schedule
                viewModel.loadScheduleWithMessages(cherga, pidcherga)
                // Complete onboarding
                viewModel.completeOnboarding()
            }
        )
        return
    }

    // Main app with bottom navigation
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> {
                // Home tab
                HomeTab(
                    remName = uiState.savedRemName,
                    cityName = uiState.savedCityName,
                    streetName = uiState.savedStreetName,
                    addressName = uiState.savedAddressName,
                    cherga = uiState.savedCherga,
                    pidcherga = uiState.savedPidcherga,
                    currentStatus = uiState.currentStatus,
                    schedules = uiState.scheduleList,
                    groupedSchedule = uiState.groupedSchedule,
                    onRefresh = {
                        viewModel.loadScheduleWithMessages(uiState.savedCherga, uiState.savedPidcherga)
                    },
                    modifier = Modifier.padding(padding)
                )
            }
            1 -> {
                // Notifications tab
                NotificationsTab(
                    messages = uiState.infoMessages,
                    formattedMessage = uiState.formattedMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
            2 -> {
                // My Addresses tab
                MyAddressesTab(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
            3 -> {
                // Settings tab
                SettingsTab(
                    notificationsEnabled = uiState.notificationsEnabled,
                    notificationAdvanceMinutes = uiState.notificationAdvanceMinutes,
                    onNotificationsEnabledChange = { enabled ->
                        viewModel.setNotificationsEnabled(enabled)
                    },
                    onNotificationAdvanceMinutesChange = { minutes ->
                        viewModel.setNotificationAdvanceMinutes(minutes)
                    },
                    onResetOnboarding = {
                        viewModel.resetOnboarding()
                    },
                    onClearData = {
                        viewModel.clearData()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(
        label = "Головна",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavItem(
        label = "Повідомлення",
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications
    ),
    BottomNavItem(
        label = "Мої адреси",
        selectedIcon = Icons.Filled.LocationOn,
        unselectedIcon = Icons.Outlined.LocationOn
    ),
    BottomNavItem(
        label = "Налаштування",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

