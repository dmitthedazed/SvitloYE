@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.occaecat.ztoeschedule.presentation.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.presentation.ui.components.SettingsGroupItem
import com.occaecat.ztoeschedule.presentation.ui.more.IntegrationsScreen
import com.occaecat.ztoeschedule.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun SettingsScreenRoot(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backStack = viewModel.backStack

    BackHandler(enabled = backStack.size > 1) {
        viewModel.onAction(SettingsAction.GoBack)
    }

    SettingsScreen(
        state = state,
        backStack = backStack,
        onAction = { action ->
            if (action is SettingsAction.GoBack && backStack.size <= 1) {
                onBackClick()
            } else {
                viewModel.onAction(action)
            }
        }
    )
}

@Composable
private fun SettingsScreen(
    state: SettingsState,
    backStack: List<SettingsRoute>,
    onAction: (SettingsAction) -> Unit
) {
    val currentRoute = backStack.lastOrNull() ?: SettingsRoute.Main

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = {
            val initialIndex = backStack.indexOf(initialState)
            val targetIndex = backStack.indexOf(targetState)
            val isForward = if (initialIndex == -1) false else targetIndex > initialIndex

            if (isForward) {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            }
        },
        label = "SettingsNavigation"
    ) { route ->
        when (route) {
            SettingsRoute.Main -> MainSettingsList(
                onAction = onAction
            )
            SettingsRoute.Style -> StyleSettingsScreen(
                state = state,
                onAction = onAction
            )
            SettingsRoute.Notifications -> NotificationSettingsScreen(
                state = state,
                onAction = onAction
            )
            SettingsRoute.Language -> PlaceholderSettingsScreen(
                title = "Мова",
                text = "Використовується системна мова",
                onBackClick = { onAction(SettingsAction.GoBack) }
            )
            SettingsRoute.Developers -> DeveloperSettingsScreen(
                onAction = onAction
            )
            SettingsRoute.Integrations -> IntegrationsScreen(
                onBack = { onAction(SettingsAction.GoBack) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainSettingsList(
    onAction: (SettingsAction) -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    val appearanceItems = listOf(
        SettingsItem(
            title = "Стиль та тема",
            subtitle = "Кольори, режим темряви, розмір",
            icon = { Icon(Icons.Default.Palette, null) },
            onClick = { onAction(SettingsAction.Navigate(SettingsRoute.Style)) }
        ),
        SettingsItem(
            title = "Сповіщення",
            subtitle = "Налаштування повідомлень",
            icon = { Icon(Icons.Default.Notifications, null) },
            onClick = { onAction(SettingsAction.Navigate(SettingsRoute.Notifications)) }
        ),
        SettingsItem(
            title = "Мова",
            subtitle = "Українська (Системна)",
            icon = { Icon(Icons.Default.Language, null) },
            onClick = { onAction(SettingsAction.Navigate(SettingsRoute.Language)) }
        )
    )

    val advancedItems = listOf(
        SettingsItem(
            title = "Видалити дані",
            subtitle = "Очистити кеш та збережені адреси",
            icon = { Icon(Icons.Default.DeleteForever, null) },
            onClick = { showClearDialog = true }
        ),
        SettingsItem(
            title = "Налаштування розробника",
            subtitle = "Інструменти налагодження та інтеграції",
            icon = { Icon(Icons.Default.Code, null) },
            onClick = { onAction(SettingsAction.Navigate(SettingsRoute.Developers)) }
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsAction.GoBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            )
        ) {
            itemsIndexed(appearanceItems) { index, item ->
                SettingsGroupItem(
                    index = index,
                    totalCount = appearanceItems.size,
                    leadingContent = { item.icon() },
                    headlineContent = { Text(item.title) },
                    supportingContent = {
                        Text(item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                    },
                    onClick = item.onClick
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            itemsIndexed(advancedItems) { index, item ->
                SettingsGroupItem(
                    index = index,
                    totalCount = advancedItems.size,
                    leadingContent = { item.icon() },
                    headlineContent = { Text(item.title) },
                    supportingContent = {
                        Text(item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                    },
                    onClick = item.onClick
                )
            }
        }
    }


    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Видалити всі дані?") },
            text = { Text("Всі збережені адреси та налаштування будуть видалені.") },
            confirmButton = {
                Button(
                    onClick = { showClearDialog = false; onAction(SettingsAction.ClearData) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ВИДАЛИТИ ВСЕ")
                }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Скасувати") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeveloperSettingsScreen(
    onAction: (SettingsAction) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scope = rememberCoroutineScope()
    
    var gpsAddress by remember { mutableStateOf<String?>(null) }
    var isLoadingGps by remember { mutableStateOf(false) }
    var gpsError by remember { mutableStateOf<String?>(null) }
    
    val fetchLocation: () -> Unit = {
        isLoadingGps = true
        gpsError = null
        scope.launch {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val geocoder = Geocoder(context, Locale("uk", "UA"))
                                    if (!Geocoder.isPresent()) {
                                        withContext(Dispatchers.Main) {
                                            gpsError = "Геокодування недоступне"
                                            isLoadingGps = false
                                        }
                                        return@launch
                                    }
                                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                    withContext(Dispatchers.Main) {
                                        if (!addresses.isNullOrEmpty()) {
                                            val address = addresses[0]
                                            val parts = mutableListOf<String>()
                                            if (!address.adminArea.isNullOrEmpty()) parts.add(address.adminArea)
                                            if (!address.thoroughfare.isNullOrEmpty()) parts.add(address.thoroughfare)
                                            if (!address.featureName.isNullOrEmpty()) parts.add(address.featureName)
                                            gpsAddress = if (parts.isNotEmpty()) parts.joinToString(", ") else "Невідома адреса"
                                        } else {
                                            gpsError = "Адреса не знайдена"
                                        }
                                        isLoadingGps = false
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        gpsError = "Помилка геокодування: ${e.message}"
                                        isLoadingGps = false
                                    }
                                }
                            }
                        } else {
                            gpsError = "Локація недоступна"
                            isLoadingGps = false
                        }
                    }
                    .addOnFailureListener { e ->
                        gpsError = "Помилка GPS: ${e.message}"
                        isLoadingGps = false
                    }
            } catch (e: Exception) {
                gpsError = "Помилка: ${e.message}"
                isLoadingGps = false
            }
        }
    }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchLocation()
        } else {
            gpsError = "Дозвіл відхилено"
            isLoadingGps = false
        }
    }
    
    val requestLocationWithPermission: () -> Unit = {
        val hasFinePermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarsePermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasFinePermission || hasCoarsePermission) {
            fetchLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Розробник") },
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsAction.GoBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // GPS Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                onClick = { requestLocationWithPermission() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoadingGps) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "GPS",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Отримати адресу по GPS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (gpsError != null) {
                            Text(
                                text = gpsError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (gpsAddress != null) {
                            Text(
                                text = gpsAddress!!,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                text = "Натисніть для перевірки",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    if (gpsAddress != null && gpsError == null) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(gpsAddress!!))
                                }
                        )
                    }
                }
            }

            // Integrations & Demo items
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsGroupItem(
                    index = 0,
                    totalCount = 2,
                    leadingContent = { Icon(Icons.Default.Extension, null) },
                    headlineContent = { Text("Функції та інтеграції") },
                    supportingContent = { Text("Android Auto, віджети та інше") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp)) },
                    onClick = { onAction(SettingsAction.Navigate(SettingsRoute.Integrations)) }
                )
                
                SettingsGroupItem(
                    index = 1,
                    totalCount = 3,
                    leadingContent = { Icon(Icons.Default.BugReport, null, tint = MaterialTheme.colorScheme.error) },
                    headlineContent = { Text("Додати демо-локацію") },
                    supportingContent = { Text("Для тестування сповіщень") },
                    onClick = { onAction(SettingsAction.AddDemoLocation) }
                )
                
                SettingsGroupItem(
                    index = 2,
                    totalCount = 3,
                    leadingContent = { Icon(Icons.Default.SettingsBackupRestore, null, tint = MaterialTheme.colorScheme.tertiary) },
                    headlineContent = { Text("Скинути онбординг") },
                    supportingContent = { Text("Перезапустити привітання") },
                    onClick = { onAction(SettingsAction.ResetSettings) }
                )
            }
        }
    }
}

private data class SettingsItem(
    val title: String,
    val subtitle: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderSettingsScreen(title: String, text: String, onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(text)
        }
    }
}
