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
    onNotificationsEnabledChange: (Boolean) -> Unit = {},
    onNotificationAdvanceMinutesChange: (Int) -> Unit = {},
    onStatusNotificationEnabledChange: (Boolean) -> Unit = {},
    onLiveActivityEnabledChange: (Boolean) -> Unit = {},
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
    var localStatusNotification by remember { mutableStateOf(statusNotificationEnabled) }
    var localLiveActivity by remember { mutableStateOf(liveActivityEnabled) }
    var enableChangeNotifications by remember { mutableStateOf(notificationsEnabled) }

    // Sync local state with passed props
    LaunchedEffect(statusNotificationEnabled) {
        localStatusNotification = statusNotificationEnabled
    }
    LaunchedEffect(liveActivityEnabled) {
        localLiveActivity = liveActivityEnabled
    }
    LaunchedEffect(notificationsEnabled) {
        enableChangeNotifications = notificationsEnabled
    }

    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableChangeNotifications = true
            onNotificationsEnabledChange(true)
            NotificationScheduler.schedulePowerMonitoring(context)
        }
    }

    // Request permission helper
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enableChangeNotifications = true
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Notification settings section
        Text(
            text = "Сповіщення",
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
                    headlineContent = { Text("Постійне сповіщення статусу") },
                    supportingContent = { Text("Показувати поточний статус світла") },
                    leadingContent = { 
                        Icon(Icons.Outlined.Notifications, null) 
                    },
                    trailingContent = {
                        Switch(
                            checked = localStatusNotification,
                            onCheckedChange = { enabled ->
                                if (enabled && !notificationManager.hasNotificationPermission()) {
                                    requestNotificationPermission()
                                } else {
                                    localStatusNotification = enabled
                                    onStatusNotificationEnabledChange(enabled)
                                    if (enabled) {
                                        if (localLiveActivity) {
                                            com.occaecat.ztoeschedule.domain.notification.LiveActivityNotificationService.start(context)
                                        } else {
                                            StatusNotificationService.start(context)
                                        }
                                    } else {
                                        StatusNotificationService.stop(context)
                                        com.occaecat.ztoeschedule.domain.notification.LiveActivityNotificationService.stop(context)
                                    }
                                }
                            }
                        )
                    }
                )

                // Live Activity Toggle
                AnimatedVisibility(visible = localStatusNotification) {
                    ListItem(
                        headlineContent = { Text("Live Activity (Rich Style)") },
                        supportingContent = { Text("Розширений стиль сповіщення (Android 12+)") },
                        leadingContent = { Spacer(Modifier.width(24.dp)) },
                        trailingContent = {
                            Switch(
                                checked = localLiveActivity,
                                onCheckedChange = { enabled ->
                                    localLiveActivity = enabled
                                    onLiveActivityEnabledChange(enabled)
                                    if (enabled) {
                                        StatusNotificationService.stop(context)
                                        com.occaecat.ztoeschedule.domain.notification.LiveActivityNotificationService.start(context)
                                    } else {
                                        com.occaecat.ztoeschedule.domain.notification.LiveActivityNotificationService.stop(context)
                                        StatusNotificationService.start(context)
                                    }
                                },
                                modifier = Modifier.scale(0.85f)
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                // Change notifications
                ListItem(
                    headlineContent = { Text("Сповіщення про зміни") },
                    supportingContent = { 
                        Text(if (enableChangeNotifications) "Попереджати про вимкнення/увімкнення" else "Вимкнено") 
                    },
                    leadingContent = { Icon(Icons.Default.NotificationsActive, null) },
                    trailingContent = {
                        Switch(
                            checked = enableChangeNotifications,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (!notificationManager.hasNotificationPermission()) {
                                        requestNotificationPermission()
                                    } else {
                                        enableChangeNotifications = true
                                        onNotificationsEnabledChange(true)
                                        NotificationScheduler.schedulePowerMonitoring(context)
                                    }
                                } else {
                                    enableChangeNotifications = false
                                    onNotificationsEnabledChange(false)
                                    NotificationScheduler.cancelPowerMonitoring(context)
                                }
                            }
                        )
                    }
                )

                // Advance time selection
                AnimatedVisibility(visible = enableChangeNotifications) {
                    ListItem(
                        headlineContent = { Text("Попереджати за") },
                        supportingContent = { Text("$notificationAdvanceMinutes ${getMinutesLabel(notificationAdvanceMinutes)} до зміни") },
                        leadingContent = { Spacer(Modifier.width(24.dp)) },
                        trailingContent = {
                            TextButton(onClick = { showAdvanceTimeDialog = true }) {
                                Text("Змінити")
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                // Test notification button
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (!notificationManager.hasNotificationPermission()) {
                                requestNotificationPermission()
                            } else {
                                notificationManager.sendTestNotification()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Notifications, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Тестове сповіщення")
                    }
                }
            }
        }

        // Info and Actions
        Text(
            text = "Додаток",
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
                    headlineContent = { Text("ZTOE Schedule") },
                    supportingContent = { 
                        Text("Версія 1.0.0" + if(lastUpdateTime.isNotEmpty()) " • Оновлено: $lastUpdateTime" else "") 
                    },
                    leadingContent = { Icon(Icons.Default.Info, null) }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                ListItem(
                    headlineContent = { Text("Пройти налаштування знову") },
                    supportingContent = { Text("Перезапустити процес вибору адреси") },
                    leadingContent = { Icon(Icons.Default.SettingsBackupRestore, null) },
                    modifier = Modifier.clickable { showResetDialog = true }
                )

                ListItem(
                    headlineContent = { 
                        Text("Очистити всі дані", color = MaterialTheme.colorScheme.error) 
                    },
                    supportingContent = { Text("Видалити збережені адреси та налаштування") },
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
