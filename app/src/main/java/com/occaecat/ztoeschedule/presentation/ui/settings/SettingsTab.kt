package com.occaecat.ztoeschedule.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.domain.notification.NotificationScheduler
import com.occaecat.ztoeschedule.domain.notification.PowerNotificationManager

/**
 * Settings tab with notification settings
 */
@Composable
fun SettingsTab(
    notificationsEnabled: Boolean = true,
    notificationAdvanceMinutes: Int = 15,
    onNotificationsEnabledChange: (Boolean) -> Unit = {},
    onNotificationAdvanceMinutesChange: (Int) -> Unit = {},
    onResetOnboarding: () -> Unit,
    onClearData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notificationManager = remember { PowerNotificationManager(context) }

    var showResetDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showAdvanceTimeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Налаштування",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Notification settings section
        Text(
            text = "Сповіщення",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Enable/disable notifications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Увімкнути сповіщення",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (notificationsEnabled) "Ви отримуватимете сповіщення" else "Вимкнено",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            onNotificationsEnabledChange(enabled)
                            if (enabled) {
                                NotificationScheduler.schedulePowerMonitoring(context)
                            } else {
                                NotificationScheduler.cancelPowerMonitoring(context)
                            }
                        }
                    )
                }

                HorizontalDivider()

                // Advance time selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Час до події",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "За $notificationAdvanceMinutes ${getMinutesLabel(notificationAdvanceMinutes)} до зміни",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(onClick = { showAdvanceTimeDialog = true }) {
                        Text("Змінити")
                    }
                }

                HorizontalDivider()

                // Test notification button
                Button(
                    onClick = { notificationManager.sendTestNotification() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Тестове сповіщення")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // App info section
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ZTOE Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Версія 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Дії",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        // Reset onboarding
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showResetDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Пройти налаштування знову",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Перезапустити процес вибору адреси",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Clear all data
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showClearDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Очистити всі дані",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Видалити збережену адресу та налаштування",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
