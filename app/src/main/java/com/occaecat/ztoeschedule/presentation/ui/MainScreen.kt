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
import androidx.compose.ui.text.style.TextOverflow
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
import com.kyant.backdrop.backdrops.layerBackdrop
import com.occaecat.ztoeschedule.presentation.ui.glass.dock.FloatingBottomBar
import com.occaecat.ztoeschedule.presentation.ui.glass.glassCapsule
import com.occaecat.ztoeschedule.presentation.ui.glass.dock.FloatingBottomBarItem
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    viewModel: EnergyScheduleViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        viewModel.reloadAddressesIfChanged()
        onPauseOrDispose { }
    }
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
    var activityLaunched by rememberSaveable { mutableStateOf(false) }
    var showAddAddressSheet by remember { mutableStateOf(false) }
    
    // Scroll behavior for TopAppBar
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    // One list state per address page so pages can be kept at the same scroll position
    val homeListStates = remember { mutableMapOf<String, androidx.compose.foundation.lazy.LazyListState>() }
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
    val bottomNavItemShape = RoundedCornerShape(percent = 50)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val context = LocalContext.current
        // Liquid Glass is a mode for the floating chrome only (dock, FAB); screens keep their
        // Material surfaces. Lens and highlight shaders need RuntimeShader: Android 13+
        val liquidGlass = com.occaecat.ztoeschedule.ui.theme.LocalLiquidGlass.current && !useWideLayout &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        // Screen content recorded once and refracted by every glass element and the edge blurs
        val pageColor = MaterialTheme.colorScheme.surface
        val contentBackdrop = rememberLayerBackdrop {
            drawRect(pageColor)
            drawContent()
        }

        Box(modifier = Modifier.fillMaxSize()) {
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
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        if (liquidGlass) com.occaecat.ztoeschedule.presentation.ui.glass.GlassSnackbar(contentBackdrop, data, Modifier.padding(bottom = 84.dp))
                        else Snackbar(data)
                    }
                },
                topBar = {
                    if (shouldShowBars) {
                        Column {
                            Box(contentAlignment = Alignment.BottomCenter) {
                                TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    // Transparent: the page colour and blur come from EdgeBlur underneath
                                    containerColor = Color.Transparent,
                                    scrolledContainerColor = Color.Transparent
                                ),
                                scrollBehavior = scrollBehavior,
                                title = { 
                                    AnimatedContent(
                                        targetState = currentTitle, 
                                        transitionSpec = {
                                            (slideInVertically(motionScheme.defaultSpatialSpec()) { -it / 2 } + fadeIn(motionScheme.defaultEffectsSpec()))
                                                .togetherWith(slideOutVertically(motionScheme.defaultSpatialSpec()) { it / 2 } + fadeOut(motionScheme.fastEffectsSpec()))
                                        },
                                        label = "title_animation",
                                        contentAlignment = Alignment.CenterStart
                                    ) { title -> 
                                        Text(
                                            text = title, 
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } 
                                },
                                actions = { 
                                    if (currentRoute == "home" && uiState.addressDataList.size > 1) {
                                        com.occaecat.ztoeschedule.presentation.ui.components.PageIndicatorDots(
                                            current = pagerState.currentPage,
                                            total = uiState.addressDataList.size,
                                            description = stringResource(R.string.home_page_indicator, pagerState.currentPage + 1, uiState.addressDataList.size),
                                            fillCompleted = false,
                                            modifier = Modifier
                                                .padding(end = 16.dp)
                                                .then(
                                                    if (liquidGlass) Modifier
                                                        .glassCapsule(contentBackdrop)
                                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                                    else Modifier
                                                )
                                        )
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
                                    wavelength = 20.dp,
                                    // Default speed (one wavelength per second) feels frantic
                                    waveSpeed = 5.dp
                                )
                            }
                            }
                            if (!uiState.isConnected) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .then(if (liquidGlass) Modifier.glassCapsule(contentBackdrop, RoundedCornerShape(20.dp)) else Modifier),
                                    color = if (liquidGlass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.SignalWifiOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        val offlineText = if (!offlineUpdated.isNullOrBlank()) {
                                            stringResource(R.string.error_offline_banner) + " · " +
                                                stringResource(R.string.home_last_updated, offlineUpdated)
                                        } else {
                                            stringResource(R.string.error_offline_banner)
                                        }
                                        Text(
                                            text = offlineText,
                                            style = MaterialTheme.typography.bodyMedium,
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
                        visible = !useWideLayout && shouldShowBars && !liquidGlass,
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
                                        shapes = ToggleButtonDefaults.shapes(bottomNavItemShape),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .clip(bottomNavItemShape)
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
                        val onAddClick = {
                            if (!uiState.isConnected) {
                                showOfflineSnackbar()
                            } else {
                                showAddAddressSheet = true
                            }
                        }
                        if (liquidGlass) {
                            // The glass dock is an overlay, so lift the FAB above it
                            com.occaecat.ztoeschedule.presentation.ui.glass.GlassExtendedFab(
                                backdrop = contentBackdrop,
                                text = stringResource(R.string.home_add),
                                icon = Icons.Default.AddLocation,
                                onClick = onAddClick,
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .padding(bottom = 84.dp)
                            )
                        } else {
                            ExtendedFloatingActionButton(
                                modifier = Modifier.navigationBarsPadding(),
                                onClick = onAddClick,
                                icon = { Icon(Icons.Default.AddLocation, null) },
                                text = { Text(stringResource(R.string.home_add)) },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } 
                }
            ) { padding ->
                val br = listOf("home", "notifications", "addresses", "more"); fun gRI(r: String?): Int = br.indexOf(r)
                // The top bar's collapse state is shared by every tab; a tab that can't scroll
                // would otherwise be stuck without a top bar
                LaunchedEffect(currentRoute) {
                    scrollBehavior.state.heightOffset = 0f
                    scrollBehavior.state.contentOffset = 0f
                }
                // Keep neighbouring address pages at the same scroll position as the current one
                LaunchedEffect(pagerState.currentPage, uiState.addressDataList.map { it.address.id }) {
                    val ids = uiState.addressDataList.map { it.address.id }
                    val current = ids.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
                    val currentState = homeListStates.getOrPut(current) { androidx.compose.foundation.lazy.LazyListState() }
                    snapshotFlow { currentState.firstVisibleItemIndex to currentState.firstVisibleItemScrollOffset }
                        .collect { (index, offset) ->
                            ids.forEach { id ->
                                if (id != current) {
                                    homeListStates.getOrPut(id) { androidx.compose.foundation.lazy.LazyListState() }
                                        .requestScrollToItem(index, offset)
                                }
                            }
                        }
                }
                // With the liquid-glass bar the navigation is an overlay, not part of the Scaffold padding
                val tabPadding = if (liquidGlass) {
                    PaddingValues(
                        start = padding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        top = padding.calculateTopPadding(),
                        end = padding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        bottom = padding.calculateBottomPadding() + 96.dp
                    )
                } else padding
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize().layerBackdrop(contentBackdrop)) {
                        NavHost(
                            navController = navController, startDestination = "home", modifier = Modifier.fillMaxSize(),
                            // Tabs use Material "fade through"; other destinations a short shared-axis slide
                            enterTransition = {
                                when {
                                    reduceMotion -> EnterTransition.None
                                    gRI(initialState.destination.route) != -1 && gRI(targetState.destination.route) != -1 -> fadeThroughIn()
                                    else -> sharedAxisIn(forward = true)
                                }
                            },
                            exitTransition = {
                                when {
                                    reduceMotion -> ExitTransition.None
                                    gRI(initialState.destination.route) != -1 && gRI(targetState.destination.route) != -1 -> fadeThroughOut()
                                    else -> sharedAxisOut(forward = true)
                                }
                            },
                            popEnterTransition = {
                                when {
                                    reduceMotion -> EnterTransition.None
                                    gRI(initialState.destination.route) != -1 && gRI(targetState.destination.route) != -1 -> fadeThroughIn()
                                    else -> sharedAxisIn(forward = false)
                                }
                            },
                            popExitTransition = {
                                when {
                                    reduceMotion -> ExitTransition.None
                                    gRI(initialState.destination.route) != -1 && gRI(targetState.destination.route) != -1 -> fadeThroughOut()
                                    else -> sharedAxisOut(forward = false)
                                }
                            }
                        ) {
                            composable("home") { 
                                val ctx = LocalContext.current
                                LaunchedEffect(pagerState.currentPage, uiState.addressDataList) { 
                                    // The list can change size (address added/removed) before the pager catches up
                                    uiState.addressDataList.getOrNull(pagerState.currentPage)?.let { current ->
                                        val id = current.address.id
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
                                            contentPadding = tabPadding,
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
                                            // Neighbours are composed lazily as the user swipes, so entering
                                            // Home builds one page instead of three
                                            beyondViewportPageCount = 0
                                        ) { page -> 
                                            val d = uiState.addressDataList.getOrNull(page) ?: return@HorizontalPager
                                            HomeTab(
                                                remId = d.address.remId,
                                                cityId = d.address.cityId,
                                                remName = d.address.remName,
                                                cityName = d.address.cityName,
                                                streetName = d.address.streetName,
                                                addressName = d.address.addressName.ifBlank { d.address.name },
                                                cherga = d.address.cherga,
                                                pidcherga = d.address.pidcherga,
                                                iconName = d.address.iconName,
                                                isPrimary = page == 0,
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
                                                contentPadding = tabPadding,
                                                listState = homeListStates.getOrPut(d.address.id) { androidx.compose.foundation.lazy.LazyListState() },
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
                            composable("notifications") { NotificationsTab(uiState.infoMessages, uiState.formattedMessage, uiState.lastUpdateTime, Modifier.fillMaxSize(), tabPadding, uiState.isLoading) }
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
                                    contentPadding = tabPadding
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
                                    contentPadding = tabPadding
                                ) 
                            }
                    composable("integrations") { IntegrationsScreen(onBack = { navController.popBackStack() }) }
                }
            }
            if (!useWideLayout && shouldShowBars && !liquidGlass) {
                EdgeBlur(
                    backdrop = contentBackdrop,
                    fromTop = false,
                    height = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            if (shouldShowBars) {
                // Covers the status bar and the top bar, shrinking as the bar scrolls away
                val topBarVisibleHeight = with(androidx.compose.ui.platform.LocalDensity.current) {
                    (TopAppBarDefaults.TopAppBarExpandedHeight.toPx() + scrollBehavior.state.heightOffset).coerceAtLeast(0f).toDp()
                }
                EdgeBlur(
                    backdrop = contentBackdrop,
                    fromTop = true,
                    height = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + topBarVisibleHeight + 24.dp,
                    tintAlpha = 0.8f,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
            }
        }

        // Liquid Glass bottom nav overlay — sibling to Row inside inner Box, blurs through gradient background
        if (liquidGlass && shouldShowBars) {
            AnimatedVisibility(
                visible = true,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = if (reduceMotion) EnterTransition.None else slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = if (reduceMotion) ExitTransition.None else slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                val selectedTab = navItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingBottomBar(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
                        selectedIndex = selectedTab,
                        onSelected = { index ->
                            val route = navItems[index].route
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        tabsCount = navItems.size,
                        isBlurEnabled = true,
                        blurBackdrop = contentBackdrop
                    ) { activateTab ->
                        navItems.forEachIndexed { index, item ->
                            FloatingBottomBarItem(
                                selected = index == selectedTab,
                                onClick = { activateTab(index) },
                            ) {
                                val tabColor = LocalContentColor.current
                                val icon: @Composable () -> Unit = {
                                    Icon(
                                        imageVector = if (index == selectedTab) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                        tint = tabColor
                                    )
                                }
                                if (item.route == "notifications" && uiState.infoMessages.isNotEmpty()) {
                                    BadgedBox(badge = { Badge { Text("${uiState.infoMessages.size}") } }) { icon() }
                                } else {
                                    icon()
                                }
                                Text(
                                    text = item.label,
                                    color = tabColor,
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
        } // close inner Box

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyHomePlaceholder(
    contentPadding: PaddingValues,
    onAddAddress: () -> Unit
) {
    com.occaecat.ztoeschedule.presentation.ui.components.EmptyStatePage(
        icon = Icons.Default.Bolt,
        shape = MaterialShapes.Sunny.toShape(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        rotateHero = true,
        title = stringResource(R.string.home_no_address_title),
        body = stringResource(R.string.home_no_address_desc),
        contentPadding = contentPadding,
        actionText = "Додати адресу",
        actionIcon = Icons.Default.AddLocationAlt,
        onAction = onAddAddress,
        chips = listOf(
            com.occaecat.ztoeschedule.presentation.ui.components.EmptyStateChip(Icons.Default.Schedule, "Графік"),
            com.occaecat.ztoeschedule.presentation.ui.components.EmptyStateChip(Icons.Default.NotificationsActive, "Сповіщення"),
            com.occaecat.ztoeschedule.presentation.ui.components.EmptyStateChip(Icons.Default.Widgets, "Віджети")
        )
    )
}

// Material motion: fade through for switching top-level tabs
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

// Fade only: scaling a whole tab forces every frame of the pager to be re-rasterised, which
// is what made returning to Home stutter
private fun fadeThroughIn(): EnterTransition =
    fadeIn(tween(durationMillis = 210, delayMillis = 90, easing = EmphasizedDecelerate))

private fun fadeThroughOut(): ExitTransition =
    fadeOut(tween(durationMillis = 90, easing = EmphasizedAccelerate))

// Shared X axis for drilling into a destination and back
private fun sharedAxisIn(forward: Boolean): EnterTransition =
    slideInHorizontally(tween(300, easing = EmphasizedDecelerate)) { (if (forward) 1 else -1) * it / 10 } +
        fadeIn(tween(210, delayMillis = 90, easing = EmphasizedDecelerate))

private fun sharedAxisOut(forward: Boolean): ExitTransition =
    slideOutHorizontally(tween(300, easing = EmphasizedDecelerate)) { (if (forward) -1 else 1) * it / 10 } +
        fadeOut(tween(90, easing = EmphasizedAccelerate))

/**
 * Soft blur at a screen edge (behind the floating navigation, or under the status bar once the
 * top bar scrolls away), strongest at the edge and fading out towards the content.
 *
 * The blurred copy is drawn over an opaque background: a blur near the layer edge becomes
 * semi-transparent, and without the fill the sharp content underneath would show through as seams.
 */
@Composable
private fun EdgeBlur(
    backdrop: com.kyant.backdrop.Backdrop,
    fromTop: Boolean,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    tintAlpha: Float = 0.55f
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val blurPx = with(density) { 6.dp.toPx() }
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
            .drawWithContent {
                drawRect(surface)
                drawContent()
                val edgeStops = listOf(Color.Transparent, surface.copy(alpha = tintAlpha))
                drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(if (fromTop) edgeStops.reversed() else edgeStops))
                // Fade the layer out towards the content over the last 24dp so there is no hard edge
                val fade = (24.dp.toPx() / size.height).coerceIn(0f, 1f)
                val maskStops = if (fromTop) {
                    arrayOf(0f to Color.Black, (1f - fade) to Color.Black, 1f to Color.Transparent)
                } else {
                    arrayOf(0f to Color.Transparent, fade to Color.Black, 1f to Color.Black)
                }
                drawRect(
                    androidx.compose.ui.graphics.Brush.verticalGradient(*maskStops),
                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                )
            }
            // Must come after drawWithContent so the fill sits under, and the mask over, the blur.
            // Plain variant: drawBackdrop adds a glass rim highlight that showed up as seams at the edges
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { androidx.compose.ui.graphics.RectangleShape },
                effects = { blur(blurPx) }
            )
    )
}
