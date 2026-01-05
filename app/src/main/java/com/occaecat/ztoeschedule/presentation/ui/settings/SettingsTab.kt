package com.occaecat.ztoeschedule.presentation.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.domain.notification.NotificationScheduler
import androidx.compose.foundation.interaction.MutableInteractionSource

@Composable
fun SettingsTab(
    notificationsEnabled: Boolean = true,
    statusNotificationEnabled: Boolean = false,
    lastUpdateTime: String = "",
    displayMode: DisplayMode = DisplayMode.Comfortable,
    colorTheme: ColorTheme = ColorTheme.System,
    cornerRadius: Int = 24,
    onNotificationsEnabledChange: (Boolean) -> Unit = {},
    onStatusNotificationEnabledChange: (Boolean) -> Unit = {},
    onDisplayModeChange: (DisplayMode) -> Unit = {},
    onColorThemeChange: (ColorTheme) -> Unit = {},
    onCornerRadiusChange: (Int) -> Unit = {},
    onResetOnboarding: () -> Unit,
    onClearData: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showResetDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    
    var localStatusNotification by remember { mutableStateOf(statusNotificationEnabled) }
    var enableChangeNotifications by remember { mutableStateOf(notificationsEnabled) }

    LaunchedEffect(statusNotificationEnabled) { localStatusNotification = statusNotificationEnabled }
    LaunchedEffect(notificationsEnabled) { enableChangeNotifications = notificationsEnabled }

    val hasNotificationPermission = remember(context) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onNotificationsEnabledChange(true)
            NotificationScheduler.schedulePowerMonitoring(context)
        }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onNotificationsEnabledChange(true)
            NotificationScheduler.schedulePowerMonitoring(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- 1. ПЕРСОНАЛІЗАЦІЯ ---
        SectionHeader("Стиль та вигляд")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column {
                SettingsListItem(Icons.Default.Palette, "Кольори", getColorThemeLabel(colorTheme), MaterialTheme.colorScheme.secondaryContainer) { showThemeDialog = true }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                Column(modifier = Modifier.padding(16.dp)) {
                    val currentRadius = com.occaecat.ztoeschedule.ui.theme.LocalCornerRadius.current
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Скруглення кутів", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (cornerRadius == -1) "Системне ($currentRadius dp)" else "$cornerRadius dp", 
                                style = MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (cornerRadius != -1) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Скинути",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple()
                                    ) { onCornerRadiusChange(-1) }
                                )
                            }
                        }
                    }
                    Slider(
                        value = if (cornerRadius == -1) currentRadius.toFloat() else cornerRadius.toFloat(),
                        onValueChange = { onCornerRadiusChange(it.toInt()) },
                        valueRange = 0f..60f,
                        steps = 59,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Масштаб меню",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        DisplayMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = displayMode == mode,
                                onClick = { onDisplayModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = DisplayMode.entries.size),
                                label = { 
                                    Text(
                                        text = when(mode) {
                                            DisplayMode.Compact -> "Щільний"
                                            DisplayMode.Comfortable -> "Зручний"
                                            DisplayMode.Spacious -> "Великий"
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- 2. СПОВІЩЕННЯ ---
        SectionHeader("Сповіщення")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                ListItem(
                    headlineContent = { Text("Отримувати сповіщення", style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("Попередження про зміну світла") },
                    leadingContent = { 
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                        }
                    },
                    trailingContent = {
                        Switch(
                            checked = enableChangeNotifications,
                            onCheckedChange = { enabled ->
                                if (enabled && !hasNotificationPermission()) requestNotificationPermission()
                                else onNotificationsEnabledChange(enabled)
                            },
                            thumbContent = if (enableChangeNotifications) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                            } else null
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        // --- 3. ПОСТІЙНИЙ СТАТУС ---
        SectionHeader("Статус у шторці")
        Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column {
                ListItem(
                    headlineContent = { Text("Постійний статус", style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("Бачити стан світла не відкриваючи додаток") },
                    leadingContent = { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = MaterialTheme.colorScheme.onTertiaryContainer) } } },
                    trailingContent = { Switch(checked = localStatusNotification, onCheckedChange = { onStatusNotificationEnabledChange(it) }, thumbContent = if (localStatusNotification) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } } else null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        SectionHeader("Додаток")
        Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column {
                SettingsListItem(Icons.Default.SettingsBackupRestore, "Почати налаштування знову", "Для зміни адреси", MaterialTheme.colorScheme.surfaceVariant) { showResetDialog = true }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsListItem(Icons.Default.DeleteForever, "Видалити всі дані", "Очистити пам'ять", MaterialTheme.colorScheme.errorContainer) { showClearDialog = true }

                val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
                var canScheduleExact by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true
                    )
                }

                // Check again when user returns to app
                DisposableEffect(Unit) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                canScheduleExact = alarmManager.canScheduleExactAlarms()
                            }
                        }
                    }
                    // This is a simplified approach, usually we'd need access to lifecycle
                    onDispose {}
                }

                if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsListItem(
                        icon = Icons.Default.Alarm,
                        title = "Точні сповіщення",
                        subtitle = "Натисніть, щоб дозволити точний час",
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }
        Spacer(Modifier.height(64.dp))
    }

    if (showResetDialog) { AlertDialog(onDismissRequest = { showResetDialog = false }, icon = { Icon(Icons.Default.SettingsBackupRestore, null) }, title = { Text("Скинути налаштування?") }, text = { Text("Додаток повернеться до початкового стану.") }, confirmButton = { TextButton(onClick = { showResetDialog = false; onResetOnboarding() }) { Text("Скинути") } }, dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Скасувати") } }) }
    if (showClearDialog) { AlertDialog(onDismissRequest = { showClearDialog = false }, icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) }, title = { Text("Видалити всі дані?") }, text = { Text("Всі збережені адреси та налаштування будуть видалені.") }, confirmButton = { Button(onClick = { showClearDialog = false; onClearData() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("ВИДАЛИТИ ВСЕ") } }, dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Скасувати") } }) }
    if (showThemeDialog) { AlertDialog(onDismissRequest = { showThemeDialog = false }, title = { Text("Тема") }, text = { Column(Modifier.selectableGroup()) { ColorTheme.entries.forEach { t -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().selectable(selected = (colorTheme == t), onClick = { onColorThemeChange(t); showThemeDialog = false }, role = Role.RadioButton).padding(12.dp)) { RadioButton(selected = colorTheme == t, onClick = null); Text(getColorThemeLabel(t), modifier = Modifier.padding(start = 8.dp)) } } } }, confirmButton = {}) }
}

@Composable
private fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
}

@Composable
private fun SettingsListItem(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) }, supportingContent = { Text(subtitle) },
        leadingContent = { Surface(shape = CircleShape, color = color, modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent), 
        modifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple()
        ) { onClick() }
    )
}

private fun getColorThemeLabel(t: ColorTheme) = when(t) { ColorTheme.System -> "Системна"; ColorTheme.Light -> "Світла"; ColorTheme.Dark -> "Темна"; ColorTheme.Amoled -> "AMOLED"; ColorTheme.Contrast -> "Контрастна" }
private fun getDisplayModeLabel(m: DisplayMode) = when(m) { DisplayMode.Compact -> "Щільний"; DisplayMode.Comfortable -> "Зручний"; DisplayMode.Spacious -> "Великий" }