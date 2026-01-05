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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.app.Activity
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.*
import android.content.Intent
import com.occaecat.ztoeschedule.presentation.ui.addresses.MyAddressesTab
import com.occaecat.ztoeschedule.presentation.ui.home.HomeTab
import com.occaecat.ztoeschedule.presentation.ui.more.AboutScreen
import com.occaecat.ztoeschedule.presentation.ui.more.DonateScreen
import com.occaecat.ztoeschedule.presentation.ui.more.FaqScreen
import com.occaecat.ztoeschedule.presentation.ui.onboarding.ImprovedOnboardingFlow
import com.occaecat.ztoeschedule.presentation.ui.more.IntegrationsScreen
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { uiState.addressDataList.size })
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var activityLaunched by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() } }
    LaunchedEffect(uiState.infoMessage) { uiState.infoMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearInfoMessage() } }
    val addAddressLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.cancelAddingAddress()
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val name = data.getStringExtra("name") ?: return@rememberLauncherForActivityResult
        val icon = data.getStringExtra("icon") ?: "home"
        val remId = data.getStringExtra("remId") ?: return@rememberLauncherForActivityResult
        val remName = data.getStringExtra("remName") ?: ""
        val cityId = data.getStringExtra("cityId") ?: return@rememberLauncherForActivityResult
        val cityName = data.getStringExtra("cityName") ?: ""
        val streetId = data.getStringExtra("streetId") ?: return@rememberLauncherForActivityResult
        val streetName = data.getStringExtra("streetName") ?: ""
        val addressId = data.getStringExtra("addressId") ?: return@rememberLauncherForActivityResult
        val addressName = data.getStringExtra("addressName") ?: ""
        val cherga = data.getIntExtra("cherga", 0)
        val pidcherga = data.getIntExtra("pidcherga", 0)
        viewModel.addSavedAddress(name, icon, remId, remName, cityId, cityName, streetId, streetName, addressId, addressName, cherga, pidcherga)
    }
    
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

    // Auto-navigate to inspect if deep link received
    LaunchedEffect(uiState.inspectedAddress) {
        if (uiState.inspectedAddress != null && currentRoute != "inspect" && !useWideLayout && !activityLaunched) {
            activityLaunched = true
            val addr = uiState.inspectedAddress!!
            val intent = Intent(context, com.occaecat.ztoeschedule.InspectActivity::class.java).apply {
                putExtra("remId", addr.remId)
                putExtra("remName", addr.remName)
                putExtra("cityId", addr.cityId)
                putExtra("cityName", addr.cityName)
                putExtra("streetId", addr.streetId)
                putExtra("streetName", addr.streetName)
                putExtra("addressId", addr.addressId)
                putExtra("addressName", addr.addressName)
                putExtra("name", addr.name)
                putExtra("iconName", addr.iconName)
                putExtra("priority", addr.priority)
                putExtra("cherga", addr.cherga)
                putExtra("pidcherga", addr.pidcherga)
            }
            context.startActivity(intent)
        }
        
        // Reset flag when inspectedAddress is cleared
        if (uiState.inspectedAddress == null) {
            activityLaunched = false
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

    // Wait for initial load to complete before deciding UI flow
    if (!uiState.isInitialLoadComplete) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Helper class for navigation items
    data class BottomNavItem(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)

    // Show onboarding if: 
    // 1. Onboarding not completed OR no saved address
    // 2. AND not in inspection mode (deep link bypass)
    val needsOnboarding = (!uiState.onboardingCompleted || !uiState.hasSavedSelection) && uiState.inspectedAddress == null
    
    if (needsOnboarding) {
        ImprovedOnboardingFlow(
            uiState.remList, uiState.cityList, uiState.streetList, uiState.filteredHouseNumbers, uiState.houseNumberSearchQuery, uiState.isLoading,
            uiState.selectedCategory,
            { viewModel.loadRemList() }, { viewModel.loadCityList(it) }, { viewModel.loadStreetList(it) }, { viewModel.loadAddressList(it) },
            { viewModel.filterHouseNumbers(it) }, { viewModel.selectCategory(it) }, { viewModel.clearHouseNumberSearch() }, onComplete = { rI, rN, cI, cN, sI, sN, aI, aN, c, p, n, i ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.saveSelection(rI, rN, cI, cN, sI, sN, aI, aN, c, p, n, i); viewModel.completeOnboarding()
            }
        )
        return
    }

    val isAdding = remember(uiState.isAddingNewAddress) { uiState.isAddingNewAddress }
    val shouldShowBars = remember(isAdding) { !isAdding }

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
                    Text("Ваші локації", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    uiState.addressDataList.forEachIndexed { index, data ->
                        NavigationDrawerItem(
                            label = { Column { Text(data.address.name, style = MaterialTheme.typography.titleMedium); Text("${data.address.cityName}, ${data.address.streetName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                            selected = currentRoute == "home" && pagerState.currentPage == index,
                            icon = { Icon(when (data.address.iconName) { "home" -> Icons.Default.Home; "work" -> Icons.Default.Work; "apartment" -> Icons.Default.Apartment; else -> Icons.Default.LocationOn }, null) },
                                                        badge = { 
                                                            if (data.isOffline) {
                                                                Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                                            } else {
                                                                val statusColor = when (data.currentStatus?.status) {
                                                                    ScheduleStatus.Available -> MaterialTheme.colorScheme.primary
                                                                    ScheduleStatus.Probable -> MaterialTheme.colorScheme.tertiary
                                                                    else -> MaterialTheme.colorScheme.error
                                                                }
                                                                Surface(color = statusColor, shape = CircleShape, modifier = Modifier.size(8.dp)) {} 
                                                            }
                                                        },
                                                        onClick = { scope.launch { drawerState.close(); if (currentRoute != "home") navController.navigate("home") { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true }; pagerState.animateScrollToPage(index) } },
                                                        modifier = Modifier.padding(vertical = 2.dp), shape = MaterialTheme.shapes.medium
                                                    )                    }
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
        val context = LocalContext.current
        Row(Modifier.fillMaxSize()) {
            if (useWideLayout) {
                NavigationRail(
                    modifier = Modifier.statusBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Icon(
                            imageVector = Icons.Default.Bolt, 
                            contentDescription = null, 
                            modifier = Modifier.padding(vertical = 24.dp), 
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                                            Text(title, style = MaterialTheme.typography.titleLarge)
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
                floatingActionButton = { 
                    val context = LocalContext.current
                    AnimatedVisibility(visible = !useWideLayout && (currentRoute == "addresses" || (currentRoute == "home" && uiState.addressDataList.isEmpty())), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) { 
                        ExtendedFloatingActionButton(
                            onClick = { 
                                addAddressLauncher.launch(Intent(context, com.occaecat.ztoeschedule.AddressPickerActivity::class.java)) 
                            }, 
                            icon = { Icon(Icons.Default.AddLocation, null) }, 
                            text = { Text("Додати") }, 
                            containerColor = MaterialTheme.colorScheme.primaryContainer, 
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) 
                    } 
                }
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
                            HomeTab(d.address.remName, d.address.cityName, d.address.streetName, d.address.addressName, d.address.cherga, d.address.pidcherga, d.currentStatus, d.scheduleList, d.groupedSchedule, { viewModel.refreshAllSchedules() }, Modifier.fillMaxSize(), padding, d.lastUpdateTime, d.isOffline, uiState.isLoading, d.address.streetId, d.address.addressId) 
                        } 
                    }
                    composable("notifications") { NotificationsTab(uiState.infoMessages, uiState.formattedMessage, Modifier.fillMaxSize(), padding, uiState.isLoading) }
                    composable("addresses") { 
                        val context = LocalContext.current
                        MyAddressesTab(
                            addresses = uiState.savedAddresses,
                            addressStatuses = uiState.addressStatuses,
                            isAddingNew = uiState.isAddingNewAddress,
                            remList = uiState.remList,
                            cityList = uiState.cityList,
                            streetList = uiState.streetList,
                            houseNumbers = uiState.filteredHouseNumbers,
                            searchQuery = uiState.houseNumberSearchQuery,
                            isLoading = uiState.isLoading,
                            useWideLayout = useWideLayout,
                            inspectedScheduleList = uiState.inspectedScheduleList,
                            inspectedGroupedSchedule = uiState.inspectedGroupedSchedule,
                            isInspectingLoading = uiState.isInspectingLoading,
                            onStartAdding = { context.startActivity(Intent(context, com.occaecat.ztoeschedule.AddressPickerActivity::class.java)) },
                            onCancelAdding = { viewModel.cancelAddingAddress() },
                            onLoadRem = { viewModel.loadRemList() },
                            onLoadCity = { viewModel.loadCityList(it) },
                            onLoadStreet = { viewModel.loadStreetList(it) },
                            onLoadAddress = { viewModel.loadAddressList(it) },
                            onSearchQueryChange = { viewModel.filterHouseNumbers(it) },
                            onClearSearch = { viewModel.clearHouseNumberSearch() },
                            onSaveAddress = { n, i, rI, rN, cI, cN, sI, sN, aI, aN, c, p -> viewModel.addSavedAddress(n, i, rI, rN, cI, cN, sI, sN, aI, aN, c, p) },
                            onDeleteAddress = { viewModel.deleteSavedAddress(it) },
                            onUpdateOrder = { viewModel.updateAddressesOrder(it) },
                            onRefreshAddress = { c, p -> viewModel.loadScheduleWithMessages(c, p) },
                            onInspectAddress = { savedAddr ->
                                viewModel.startInspectingAddress(savedAddr)
                                if (!useWideLayout) {
                                    activityLaunched = true
                                    val intent = Intent(context, com.occaecat.ztoeschedule.InspectActivity::class.java).apply {
                                        putExtra("remId", savedAddr.remId)
                                        putExtra("remName", savedAddr.remName)
                                        putExtra("cityId", savedAddr.cityId)
                                        putExtra("cityName", savedAddr.cityName)
                                        putExtra("streetId", savedAddr.streetId)
                                        putExtra("streetName", savedAddr.streetName)
                                        putExtra("addressId", savedAddr.addressId)
                                        putExtra("addressName", savedAddr.addressName)
                                        putExtra("name", savedAddr.name)
                                        putExtra("iconName", savedAddr.iconName)
                                        putExtra("priority", savedAddr.priority)
                                        putExtra("cherga", savedAddr.cherga)
                                        putExtra("pidcherga", savedAddr.pidcherga)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = padding
                        ) 
                    }
                    composable("more") { 
                        val context = LocalContext.current
                        MoreTab(
                            scheduleList = uiState.addressDataList.getOrNull(pagerState.currentPage)?.scheduleList ?: emptyList(),
                            currentAddressRemName = uiState.addressDataList.getOrNull(pagerState.currentPage)?.address?.remName ?: "",
                            currentAddressCityName = uiState.addressDataList.getOrNull(pagerState.currentPage)?.address?.cityName ?: "",
                            currentAddressStreetName = uiState.addressDataList.getOrNull(pagerState.currentPage)?.address?.streetName ?: "",
                            currentAddressHouseName = uiState.addressDataList.getOrNull(pagerState.currentPage)?.address?.addressName ?: "",
                            onNavigateToSettings = { context.startActivity(Intent(context, com.occaecat.ztoeschedule.SettingsActivity::class.java)) }, 
                            onNavigateToIntegrations = { navController.navigate("integrations") },
                            onNavigateToDonate = { navController.navigate("donate") }, 
                            onNavigateToAbout = { navController.navigate("about") }, 
                            onNavigateToFaq = { navController.navigate("faq") },
                            onAddDemoLocation = { viewModel.addDemoLocation() }, 
                            displayMode = uiState.displayMode, 
                            modifier = Modifier.fillMaxSize(), 
                            contentPadding = padding
                        ) 
                    }
                    composable("donate") { DonateScreen { navController.popBackStack() } }
                    composable("about") { AboutScreen { navController.popBackStack() } }
                    composable("faq") { FaqScreen { navController.popBackStack() } }
                    composable("integrations") { IntegrationsScreen { navController.popBackStack() } }
                }
            }
        }

        if (uiState.showWidgetConfig) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setShowWidgetConfig(false) }, sheetState = sheetState, modifier = Modifier.fillMaxHeight().semantics { paneTitle = "Вибір адреси для віджета" }, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    Text(text = "Оберіть адресу для віджета", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                    uiState.savedAddresses.forEach { a -> 
                        ListItem(
                            headlineContent = { Text(a.name, style = MaterialTheme.typography.titleMedium) }, 
                            supportingContent = { Text("${a.cityName}, ${a.streetName}") }, 
                            leadingContent = { Icon(imageVector = when (a.iconName) { "home" -> Icons.Default.Home; "work" -> Icons.Default.Work; "apartment" -> Icons.Default.Apartment; else -> Icons.Default.LocationOn }, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, 
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple()
                            ) { 
                                scope.launch { viewModel.selectWidgetAddress(a); sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) { viewModel.setShowWidgetConfig(false) } } 
                            }
                        ) 
                    }
                    Spacer(modifier = Modifier.height(8.dp)); OutlinedButton(onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { viewModel.setShowWidgetConfig(false) } }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = MaterialTheme.shapes.medium) { Text("Скасувати") }
                }
            }
        }
    }
}