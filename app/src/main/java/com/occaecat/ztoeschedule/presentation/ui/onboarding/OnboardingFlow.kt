package com.occaecat.ztoeschedule.presentation.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.data.model.*
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber
import com.occaecat.ztoeschedule.presentation.ui.addresses.AddressCustomizationScreen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

import androidx.compose.ui.res.stringResource
import com.occaecat.ztoeschedule.R

/**
 * Modern Onboarding Flow using a step-based approach instead of Pager for better stability.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFlow(
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
    onCancel: (() -> Unit)? = null,
    showWelcome: Boolean = true,
    onComplete: (
        remId: String?, remName: String?, cityId: String?, cityName: String?,
        streetId: String?, streetName: String?, addressId: String,
        addressName: String, cherga: Int, pidcherga: Int,
        customName: String, iconName: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Step 0: Welcome, 1: Permissions, 2: REM, 3: City, 4: Street, 5: House, 6: Customize
    var step by remember { mutableIntStateOf(if (showWelcome) 0 else 2) }
    
    // Support system back gesture to go to previous step
    val canGoBack = step > (if (showWelcome) 0 else 2)
    androidx.activity.compose.BackHandler(enabled = canGoBack) {
        step--
    }

    var selectedRem by remember { mutableStateOf<Rem?>(null) }
    var selectedCity by remember { mutableStateOf<City?>(null) }
    var selectedStreet by remember { mutableStateOf<Street?>(null) }
    var selectedHouse by remember { mutableStateOf<ParsedHouseNumber?>(null) }
    
    // Function to check if notification permission is granted
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    LaunchedEffect(Unit) {
        if (remList.isEmpty()) onLoadRem()
    }

    // Add statusBarsPadding to the main container
    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (step) {
                            0 -> stringResource(R.string.step_welcome)
                            1 -> stringResource(R.string.step_notifications)
                            2 -> stringResource(R.string.step_rem)
                            3 -> stringResource(R.string.step_city)
                            4 -> stringResource(R.string.step_street)
                            5 -> stringResource(R.string.step_house)
                            6 -> stringResource(R.string.step_customize)
                            else -> ""
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (step > (if (showWelcome) 0 else 2)) {
                        IconButton(onClick = { step-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                        }
                    } else if (onCancel != null) {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, "Закрити")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress indicator
            if (step > 0) {
                LinearProgressIndicator(
                    progress = { step / 6f },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "onboarding_transition"
                ) { targetStep ->
                    when (targetStep) {
                        0 -> WelcomePage(onStart = { 
                            if (hasNotificationPermission()) {
                                step = 2
                            } else {
                                step = 1
                            }
                        })
                        1 -> NotificationPermissionPage(
                            onGranted = { step = 2 },
                            onSkip = { step = 2 }
                        )
                        2 -> RemSelectionPage(
                            rems = remList,
                            isLoading = isLoading,
                            onRemSelected = {
                                selectedRem = it
                                onLoadCity(it.id)
                                step = 3
                            }
                        )
                        3 -> CitySelectionPage(
                            cities = cityList,
                            isLoading = isLoading,
                            onCitySelected = {
                                selectedCity = it
                                onLoadStreet(it.id)
                                step = 4
                            }
                        )
                        4 -> StreetSelectionPage(
                            streets = streetList,
                            isLoading = isLoading,
                            onStreetSelected = {
                                selectedStreet = it
                                onLoadAddress(it.id)
                                step = 5
                            }
                        )
                        5 -> HouseNumberSelectionPage(
                            houseNumbers = houseNumbers,
                            searchQuery = searchQuery,
                            isLoading = isLoading,
                            onSearchQueryChange = onSearchQueryChange,
                            onClearSearch = onClearSearch,
                            onHouseSelected = {
                                selectedHouse = it
                                step = 6
                            }
                        )
                        6 -> AddressCustomizationScreen(
                            onComplete = { name, icon ->
                                onComplete(
                                    selectedRem?.id, selectedRem?.name,
                                    selectedCity?.id, selectedCity?.name,
                                    selectedStreet?.id, selectedStreet?.name,
                                    selectedHouse!!.originalAddressId,
                                    selectedHouse!!.houseNumber,
                                    selectedHouse!!.cherga,
                                    selectedHouse!!.pidcherga,
                                    name, icon
                                )
                            },
                            onBack = { step = 5 }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationPermissionPage(
    onGranted: () -> Unit,
    onSkip: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onGranted() else onSkip()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Будьте в курсі змін",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Ми будемо надсилати вам сповіщення перед відключенням та при зміні статусу світла",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = { 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onGranted()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Дозволити сповіщення", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Налаштувати пізніше", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WelcomePage(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.onboarding_welcome_desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.onboarding_start_btn), style = MaterialTheme.typography.titleMedium)
        }
    }
}