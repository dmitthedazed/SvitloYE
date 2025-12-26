package com.occaecat.ztoeschedule.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.*
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.presentation.ui.addresses.MyAddressesTab
import com.occaecat.ztoeschedule.presentation.ui.home.HomeTab
import com.occaecat.ztoeschedule.presentation.ui.notifications.NotificationsTab
import com.occaecat.ztoeschedule.presentation.ui.onboarding.OnboardingFlow
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsTab
import com.occaecat.ztoeschedule.presentation.ui.more.MoreTab
import com.occaecat.ztoeschedule.presentation.ui.more.DonateScreen
import com.occaecat.ztoeschedule.presentation.ui.more.AboutScreen
import com.occaecat.ztoeschedule.presentation.ui.more.FaqScreen
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel

import com.occaecat.ztoeschedule.presentation.ui.addresses.AddAddressScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: EnergyScheduleViewModel = hiltViewModel(),
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val uiState by viewModel.uiState.collectAsState()
    val isWideScreen = widthSizeClass != WindowWidthSizeClass.Compact
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val haptic = LocalHapticFeedback.current

    val pagerState = rememberPagerState(pageCount = { uiState.addressDataList.size })
    
    // Spring specs for 2025 Motion
    val springSpec = spring<IntOffset>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
    val floatSpring = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)

    val currentTitle = remember(currentRoute, pagerState.currentPage, uiState.addressDataList) {
        when {
            currentRoute == "home" -> if (uiState.addressDataList.isNotEmpty()) uiState.addressDataList[pagerState.currentPage].address.name else "Головна"
            currentRoute == "notifications" -> "Події"
            currentRoute == "addresses" -> "Локації"
            currentRoute == "more" -> "Меню"
            currentRoute == "settings" -> "Налаштування"
            currentRoute == "inspect" -> uiState.inspectedAddress?.name ?: "Перегляд"
            else -> "СвітлоЄ?"
        }
    }

    if (!uiState.onboardingCompleted || !uiState.hasSavedSelection) {
        OnboardingFlow(
            remList = uiState.remList, cityList = uiState.cityList, streetList = uiState.streetList,
            houseNumbers = uiState.filteredHouseNumbers, searchQuery = uiState.houseNumberSearchQuery,
            isLoading = uiState.isLoading, onLoadRem = { viewModel.loadRemList() },
            onLoadCity = { viewModel.loadCityList(it) }, onLoadStreet = { viewModel.loadStreetList(it) },
            onLoadAddress = { viewModel.loadAddressList(it) }, onSearchQueryChange = { viewModel.filterHouseNumbers(it) },
            onClearSearch = { viewModel.clearHouseNumberSearch() }, onComplete = { remId, remName, cityId, cityName, streetId, streetName, addrId, addrName, cherga, pid, customName, iconName ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.saveSelection(remId, remName, cityId, cityName, streetId, streetName, addrId, addrName, cherga, pid, customName, iconName)
                viewModel.completeOnboarding()
            }
        )
        return
    }

    val isInspecting = currentRoute == "inspect" && !isWideScreen
    val isSettings = currentRoute == "settings" && !isWideScreen
    val isAdding = currentRoute == "add_address"
    val shouldShowBars = !isInspecting && !isSettings && !isAdding

    // Predictive Back
    if (isInspecting) {
        PredictiveBackHandler(enabled = true) { progress ->
            try { progress.collect { }; navController.popBackStack() } 
            catch (e: Exception) { navController.popBackStack() }
        }
    }

    val navItems = listOf(
        BottomNavItem("home", "Головна", Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem("notifications", "Події", Icons.Filled.Notifications, Icons.Outlined.Notifications),
        BottomNavItem("addresses", "Локації", Icons.Filled.LocationOn, Icons.Outlined.LocationOn),
        BottomNavItem("more", "Меню", Icons.Filled.Menu, Icons.Outlined.Menu)
    )

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            if (isWideScreen) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = { Icon(Icons.Default.Bolt, null, Modifier.padding(vertical = 12.dp), tint = MaterialTheme.colorScheme.primary) }
                ) {
                    navItems.forEach { item ->
                        NavigationRailItem(
                            icon = { Icon(if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon, item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (currentRoute != item.route) navController.navigate(item.route) { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } 
                            }
                        )
                    }
                }
            }

            Scaffold(
                topBar = {
                    if (shouldShowBars) {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            title = {
                                AnimatedContent(
                                    targetState = currentTitle,
                                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.92f)).togetherWith(fadeOut() + scaleOut(targetScale = 0.92f)) },
                                    label = "title"
                                ) { title ->
                                    Column {
                                        Text(title, fontWeight = FontWeight.Bold)
                                        if (currentRoute == "home" && uiState.addressDataList.size > 1) {
                                            Text("${pagerState.currentPage + 1} з ${uiState.addressDataList.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        )
                    }
                },
                bottomBar = {
                    AnimatedVisibility(
                        visible = !isWideScreen && shouldShowBars,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = springSpec) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = springSpec) + fadeOut()
                    ) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            navItems.forEach { item ->
                                NavigationBarItem(
                                    icon = { Icon(if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon, item.label) },
                                    label = { Text(item.label) },
                                    selected = currentRoute == item.route,
                                    onClick = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (currentRoute != item.route) navController.navigate(item.route) { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } 
                                    }
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                val bottomNavRoutes = listOf("home", "notifications", "addresses", "more")
                fun getRouteIndex(route: String?): Int = bottomNavRoutes.indexOf(route)

                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        val fromIndex = getRouteIndex(initialState.destination.route)
                        val toIndex = getRouteIndex(targetState.destination.route)
                        if (fromIndex != -1 && toIndex != -1) {
                            if (toIndex > fromIndex) slideInHorizontally { it } + fadeIn() else slideInHorizontally { -it } + fadeIn()
                        } else {
                            slideInHorizontally { it } + fadeIn()
                        }
                    },
                    exitTransition = {
                        val fromIndex = getRouteIndex(initialState.destination.route)
                        val toIndex = getRouteIndex(targetState.destination.route)
                        if (fromIndex != -1 && toIndex != -1) {
                            if (toIndex > fromIndex) slideOutHorizontally { -it } + fadeOut() else slideOutHorizontally { it } + fadeOut()
                        } else {
                            slideOutHorizontally { -it } + fadeOut()
                        }
                    },
                    popEnterTransition = {
                        val fromIndex = getRouteIndex(initialState.destination.route)
                        val toIndex = getRouteIndex(targetState.destination.route)
                        if (fromIndex != -1 && toIndex != -1) {
                            if (toIndex > fromIndex) slideInHorizontally { it } + fadeIn() else slideInHorizontally { -it } + fadeIn()
                        } else {
                            slideInHorizontally { -it } + fadeIn()
                        }
                    },
                    popExitTransition = {
                        val fromIndex = getRouteIndex(initialState.destination.route)
                        val toIndex = getRouteIndex(targetState.destination.route)
                        if (fromIndex != -1 && toIndex != -1) {
                            if (toIndex > fromIndex) slideOutHorizontally { -it } + fadeOut() else slideOutHorizontally { it } + fadeOut()
                        } else {
                            slideOutHorizontally { it } + fadeOut()
                        }
                    }
                ) {
                    composable("home") {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                            val data = uiState.addressDataList[page]
                            HomeTab(data.address.remName, data.address.cityName, data.address.streetName, data.address.addressName, data.address.cherga, data.address.pidcherga, data.currentStatus, data.scheduleList, data.groupedSchedule, { viewModel.refreshAllSchedules() }, Modifier.fillMaxSize(), padding, data.lastUpdateTime, data.isOffline, uiState.isLoading)
                        }
                    }
                    composable("notifications") { NotificationsTab(uiState.infoMessages, uiState.formattedMessage, Modifier.fillMaxSize(), padding, uiState.isLoading) }
                    composable("addresses") {
                        MyAddressesTab(
                            uiState.savedAddresses, uiState.addressStatuses, uiState.isAddingNewAddress, uiState.remList, uiState.cityList, uiState.streetList, uiState.filteredHouseNumbers, uiState.houseNumberSearchQuery, uiState.isLoading, isWideScreen, uiState.inspectedScheduleList, uiState.inspectedGroupedSchedule, uiState.isInspectingLoading,
                            { viewModel.startAddingAddress(); navController.navigate("add_address") }, { viewModel.cancelAddingAddress() }, { viewModel.loadRemList() }, { viewModel.loadCityList(it) }, { viewModel.loadStreetList(it) }, { viewModel.loadAddressList(it) }, { viewModel.filterHouseNumbers(it) }, { viewModel.clearHouseNumberSearch() }, { n, i, rI, rN, cI, cN, sI, sN, aI, aN, c, p -> viewModel.addSavedAddress(n, i, rI, rN, cI, cN, sI, sN, aI, aN, c, p) }, { viewModel.deleteSavedAddress(it) }, { viewModel.updateAddressesOrder(it) }, { c, p -> viewModel.loadScheduleWithMessages(c, p) }, { viewModel.startInspectingAddress(it); if (!isWideScreen) navController.navigate("inspect") }, Modifier.fillMaxSize(), padding
                        )
                    }
                    composable("add_address") {
                        AddAddressScreen(
                            uiState.remList, uiState.cityList, uiState.streetList, uiState.filteredHouseNumbers, uiState.houseNumberSearchQuery, uiState.isLoading,
                            { viewModel.loadRemList() }, { viewModel.loadCityList(it) }, { viewModel.loadStreetList(it) }, { viewModel.loadAddressList(it) }, { viewModel.filterHouseNumbers(it) }, { viewModel.clearHouseNumberSearch() },
                            { n, i, rI, rN, cI, cN, sI, sN, aI, aN, c, p -> 
                                viewModel.addSavedAddress(n, i, rI, rN, cI, cN, sI, sN, aI, aN, c, p)
                                navController.popBackStack()
                            },
                            { navController.popBackStack() }
                        )
                    }
                    composable("more") {
                        MoreTab(
                            scheduleList = uiState.addressDataList.getOrNull(pagerState.currentPage)?.scheduleList ?: emptyList(),
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToDonate = { navController.navigate("donate") },
                            onNavigateToAbout = { navController.navigate("about") },
                            onNavigateToFaq = { navController.navigate("faq") },
                            displayMode = uiState.displayMode,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = padding
                        )
                    }
                    composable("donate") { DonateScreen { navController.popBackStack() } }
                    composable("about") { AboutScreen { navController.popBackStack() } }
                    composable("faq") { FaqScreen { navController.popBackStack() } }
                    composable("settings") {
                        Scaffold(topBar = { TopAppBar(title = { Text("Налаштування") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }) }) { innerPadding ->
                            SettingsTab(uiState.notificationsEnabled, uiState.notificationAdvanceMinutes, uiState.statusNotificationEnabled, uiState.liveActivityEnabled, uiState.lastUpdateTime, uiState.displayMode, uiState.colorTheme, uiState.fontScale, uiState.smartNotificationSettings, uiState.notificationMode, { viewModel.setNotificationsEnabled(it) }, { viewModel.setNotificationAdvanceMinutes(it) }, { viewModel.setStatusNotificationEnabled(it) }, { viewModel.setLiveActivityEnabled(it) }, { viewModel.setDisplayMode(it) }, { viewModel.setColorTheme(it) }, { viewModel.setFontScale(it) }, { viewModel.setSmartNotificationSettings(it) }, { viewModel.setNotificationMode(it) }, { viewModel.resetOnboarding() }, { viewModel.clearData() }, Modifier.fillMaxSize(), innerPadding)
                        }
                    }
                    composable("inspect") {
                        val addr = uiState.inspectedAddress
                        if (addr != null) {
                            Scaffold(topBar = { TopAppBar(title = { Text(addr.name, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { navController.popBackStack(); viewModel.stopInspectingAddress() }) { Icon(Icons.Default.ArrowBack, null) } }) }) { innerPadding ->
                                HomeTab(addr.remName, addr.cityName, addr.streetName, addr.addressName, addr.cherga, addr.pidcherga, null, uiState.inspectedScheduleList, uiState.inspectedGroupedSchedule, { viewModel.startInspectingAddress(addr) }, Modifier.fillMaxSize(), innerPadding, "", false)
                            }
                        }
                    }
                }
            }
        }

        // Global Dialogs
        if (uiState.showWidgetConfig) {
            AlertDialog(onDismissRequest = { viewModel.setShowWidgetConfig(false) }, title = { Text("Віджет") }, text = { Column { uiState.savedAddresses.forEach { a -> ListItem(headlineContent = { Text(a.name) }, modifier = Modifier.clickable { viewModel.selectWidgetAddress(a) }) } } }, confirmButton = { TextButton(onClick = { viewModel.setShowWidgetConfig(false) }) { Text("Закрити") } })
        }
    }
}

@Composable
private fun NoConnectionScreen(countdown: Int, isLoading: Boolean, onRetry: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                val iconAlpha by animateFloatAsState(if (isLoading) 0.5f else 1f, label = "a")
                Surface(modifier = Modifier.size(120.dp).graphicsLayer { alpha = iconAlpha }, shape = CircleShape, color = (if (isLoading) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer).copy(alpha = 0.4f)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        else Icon(Icons.Default.WifiOff, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isLoading) "Оновлення..." else "Немає зв'язку", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(if (isLoading) "Будь ласка, зачекайте" else "Перевірте підключення", textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = !isLoading) {
                    Text(if (isLoading) "Зачекайте..." else "Спробувати знову")
                }
            }
        }
    }
}

private data class BottomNavItem(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)
