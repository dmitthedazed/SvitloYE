package com.occaecat.ztoeschedule.presentation.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.domain.notification.NotificationScheduler
import com.occaecat.ztoeschedule.domain.notification.PowerNotificationManager
import com.occaecat.ztoeschedule.domain.notification.StatusNotificationService

import com.occaecat.ztoeschedule.data.model.SmartNotificationSettings
import com.occaecat.ztoeschedule.data.model.PriorityMode

/**
 * Settings tab with notification settings
 */
@Composable
fun SettingsTab(
    notificationsEnabled: Boolean = true,
    notificationAdvanceMinutes: Int = 15,
    statusNotificationEnabled: Boolean = false,
    liveActivityEnabled: Boolean = false,
    lastUpdateTime: String = "",
    displayMode: DisplayMode = DisplayMode.COMFORTABLE,
    colorTheme: ColorTheme = ColorTheme.SYSTEM,
    fontScale: FontScale = FontScale.NORMAL,
    smartNotificationSettings: SmartNotificationSettings = SmartNotificationSettings(),
    onNotificationsEnabledChange: (Boolean) -> Unit = {},
    onNotificationAdvanceMinutesChange: (Int) -> Unit = {},
    onStatusNotificationEnabledChange: (Boolean) -> Unit = {},
    onLiveActivityEnabledChange: (Boolean) -> Unit = {},
    onDisplayModeChange: (DisplayMode) -> Unit = {},
    onColorThemeChange: (ColorTheme) -> Unit = {},
    onFontScaleChange: (FontScale) -> Unit = {},
    onSmartNotificationSettingsChange: (SmartNotificationSettings) -> Unit = {},
    onResetOnboarding: () -> Unit,
    onClearData: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val notificationManager = remember { PowerNotificationManager(context) }
    val scrollState = rememberScrollState()

    var showResetDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showAdvanceTimeDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDisplayModeDialog by remember { mutableStateOf(false) }
    var showFontScaleDialog by remember { mutableStateOf(false) }
    var showSmartSettingsDialog by remember { mutableStateOf(false) }
    
    var localStatusNotification by remember { mutableStateOf(statusNotificationEnabled) }
    var localLiveActivity by remember { mutableStateOf(liveActivityEnabled) }
    var enableChangeNotifications by remember { mutableStateOf(notificationsEnabled) }

    // ... (rest of sync logic)

    // ... (rest of notification permission logic)

    // ... (rest of Column)

        // Notification settings section
        Text(
            text = stringResource(R.string.settings_notifications_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column {
                // Static status notification
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_status_notif)) },
                    supportingContent = { Text(stringResource(R.string.settings_status_notif_desc)) },
                    leadingContent = { 
                        Icon(Icons.Outlined.Notifications, null) 
                    },
                    trailingContent = {
                        Switch(
                            checked = localStatusNotification,
                            onCheckedChange = { enabled ->
                                // ... (existing logic)
                            }
                        )
                    }
                )

                // ... (Live Activity)

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                // Smart Notifications (Quiet Hours & Priority)
                ListItem(
                    headlineContent = { Text("Розумні сповіщення") },
                    supportingContent = { 
                        Text("Тихі години: ${smartNotificationSettings.quietHoursStart}:00 - ${smartNotificationSettings.quietHoursEnd}:00") 
                    },
                    leadingContent = { Icon(Icons.Default.Nightlight, null) },
                    modifier = Modifier.clickable { showSmartSettingsDialog = true }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                // ... (Change notifications & Advance time)
            }
        }

    // ... (Rest of UI)

    // Smart Settings Dialog
    if (showSmartSettingsDialog) {
        var startHour by remember { mutableIntStateOf(smartNotificationSettings.quietHoursStart) }
        var endHour by remember { mutableIntStateOf(smartNotificationSettings.quietHoursEnd) }
        var selectedPriority by remember { mutableStateOf(smartNotificationSettings.priorityMode) }

        AlertDialog(
            onDismissRequest = { showSmartSettingsDialog = false },
            title = { Text("Налаштування сповіщень") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Тихі години (не турбувати)", style = MaterialTheme.typography.titleSmall)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Початок", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { if (startHour < 23) startHour++ else startHour = 0 }) {
                                Text("${startHour}:00")
                            }
                        }
                        Text("-")
                        Column {
                            Text("Кінець", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { if (endHour < 23) endHour++ else endHour = 0 }) {
                                Text("${endHour}:00")
                            }
                        }
                    }

                    HorizontalDivider()

                    Text("Режим пріоритету", style = MaterialTheme.typography.titleSmall)
                    PriorityMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPriority = mode }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPriority == mode,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(getPriorityModeLabel(mode), style = MaterialTheme.typography.bodyMedium)
                                Text(getPriorityModeDescription(mode), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSmartNotificationSettingsChange(
                            smartNotificationSettings.copy(
                                quietHoursStart = startHour,
                                quietHoursEnd = endHour,
                                priorityMode = selectedPriority
                            )
                        )
                        showSmartSettingsDialog = false
                    }
                ) {
                    Text("Зберегти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmartSettingsDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

// ... (Existing helpers)

private fun getPriorityModeLabel(mode: PriorityMode): String {
    return when (mode) {
        PriorityMode.ALL -> "Всі сповіщення"
        PriorityMode.SMART -> "Розумний режим"
        PriorityMode.CRITICAL_ONLY -> "Тільки відключення"
        PriorityMode.SILENT -> "Без звуку"
    }
}

private fun getPriorityModeDescription(mode: PriorityMode): String {
    return when (mode) {
        PriorityMode.ALL -> "Сповіщати про всі зміни"
        PriorityMode.SMART -> "Ігнорувати повторні нагадування"
        PriorityMode.CRITICAL_ONLY -> "Тільки коли зникає світло"
        PriorityMode.SILENT -> "Тільки оновлення в шторці"
    }
}

        // Info and Actions
        Text(
            text = stringResource(R.string.settings_app_section),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("СвітлоЄ? Житомир") },
                    supportingContent = { 
                        Text("Версія 1.0.0" + if(lastUpdateTime.isNotEmpty()) " • " + stringResource(R.string.home_last_updated, lastUpdateTime) else "") 
                    },
                    leadingContent = { Icon(Icons.Default.Info, null) }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_re_setup)) },
                    supportingContent = { Text(stringResource(R.string.settings_re_setup_desc)) },
                    leadingContent = { Icon(Icons.Default.SettingsBackupRestore, null) },
                    modifier = Modifier.clickable { showResetDialog = true }
                )

                ListItem(
                    headlineContent = { 
                        Text(stringResource(R.string.settings_clear_data), color = MaterialTheme.colorScheme.error) 
                    },
                    supportingContent = { Text(stringResource(R.string.settings_clear_data_desc)) },
                    leadingContent = { 
                        Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) 
                    },
                    modifier = Modifier.clickable { showClearDialog = true }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }

    // Reset onboarding dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Пройти налаштування знову?") },
            text = { Text("Ви зможете вибрати нову адресу. Поточна адреса залишиться збереженою.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetOnboarding()
                    }
                ) {
                    Text("Так")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }

    // Clear data dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Очистити всі дані?") },
            text = { Text("Це видалить збережену адресу та всі налаштування. Дію неможливо скасувати.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearData()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }

    // Advance time selection dialog
    if (showAdvanceTimeDialog) {
        val timeOptions = listOf(5, 10, 15, 30, 60)

        AlertDialog(
            onDismissRequest = { showAdvanceTimeDialog = false },
            title = { Text("Попереджати за скільки хвилин?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    timeOptions.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = notificationAdvanceMinutes == minutes,
                                onClick = {
                                    onNotificationAdvanceMinutesChange(minutes)
                                    showAdvanceTimeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$minutes ${getMinutesLabel(minutes)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAdvanceTimeDialog = false }) {
                    Text("Закрити")
                }
            }
        )
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Оберіть тему") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorTheme.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onColorThemeChange(theme)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = colorTheme == theme,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = getColorThemeLabel(theme),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }

    // Display Mode Selection Dialog
    if (showDisplayModeDialog) {
        AlertDialog(
            onDismissRequest = { showDisplayModeDialog = false },
            title = { Text("Режим відображення") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DisplayMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDisplayModeChange(mode)
                                    showDisplayModeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = displayMode == mode,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = getDisplayModeLabel(mode),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = getDisplayModeDescription(mode),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisplayModeDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }

    // Font Scale Selection Dialog
    if (showFontScaleDialog) {
        AlertDialog(
            onDismissRequest = { showFontScaleDialog = false },
            title = { Text("Розмір шрифту") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FontScale.entries.forEach { scale ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onFontScaleChange(scale)
                                    showFontScaleDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = fontScale == scale,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = getFontScaleLabel(scale),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * scale.multiplier
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontScaleDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

/**
 * Helper function to get proper Ukrainian word form for minutes
 */
private fun getMinutesLabel(minutes: Int): String {
    return when {
        minutes == 1 -> "хвилину"
        minutes in 2..4 -> "хвилини"
        minutes % 10 == 1 && minutes % 100 != 11 -> "хвилину"
        minutes % 10 in 2..4 && minutes % 100 !in 12..14 -> "хвилини"
        else -> "хвилин"
    }
}

private fun getColorThemeLabel(theme: ColorTheme): String {
    return when (theme) {
        ColorTheme.SYSTEM -> "Системна"
        ColorTheme.LIGHT -> "Світла"
        ColorTheme.DARK -> "Темна"
        ColorTheme.AMOLED -> "AMOLED (Чорна)"
        ColorTheme.CONTRAST -> "Високий контраст"
    }
}

private fun getDisplayModeLabel(mode: DisplayMode): String {
    return when (mode) {
        DisplayMode.COMPACT -> "Щільний"
        DisplayMode.COMFORTABLE -> "Звичайний"
        DisplayMode.SPACIOUS -> "Просторий"
    }
}

private fun getFontScaleLabel(scale: FontScale): String {
    return when (scale) {
        FontScale.SMALL -> "Маленький"
        FontScale.NORMAL -> "Звичайний"
        FontScale.LARGE -> "Великий"
        FontScale.XLARGE -> "Дуже великий"
        FontScale.ACCESSIBILITY -> "Максимальний"
    }
}

private fun getDisplayModeDescription(mode: DisplayMode): String {
    return when (mode) {
        DisplayMode.COMPACT -> "Більше інформації на екрані"
        DisplayMode.COMFORTABLE -> "Збалансований вигляд"
        DisplayMode.SPACIOUS -> "Великі елементи для зручності"
    }
}
