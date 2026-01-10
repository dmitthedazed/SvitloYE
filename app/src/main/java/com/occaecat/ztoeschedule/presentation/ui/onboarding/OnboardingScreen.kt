package com.occaecat.ztoeschedule.presentation.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.presentation.ui.addresses.AddressPickerResult
import com.occaecat.ztoeschedule.presentation.ui.addresses.AddressPickerScreen
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel

/**
 * Onboarding step enum - simplified to 3 steps
 */
private enum class OnboardingStep {
    WELCOME,
    ADD_ADDRESS,
    PERMISSIONS
}

/**
 * Main onboarding screen with integrated address picker
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    viewModel: EnergyScheduleViewModel,
    onComplete: (AddressPickerResult?) -> Unit,
    onSkip: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var showSkipDialog by remember { mutableStateOf(false) }
    var addressResult by remember { mutableStateOf<AddressPickerResult?>(null) }
    
    // Track picker step for proper back handling
    var pickerStep by remember { mutableIntStateOf(0) }

    // Back handler
    BackHandler(enabled = currentStep != OnboardingStep.WELCOME || pickerStep > 0) {
        when {
            currentStep == OnboardingStep.PERMISSIONS -> {
                currentStep = OnboardingStep.ADD_ADDRESS
            }
            currentStep == OnboardingStep.ADD_ADDRESS && pickerStep == 0 -> {
                currentStep = OnboardingStep.WELCOME
            }
            // If pickerStep > 0, the picker handles its own back navigation
        }
    }

    // Skip confirmation dialog
    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text(stringResource(R.string.onboarding_skip_dialog_title)) },
            text = { Text(stringResource(R.string.onboarding_skip_dialog_message)) },
            confirmButton = {
                TextButton(onClick = { showSkipDialog = false; onSkip() }) {
                    Text(stringResource(R.string.onboarding_skip_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) {
                    Text(stringResource(R.string.onboarding_skip_dialog_cancel))
                }
            }
        )
    }

    val motionScheme = MaterialTheme.motionScheme
    
    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                (slideInHorizontally(motionScheme.defaultSpatialSpec()) { it / 3 } + fadeIn(motionScheme.slowEffectsSpec()))
                    .togetherWith(slideOutHorizontally(motionScheme.defaultSpatialSpec()) { -it / 3 } + fadeOut(motionScheme.slowEffectsSpec()))
            } else {
                (slideInHorizontally(motionScheme.defaultSpatialSpec()) { -it / 3 } + fadeIn(motionScheme.slowEffectsSpec()))
                    .togetherWith(slideOutHorizontally(motionScheme.defaultSpatialSpec()) { it / 3 } + fadeOut(motionScheme.slowEffectsSpec()))
            }
        },
        label = "onboarding_step"
    ) { step ->
        when (step) {
            OnboardingStep.WELCOME -> WelcomeContent(
                onContinue = { 
                    viewModel.loadRemList()
                    currentStep = OnboardingStep.ADD_ADDRESS 
                },
                onSkip = { showSkipDialog = true }
            )
            OnboardingStep.ADD_ADDRESS -> {
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
                    onCancel = { currentStep = OnboardingStep.WELCOME },
                    onComplete = { result ->
                        addressResult = result
                        currentStep = OnboardingStep.PERMISSIONS
                    },
                    onGoBack = { currentStep = OnboardingStep.WELCOME },
                    onStepChanged = { pickerStep = it },
                    showTopBar = false,
                    skipConfirmation = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
            OnboardingStep.PERMISSIONS -> PermissionsContent(
                onFinish = { onComplete(addressResult) }
            )
        }
    }
}

@Composable
private fun WelcomeContent(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(Modifier.weight(1f))
        
        // App icon
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Title
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(Modifier.height(12.dp))
        
        // Subtitle
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.weight(1f))
        
        // Buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Continue button (primary)
            FilledTonalButton(
                onClick = onContinue,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.onboarding_continue))
            }
            
            // Skip button (secondary)
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionsContent(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    
    // Notification permission (Android 13+)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null
    
    val notificationGranted = notificationPermissionState?.status?.isGranted ?: true
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterHorizontally),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.onboarding_permissions_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.onboarding_permissions_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Permission cards
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (notificationGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.onboarding_permissions_notifications),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (notificationGranted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.onboarding_permissions_alarms),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        if (!notificationGranted && notificationPermissionState != null) {
            Spacer(Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { notificationPermissionState.launchPermissionRequest() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_permissions_grant))
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.onboarding_finish))
        }
        
        Spacer(Modifier.height(32.dp))
    }
}
