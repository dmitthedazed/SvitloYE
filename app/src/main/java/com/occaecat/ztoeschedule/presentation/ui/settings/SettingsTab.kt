package com.occaecat.ztoeschedule.presentation.ui.settings

import android.Manifest
import android.os.Build
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
    displayMode: DisplayMode = DisplayMode.Comfortable,
    colorTheme: ColorTheme = ColorTheme.System,
    fontScale: FontScale = FontScale.Normal,
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
    var showThemeDialog by remember { mutableStateOf(false) }
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
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
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
                            },
                            thumbContent = if (enableChangeNotifications) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                            } else null
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                if (enableChangeNotifications) {
                    HorizontalDivider(Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text("Які сповіщення надсилати?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                    NotificationModeCard(0, "Все підряд", "Усі зміни та нагадування", notificationMode == 0) { onNotificationModeChange(0) }
                    NotificationModeCard(1, "Тільки важливо", "Тільки коли вимикають світло", notificationMode == 1) { onNotificationModeChange(1) }
                    NotificationModeCard(2, "Тихий режим", "Тільки оновлення в шторці", notificationMode == 2) { onNotificationModeChange(2) }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Попереджати за", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = "$notificationAdvanceMinutes хв", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(value = notificationAdvanceMinutes.toFloat(), onValueChange = { onNotificationAdvanceMinutesChange(it.toInt()) }, valueRange = 0f..60f, steps = 11, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant))
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsListItem(Icons.Default.Nightlight, "Тихі години", "${smartNotificationSettings.quietHoursStart}:00 - ${smartNotificationSettings.quietHoursEnd}:00", MaterialTheme.colorScheme.primaryContainer) { showSmartSettingsDialog = true }
                }
            }
        }

        // --- 3. ПОСТІЙНИЙ СТАТУС ---
        SectionHeader("Статус у шторці")
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column {
                ListItem(
                    headlineContent = { Text("Постійний статус", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Бачити стан світла не відкриваючи додаток") },
                    leadingContent = { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = MaterialTheme.colorScheme.onTertiaryContainer) } } },
                    trailingContent = { Switch(checked = localStatusNotification, onCheckedChange = { onStatusNotificationEnabledChange(it) }, thumbContent = if (localStatusNotification) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } } else null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                AnimatedVisibility(visible = localStatusNotification, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column {
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ListItem(
                            headlineContent = { Text("Live Activity") },
                            supportingContent = { Text("Більше інформації та таймер (Android 13+)") },
                            leadingContent = { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Widgets, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } } },
                            trailingContent = { Switch(checked = liveActivityEnabled, onCheckedChange = onLiveActivityEnabledChange, modifier = Modifier.scale(0.85f), thumbContent = if (liveActivityEnabled) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } } else null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }

        SectionHeader("Додаток")
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
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

    if (showResetDialog) { AlertDialog(onDismissRequest = { showResetDialog = false }, icon = { Icon(Icons.Default.SettingsBackupRestore, null) }, title = { Text("Скинути налаштування?") }, text = { Text("Додаток повернеться до початкового стану.") }, confirmButton = { TextButton(onClick = { showResetDialog = false; onResetOnboarding() }) { Text("Скинути") } }, dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Скасувати") } }) }
    if (showClearDialog) { AlertDialog(onDismissRequest = { showClearDialog = false }, icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) }, title = { Text("Видалити всі дані?") }, text = { Text("Всі збережені адреси та налаштування будуть видалені.") }, confirmButton = { Button(onClick = { showClearDialog = false; onClearData() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("ВИДАЛИТИ ВСЕ") } }, dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Скасувати") } }) }
    if (showThemeDialog) { AlertDialog(onDismissRequest = { showThemeDialog = false }, title = { Text("Тема") }, text = { Column(Modifier.selectableGroup()) { ColorTheme.entries.forEach { t -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().selectable(selected = (colorTheme == t), onClick = { onColorThemeChange(t); showThemeDialog = false }, role = Role.RadioButton).padding(12.dp)) { RadioButton(selected = colorTheme == t, onClick = null); Text(getColorThemeLabel(t), modifier = Modifier.padding(start = 8.dp)) } } } }, confirmButton = {}) }
    if (showFontScaleDialog) { AlertDialog(onDismissRequest = { showFontScaleDialog = false }, title = { Text("Розмір тексту") }, text = { Column(Modifier.selectableGroup()) { FontScale.entries.forEach { s -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().selectable(selected = (fontScale == s), onClick = { onFontScaleChange(s); showFontScaleDialog = false }, role = Role.RadioButton).padding(12.dp)) { RadioButton(selected = fontScale == s, onClick = null); Text(getFontScaleLabel(s), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize * s.multiplier)) } } } }, confirmButton = {}) }
    if (showSmartSettingsDialog) {
        var range by remember { mutableStateOf(smartNotificationSettings.quietHoursStart.toFloat()..(if (smartNotificationSettings.quietHoursEnd < smartNotificationSettings.quietHoursStart) smartNotificationSettings.quietHoursEnd + 24f else smartNotificationSettings.quietHoursEnd.toFloat())) }
        var isWorkdayOnly by remember { mutableStateOf(smartNotificationSettings.workdayMode) }
        AlertDialog(
            onDismissRequest = { showSmartSettingsDialog = false }, title = { Text("Тихі години") },
            text = { Column { Text("Виберіть період без звуку:"); Spacer(Modifier.height(24.dp)); val start = range.start.toInt() % 24; val end = range.endInclusive.toInt() % 24; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${start}:00", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Icon(Icons.Default.ArrowForward, null, modifier = Modifier.padding(top = 4.dp), tint = MaterialTheme.colorScheme.outline); Text("${end}:00", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.height(8.dp)); RangeSlider(value = range, onValueChange = { range = it }, valueRange = 0f..24f, steps = 23, colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant)); Text(text = "Тривалість: ${range.endInclusive.toInt() - range.start.toInt()} год", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(24.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)); Spacer(Modifier.height(16.dp)); Row(Modifier.fillMaxWidth().clickable { isWorkdayOnly = !isWorkdayOnly }, verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = isWorkdayOnly, onCheckedChange = { isWorkdayOnly = it }); Text(text = "Тільки у будні", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp)) }; Text(text = "У вихідні сповіщення приходитимуть без обмежень", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 48.dp)) } },
            confirmButton = { TextButton(onClick = { onSmartNotificationSettingsChange(smartNotificationSettings.copy(quietHoursStart = range.start.toInt() % 24, quietHoursEnd = range.endInclusive.toInt() % 24, workdayMode = isWorkdayOnly)); showSmartSettingsDialog = false }) { Text("Зберегти") } }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
}

@Composable
private fun SettingsListItem(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) }, supportingContent = { Text(subtitle) },
        leadingContent = { Surface(shape = CircleShape, color = color, modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)) } } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent), modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun NotificationModeCard(id: Int, title: String, desc: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick() },
        shape = MaterialTheme.shapes.medium, color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = isSelected, onClick = null); Column(Modifier.padding(start = 12.dp)) { Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold); Text(desc, style = MaterialTheme.typography.bodySmall) } }
    }
}

private fun getColorThemeLabel(t: ColorTheme) = when(t) { ColorTheme.System -> "Системна"; ColorTheme.Light -> "Світла"; ColorTheme.Dark -> "Темна"; ColorTheme.Amoled -> "AMOLED"; ColorTheme.Contrast -> "Контрастна" }
private fun getFontScaleLabel(s: FontScale) = when(s) { FontScale.Small -> "Дрібний"; FontScale.Normal -> "Стандарт"; FontScale.Large -> "Великий"; FontScale.Xlarge -> "Дуже великий"; FontScale.Accessibility -> "Максимальний" }
private fun getDisplayModeLabel(m: DisplayMode) = when(m) { DisplayMode.Compact -> "Щільний"; DisplayMode.Comfortable -> "Зручний"; DisplayMode.Spacious -> "Великий" }