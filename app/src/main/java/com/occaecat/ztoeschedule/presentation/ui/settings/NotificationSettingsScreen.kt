@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.occaecat.ztoeschedule.presentation.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.occaecat.ztoeschedule.data.model.NotificationSettings
import java.util.Calendar

@Composable
fun NotificationSettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val context = LocalContext.current
    val settings = state.notifications
    fun update(transform: (NotificationSettings) -> NotificationSettings) =
        onAction(SettingsAction.UpdateNotifications(transform))

    // Permissions live in system settings: re-check every time we come back
    val alarmManager = remember { context.getSystemService(AlarmManager::class.java) }
    fun canPost() = (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    fun canScheduleExact() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    var canPost by remember { mutableStateOf(canPost()) }
    var canScheduleExact by remember { mutableStateOf(canScheduleExact()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canPost = canPost()
                canScheduleExact = canScheduleExact()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Turning something on without permission asks for it first, then applies the change
    var pendingChange by remember { mutableStateOf<((NotificationSettings) -> NotificationSettings)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canPost = canPost()
        if (granted) pendingChange?.let(::update)
        pendingChange = null
    }
    fun enable(transform: (NotificationSettings) -> NotificationSettings) {
        if (!canPost() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingChange = transform
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            update(transform)
        }
    }

    var showLeadDialog by remember { mutableStateOf(false) }
    if (showLeadDialog) {
        LeadMinutesDialog(
            selected = settings.leadMinutes,
            onSelect = { minutes -> update { it.copy(leadMinutes = minutes) }; showLeadDialog = false },
            onDismiss = { showLeadDialog = false }
        )
    }

    fun pickTime(minuteOfDay: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            minuteOfDay / 60, minuteOfDay % 60,
            DateFormat.is24HourFormat(context)
        ).show()
    }

    val alerts = settings.alertsEnabled
    val quiet = settings.quietHours

    SettingsPage(
        title = "Сповіщення",
        subtitle = if (alerts) "Увімкнено" else "Вимкнено",
        onBack = { onAction(SettingsAction.GoBack) }
    ) {
        settingsSection(
            null,
            buildList {
                if (!canPost) add(
                    SettingsRow(
                        title = "Сповіщення заборонені",
                        subtitle = "Android не показує сповіщення застосунку. Натисніть, щоб дозволити",
                        icon = Icons.Default.NotificationsOff,
                        accent = SettingsAccent.Error,
                        trailing = SettingsTrailing.External,
                        onClick = { context.startActivity(appNotificationSettingsIntent(context.packageName)) }
                    )
                )
                add(
                    SettingsRow(
                        title = "Сповіщення про світло",
                        subtitle = "Попередження, зміни стану й оновлення графіка",
                        icon = Icons.Default.NotificationsActive,
                        accent = SettingsAccent.Primary,
                        trailing = SettingsTrailing.Toggle(alerts) { on ->
                            if (on) enable { it.copy(alertsEnabled = true) } else update { it.copy(alertsEnabled = false) }
                        }
                    )
                )
            }
        )

        settingsSection(
            "Що сповіщати",
            listOf(
                SettingsRow(
                    title = "Попередження",
                    subtitle = "Нагадування перед відключенням",
                    icon = Icons.Default.Alarm,
                    accent = SettingsAccent.Secondary,
                    enabled = alerts && settings.outageAlerts,
                    trailing = SettingsTrailing.Value(leadLabel(settings.leadMinutes)),
                    onClick = { showLeadDialog = true }
                ),
                SettingsRow(
                    title = "Відключення",
                    subtitle = "Коли світло зникає за графіком",
                    icon = Icons.Default.PowerOff,
                    accent = SettingsAccent.Error,
                    enabled = alerts,
                    trailing = SettingsTrailing.Toggle(settings.outageAlerts) { on -> update { it.copy(outageAlerts = on) } }
                ),
                SettingsRow(
                    title = "Увімкнення",
                    subtitle = "Коли світло має з'явитися",
                    icon = Icons.Default.Power,
                    accent = SettingsAccent.Tertiary,
                    enabled = alerts,
                    trailing = SettingsTrailing.Toggle(settings.restoreAlerts) { on -> update { it.copy(restoreAlerts = on) } }
                ),
                SettingsRow(
                    title = "Оновлення графіка",
                    subtitle = "Коли графік на сьогодні чи завтра змінився",
                    icon = Icons.Default.EditCalendar,
                    accent = SettingsAccent.Secondary,
                    enabled = alerts,
                    trailing = SettingsTrailing.Toggle(settings.scheduleChangeAlerts) { on -> update { it.copy(scheduleChangeAlerts = on) } }
                )
            )
        )

        settingsSection(
            "Тихі години",
            buildList {
                add(
                    SettingsRow(
                        title = "Тихі години",
                        subtitle = "Без попереджень, решта сповіщень — без звуку",
                        icon = Icons.Default.Bedtime,
                        accent = SettingsAccent.Neutral,
                        enabled = alerts,
                        trailing = SettingsTrailing.Toggle(quiet.enabled) { on ->
                            update { it.copy(quietHours = it.quietHours.copy(enabled = on)) }
                        }
                    )
                )
                if (quiet.enabled) {
                    add(
                        SettingsRow(
                            title = "Початок",
                            icon = Icons.Default.NightsStay,
                            accent = SettingsAccent.Neutral,
                            enabled = alerts,
                            trailing = SettingsTrailing.Value(formatMinuteOfDay(context, quiet.startMinute)),
                            onClick = {
                                pickTime(quiet.startMinute) { m -> update { it.copy(quietHours = it.quietHours.copy(startMinute = m)) } }
                            }
                        )
                    )
                    add(
                        SettingsRow(
                            title = "Кінець",
                            icon = Icons.Default.WbSunny,
                            accent = SettingsAccent.Neutral,
                            enabled = alerts,
                            trailing = SettingsTrailing.Value(formatMinuteOfDay(context, quiet.endMinute)),
                            onClick = {
                                pickTime(quiet.endMinute) { m -> update { it.copy(quietHours = it.quietHours.copy(endMinute = m)) } }
                            }
                        )
                    )
                }
            }
        )

        settingsSection(
            "У шторці",
            buildList {
                add(
                    SettingsRow(
                        title = "Постійний статус",
                        subtitle = "Закріплене сповіщення з поточним станом і таймером",
                        icon = Icons.Default.Bolt,
                        accent = SettingsAccent.Tertiary,
                        trailing = SettingsTrailing.Toggle(settings.statusNotification) { on ->
                            if (on) enable { it.copy(statusNotification = true) } else update { it.copy(statusNotification = false) }
                        }
                    )
                )
                // Android 16+: promoted "Live Update" notifications on the lock screen and status bar
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) add(
                    SettingsRow(
                        title = "Live Updates",
                        subtitle = "Таймер у рядку стану й на заблокованому екрані",
                        icon = Icons.Default.Timelapse,
                        accent = SettingsAccent.Tertiary,
                        enabled = settings.statusNotification,
                        trailing = SettingsTrailing.External,
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            )
                        }
                    )
                )
            }
        )

        settingsSection(
            "Система",
            buildList {
                if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(
                    SettingsRow(
                        title = "Точні будильники вимкнено",
                        subtitle = "Без них сповіщення можуть запізнюватись. Натисніть, щоб дозволити",
                        icon = Icons.Default.AlarmOff,
                        accent = SettingsAccent.Error,
                        trailing = SettingsTrailing.External,
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    .setData(Uri.fromParts("package", context.packageName, null))
                            )
                        }
                    )
                )
                add(
                    SettingsRow(
                        title = "Системні налаштування",
                        subtitle = "Канали, звук і вібрація",
                        icon = Icons.Default.Settings,
                        accent = SettingsAccent.Neutral,
                        trailing = SettingsTrailing.External,
                        onClick = { context.startActivity(appNotificationSettingsIntent(context.packageName)) }
                    )
                )
            }
        )
    }
}

@Composable
private fun LeadMinutesDialog(selected: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Попередження") },
        text = {
            Column {
                NotificationSettings.LeadMinuteOptions.forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(minutes) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = minutes == selected, onClick = { onSelect(minutes) })
                        Text(leadLabel(minutes))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}

private fun leadLabel(minutes: Int): String = when {
    minutes == 0 -> "Вимкнено"
    minutes >= 60 && minutes % 60 == 0 -> "За ${minutes / 60} год"
    else -> "За $minutes хв"
}

private fun formatMinuteOfDay(context: android.content.Context, minuteOfDay: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
    }
    return DateFormat.getTimeFormat(context).format(calendar.time)
}

private fun appNotificationSettingsIntent(packageName: String) =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
