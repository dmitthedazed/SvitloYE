package com.occaecat.ztoeschedule.presentation.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.*
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.*
import com.occaecat.ztoeschedule.presentation.ui.addresses.AddAddressScreen
import com.occaecat.ztoeschedule.presentation.ui.addresses.MyAddressesTab
import com.occaecat.ztoeschedule.presentation.ui.home.HomeTab
import com.occaecat.ztoeschedule.presentation.ui.more.AboutScreen
import com.occaecat.ztoeschedule.presentation.ui.more.DonateScreen
import com.occaecat.ztoeschedule.presentation.ui.more.FaqScreen
import com.occaecat.ztoeschedule.presentation.ui.more.MoreTab
import com.occaecat.ztoeschedule.presentation.ui.notifications.NotificationsTab
import com.occaecat.ztoeschedule.presentation.ui.onboarding.OnboardingFlow
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsTab
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: EnergyScheduleViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass
) {
    val uiState by viewModel.uiState.collectAsState()
    val showNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val useWideLayout = showNavRail
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { uiState.addressDataList.size })
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() } }
    LaunchedEffect(uiState.infoMessage) { uiState.infoMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearInfoMessage() } }
    LaunchedEffect(uiState.isAddingNewAddress) { if (uiState.isAddingNewAddress && currentRoute != "add_address") navController.navigate("add_address") { launchSingleTop = true } }
    
    LaunchedEffect(uiState.requestedAddressId, uiState.addressDataList) {
        uiState.requestedAddressId?.let { id ->
            val idx = uiState.addressDataList.indexOfFirst { it.address.id == id }
            if (idx != -1) { 
                pagerState.animateScrollToPage(idx)
                if (currentRoute != "home") navController.navigate("home") { 
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true 
                } 
            }
            viewModel.setRequestedAddressId(null)
        }
    }

    val currentTitle by remember(uiState.addressDataList, currentRoute, pagerState.currentPage) {
        derivedStateOf {
            when {
                currentRoute == "home" -> if (uiState.addressDataList.isNotEmpty() && pagerState.currentPage in uiState.addressDataList.indices) uiState.addressDataList[pagerState.currentPage].address.name else "Головна"
                currentRoute == "notifications" -> "Події"
                currentRoute == "addresses" -> "Локації"
                currentRoute == "more" -> "Меню"
                currentRoute == "settings" -> "Налаштування"
                currentRoute == "inspect" -> uiState.inspectedAddress?.name ?: "Перегляд"
                else -> "СвітлоЄ?"
            }
        }
    }

    if (!uiState.onboardingCompleted || !uiState.hasSavedSelection) {
        OnboardingFlow(
            uiState.remList, uiState.cityList, uiState.streetList, uiState.filteredHouseNumbers, uiState.houseNumberSearchQuery, uiState.isLoading,
            { viewModel.loadRemList() }, { viewModel.loadCityList(it) }, { viewModel.loadStreetList(it) }, { viewModel.loadAddressList(it) },
            { viewModel.filterHouseNumbers(it) }, { viewModel.clearHouseNumberSearch() }, onComplete = { rI, rN, cI, cN, sI, sN, aI, aN, c, p, n, i ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.saveSelection(rI, rN, cI, cN, sI, sN, aI, aN, c, p, n, i); viewModel.completeOnboarding()
            }
        )
        return
    }

    val isInspecting = remember(currentRoute, useWideLayout) { currentRoute == "inspect" && !useWideLayout }
    val isSettings = remember(currentRoute, useWideLayout) { currentRoute == "settings" && !useWideLayout }
    val isAdding = remember(currentRoute) { currentRoute == "add_address" }
    val shouldShowBars = remember(isInspecting, isSettings, isAdding) { !isInspecting && !isSettings && !isAdding }

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

    ModalNavigationDrawer(
        drawerState = drawerState, 
        gesturesEnabled = !useWideLayout && shouldShowBars,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                    Spacer(Modifier.height(12.dp))
                    Text("Ваші локації", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    uiState.addressDataList.forEachIndexed { index, data ->
                        NavigationDrawerItem(
                            label = { Column { Text(data.address.name, fontWeight = FontWeight.SemiBold); Text("${data.address.cityName}, ${data.address.streetName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                            selected = currentRoute == "home" && pagerState.currentPage == index,
                            icon = { Icon(when (data.address.iconName) { "home" -> Icons.Default.Home; "work" -> Icons.Default.Work; "apartment" -> Icons.Default.Apartment; else -> Icons.Default.LocationOn }, null) },
                            badge = { if (data.isOffline) Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) else Surface(color = if (data.currentStatus?.status == ScheduleStatus.Available) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error, shape = CircleShape, modifier = Modifier.size(8.dp)) {} },
                            onClick = { scope.launch { drawerState.close(); if (currentRoute != "home") navController.navigate("home") { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true }; pagerState.animateScrollToPage(index) } },
                            modifier = Modifier.padding(vertical = 2.dp), shape = MaterialTheme.shapes.medium
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    NavigationDrawerItem(
                        label = { Text("Керування адресами") }, 
                        selected = currentRoute == "addresses", 
                        icon = { Icon(Icons.Default.SettingsSuggest, null) },
                        onClick = { scope.launch { drawerState.close(); navController.navigate("addresses") { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } } },
                        modifier = Modifier.padding(bottom = 12.dp), shape = MaterialTheme.shapes.medium
                    )
                }
            }
        }
    ) {
        Row(Modifier.fillMaxSize()) {
            if (useWideLayout) {
                NavigationRail(
                    modifier = Modifier.statusBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Bolt, null, Modifier.padding(vertical = 12.dp), tint = MaterialTheme.colorScheme.primary)
                            FloatingActionButton(onClick = { viewModel.startAddingAddress(); navController.navigate("add_address") }, containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) { Icon(Icons.Default.Add, "Додати") }; Spacer(Modifier.height(8.dp))
                        }
                    }
                ) {
                    navItems.forEach { item ->
                        NavigationRailItem(
                            icon = { if (item.route == "notifications" && uiState.infoMessages.isNotEmpty()) { BadgedBox(badge = { Badge { Text("${uiState.infoMessages.size}") } }) { Icon(if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon, item.label) } } else Icon(if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon, item.label) },
                            label = { Text(item.label) }, selected = currentRoute == item.route,
                            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); if (currentRoute != item.route) navController.navigate(item.route) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } }
                        )
                    }
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    if (shouldShowBars) {
                        Column {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                navigationIcon = { 
                                    if (!useWideLayout) { 
                                        IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) { 
                                            Icon(Icons.Default.Menu, "Меню") 
                                        } 
                                    } 
                                },
                                title = { 
                                    AnimatedContent(
                                        targetState = currentTitle, 
                                        transitionSpec = { if (targetState != initialState) (slideInVertically { it / 2 } + fadeIn() + scaleIn(initialScale = 0.95f)).togetherWith(slideOutVertically { -it / 2 } + fadeOut() + scaleOut(targetScale = 0.95f)) else fadeIn() togetherWith fadeOut() },
                                        label = "title_animation"
                                    ) { title -> 
                                        Column { 
                                            Text(title, fontWeight = FontWeight.Bold)
                                            if (currentRoute == "home" && uiState.addressDataList.size > 1) {
                                                Text("${pagerState.currentPage + 1} з ${uiState.addressDataList.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        } 
                                    } 
                                },
                                actions = { 
                                    Box { 
                                        IconButton(onClick = { showMenu = true }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) { 
                                            Icon(Icons.Default.MoreVert, "Меню") 
                                        }
                                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) { 
                                            DropdownMenuItem(text = { Text("Оновити все") }, leadingIcon = { Icon(Icons.Default.Refresh, null) }, onClick = { showMenu = false; viewModel.refreshAllSchedules() })
                                            DropdownMenuItem(text = { Text("Налаштувати віджет") }, leadingIcon = { Icon(Icons.Default.Widgets, null) }, onClick = { showMenu = false; viewModel.setShowWidgetConfig(true) })
                                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                            DropdownMenuItem(text = { Text("Допомога (FAQ)") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null) }, onClick = { showMenu = false; navController.navigate("faq") }) 
                                        } 
                                    } 
                                }
                            )
                            if (uiState.isLoading) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(3.dp).semantics { liveRegion = LiveRegionMode.Polite }, 
                                    color = MaterialTheme.colorScheme.primary, 
                                    trackColor = Color.Transparent
                                )
                            }
                        }
                    }
                },
                bottomBar = {
                    AnimatedVisibility(visible = !useWideLayout && shouldShowBars, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            navItems.forEach { item ->
                                NavigationBarItem(
                                    icon = { if (item.route == "notifications" && uiState.infoMessages.isNotEmpty()) { BadgedBox(badge = { Badge { Text("${uiState.infoMessages.size}") } }) { Icon(if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon, item.label) } } else Icon(if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon, item.label) },
                                    label = { Text(item.label) }, selected = currentRoute == item.route,
                                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); if (currentRoute != item.route) navController.navigate(item.route) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = { AnimatedVisibility(visible = !useWideLayout && (currentRoute == "addresses" || (currentRoute == "home" && uiState.addressDataList.isEmpty())), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) { ExtendedFloatingActionButton(onClick = { viewModel.startAddingAddress(); navController.navigate("add_address") }, icon = { Icon(Icons.Default.AddLocation, null) }, text = { Text("Додати") }, containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) } }
            ) { padding ->
                val br = listOf("home", "notifications", "addresses", "more"); fun gRI(r: String?): Int = br.indexOf(r)
                NavHost(
                    navController = navController, startDestination = "home", modifier = Modifier.fillMaxSize(),
                    enterTransition = { val f = gRI(initialState.destination.route); val t = gRI(targetState.destination.route); if (f != -1 && t != -1) { if (t > f) slideInHorizontally { it } + fadeIn() else slideInHorizontally { -it } + fadeIn() } else slideInHorizontally { it } + fadeIn() },
                    exitTransition = { val f = gRI(initialState.destination.route); val t = gRI(targetState.destination.route); if (f != -1 && t != -1) { if (t > f) slideOutHorizontally { -it } + fadeOut() else slideOutHorizontally { it } + fadeOut() } else slideOutHorizontally { -it } + fadeOut() },
                    popEnterTransition = { val f = gRI(initialState.destination.route); val t = gRI(targetState.destination.route); if (f != -1 && t != -1) { if (t > f) slideInHorizontally { it } + fadeIn() else slideInHorizontally { -it } + fadeIn() } else slideInHorizontally { it } + fadeIn() },
                    popExitTransition = { val f = gRI(initialState.destination.route); val t = gRI(targetState.destination.route); if (f != -1 && t != -1) { val s = if (t > f) slideOutHorizontally { -it } else slideOutHorizontally { it }; s + fadeOut() + scaleOut(targetScale = 0.9f) } else slideOutHorizontally { it } + fadeOut() + scaleOut(targetScale = 0.9f) }
                ) {
                    composable("home") { 
                        val ctx = LocalContext.current
                        LaunchedEffect(pagerState.currentPage, uiState.addressDataList) { 
                            if (uiState.addressDataList.isNotEmpty()) { 
                                val id = uiState.addressDataList[pagerState.currentPage].address.id
                                androidx.core.content.pm.ShortcutManagerCompat.reportShortcutUsed(ctx, "address_$id") 
                            } 
                        }
                        HorizontalPager(
                            state = pagerState, 
                            modifier = Modifier.fillMaxSize().focusable().onKeyEvent { 
                                if (it.type == KeyEventType.KeyUp) { 
                                    when (it.key) { 
                                        Key.DirectionRight -> if (pagerState.currentPage < pagerState.pageCount - 1) { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }; true } else false
                                        Key.DirectionLeft -> if (pagerState.currentPage > 0) { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }; true } else false
                                        else -> false 
                                    } 
                                } else false 
                            }, 
                            beyondViewportPageCount = 1
                        ) { page -> 
                            val d = uiState.addressDataList[page]
                            HomeTab(d.address.remName, d.address.cityName, d.address.streetName, d.address.addressName, d.address.cherga, d.address.pidcherga, d.currentStatus, d.scheduleList, d.groupedSchedule, { viewModel.refreshAllSchedules() }, Modifier.fillMaxSize(), padding, d.lastUpdateTime, d.isOffline, uiState.isLoading) 
                        } 
                    }
                    composable("notifications") { NotificationsTab(uiState.infoMessages, uiState.formattedMessage, Modifier.fillMaxSize(), padding, uiState.isLoading) }
                    composable("addresses") { MyAddressesTab(uiState.savedAddresses, uiState.addressStatuses, uiState.isAddingNewAddress, uiState.remList, uiState.cityList, uiState.streetList, uiState.filteredHouseNumbers, uiState.houseNumberSearchQuery, uiState.isLoading, useWideLayout, uiState.inspectedScheduleList, uiState.inspectedGroupedSchedule, uiState.isInspectingLoading, { viewModel.startAddingAddress(); navController.navigate("add_address") }, { viewModel.cancelAddingAddress() }, { viewModel.loadRemList() }, { viewModel.loadCityList(it) }, { viewModel.loadStreetList(it) }, { viewModel.loadAddressList(it) }, { viewModel.filterHouseNumbers(it) }, { viewModel.clearHouseNumberSearch() }, { n, i, rI, rN, cI, cN, sI, sN, aI, aN, c, p -> viewModel.addSavedAddress(n, i, rI, rN, cI, cN, sI, sN, aI, aN, c, p) }, { viewModel.deleteSavedAddress(it) }, { viewModel.updateAddressesOrder(it) }, { c, p -> viewModel.loadScheduleWithMessages(c, p) }, { viewModel.startInspectingAddress(it); if (!useWideLayout) navController.navigate("inspect") }, Modifier.fillMaxSize(), padding) }
                    composable("add_address") { AddAddressScreen(uiState.remList, uiState.cityList, uiState.streetList, uiState.filteredHouseNumbers, uiState.houseNumberSearchQuery, uiState.isLoading, { viewModel.loadRemList() }, { viewModel.loadCityList(it) }, { viewModel.loadStreetList(it) }, { viewModel.loadAddressList(it) }, { viewModel.filterHouseNumbers(it) }, { viewModel.clearHouseNumberSearch() }, { n, i, rI, rN, cI, cN, sI, sN, aI, aN, c, p -> viewModel.addSavedAddress(n, i, rI, rN, cI, cN, sI, sN, aI, aN, c, p); navController.popBackStack() }, { navController.popBackStack() }) }
                    composable("more") { MoreTab(scheduleList = uiState.addressDataList.getOrNull(pagerState.currentPage)?.scheduleList ?: emptyList(), onNavigateToSettings = { navController.navigate("settings") }, onNavigateToDonate = { navController.navigate("donate") }, onNavigateToAbout = { navController.navigate("about") }, onNavigateToFaq = { navController.navigate("faq") }, displayMode = uiState.displayMode, modifier = Modifier.fillMaxSize(), contentPadding = padding) }
                    composable("donate") { DonateScreen { navController.popBackStack() } }
                    composable("about") { AboutScreen { navController.popBackStack() } }
                    composable("faq") { FaqScreen { navController.popBackStack() } }
                    composable("settings") { Scaffold(topBar = { TopAppBar(title = { Text("Налаштування") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { innerPadding -> SettingsTab(uiState.notificationsEnabled, uiState.notificationAdvanceMinutes, uiState.statusNotificationEnabled, uiState.liveActivityEnabled, uiState.lastUpdateTime, uiState.displayMode, uiState.colorTheme, uiState.fontScale, uiState.smartNotificationSettings, uiState.notificationMode, { viewModel.setNotificationsEnabled(it) }, { viewModel.setNotificationAdvanceMinutes(it) }, { viewModel.setStatusNotificationEnabled(it) }, { viewModel.setLiveActivityEnabled(it) }, { viewModel.setDisplayMode(it) }, { viewModel.setColorTheme(it) }, { viewModel.setFontScale(it) }, { viewModel.setSmartNotificationSettings(it) }, { viewModel.setNotificationMode(it) }, { viewModel.resetOnboarding() }, { viewModel.clearData() }, Modifier.fillMaxSize(), innerPadding) } }
                    composable("inspect") { val addr = uiState.inspectedAddress; if (addr != null) { Scaffold(topBar = { TopAppBar(title = { Text(addr.name, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { navController.popBackStack(); viewModel.stopInspectingAddress() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { innerPadding -> HomeTab(addr.remName, addr.cityName, addr.streetName, addr.addressName, addr.cherga, addr.pidcherga, null, uiState.inspectedScheduleList, uiState.inspectedGroupedSchedule, { viewModel.startInspectingAddress(addr) }, Modifier.fillMaxSize(), innerPadding, "", false) } } }
                }
            }
        }

        if (uiState.showWidgetConfig) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setShowWidgetConfig(false) }, sheetState = sheetState, modifier = Modifier.fillMaxHeight().semantics { paneTitle = "Вибір адреси для віджета" }, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    Text(text = "Оберіть адресу для віджета", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                    uiState.savedAddresses.forEach { a -> ListItem(headlineContent = { Text(a.name, fontWeight = FontWeight.SemiBold) }, supportingContent = { Text("${a.cityName}, ${a.streetName}") }, leadingContent = { Icon(imageVector = when (a.iconName) { "home" -> Icons.Default.Home; "work" -> Icons.Default.Work; "apartment" -> Icons.Default.Apartment; else -> Icons.Default.LocationOn }, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, modifier = Modifier.clickable { scope.launch { viewModel.selectWidgetAddress(a); sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) { viewModel.setShowWidgetConfig(false) } } }) }
                    Spacer(modifier = Modifier.height(8.dp)); OutlinedButton(onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { viewModel.setShowWidgetConfig(false) } }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = MaterialTheme.shapes.medium) { Text("Скасувати") }
                }
            }
        }
    }
}

private data class BottomNavItem(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)