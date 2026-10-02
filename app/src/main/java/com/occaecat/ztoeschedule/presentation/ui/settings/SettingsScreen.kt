@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.occaecat.ztoeschedule.presentation.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.occaecat.ztoeschedule.BuildConfig
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.presentation.ui.components.ExpressiveHeroBadge
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

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

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
            // Material shared X axis: short slide + cross-fade, like the rest of the app
            // Going back pops the page first, so the page we leave is no longer in the stack
            val initialIndex = backStack.indexOf(initialState)
            val forward = initialIndex != -1 && backStack.indexOf(targetState) > initialIndex
            val dir = if (forward) 1 else -1
            (slideInHorizontally(tween(300, easing = EmphasizedDecelerate)) { dir * it / 10 } +
                fadeIn(tween(210, delayMillis = 90, easing = EmphasizedDecelerate)))
                .togetherWith(
                    slideOutHorizontally(tween(300, easing = EmphasizedDecelerate)) { -dir * it / 10 } +
                        fadeOut(tween(90, easing = EmphasizedAccelerate))
                )
        },
        label = "SettingsNavigation"
    ) { route ->
        when (route) {
            SettingsRoute.Main -> MainSettingsList(state = state, onAction = onAction)
            SettingsRoute.Style -> StyleSettingsScreen(state = state, onAction = onAction)
            SettingsRoute.Notifications -> NotificationSettingsScreen(state = state, onAction = onAction)
            // Only Ukrainian is shipped; the old placeholder page is folded into the main list
            SettingsRoute.Language -> MainSettingsList(state = state, onAction = onAction)
            SettingsRoute.Developers -> DeveloperSettingsScreen(onAction = onAction)
            SettingsRoute.Integrations -> IntegrationsScreen(onBack = { onAction(SettingsAction.GoBack) })
        }
    }
}

private fun themeSummary(state: SettingsState): String {
    val theme = when (state.colorTheme) {
        ColorTheme.System -> "Системна тема"
        ColorTheme.Light -> "Світла тема"
        ColorTheme.Dark -> "Темна тема"
        ColorTheme.Amoled -> "Чорна тема"
        ColorTheme.Contrast -> "Контрастна тема"
    }
    val extras = buildList {
        if (state.dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add("Material You")
        if (state.liquidGlass) add("Liquid Glass")
    }
    return (listOf(theme) + extras).joinToString(" · ")
}

@Composable
private fun MainSettingsList(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    SettingsPage(
        title = "Налаштування",
        onBack = { onAction(SettingsAction.GoBack) }
    ) {
        item(key = "hero") { AppIdentityCard() }

        settingsSection(
            "Вигляд",
            listOf(
                SettingsRow(
                    title = "Стиль та тема",
                    subtitle = themeSummary(state),
                    icon = Icons.Default.Palette,
                    accent = SettingsAccent.Primary,
                    onClick = { onAction(SettingsAction.Navigate(SettingsRoute.Style)) }
                ),
                SettingsRow(
                    title = "Розмір інтерфейсу",
                    subtitle = "Щільність списків і карток",
                    icon = Icons.Default.FormatSize,
                    trailing = SettingsTrailing.Value(
                        when (state.displayMode) {
                            DisplayMode.Compact -> "Щільний"
                            DisplayMode.Comfortable -> "Зручний"
                            DisplayMode.Spacious -> "Великий"
                        }
                    ),
                    onClick = { onAction(SettingsAction.Navigate(SettingsRoute.Style)) }
                )
            )
        )

        settingsSection(
            "Сповіщення",
            listOf(
                SettingsRow(
                    title = "Сповіщення",
                    subtitle = if (state.notifications.alertsEnabled) "Увімкнено" else "Вимкнено",
                    icon = Icons.Default.NotificationsActive,
                    accent = SettingsAccent.Tertiary,
                    onClick = { onAction(SettingsAction.Navigate(SettingsRoute.Notifications)) }
                )
            )
        )

        settingsSection(
            "Можливості",
            listOf(
                SettingsRow(
                    title = "Функції та інтеграції",
                    subtitle = "Android Auto, віджети, посилання",
                    icon = Icons.Default.Extension,
                    onClick = { onAction(SettingsAction.Navigate(SettingsRoute.Integrations)) }
                )
            )
        )

        settingsSection(
            "Дані",
            listOf(
                SettingsRow(
                    title = "Видалити дані",
                    subtitle = "Очистити кеш та збережені адреси",
                    icon = Icons.Default.DeleteForever,
                    accent = SettingsAccent.Error,
                    trailing = SettingsTrailing.None,
                    onClick = { showClearDialog = true }
                ),
                SettingsRow(
                    title = "Анонімна статистика",
                    subtitle = "Знеособлені дані про використання та збої допомагають покращувати застосунок. Адреси не передаються",
                    icon = Icons.Default.Analytics,
                    trailing = SettingsTrailing.Toggle(state.analyticsEnabled) { onAction(SettingsAction.SetAnalytics(it)) },
                    onClick = { onAction(SettingsAction.SetAnalytics(!state.analyticsEnabled)) }
                ),
                SettingsRow(
                    title = "Для розробників",
                    subtitle = "Демо-дані та діагностика",
                    icon = Icons.Default.Code,
                    accent = SettingsAccent.Neutral,
                    onClick = { onAction(SettingsAction.Navigate(SettingsRoute.Developers)) }
                )
            )
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Видалити всі дані?") },
            text = { Text("Усі збережені адреси та налаштування буде видалено. Цю дію не можна скасувати.") },
            confirmButton = {
                Button(
                    onClick = { showClearDialog = false; onAction(SettingsAction.ClearData) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Видалити") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Скасувати") } }
        )
    }
}

@Composable
fun DeveloperSettingsScreen(
    onAction: (SettingsAction) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
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
                                    val geocoder = Geocoder(context, Locale.forLanguageTag("uk-UA"))
                                    if (!Geocoder.isPresent()) {
                                        withContext(Dispatchers.Main) {
                                            gpsError = "Геокодування недоступне"
                                            isLoadingGps = false
                                        }
                                        return@launch
                                    }
                                    @Suppress("DEPRECATION")
                                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                    withContext(Dispatchers.Main) {
                                        if (!addresses.isNullOrEmpty()) {
                                            val address = addresses[0]
                                            val parts = listOfNotNull(
                                                address.adminArea?.takeIf { it.isNotEmpty() },
                                                address.thoroughfare?.takeIf { it.isNotEmpty() },
                                                address.featureName?.takeIf { it.isNotEmpty() }
                                            )
                                            gpsAddress = parts.joinToString(", ").ifEmpty { "Невідома адреса" }
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
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchLocation() else {
            gpsError = "Дозвіл відхилено"
            isLoadingGps = false
        }
    }

    val requestLocationWithPermission: () -> Unit = {
        val hasPermission = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (hasPermission) {
            fetchLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    SettingsPage(
        title = "Для розробників",
        subtitle = "Діагностика та демо-дані",
        onBack = { onAction(SettingsAction.GoBack) }
    ) {
        settingsSection(
            "Діагностика",
            listOf(
                SettingsRow(
                    title = "Адреса за GPS",
                    subtitle = gpsError ?: gpsAddress ?: "Визначити поточну адресу",
                    icon = Icons.Default.MyLocation,
                    accent = if (gpsError != null) SettingsAccent.Error else SettingsAccent.Primary,
                    trailing = when {
                        isLoadingGps -> SettingsTrailing.Progress
                        gpsAddress != null && gpsError == null -> SettingsTrailing.Action("Копіювати")
                        else -> SettingsTrailing.None
                    },
                    onClick = {
                        val address = gpsAddress
                        if (address != null && gpsError == null) {
                            clipboardManager.setText(AnnotatedString(address))
                        } else if (!isLoadingGps) {
                            requestLocationWithPermission()
                        }
                    }
                )
            )
        )

        settingsSection(
            "Демо-дані",
            listOf(
                SettingsRow(
                    title = "Додати демо-графік",
                    subtitle = "Реалістичний графік з відключеннями",
                    icon = Icons.Default.CalendarMonth,
                    accent = SettingsAccent.Tertiary,
                    trailing = SettingsTrailing.None,
                    onClick = { onAction(SettingsAction.AddPreviewLocation) }
                ),
                SettingsRow(
                    title = "Додати демо-локацію",
                    subtitle = "Статус змінюється щохвилини — для тестування сповіщень",
                    icon = Icons.Default.BugReport,
                    accent = SettingsAccent.Tertiary,
                    trailing = SettingsTrailing.None,
                    onClick = { onAction(SettingsAction.AddDemoLocation) }
                )
            )
        )

        settingsSection(
            "Скидання",
            listOf(
                SettingsRow(
                    title = "Скинути онбординг",
                    subtitle = "Показати привітання при наступному запуску",
                    icon = Icons.Default.SettingsBackupRestore,
                    accent = SettingsAccent.Neutral,
                    trailing = SettingsTrailing.None,
                    onClick = { onAction(SettingsAction.ResetSettings) }
                )
            )
        )
    }
}
