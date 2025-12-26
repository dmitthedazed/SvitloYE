package com.occaecat.ztoeschedule.presentation.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.data.model.FontScale
import com.occaecat.ztoeschedule.data.model.PriorityMode
import com.occaecat.ztoeschedule.data.model.SmartNotificationSettings
import com.occaecat.ztoeschedule.domain.notification.NotificationScheduler
import com.occaecat.ztoeschedule.domain.notification.PowerNotificationManager
import com.occaecat.ztoeschedule.domain.notification.StatusNotificationService

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
    notificationMode: Int = 0, // 0: Все, 1: Тільки важливо, 2: Тихо
    onNotificationsEnabledChange: (Boolean) -> Unit = {},
    onNotificationAdvanceMinutesChange: (Int) -> Unit = {},
    onStatusNotificationEnabledChange: (Boolean) -> Unit = {},
    onLiveActivityEnabledChange: (Boolean) -> Unit = {},
    onDisplayModeChange: (DisplayMode) -> Unit = {},
    onColorThemeChange: (ColorTheme) -> Unit = {},
    onFontScaleChange: (FontScale) -> Unit = {},
    onSmartNotificationSettingsChange: (SmartNotificationSettings) -> Unit = {},
    onNotificationModeChange: (Int) -> Unit = {},
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
    var enableChangeNotifications by remember { mutableStateOf(notificationsEnabled) }

    LaunchedEffect(statusNotificationEnabled) { localStatusNotification = statusNotificationEnabled }
    LaunchedEffect(notificationsEnabled) { enableChangeNotifications = notificationsEnabled }

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
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 1. ПЕРСОНАЛІЗАЦІЯ ---
        SectionHeader("Стиль та вигляд")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column {
                SettingsListItem(Icons.Default.Palette, "Кольори", getColorThemeLabel(colorTheme), MaterialTheme.colorScheme.secondaryContainer) { showThemeDialog = true }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsListItem(Icons.Default.TextFields, "Розмір тексту", getFontScaleLabel(fontScale), MaterialTheme.colorScheme.secondaryContainer) { showFontScaleDialog = true }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsListItem(Icons.Default.ViewAgenda, "Масштаб меню", getDisplayModeLabel(displayMode), MaterialTheme.colorScheme.secondaryContainer) { showDisplayModeDialog = true }
            }
        }

        // --- 2. СПОВІЩЕННЯ ---
        SectionHeader("Сповіщення")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                // Main Switch
                ListItem(
                    headlineContent = { Text("Отримувати сповіщення", fontWeight = FontWeight.Bold) },
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
                                if (enabled && !notificationManager.hasNotificationPermission()) requestNotificationPermission()
                                else onNotificationsEnabledChange(enabled)
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                if (enableChangeNotifications) {
                    HorizontalDivider(Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    Text("Які сповіщення надсилати?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                    
                    // Simple Mode Selector
                    NotificationModeCard(0, "Все підряд", "Усі зміни та нагадування", notificationMode == 0) { onNotificationModeChange(0) }
                    NotificationModeCard(1, "Тільки важливо", "Тільки коли вимикають світло", notificationMode == 1) { onNotificationModeChange(1) }
                    NotificationModeCard(2, "Тихий режим", "Тільки оновлення в шторці", notificationMode == 2) { onNotificationModeChange(2) }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    SettingsListItem(Icons.Default.Timer, "Попереджати за", "$notificationAdvanceMinutes хв", MaterialTheme.colorScheme.primaryContainer) { showAdvanceTimeDialog = true }
                    SettingsListItem(Icons.Default.Nightlight, "Тихі години", "${smartNotificationSettings.quietHoursStart}:00 - ${smartNotificationSettings.quietHoursEnd}:00", MaterialTheme.colorScheme.primaryContainer) { showSmartSettingsDialog = true }
                }
            }
        }

        // --- 3. ПОСТІЙНИЙ СТАТУС ---
        SectionHeader("Статус у шторці")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("Постійний статус", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Бачити стан світла не відкриваючи додаток") },
                    leadingContent = { 
                         Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = MaterialTheme.colorScheme.onTertiaryContainer) }
                        }
                    },
                    trailingContent = {
                        Switch(checked = localStatusNotification, onCheckedChange = { onStatusNotificationEnabledChange(it) })
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                if (localStatusNotification) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ListItem(
                        headlineContent = { Text("Live Activity") },
                        supportingContent = { Text("Більше інформації та таймер (Android 13+)") },
                        leadingContent = { 
                             Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Widgets, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        },
                        trailingContent = { Switch(checked = liveActivityEnabled, onCheckedChange = onLiveActivityEnabledChange, modifier = Modifier.scale(0.85f)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        // --- 4. ДОДАТОК ---
        SectionHeader("Додаток")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column {
                SettingsListItem(Icons.Default.Info, "СвітлоЄ? Житомир", "Версія 1.1.0", MaterialTheme.colorScheme.surfaceVariant) {}
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsListItem(Icons.Default.SettingsBackupRestore, "Почати налаштування знову", "Для зміни адреси", MaterialTheme.colorScheme.surfaceVariant) { showResetDialog = true }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsListItem(Icons.Default.DeleteForever, "Видалити всі дані", "Очистити пам'ять", MaterialTheme.colorScheme.errorContainer) { showClearDialog = true }
            }
        }
        
        Spacer(Modifier.height(64.dp))
    }

    // --- DIALOGS (same logic, simplified UI) ---
    if (showResetDialog) AlertDialog(onDismissRequest = { showResetDialog = false }, title = { Text("Скинути налаштування?") }, confirmButton = { TextButton(onClick = { showResetDialog = false; onResetOnboarding() }) { Text("Так") } }, dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Скасувати") } })
    if (showClearDialog) AlertDialog(onDismissRequest = { showClearDialog = false }, title = { Text("Видалити все?") }, confirmButton = { TextButton(onClick = { showClearDialog = false; onClearData() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("ВИДАЛИТИ") } })
    if (showAdvanceTimeDialog) {
        val options = listOf(5, 10, 15, 30, 60)
        AlertDialog(onDismissRequest = { showAdvanceTimeDialog = false }, title = { Text("За скільки хвилин?") }, text = { Column { options.forEach { min -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onNotificationAdvanceMinutesChange(min); showAdvanceTimeDialog = false }.padding(12.dp)) { RadioButton(selected = notificationAdvanceMinutes == min, onClick = null); Text("$min хвилин", modifier = Modifier.padding(start = 8.dp)) } } } }, confirmButton = {})
    }
    if (showThemeDialog) AlertDialog(onDismissRequest = { showThemeDialog = false }, title = { Text("Тема") }, text = { Column { ColorTheme.entries.forEach { t -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onColorThemeChange(t); showThemeDialog = false }.padding(12.dp)) { RadioButton(selected = colorTheme == t, onClick = null); Text(getColorThemeLabel(t), modifier = Modifier.padding(start = 8.dp)) } } } }, confirmButton = {})
    if (showDisplayModeDialog) AlertDialog(onDismissRequest = { showDisplayModeDialog = false }, title = { Text("Масштаб") }, text = { Column { DisplayMode.entries.forEach { m -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onDisplayModeChange(m); showDisplayModeDialog = false }.padding(12.dp)) { RadioButton(selected = displayMode == m, onClick = null); Text(getDisplayModeLabel(m), modifier = Modifier.padding(start = 8.dp)) } } } }, confirmButton = {})
    if (showFontScaleDialog) AlertDialog(onDismissRequest = { showFontScaleDialog = false }, title = { Text("Розмір тексту") }, text = { Column { FontScale.entries.forEach { s -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onFontScaleChange(s); showFontScaleDialog = false }.padding(12.dp)) { RadioButton(selected = fontScale == s, onClick = null); Text(getFontScaleLabel(s), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize * s.multiplier)) } } } }, confirmButton = {})
    if (showSmartSettingsDialog) {
        var startHour by remember { mutableIntStateOf(smartNotificationSettings.quietHoursStart) }
        var endHour by remember { mutableIntStateOf(smartNotificationSettings.quietHoursEnd) }
        AlertDialog(onDismissRequest = { showSmartSettingsDialog = false }, title = { Text("Тихі години") }, text = { Column { Text("Додаток не буде турбувати вас у цей час:"); Spacer(Modifier.height(16.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("З"); TextButton(onClick = { startHour = (startHour + 1) % 24 }) { Text("${startHour}:00", style = MaterialTheme.typography.headlineMedium) } }; Text("—", modifier = Modifier.padding(top = 24.dp)); Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("До"); TextButton(onClick = { endHour = (endHour + 1) % 24 }) { Text("${endHour}:00", style = MaterialTheme.typography.headlineMedium) } } } } }, confirmButton = { TextButton(onClick = { onSmartNotificationSettingsChange(smartNotificationSettings.copy(quietHoursStart = startHour, quietHoursEnd = endHour)); showSmartSettingsDialog = false }) { Text("Зберегти") } })
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
}

@Composable
private fun SettingsListItem(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle) },
        leadingContent = { 
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)) }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun NotificationModeCard(id: Int, title: String, desc: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isSelected, onClick = null)
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun getColorThemeLabel(t: ColorTheme) = when(t) { ColorTheme.SYSTEM -> "Системна"; ColorTheme.LIGHT -> "Світла"; ColorTheme.DARK -> "Темна"; ColorTheme.AMOLED -> "AMOLED"; ColorTheme.CONTRAST -> "Контрастна" }
private fun getFontScaleLabel(s: FontScale) = when(s) { FontScale.SMALL -> "Дрібний"; FontScale.NORMAL -> "Стандарт"; FontScale.LARGE -> "Великий"; FontScale.XLARGE -> "Дуже великий"; FontScale.ACCESSIBILITY -> "Максимальний" }
private fun getDisplayModeLabel(m: DisplayMode) = when(m) { DisplayMode.COMPACT -> "Щільний"; DisplayMode.COMFORTABLE -> "Зручний"; DisplayMode.SPACIOUS -> "Великий" }