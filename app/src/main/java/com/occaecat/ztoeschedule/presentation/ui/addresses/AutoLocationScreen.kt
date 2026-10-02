package com.occaecat.ztoeschedule.presentation.ui.addresses

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.occaecat.ztoeschedule.data.model.Rem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

import androidx.compose.ui.res.stringResource
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.presentation.ui.components.SettingsGroupItem
import com.occaecat.ztoeschedule.presentation.ui.components.StepHeroIcon
import com.occaecat.ztoeschedule.presentation.ui.components.StepHeroPage
import com.occaecat.ztoeschedule.presentation.ui.components.StepLeadingIcon
import com.occaecat.ztoeschedule.presentation.ui.components.StepPrimaryButton
import com.occaecat.ztoeschedule.presentation.ui.components.StepSecondaryButton

/**
 * State for auto-location detection
 */
sealed class AutoLocationState {
    data object Idle : AutoLocationState()
    data object RequestingPermission : AutoLocationState()
    data object GettingLocation : AutoLocationState()
    data object Geocoding : AutoLocationState()
    data object SearchingRem : AutoLocationState()
    data class Success(
        val location: Location,
        val detectedAddress: AutoDetectedAddress,
        val suggestedRem: Rem?,
        val allRems: List<RemWithDistance>
    ) : AutoLocationState()
    data class Error(val message: String, val canRetry: Boolean = true) : AutoLocationState()
}

data class RemWithDistance(
    val rem: Rem,
    val distanceKm: Double
)

data class AutoDetectedAddress(
    val city: String?,
    val street: String?,
    val house: String?,
    val rawAddress: String
)

/**
 * Auto-location screen that detects user location and suggests nearest REM
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AutoLocationScreen(
    remList: List<Rem>,
    onRemSelected: (Rem) -> Unit,
    onManualSelection: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onAddressDetected: ((AutoDetectedAddress) -> Unit)? = null,
    showRemSuggestions: Boolean = true,
    showTopBar: Boolean = false,
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<AutoLocationState>(AutoLocationState.Idle) }
    
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    // Start location detection when permissions are granted
    LaunchedEffect(locationPermissions.allPermissionsGranted, state) {
        if (locationPermissions.allPermissionsGranted && state is AutoLocationState.Idle) {
            state = AutoLocationState.GettingLocation
            detectLocation(context, remList) { newState ->
                state = newState
            }
        }
    }
    
    // Request permissions on first launch
    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            state = AutoLocationState.RequestingPermission
            locationPermissions.launchMultiplePermissionRequest()
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(stringResource(R.string.loc_permission_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            when (val currentState = state) {
                is AutoLocationState.Idle,
                is AutoLocationState.RequestingPermission -> {
                    PermissionContent(
                        onRequestPermission = { locationPermissions.launchMultiplePermissionRequest() },
                        onManualSelection = onManualSelection
                    )
                }

                is AutoLocationState.GettingLocation,
                is AutoLocationState.Geocoding,
                is AutoLocationState.SearchingRem -> {
                    LoadingContent(state = currentState)
                }

                is AutoLocationState.Success -> {
                    SuccessContent(
                        state = currentState,
                        onRemSelected = onRemSelected,
                        onManualSelection = onManualSelection,
                        onAddressDetected = onAddressDetected,
                        showRemSuggestions = showRemSuggestions
                    )
                }

                is AutoLocationState.Error -> {
                    ErrorContent(
                        message = currentState.message,
                        canRetry = currentState.canRetry,
                        onRetry = {
                            // Idle re-arms the detection effect above
                            state = AutoLocationState.Idle
                        },
                        onManualSelection = onManualSelection
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PermissionContent(
    onRequestPermission: () -> Unit,
    onManualSelection: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    StepHeroPage(
        hero = {
            StepHeroIcon(
                icon = Icons.Default.MyLocation,
                shape = MaterialShapes.Pentagon.toShape(),
                containerColor = colorScheme.secondaryContainer,
                contentColor = colorScheme.onSecondaryContainer
            )
        },
        title = stringResource(R.string.loc_permission_title),
        subtitle = stringResource(R.string.loc_permission_desc),
        actions = {
            StepPrimaryButton(
                text = stringResource(R.string.loc_grant_btn),
                onClick = onRequestPermission
            )
            StepSecondaryButton(
                text = stringResource(R.string.loc_manual_btn),
                onClick = onManualSelection
            )
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingContent(state: AutoLocationState) {
    val colorScheme = MaterialTheme.colorScheme
    val (title, subtitle) = when (state) {
        is AutoLocationState.GettingLocation -> stringResource(R.string.loc_status_getting) to stringResource(R.string.loc_status_getting_sub)
        is AutoLocationState.Geocoding -> stringResource(R.string.loc_status_geocoding) to stringResource(R.string.loc_status_geocoding_sub)
        is AutoLocationState.SearchingRem -> stringResource(R.string.loc_status_searching) to stringResource(R.string.loc_status_searching_sub)
        else -> stringResource(R.string.loc_status_loading) to null
    }

    StepHeroPage(
        hero = {
            ContainedLoadingIndicator(
                modifier = Modifier.size(144.dp),
                containerColor = colorScheme.secondaryContainer,
                indicatorColor = colorScheme.onSecondaryContainer
            )
        },
        title = title,
        subtitle = subtitle
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SuccessContent(
    state: AutoLocationState.Success,
    onRemSelected: (Rem) -> Unit,
    onManualSelection: () -> Unit,
    onAddressDetected: ((AutoDetectedAddress) -> Unit)?,
    showRemSuggestions: Boolean
) {
    val currentLocale = LocalLocale.current.platformLocale
    val colorScheme = MaterialTheme.colorScheme
    fun distanceLabel(km: Double) = "≈ ${String.format(currentLocale, "%.1f", km)} км"

    // Suggested REM first, then up to three more nearby ones
    val rems = if (showRemSuggestions) state.allRems.take(4) else emptyList()

    StepHeroPage(
        hero = {
            StepHeroIcon(
                icon = Icons.Default.CheckCircle,
                shape = MaterialShapes.Cookie9Sided.toShape(),
                containerColor = colorScheme.primaryContainer,
                contentColor = colorScheme.onPrimaryContainer
            )
        },
        title = stringResource(R.string.loc_success_title),
        subtitle = state.detectedAddress.rawAddress,
        actions = {
            if (onAddressDetected != null) {
                StepPrimaryButton(
                    text = stringResource(R.string.action_save_address),
                    icon = Icons.Default.Check,
                    onClick = { onAddressDetected(state.detectedAddress) }
                )
            }
            StepSecondaryButton(
                text = stringResource(R.string.loc_manual_rem_btn),
                onClick = onManualSelection
            )
        }
    ) {
        if (rems.isNotEmpty()) {
            Text(
                text = stringResource(R.string.loc_suggested_rem).trimEnd(':'),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, bottom = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                rems.forEachIndexed { index, remWithDistance ->
                    val isSuggested = index == 0
                    SettingsGroupItem(
                        index = index,
                        totalCount = rems.size,
                        headlineContent = {
                            Text(remWithDistance.rem.name, fontWeight = if (isSuggested) FontWeight.Bold else FontWeight.Normal)
                        },
                        supportingContent = { Text(distanceLabel(remWithDistance.distanceKm)) },
                        leadingContent = {
                            if (isSuggested) {
                                StepLeadingIcon(
                                    icon = Icons.Default.Star,
                                    containerColor = colorScheme.primary,
                                    contentColor = colorScheme.onPrimary
                                )
                            } else {
                                StepLeadingIcon(Icons.Default.Business)
                            }
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colorScheme.onSurfaceVariant)
                        },
                        onClick = { onRemSelected(remWithDistance.rem) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ErrorContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onManualSelection: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    StepHeroPage(
        hero = {
            StepHeroIcon(
                icon = Icons.Default.LocationOff,
                shape = MaterialShapes.Cookie4Sided.toShape(),
                containerColor = colorScheme.errorContainer,
                contentColor = colorScheme.onErrorContainer
            )
        },
        title = stringResource(R.string.loc_error_title),
        subtitle = message,
        actions = {
            if (canRetry) {
                StepPrimaryButton(
                    text = stringResource(R.string.loc_retry_btn),
                    onClick = onRetry
                )
            }
            StepSecondaryButton(
                text = stringResource(R.string.loc_manual_btn),
                onClick = onManualSelection
            )
        }
    )
}

private suspend fun detectLocation(
    context: Context,
    remList: List<Rem>,
    onStateChange: (AutoLocationState) -> Unit
) {
    try {
        // Step 1: Get current location
        onStateChange(AutoLocationState.GettingLocation)
        val location = getCurrentLocation(context)
        
        // Step 2: Geocode to address
        onStateChange(AutoLocationState.Geocoding)
        val detectedAddress = geocodeLocation(context, location)
        
        // Step 3: Find nearest REM
        onStateChange(AutoLocationState.SearchingRem)
        val sortedRems = findNearestRems(location, remList)
        
        onStateChange(AutoLocationState.Success(
            location = location,
            detectedAddress = detectedAddress,
            suggestedRem = sortedRems.firstOrNull()?.rem,
            allRems = sortedRems
        ))
        
    } catch (e: Exception) {
        onStateChange(AutoLocationState.Error(
            message = e.message ?: "Невідома помилка",
            canRetry = true
        ))
    }
}

@SuppressLint("MissingPermission")
private suspend fun getCurrentLocation(context: Context): Location = 
    suspendCancellableCoroutine { continuation ->
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()
        
        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }
        
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                continuation.resume(location)
            } else {
                // Fallback to last known location
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                    if (lastLocation != null) {
                        continuation.resume(lastLocation)
                    } else {
                        continuation.resumeWithException(
                            Exception("Не вдалося отримати місцезнаходження. Увімкніть GPS.")
                        )
                    }
                }.addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
            }
        }.addOnFailureListener { e ->
            continuation.resumeWithException(e)
        }
    }

private suspend fun geocodeLocation(context: Context, location: Location): AutoDetectedAddress = 
    withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) {
                val fallback = "${String.format(Locale.getDefault(), "%.4f", location.latitude)}, ${String.format(Locale.getDefault(), "%.4f", location.longitude)}"
                return@withContext AutoDetectedAddress(null, null, null, fallback)
            }
            
            val geocoder = Geocoder(context, Locale("uk", "UA"))
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val parts = mutableListOf<String>()
                val city = address.locality ?: address.subAdminArea ?: address.adminArea
                val street = address.thoroughfare
                val house = address.subThoroughfare ?: address.featureName?.takeIf { it != address.thoroughfare }

                city?.let { parts.add(it) }
                street?.let { parts.add(it) }
                house?.let { parts.add(it) }
                
                if (parts.isEmpty()) {
                    address.adminArea?.let { parts.add(it) }
                }
                
                val raw = parts.joinToString(", ").ifEmpty { 
                    "${String.format(Locale.getDefault(), "%.4f", location.latitude)}, ${String.format(Locale.getDefault(), "%.4f", location.longitude)}"
                }
                AutoDetectedAddress(city, street, house, raw)
            } else {
                val fallback = "${String.format(Locale.getDefault(), "%.4f", location.latitude)}, ${String.format(Locale.getDefault(), "%.4f", location.longitude)}"
                AutoDetectedAddress(null, null, null, fallback)
            }
        } catch (e: Exception) {
            val fallback = "${String.format(Locale.getDefault(), "%.4f", location.latitude)}, ${String.format(Locale.getDefault(), "%.4f", location.longitude)}"
            AutoDetectedAddress(null, null, null, fallback)
        }
    }

/**
 * Find nearest REMs based on approximate center coordinates
 * Note: This is a simplified implementation. In production, you'd want actual REM coordinates.
 */
private fun findNearestRems(userLocation: Location, remList: List<Rem>): List<RemWithDistance> {
    // Approximate coordinates for Zhytomyr REMs
    // In production, these should come from the API or be stored in the app
    val remCoordinates = mapOf(
        // These are approximate center points for different areas
        "Житомирський РЕМ" to Pair(50.2547, 28.6587),
        "Бердичівський РЕМ" to Pair(49.8920, 28.5992),
        "Коростенський РЕМ" to Pair(50.9514, 28.6347),
        "Новоград-Волинський РЕМ" to Pair(50.5847, 27.6181),
        "Малинський РЕМ" to Pair(50.7692, 29.2544),
        "Овруцький РЕМ" to Pair(51.3236, 28.8006),
        "Радомишльський РЕМ" to Pair(50.4983, 29.2331),
        "Ємільчинський РЕМ" to Pair(50.8786, 27.8075),
        "Попільнянський РЕМ" to Pair(50.0444, 29.5442),
        "Чуднівський РЕМ" to Pair(50.0522, 28.1200)
    )
    
    return remList.map { rem ->
        val coords = remCoordinates.entries.find { 
            rem.name.contains(it.key, ignoreCase = true) || it.key.contains(rem.name, ignoreCase = true)
        }?.value
        
        val distance = if (coords != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                coords.first, coords.second,
                results
            )
            results[0] / 1000.0 // Convert to km
        } else {
            // Default distance for unknown REMs
            100.0
        }
        
        RemWithDistance(rem, distance)
    }.sortedBy { it.distanceKm }
}
