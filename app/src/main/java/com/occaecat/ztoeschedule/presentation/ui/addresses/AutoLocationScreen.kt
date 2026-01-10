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
    onAddressDetected: ((AutoDetectedAddress) -> Unit)? = null,
    showRemSuggestions: Boolean = true,
    showTopBar: Boolean = false,
    modifier: Modifier = Modifier
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val currentState = state) {
                is AutoLocationState.Idle,
                is AutoLocationState.RequestingPermission -> {
                    PermissionContent(
                        permissionsState = locationPermissions,
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
                            state = AutoLocationState.GettingLocation
                            // Trigger retry
                        },
                        onManualSelection = onManualSelection
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionContent(
    permissionsState: MultiplePermissionsState,
    onRequestPermission: () -> Unit,
    onManualSelection: () -> Unit
) {
    Surface(
        modifier = Modifier.size(100.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Text(
        text = stringResource(R.string.loc_permission_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        text = stringResource(R.string.loc_permission_desc),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Button(
        onClick = onRequestPermission,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.MyLocation, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.loc_grant_btn))
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    OutlinedButton(
        onClick = onManualSelection,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Edit, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.loc_manual_btn))
    }
}

@Composable
private fun LoadingContent(state: AutoLocationState) {
    val (icon, title, subtitle) = when (state) {
        is AutoLocationState.GettingLocation -> Triple(
            Icons.Default.GpsFixed,
            stringResource(R.string.loc_status_getting),
            stringResource(R.string.loc_status_getting_sub)
        )
        is AutoLocationState.Geocoding -> Triple(
            Icons.Default.Map,
            stringResource(R.string.loc_status_geocoding),
            stringResource(R.string.loc_status_geocoding_sub)
        )
        is AutoLocationState.SearchingRem -> Triple(
            Icons.Default.Search,
            stringResource(R.string.loc_status_searching),
            stringResource(R.string.loc_status_searching_sub)
        )
        else -> Triple(Icons.Default.HourglassTop, stringResource(R.string.loc_status_loading), "")
    }
    
    CircularProgressIndicator(
        modifier = Modifier.size(80.dp),
        strokeWidth = 6.dp
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(32.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    
    if (subtitle.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuccessContent(
    state: AutoLocationState.Success,
    onRemSelected: (Rem) -> Unit,
    onManualSelection: () -> Unit,
    onAddressDetected: ((AutoDetectedAddress) -> Unit)?,
    showRemSuggestions: Boolean
) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Text(
        text = stringResource(R.string.loc_success_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = state.detectedAddress.rawAddress,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
    
    Spacer(modifier = Modifier.height(24.dp))

    if (onAddressDetected != null) {
        Button(
            onClick = { onAddressDetected(state.detectedAddress) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_save_address))
        }
    }

    if (showRemSuggestions && state.suggestedRem != null) {
        Text(
            text = stringResource(R.string.loc_suggested_rem),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ElevatedCard(
            onClick = { onRemSelected(state.suggestedRem) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.suggestedRem.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val distance = state.allRems.find { it.rem.id == state.suggestedRem.id }?.distanceKm
                    if (distance != null) {
                        Text(
                            text = "≈ ${String.format("%.1f", distance)} км",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Show other nearby REMs
    if (showRemSuggestions && state.allRems.size > 1) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.loc_other_rems),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        state.allRems.drop(1).take(3).forEach { remWithDistance ->
            OutlinedCard(
                onClick = { onRemSelected(remWithDistance.rem) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = remWithDistance.rem.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "≈ ${String.format("%.1f", remWithDistance.distanceKm)} км",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    TextButton(onClick = onManualSelection) {
        Icon(Icons.Default.Edit, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.loc_manual_rem_btn))
    }
}

@Composable
private fun ErrorContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onManualSelection: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.ErrorOutline,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.error
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Text(
        text = stringResource(R.string.loc_error_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    if (canRetry) {
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.loc_retry_btn))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    OutlinedButton(
        onClick = onManualSelection,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Edit, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.loc_manual_btn))
    }
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
                val fallback = "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
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
                    "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
                }
                AutoDetectedAddress(city, street, house, raw)
            } else {
                val fallback = "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
                AutoDetectedAddress(null, null, null, fallback)
            }
        } catch (e: Exception) {
            val fallback = "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
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
