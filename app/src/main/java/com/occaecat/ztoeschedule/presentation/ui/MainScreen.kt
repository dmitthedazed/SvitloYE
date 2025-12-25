package com.occaecat.ztoeschedule.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.occaecat.ztoeschedule.presentation.ui.addresses.MyAddressesTab
import com.occaecat.ztoeschedule.presentation.ui.home.HomeTab
import com.occaecat.ztoeschedule.presentation.ui.notifications.NotificationsTab
import com.occaecat.ztoeschedule.presentation.ui.onboarding.OnboardingFlow
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsTab
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModelFactory
import kotlinx.coroutines.delay

/**
 * New main screen with bottom navigation and history-based back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val viewModel: EnergyScheduleViewModel = viewModel(
        factory = EnergyScheduleViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Onboarding
    if (!uiState.onboardingCompleted || !uiState.hasSavedSelection) {
        OnboardingFlow(
            remList = uiState.remList,
            cityList = uiState.cityList,
            streetList = uiState.streetList,
            houseNumbers = uiState.filteredHouseNumbers,
            searchQuery = uiState.houseNumberSearchQuery,
            isLoading = uiState.isLoading,
            onLoadRem = { viewModel.loadRemList() },
            onLoadCity = { viewModel.loadCityList(it) },
            onLoadStreet = { viewModel.loadStreetList(it) },
            onLoadAddress = { viewModel.loadAddressList(it) },
            onSearchQueryChange = { viewModel.filterHouseNumbers(it) },
            onClearSearch = { viewModel.clearHouseNumberSearch() },
            onCancel = null,
            showWelcome = true,
            onComplete = { remId, remName, cityId, cityName, streetId, streetName, addrId, addrName, cherga, pid, customName, iconName ->
                viewModel.saveSelection(remId, remName, cityId, cityName, streetId, streetName, addrId, addrName, cherga, pid, customName, iconName)
                viewModel.loadScheduleWithMessages(cherga, pid)
                viewModel.completeOnboarding()
            }
        )
        return
    }

    // Determine if we have any data to show at all
    val hasDataToShow = uiState.scheduleList.isNotEmpty()

    // 1. Error or Loading-without-data State
    // Show NoConnectionScreen if we have no data and either:
    // - The last load failed
    // - We are currently loading (first time)
    if (!hasDataToShow) {
        if (uiState.lastLoadFailed || uiState.isLoading || !uiState.isInitialLoadComplete) {
            NoConnectionScreen(
                countdown = uiState.retryCountdown,
                isLoading = uiState.isLoading,
                onRetry = { viewModel.retryLoading() }
            )
            return
        }
    }

    // 2. Main Content (Appears only when scheduleList is not empty)
    // --- Navigation History and Back Handling ---
    var selectedTab by remember { mutableIntStateOf(0) }
    val navigationHistory = remember { mutableStateListOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Time Sync Warning Dialog
    if (uiState.isTimeOutOfSync) {
        AlertDialog(
            onDismissRequest = { }, 
            title = { Text("Неправильний час") },
            text = { Text("Час на вашому пристрої значно відрізняється від серверного. Це призведе до помилок у відображенні графіка.\n\nБудь ласка, встановіть 'Автоматичне визначення часу' у налаштуваннях Android.") },
            confirmButton = {
                Button(onClick = { viewModel.dismissTimeSyncWarning() }) {
                    Text("Зрозуміло")
                }
            },
            icon = { Icon(Icons.Default.TimerOff, null, tint = MaterialTheme.colorScheme.error) }
        )
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Logic to handle back navigation
    val canGoBack = navigationHistory.size > 1 || uiState.isAddingNewAddress
    
    BackHandler(enabled = canGoBack) {
        if (uiState.isAddingNewAddress) {
            viewModel.cancelAddingAddress()
        } else if (navigationHistory.size > 1) {
            navigationHistory.removeAt(navigationHistory.size - 1)
            selectedTab = navigationHistory.last()
        }
    }

    // Function to switch tabs and update history
    val onTabSelected: (Int) -> Unit = { index ->
        if (selectedTab != index) {
            selectedTab = index
            navigationHistory.remove(index)
            navigationHistory.add(index)
        }
    }

    // Lifecycle observer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (uiState.hasSavedSelection && uiState.savedCherga > 0 && uiState.savedPidcherga > 0) {
                    viewModel.loadScheduleWithMessages(uiState.savedCherga, uiState.savedPidcherga)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Background refresh
    LaunchedEffect(uiState.hasSavedSelection, uiState.savedCherga, uiState.savedPidcherga) {
        if (uiState.hasSavedSelection && uiState.savedCherga > 0 && uiState.savedPidcherga > 0) {
            while (true) {
                delay(300_000)
                viewModel.loadScheduleWithMessages(uiState.savedCherga, uiState.savedPidcherga)
            }
        }
    }

    // Main UI
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val showMainChrome = !(selectedTab == 2 && uiState.isAddingNewAddress)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (showMainChrome) {
                TopAppBar(
                    title = { 
                        AnimatedContent(
                            targetState = bottomNavItems[selectedTab].label,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            modifier = Modifier.offset(x = 4.dp), // Nudge slightly right
                            label = "title_animation"
                        ) { label ->
                            Text(text = label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    },

                    scrollBehavior = scrollBehavior
                )
            }
        },
        bottomBar = {
            if (showMainChrome) {
                NavigationBar {
                    bottomNavItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selectedTab == index,
                            onClick = { onTabSelected(index) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                }.using(SizeTransform(clip = false))
            },
            modifier = Modifier.fillMaxSize(),
            label = "tab_transition"
        ) { targetTab ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (targetTab) {
                    0 -> {
                        Crossfade(targetState = uiState.savedAddressName, label = "address_swap") { _ ->
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
                                onRefresh = { viewModel.loadScheduleWithMessages(uiState.savedCherga, uiState.savedPidcherga) },
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = padding,
                                lastUpdateTime = uiState.lastUpdateTime
                            )
                        }
                    }
                    1 -> NotificationsTab(
                        messages = uiState.infoMessages, 
                        formattedMessage = uiState.formattedMessage, 
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = padding
                    )
                    2 -> MyAddressesTab(
                        addresses = uiState.savedAddresses,
                        addressStatuses = uiState.addressStatuses,
                        isAddingNew = uiState.isAddingNewAddress,
                        remList = uiState.remList,
                        cityList = uiState.cityList,
                        streetList = uiState.streetList,
                        houseNumbers = uiState.filteredHouseNumbers,
                        searchQuery = uiState.houseNumberSearchQuery,
                        isLoading = uiState.isLoading,
                        onStartAdding = { viewModel.startAddingAddress() },
                        onCancelAdding = { viewModel.cancelAddingAddress() },
                        onLoadRem = { viewModel.loadRemList() },
                        onLoadCity = { viewModel.loadCityList(it) },
                        onLoadStreet = { viewModel.loadStreetList(it) },
                        onLoadAddress = { viewModel.loadAddressList(it) },
                        onSearchQueryChange = { viewModel.filterHouseNumbers(it) },
                        onClearSearch = { viewModel.clearHouseNumberSearch() },
                        onSaveAddress = { name, icon, remId, remName, cityId, cityName, streetId, streetName, addrId, addrName, cherga, pid ->
                            viewModel.addSavedAddress(name, icon, remId, remName, cityId, cityName, streetId, streetName, addrId, addrName, cherga, pid)
                        },
                        onDeleteAddress = { viewModel.deleteSavedAddress(it) },
                        onUpdateOrder = { viewModel.updateAddressesOrder(it) },
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = padding
                    )
                    3 -> SettingsTab(
                        notificationsEnabled = uiState.notificationsEnabled,
                        notificationAdvanceMinutes = uiState.notificationAdvanceMinutes,
                        statusNotificationEnabled = uiState.statusNotificationEnabled,
                        liveActivityEnabled = uiState.liveActivityEnabled,
                        lastUpdateTime = uiState.lastUpdateTime,
                        onNotificationsEnabledChange = { viewModel.setNotificationsEnabled(it) },
                        onNotificationAdvanceMinutesChange = { viewModel.setNotificationAdvanceMinutes(it) },
                        onStatusNotificationEnabledChange = { viewModel.setStatusNotificationEnabled(it) },
                        onLiveActivityEnabledChange = { viewModel.setLiveActivityEnabled(it) },
                        onResetOnboarding = { viewModel.resetOnboarding() },
                        onClearData = { viewModel.clearData() },
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = padding
                    )
                }
            }
        }
    }
}

@Composable
private fun NoConnectionScreen(
    countdown: Int,
    isLoading: Boolean,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Adaptive Icon (Animate if loading)
                val iconAlpha by animateFloatAsState(if (isLoading) 0.5f else 1f, label = "icon_alpha")
                
                Surface(
                    modifier = Modifier.size(120.dp).graphicsLayer { alpha = iconAlpha },
                    shape = CircleShape,
                    color = (if (isLoading) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer).copy(alpha = 0.4f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isLoading) "Оновлення даних..." else "Немає зв'язку з сервером",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = if (isLoading) "Будь ласка, зачекайте, ми намагаємось отримати графік." 
                               else "Будь ласка, перевірте підключення, щоб завантажити актуальний графік.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (countdown > 0 && !isLoading) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Повторна спроба через $countdown сек...",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    enabled = !isLoading && (countdown == 0 || countdown < 25),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isLoading) {
                        Text("Зачекайте...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Спробувати зараз", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class BottomNavItem(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem("Головна", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("Повідомлення", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    BottomNavItem("Мої адреси", Icons.Filled.LocationOn, Icons.Outlined.LocationOn),
    BottomNavItem("Налаштування", Icons.Filled.Settings, Icons.Outlined.Settings)
)
