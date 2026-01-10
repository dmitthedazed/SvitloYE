@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.occaecat.ztoeschedule.presentation.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.occaecat.ztoeschedule.domain.notification.NotificationScheduler
import com.occaecat.ztoeschedule.presentation.ui.components.SettingsGroupItem

import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationSettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Permission handling
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
            onAction(SettingsAction.SetNotificationsEnabled(true))
            NotificationScheduler.schedulePowerMonitoring(context)
        }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onAction(SettingsAction.SetNotificationsEnabled(true))
            NotificationScheduler.schedulePowerMonitoring(context)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Сповіщення") },
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsAction.GoBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Notifications
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingsGroupItem(
                    index = 0,
                    totalCount = 1,
                    headlineContent = { Text("Сповіщення про відключення") },
                    supportingContent = { Text("Отримувати повідомлення коли світло зникає або з'являється") },
                    leadingContent = {
                        Icon(Icons.Default.NotificationsActive, null)
                    },
                    trailingContent = {
                        Switch(
                            checked = state.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !hasNotificationPermission()) {
                                    requestNotificationPermission()
                                } else {
                                    onAction(SettingsAction.SetNotificationsEnabled(enabled))
                                }
                            }
                        )
                    },
                    onClick = {
                        if (!state.notificationsEnabled && !hasNotificationPermission()) {
                            requestNotificationPermission()
                        } else {
                            onAction(SettingsAction.SetNotificationsEnabled(!state.notificationsEnabled))
                        }
                    }
                )
            }

            // Permanent Status
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val showLiveUpdates = Build.VERSION.SDK_INT >= 36
                val totalItems = if (showLiveUpdates) 2 else 1
                
                SettingsGroupItem(
                    index = 0,
                    totalCount = totalItems,
                    headlineContent = { Text("Постійний статус") },
                    supportingContent = { Text("Закріплене повідомлення в шторці з поточним станом") },
                    leadingContent = {
                        Icon(Icons.Default.Bolt, null)
                    },
                    trailingContent = {
                        Switch(
                            checked = state.statusNotificationEnabled,
                            onCheckedChange = { onAction(SettingsAction.SetStatusNotificationEnabled(it)) }
                        )
                    },
                    onClick = { onAction(SettingsAction.SetStatusNotificationEnabled(!state.statusNotificationEnabled)) }
                )

                if (showLiveUpdates) {
                    SettingsGroupItem(
                        index = 1,
                        totalCount = totalItems,
                        headlineContent = { Text("Live Updates") },
                        supportingContent = { Text("Налаштувати відображення на заблокованому екрані") },
                        onClick = {
                            val intent = Intent("android.settings.MANAGE_APP_PROMOTED_NOTIFICATIONS").apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // System Settings Link
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingsGroupItem(
                    index = 0,
                    totalCount = 1,
                    headlineContent = { Text("Відкрити системні налаштування") },
                    leadingContent = { Icon(Icons.Default.Settings, null) },
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // Exact Alarms (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = remember { context.getSystemService(android.app.AlarmManager::class.java) }
                var canScheduleExact by remember {
                    mutableStateOf(alarmManager.canScheduleExactAlarms())
                }

                // Check again when user returns to app
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            canScheduleExact = alarmManager.canScheduleExactAlarms()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                if (!canScheduleExact) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        SettingsGroupItem(
                            index = 0,
                            totalCount = 1,
                            headlineContent = { Text("Точні сповіщення") },
                            supportingContent = { Text("Дозвіл на точний час необхідний для вчасної відправки повідомлень") },
                            leadingContent = {
                                Icon(Icons.Default.Notifications, null)
                            },
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}
