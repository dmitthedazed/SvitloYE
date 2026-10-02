package com.occaecat.ztoeschedule.presentation.ui.onboarding

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.presentation.ui.addresses.AddressPickerResult
import com.occaecat.ztoeschedule.presentation.ui.addresses.AddressPickerScreen
import com.occaecat.ztoeschedule.presentation.ui.components.AppLogo
import com.occaecat.ztoeschedule.presentation.ui.components.ExpressiveHeroBadge
import com.occaecat.ztoeschedule.presentation.ui.components.GroupedInfoRow
import com.occaecat.ztoeschedule.presentation.ui.components.PageIndicatorDots
import com.occaecat.ztoeschedule.presentation.ui.components.StepHeroIcon
import com.occaecat.ztoeschedule.presentation.ui.components.StepHeroPage
import com.occaecat.ztoeschedule.presentation.ui.components.StepPrimaryButton
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var showSkipDialog by remember { mutableStateOf(false) }
    var addressResult by remember { mutableStateOf<AddressPickerResult?>(null) }

    // Track picker step for proper back handling
    var pickerStep by remember { mutableIntStateOf(0) }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

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
            icon = { Icon(Icons.Default.FastForward, contentDescription = null) },
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
    ) {
        OnboardingTopBar(
            step = currentStep,
            // Route through the dispatcher so the address picker can unwind its own sub-steps first
            onBack = { backDispatcher?.onBackPressed() },
            onSkip = { showSkipDialog = true }
        )

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
            label = "onboarding_step",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { step ->
            when (step) {
                OnboardingStep.WELCOME -> WelcomeContent(
                    onContinue = {
                        viewModel.loadRemList()
                        currentStep = OnboardingStep.ADD_ADDRESS
                    }
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
                        showBackButton = false,
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
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OnboardingTopBar(
    step: OnboardingStep,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp)
    ) {
        Box(Modifier.align(Alignment.CenterStart)) {
            androidx.compose.animation.AnimatedVisibility(
                visible = step != OnboardingStep.WELCOME,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FilledTonalIconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.onboarding_back)
                    )
                }
            }
        }

        PageIndicatorDots(
            current = step.ordinal,
            total = OnboardingStep.entries.size,
            description = stringResource(R.string.onboarding_step_description, step.ordinal + 1, OnboardingStep.entries.size),
            modifier = Modifier.align(Alignment.Center)
        )

        Box(Modifier.align(Alignment.CenterEnd)) {
            androidx.compose.animation.AnimatedVisibility(
                visible = step != OnboardingStep.PERMISSIONS,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.onboarding_skip), maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WelcomeContent(
    onContinue: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val features = listOf(
        Triple(Icons.Default.CalendarMonth, R.string.onboarding_feature_schedule_title, R.string.onboarding_feature_schedule_desc),
        Triple(Icons.Default.NotificationsActive, R.string.onboarding_feature_alerts_title, R.string.onboarding_feature_alerts_desc),
        Triple(Icons.Default.Widgets, R.string.onboarding_feature_widgets_title, R.string.onboarding_feature_widgets_desc)
    )

    StepHeroPage(
        hero = {
            ExpressiveHeroBadge(
                shape = MaterialShapes.Cookie9Sided.toShape(),
                containerColor = colorScheme.primaryContainer,
                size = 176.dp,
                rotate = true,
                popIn = true
            ) {
                AppLogo(Modifier.size(104.dp))
            }
        },
        title = stringResource(R.string.onboarding_welcome_title),
        subtitle = stringResource(R.string.onboarding_welcome_subtitle),
        actions = {
            StepPrimaryButton(
                text = stringResource(R.string.onboarding_start),
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = onContinue
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            features.forEachIndexed { index, (icon, title, desc) ->
                GroupedInfoRow(
                    index = index,
                    count = features.size,
                    icon = icon,
                    title = stringResource(title),
                    supporting = stringResource(desc)
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PermissionsContent(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // Notification permission (Android 13+)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null
    val notificationGranted = notificationPermissionState?.status?.isGranted ?: true

    // Exact alarms (Android 12+) are granted from system settings, so re-check on resume
    val alarmManager = remember { context.getSystemService(AlarmManager::class.java) }
    fun canScheduleExact() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    var alarmsGranted by remember { mutableStateOf(canScheduleExact()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) alarmsGranted = canScheduleExact()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    StepHeroPage(
        hero = {
            StepHeroIcon(
                icon = Icons.Default.NotificationsActive,
                shape = MaterialShapes.Sunny.toShape(),
                containerColor = colorScheme.tertiaryContainer,
                contentColor = colorScheme.onTertiaryContainer
            )
        },
        title = stringResource(R.string.onboarding_permissions_title),
        subtitle = stringResource(R.string.onboarding_permissions_subtitle),
        actions = {
            StepPrimaryButton(
                text = stringResource(R.string.onboarding_finish),
                icon = Icons.Default.Check,
                onClick = onFinish
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            GroupedInfoRow(
                index = 0,
                count = 2,
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.onboarding_permissions_notifications),
                supporting = stringResource(R.string.onboarding_permissions_notifications_desc),
                trailing = {
                    PermissionAction(
                        granted = notificationGranted,
                        onRequest = { notificationPermissionState?.launchPermissionRequest() }
                    )
                }
            )
            GroupedInfoRow(
                index = 1,
                count = 2,
                icon = Icons.Default.Alarm,
                title = stringResource(R.string.onboarding_permissions_alarms),
                supporting = stringResource(R.string.onboarding_permissions_alarms_desc),
                trailing = {
                    PermissionAction(
                        granted = alarmsGranted,
                        onRequest = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                )
                            }
                        }
                    )
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_permissions_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PermissionAction(
    granted: Boolean,
    onRequest: () -> Unit
) {
    // Permissions can't be revoked from here, so the switch only ever turns on
    Switch(
        checked = granted,
        onCheckedChange = { if (!granted) onRequest() },
        thumbContent = if (granted) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
        } else null
    )
}
