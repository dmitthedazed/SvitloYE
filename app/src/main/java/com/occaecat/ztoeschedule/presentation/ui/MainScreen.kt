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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.*
import android.content.Intent
import com.occaecat.ztoeschedule.presentation.ui.addresses.AddressPickerScreen
import com.occaecat.ztoeschedule.presentation.ui.addresses.AddressPickerDialog
import com.occaecat.ztoeschedule.presentation.ui.addresses.AddressPickerResult
import com.occaecat.ztoeschedule.presentation.ui.adaptive.MainNavigationChrome
import com.occaecat.ztoeschedule.presentation.ui.adaptive.mainScaffoldLayoutFor

import com.occaecat.ztoeschedule.presentation.ui.home.HomeTab
import com.occaecat.ztoeschedule.presentation.ui.addresses.MyAddressesTab
import com.occaecat.ztoeschedule.presentation.ui.more.IntegrationsScreen
import com.occaecat.ztoeschedule.presentation.ui.more.MoreTab
import com.occaecat.ztoeschedule.presentation.ui.notifications.NotificationsTab
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel
import kotlinx.coroutines.launch

import com.occaecat.ztoeschedule.ui.theme.robotoFlexTopBar
import com.occaecat.ztoeschedule.ui.theme.LocalGlassBackdrop
import com.occaecat.ztoeschedule.presentation.ui.glass.GlassNavItem
import com.occaecat.ztoeschedule.presentation.ui.glass.LiquidGlassBackground
import com.occaecat.ztoeschedule.presentation.ui.glass.LiquidGlassNavBar
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    viewModel: EnergyScheduleViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reduceMotion = false
    val mainScaffoldLayout = remember(windowSizeClass.widthSizeClass) {
        mainScaffoldLayoutFor(windowSizeClass.widthSizeClass)
    }
    val showNavRail = mainScaffoldLayout.navigationChrome == MainNavigationChrome.NavigationRail
    val useWideLayout = mainScaffoldLayout.useWideLayout
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { uiState.addressDataList.size })
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var activityLaunched by rememberSaveable { mutableStateOf(false) }
    var showAddAddressSheet by remember { mutableStateOf(false) }
    
    // Scroll behavior for TopAppBar
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val motionScheme = MaterialTheme.motionScheme

    val offlineUpdated = remember(uiState.addressDataList) {
        uiState.addressDataList.firstOrNull { it.lastUpdateTime.isNotEmpty() }?.lastUpdateTime
    }
    val offlineMessage = stringResource(R.string.error_no_connection)
    val navHomeLabel = stringResource(R.string.nav_home)
    val navNotificationsLabel = stringResource(R.string.nav_notifications)
    val navAddressesLabel = stringResource(R.string.nav_addresses)
    val navMoreLabel = stringResource(R.string.nav_more)
    val settingsLabel = stringResource(R.string.more_settings)
    val appNameLabel = stringResource(R.string.app_name)

    fun showOfflineSnackbar() {
        scope.launch { snackbarHostState.showSnackbar(offlineMessage) }
    }

    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() } }
    LaunchedEffect(uiState.infoMessage) { uiState.infoMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearInfoMessage() } }
    
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
                putExtra("houseName", addr.addressName.ifBlank { addr.name })
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
                currentRoute == "home" -> if (uiState.addressDataList.isNotEmpty() && pagerState.currentPage in uiState.addressDataList.indices) uiState.addressDataList[pagerState.currentPage].address.name else navHomeLabel
                currentRoute == "notifications" -> navNotificationsLabel
                currentRoute == "addresses" -> navAddressesLabel
                currentRoute == "more" -> navMoreLabel
                currentRoute == "settings" -> settingsLabel
                currentRoute == "inspect" -> uiState.inspectedAddress?.name ?: "Inspect"
                else -> appNameLabel
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

    val isAdding = remember(uiState.isAddingNewAddress) { uiState.isAddingNewAddress }
    val shouldShowBars = remember(isAdding) { !isAdding }

    val navItems = listOf(
        BottomNavItem("home", stringResource(R.string.nav_home), Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem("notifications", stringResource(R.string.nav_notifications), Icons.Filled.Notifications, Icons.Outlined.Notifications),
        BottomNavItem("addresses", stringResource(R.string.nav_addresses), Icons.Filled.LocationOn, Icons.Outlined.LocationOn),
        BottomNavItem("more", stringResource(R.string.nav_more), Icons.Filled.Menu, Icons.Outlined.Menu)
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val context = LocalContext.current
        val liquidGlass = com.occaecat.ztoeschedule.ui.theme.LocalLiquidGlass.current && !useWideLayout
        val bgBackdrop = if (liquidGlass) rememberLayerBackdrop() else null

        // Animated gradient background — the backdrop source for all glass elements
        if (bgBackdrop != null) {
            LiquidGlassBackground(modifier = Modifier.layerBackdrop(bgBackdrop))
        }

        androidx.compose.runtime.CompositionLocalProvider(LocalGlassBackdrop provides bgBackdrop) {
        Box(modifier = Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxSize()
        ) {
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
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    if (shouldShowBars) {
                        Column {
                            Box(contentAlignment = Alignment.BottomCenter) {
                                TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                scrollBehavior = scrollBehavior,
                                title = { 
                                    AnimatedContent(
                                        targetState = currentTitle, 
                                        transitionSpec = {
                                            slideInVertically(
                                                animationSpec = motionScheme.defaultSpatialSpec(),
                                                initialOffsetY = { (-it * 1.25).toInt() }
                                            ).togetherWith(
                                                slideOutVertically(
                                                    animationSpec = motionScheme.defaultSpatialSpec(),
                                                    targetOffsetY = { (it * 1.25).toInt() }
                                                )
                                            )
                                        },
                                        label = "title_animation",
                                        modifier = Modifier.fillMaxWidth(0.9f),
                                        contentAlignment = Alignment.CenterStart
                                    ) { title -> 
                                        Column(horizontalAlignment = Alignment.Start) {
                                            Text(
                                                text = title, 
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontSize = 32.sp,
                                                    lineHeight = 32.sp,
                                                    fontFamily = robotoFlexTopBar
                                                ),
                                                textAlign = TextAlign.Start,
                                                maxLines = 1
                                            )
                                            if (currentRoute == "home" && uiState.addressDataList.size > 1) {
                                                Text(
                                                    stringResource(R.string.home_page_indicator, pagerState.currentPage + 1, uiState.addressDataList.size),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    } 
                                },
                                actions = { 
                                    Box { 
                                        IconButton(onClick = { showMenu = true }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) { 
                                            Icon(Icons.Default.MoreVert, stringResource(R.string.home_menu)) 
                                        }
                                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) { 
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.home_refresh_all)) },
                                                leadingIcon = { Icon(Icons.Default.Refresh, null) },
                                                enabled = uiState.isConnected,
                                                onClick = {
                                                    showMenu = false
                                                    if (!uiState.isConnected) {
                                                        showOfflineSnackbar()
                                                    } else {
                                                        viewModel.refreshAllSchedules()
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(text = { Text(stringResource(R.string.home_configure_widget)) }, leadingIcon = { Icon(Icons.Default.Widgets, null) }, onClick = { showMenu = false; viewModel.setShowWidgetConfig(true) })
                                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                            DropdownMenuItem(text = { Text(stringResource(R.string.home_help_faq)) }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null) }, onClick = { showMenu = false; navController.navigate("faq") }) 
                                        } 
                                    } 
                                }
                            )
                            if (uiState.isLoading) {
                                LinearWavyProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .offset(y = 2.dp),
                                    color = MaterialTheme.colorScheme.primary, 
                                    trackColor = Color.Transparent,
                                    wavelength = 20.dp
                                )
                            }
                            }
                            if (!uiState.isConnected) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.SignalWifiOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val offlineText = if (!offlineUpdated.isNullOrBlank()) {
                                            stringResource(R.string.error_offline_banner) + " - " +
                                                stringResource(R.string.home_last_updated, offlineUpdated)
                                        } else {
                                            stringResource(R.string.error_offline_banner)
                                        }
                                        Text(
                                            text = offlineText,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(
                                            onClick = { viewModel.refreshAllSchedules(allowOffline = true) }
                                        ) {
                                            Text(stringResource(R.string.error_retry_now))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    AnimatedVisibility(
                        visible = !useWideLayout && shouldShowBars && bgBackdrop == null,
                        enter = if (reduceMotion) EnterTransition.None else slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = if (reduceMotion) ExitTransition.None else slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        // Centered Floating Toolbar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            HorizontalFloatingToolbar(
                                expanded = true,
                                modifier = Modifier.zIndex(1f)
                            ) {
                                navItems.forEach { item ->
                                    val isSelected = currentRoute == item.route
                                    
                                    ToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = { 
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (!isSelected) {
                                                navController.navigate(item.route) { 
                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true 
                                                }
                                            }
                                        },
                                        shapes = ToggleButtonDefaults.shapes(CircleShape),
                                        modifier = Modifier.height(48.dp) // Further reduced height
                                    ) {
                                        val icon = if (item.route == "notifications" && uiState.infoMessages.isNotEmpty()) {
                                            if (isSelected) item.selectedIcon else item.unselectedIcon
                                        } else {
                                            if (isSelected) item.selectedIcon else item.unselectedIcon
                                        }
                                        
                                        Icon(
                                            imageVector = icon, 
                                            contentDescription = item.label,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        AnimatedVisibility(visible = isSelected) {
                                            Text(
                                                text = item.label, 
                                                modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                floatingActionButton = { 
                    val context = LocalContext.current
                    AnimatedVisibility(
                        visible = !useWideLayout && (
                            (currentRoute == "addresses" && uiState.savedAddresses.isNotEmpty()) ||
                                (currentRoute == "home" && uiState.addressDataList.isEmpty() && !uiState.savedAddresses.isEmpty())
                        ),
                        enter = if (reduceMotion) EnterTransition.None else scaleIn() + fadeIn(),
                        exit = if (reduceMotion) ExitTransition.None else scaleOut() + fadeOut()
                    ) { 
                        ExtendedFloatingActionButton(
                            modifier = Modifier.navigationBarsPadding(),
                            onClick = { 
                                if (!uiState.isConnected) {
                                    showOfflineSnackbar()
                                } else {
                                    showAddAddressSheet = true
                                }
                            }, 
                            icon = { Icon(Icons.Default.AddLocation, null) }, 
                            text = { Text(stringResource(R.string.home_add)) }, 
                            containerColor = MaterialTheme.colorScheme.primaryContainer, 
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) 
                    } 
                }
            ) { padding ->
                val br = listOf("home", "notifications", "addresses", "more"); fun gRI(r: String?): Int = br.indexOf(r)
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController, startDestination = "home", modifier = Modifier.fillMaxSize(),
                            enterTransition = { if (reduceMotion) EnterTransition.None else run { val f = gRI(initialState.destination.route); val t = gRI(targetState.destination.route); if (f != -1 && t != -1) { if (t > f) slideInHorizontally { it } + fadeIn() else slideInHorizontally { -it } + fadeIn() } else slideInHorizontally { it } + fadeIn() } },
                            exitTransition = { if (reduceMotion) ExitTransition.None else run { val f = gRI(initialState.destination.route); val t = gRI(targetState.destination.route); if (f != -1 && t != -1) { if (t > f) slideOutHorizontally { -it } + fadeOut() else slideOutHorizontally { it } + fadeOut() } else slideOutHorizontally { -it } + fadeOut() } },
                            popEnterTransition = { if (reduceMotion) EnterTransition.None else run { val f = gRI(initialState.destination.route); val t = gRI(targetState.destination.route); if (f != -1 && t != -1) { if (t > f) slideInHorizontally { it } + fadeIn() else slideInHorizontally { -it } + fadeIn() } else slideInHorizontally { it } + fadeIn() } },
                            popExitTransition = { if (reduceMotion) ExitTransition.None else run { val f = gRI(initialState.destination.route); val t = gRI(targetState.destination.route); if (f != -1 && t != -1) { val s = if (t > f) slideOutHorizontally { -it } else slideOutHorizontally { it }; s + fadeOut() + scaleOut(targetScale = 0.9f) } else slideOutHorizontally { it } + fadeOut() + scaleOut(targetScale = 0.9f) } }
                        ) {
                            composable("home") { 
                                val ctx = LocalContext.current
                                LaunchedEffect(pagerState.currentPage, uiState.addressDataList) { 
                                    if (uiState.addressDataList.isNotEmpty()) { 
                                        val id = uiState.addressDataList[pagerState.currentPage].address.id
                                        androidx.core.content.pm.ShortcutManagerCompat.reportShortcutUsed(ctx, "address_$id") 
                                    } 
                                }
                                
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (!uiState.isInitialLoadComplete) {
                                        Box(
                                            modifier = Modifier
                                                .padding(padding)
                                                .fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    } else if (uiState.addressDataList.isEmpty()) {
                                        EmptyHomePlaceholder(
                                            modifier = Modifier
                                                .padding(padding)
                                                .fillMaxSize(),
                                            onAddAddress = {
                                                if (!uiState.isConnected) {
                                                    showOfflineSnackbar()
                                                } else {
                                                    showAddAddressSheet = true
                                                }
                                            }
                                        )
                                    } else {
                                        HorizontalPager(
                                            state = pagerState, 
                                            userScrollEnabled = !reduceMotion,
                                            modifier = Modifier.fillMaxSize().focusable().onKeyEvent { 
                                                if (it.type == KeyEventType.KeyUp) { 
                                                    when (it.key) { 
                                                        Key.DirectionRight -> if (pagerState.currentPage < pagerState.pageCount - 1) { scope.launch { if (reduceMotion) pagerState.scrollToPage(pagerState.currentPage + 1) else pagerState.animateScrollToPage(pagerState.currentPage + 1) }; true } else false
                                                        Key.DirectionLeft -> if (pagerState.currentPage > 0) { scope.launch { if (reduceMotion) pagerState.scrollToPage(pagerState.currentPage - 1) else pagerState.animateScrollToPage(pagerState.currentPage - 1) }; true } else false
                                                        else -> false 
                                                    } 
                                                } else false 
                                            }, 
                                            beyondViewportPageCount = 1
                                        ) { page -> 
                                            val d = uiState.addressDataList[page]
                                            HomeTab(
                                                remId = d.address.remId,
                                                cityId = d.address.cityId,
                                                remName = d.address.remName,
                                                cityName = d.address.cityName,
                                                streetName = d.address.streetName,
                                                addressName = d.address.addressName.ifBlank { d.address.name },
                                                cherga = d.address.cherga,
                                                pidcherga = d.address.pidcherga,
                                                currentStatus = d.currentStatus,
                                                schedules = d.scheduleList,
                                                groupedSchedule = d.groupedSchedule,
                                                onRefresh = {
                                                    if (!uiState.isConnected) {
                                                        showOfflineSnackbar()
                                                    } else {
                                                        viewModel.refreshAllSchedules()
                                                    }
                                                },
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = padding,
                                                lastUpdateTime = d.lastUpdateTime,
                                                isOffline = d.isOffline,
                                                isLoading = uiState.isLoading,
                                                streetId = d.address.streetId,
                                                addressId = d.address.addressId
                                            )
                                        }
                                    }

                                }
                            }
                            composable("notifications") { NotificationsTab(uiState.infoMessages, uiState.formattedMessage, uiState.lastUpdateTime, Modifier.fillMaxSize(), padding, uiState.isLoading) }
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
                                    onStartAdding = { 
                                        if (!uiState.isConnected) {
                                            showOfflineSnackbar()
                                        } else {
                                            showAddAddressSheet = true
                                        }
                                    },
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
                                    onRefreshAddress = { c, p ->
                                        if (!uiState.isConnected) {
                                            showOfflineSnackbar()
                                        } else {
                                            viewModel.loadScheduleWithMessages(c, p)
                                        }
                                    },
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
                                val currentData = uiState.addressDataList.getOrNull(pagerState.currentPage)
                                MoreTab(
                                    scheduleList = currentData?.scheduleList ?: emptyList(), 
                                    currentAddressRemName = currentData?.address?.remName ?: "", 
                                    currentAddressCityName = currentData?.address?.cityName ?: "", 
                                    currentAddressStreetName = currentData?.address?.streetName ?: "", 
                                    currentAddressHouseName = currentData?.address?.addressName ?: "", 
                                    onNavigateToSettings = { context.startActivity(Intent(context, com.occaecat.ztoeschedule.SettingsActivity::class.java)) }, 
                                    onNavigateToIntegrations = { navController.navigate("integrations") },
                                    onNavigateToAbout = { context.startActivity(Intent(context, com.occaecat.ztoeschedule.InfoActivity::class.java).apply { putExtra("type", "about") }) }, 
                                    onNavigateToFaq = { context.startActivity(Intent(context, com.occaecat.ztoeschedule.InfoActivity::class.java).apply { putExtra("type", "faq") }) },
                                    onNavigateToFeedback = { context.startActivity(Intent(context, com.occaecat.ztoeschedule.InfoActivity::class.java).apply { putExtra("type", "feedback") }) },
                                    onAddDemoLocation = { viewModel.addDemoLocation() },
                                    contentPadding = padding
                                ) 
                            }
                    composable("integrations") { IntegrationsScreen(onBack = { navController.popBackStack() }) }
                }
            }
        }
            }
        }

        // Liquid Glass bottom nav overlay — sibling to Row inside inner Box, blurs through gradient background
        if (bgBackdrop != null && shouldShowBars && !useWideLayout) {
            AnimatedVisibility(
                visible = true,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = if (reduceMotion) EnterTransition.None else slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = if (reduceMotion) ExitTransition.None else slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                LiquidGlassNavBar(
                    items = navItems.map { GlassNavItem(it.route, it.label, it.selectedIcon, it.unselectedIcon) },
                    currentRoute = currentRoute,
                    backdrop = bgBackdrop,
                    onNavigate = { route ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
        } // close inner Box
        } // close CompositionLocalProvider

        val widgetPaneTitle = stringResource(R.string.widget_select_pane_title)
        if (uiState.showWidgetConfig) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setShowWidgetConfig(false) }, sheetState = sheetState, modifier = Modifier.fillMaxHeight().semantics { paneTitle = widgetPaneTitle }, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    Text(text = stringResource(R.string.widget_select_address_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
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
                    Spacer(modifier = Modifier.height(8.dp)); OutlinedButton(onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { viewModel.setShowWidgetConfig(false) } }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = MaterialTheme.shapes.medium) { Text(stringResource(R.string.action_cancel)) }
                }
            }
        }

        if (showAddAddressSheet) {
            AddressPickerDialog(
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
                onDismiss = { showAddAddressSheet = false },
                onComplete = { result ->
                    viewModel.addSavedAddress(
                        name = result.displayName,
                        icon = result.iconName,
                        rI = result.remId,
                        rN = result.remName,
                        cI = result.cityId,
                        cN = result.cityName,
                        sI = result.streetId,
                        sN = result.streetName,
                        aI = result.addressId,
                        aN = result.addressName,
                        c = result.cherga,
                        p = result.pidcherga
                    )
                    showAddAddressSheet = false
                }
            )
        }
    }
}

@Composable
private fun EmptyHomePlaceholder(
    modifier: Modifier = Modifier,
    onAddAddress: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(220.dp)
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    )
                    if (index < 2) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.home_no_address_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_no_address_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddAddress) {
            Text(stringResource(R.string.home_add))
        }
    }
}
