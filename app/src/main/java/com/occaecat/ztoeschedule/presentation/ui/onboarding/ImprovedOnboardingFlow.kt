package com.occaecat.ztoeschedule.presentation.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.occaecat.ztoeschedule.data.model.*
import com.occaecat.ztoeschedule.data.repository.ConsumerCategory
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber
import com.occaecat.ztoeschedule.presentation.ui.addresses.AddressPickerDialog
import com.occaecat.ztoeschedule.presentation.ui.addresses.AddressPickerResult
import kotlinx.coroutines.launch

private data class SelectedAddressInfo(
    val remId: String,
    val remName: String,
    val cityId: String,
    val cityName: String,
    val streetId: String,
    val streetName: String,
    val addressId: String,
    val addressName: String,
    val cherga: Int,
    val pidcherga: Int,
    val displayName: String,
    val iconName: String
)

/**
 * Improved Onboarding Flow with:
 * - Progress indicator (Step X of Y)
 * - Dialog-based address selection (not HorizontalPager)
 * - Skip option for address selection
 * - Final completion screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedOnboardingFlow(
    remList: List<Rem>,
    cityList: List<City>,
    streetList: List<Street>,
    houseNumbers: List<ParsedHouseNumber>,
    searchQuery: String,
    isLoading: Boolean,
    selectedCategory: ConsumerCategory? = null,
    onLoadRem: () -> Unit,
    onLoadCity: (String) -> Unit,
    onLoadStreet: (String) -> Unit,
    onLoadAddress: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (ConsumerCategory?) -> Unit = {},
    onClearSearch: () -> Unit,
    onCancel: (() -> Unit)? = null,
    onComplete: (
        remId: String?, remName: String?, cityId: String?, cityName: String?,
        streetId: String?, streetName: String?, addressId: String,
        addressName: String, cherga: Int, pidcherga: Int,
        customName: String, iconName: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    // 4 steps: Welcome → Address Selection → Schedule Preview → Notifications
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4
    
    // Address selection state
    var showAddressDialog by remember { mutableStateOf(false) }
    var selectedInfo by remember { mutableStateOf<SelectedAddressInfo?>(null) }

    LaunchedEffect(Unit) {
        if (remList.isEmpty()) onLoadRem()
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (currentStep) {
            0 -> WelcomeOnboardingStep(
                onNext = { currentStep = 1 },
                onSkip = { onCancel?.invoke() },
                currentStep = currentStep,
                totalSteps = totalSteps
            )
            1 -> AddressSelectionStep(
                selectedInfo = selectedInfo,
                onOpenDialog = { showAddressDialog = true },
                onContinue = { currentStep = 2 },
                onSkip = { currentStep = 2 },
                onBack = { currentStep = 0 },
                currentStep = currentStep,
                totalSteps = totalSteps
            )
            2 -> SchedulePreviewStep(
                addressSelected = selectedInfo != null,
                onNext = { currentStep = 3 },
                onBack = { currentStep = 1 },
                currentStep = currentStep,
                totalSteps = totalSteps
            )
            3 -> NotificationsStep(
                addressReady = selectedInfo != null,
                onComplete = {
                    val info = selectedInfo ?: return@NotificationsStep
                    onComplete(
                        info.remId, info.remName,
                        info.cityId, info.cityName,
                        info.streetId, info.streetName,
                        info.addressId, info.addressName,
                        info.cherga, info.pidcherga,
                        info.displayName, info.iconName
                    )
                },
                onBack = { currentStep = 2 },
                currentStep = currentStep,
                totalSteps = totalSteps
            )
        }

        if (showAddressDialog) {
            AddressPickerDialog(
                remList = remList,
                cityList = cityList,
                streetList = streetList,
                houseNumbers = houseNumbers,
                searchQuery = searchQuery,
                isLoading = isLoading,
                selectedCategory = selectedCategory,
                onLoadRem = onLoadRem,
                onLoadCity = onLoadCity,
                onLoadStreet = onLoadStreet,
                onLoadAddress = onLoadAddress,
                onSearchQueryChange = onSearchQueryChange,
                onCategorySelected = onCategorySelected,
                onClearSearch = onClearSearch,
                onDismiss = { showAddressDialog = false },
                onComplete = { result: AddressPickerResult ->
                    selectedInfo = SelectedAddressInfo(
                        remId = result.remId,
                        remName = result.remName,
                        cityId = result.cityId,
                        cityName = result.cityName,
                        streetId = result.streetId,
                        streetName = result.streetName,
                        addressId = result.addressId,
                        addressName = result.addressName,
                        cherga = result.cherga,
                        pidcherga = result.pidcherga,
                        displayName = result.displayName,
                        iconName = result.iconName
                    )
                    showAddressDialog = false
                    currentStep = 2
                }
            )
        }
    }
}

@Composable
private fun WelcomeOnboardingStep(
    onNext: () -> Unit,
    onSkip: () -> Unit,
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Крок ${currentStep + 1} з $totalSteps",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onSkip) {
                    Text("Пропустити")
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { (currentStep + 1).toFloat() / totalSteps },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Content
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Ласкаво просимо!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "СвітлоЄ? - ваш помічник для моніторингу графіків відключень електроенергії в Житомирській області",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                FilledTonalButton(
                    onClick = onNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Далі")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressSelectionStep(
    selectedInfo: SelectedAddressInfo?,
    onOpenDialog: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Крок ${currentStep + 1} з $totalSteps",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onSkip) {
                    Text("Пропустити")
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { (currentStep + 1).toFloat() / totalSteps },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Оберіть вашу адресу",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Це допоможе нам показувати точний графік для вашого місцезнаходження",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Address selection button
            if (selectedInfo == null) {
                OutlinedButton(
                    onClick = onOpenDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.CenterVertically),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Виберіть адресу")
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Адреса вибрана",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                    selectedInfo.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = onOpenDialog) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Назад")
                }
                FilledTonalButton(
                    onClick = onContinue,
                    modifier = Modifier.weight(1f),
                    enabled = selectedInfo != null
                ) {
                    Text("Далі")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SchedulePreviewStep(
    addressSelected: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Крок ${currentStep + 1} з $totalSteps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LinearProgressIndicator(
                    progress = { (currentStep + 1).toFloat() / totalSteps },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    "Актуальний графік",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Ви бачите детальний графік відключень з точним часом та тривалістю",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Назад")
                }
                FilledTonalButton(
                    onClick = onNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Далі")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationsStep(
    addressReady: Boolean,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var permissionGranted by remember {
        mutableStateOf(
            !needsPermission ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Крок ${currentStep + 1} з $totalSteps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LinearProgressIndicator(
                    progress = { (currentStep + 1).toFloat() / totalSteps },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    "Завжди в курсі",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Отримуйте сповіщення про зміни графіків та критичні оновлення",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (needsPermission) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 2.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (permissionGranted) "Сповіщення увімкнено" else "Дозвольте сповіщення, щоб отримувати оновлення",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!permissionGranted) {
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Надати дозвіл")
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Назад")
                }
                FilledTonalButton(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    enabled = addressReady
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Готово!")
                }
            }
        }
    }
}

