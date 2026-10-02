package com.occaecat.ztoeschedule.presentation.ui.more

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsAccent
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsPage
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsRow
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsTrailing
import com.occaecat.ztoeschedule.presentation.ui.settings.settingsSection
import com.occaecat.ztoeschedule.widget.glance.DetailedScheduleWidgetReceiver
import com.occaecat.ztoeschedule.widget.glance.LightWidgetReceiver
import com.occaecat.ztoeschedule.widget.glance.PowerStatusWidgetReceiver

@Composable
fun IntegrationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    SettingsPage(
        title = "Функції та інтеграції",
        onBack = onBack
    ) {
        settingsSection(
            "Віджети на головний екран",
            listOf(
                SettingsRow(
                    title = "Статус світла",
                    subtitle = "Компактний: є світло чи ні та до котрої",
                    icon = Icons.Default.Bolt,
                    accent = SettingsAccent.Primary,
                    trailing = SettingsTrailing.Action("Додати"),
                    onClick = { pinWidget(context, PowerStatusWidgetReceiver::class.java) }
                ),
                SettingsRow(
                    title = "Графік на день",
                    subtitle = "Детальний: усі інтервали на сьогодні",
                    icon = Icons.Default.CalendarViewDay,
                    accent = SettingsAccent.Primary,
                    trailing = SettingsTrailing.Action("Додати"),
                    onClick = { pinWidget(context, DetailedScheduleWidgetReceiver::class.java) }
                ),
                SettingsRow(
                    title = "Лампочка",
                    subtitle = "Мінімальний індикатор 1×1",
                    icon = Icons.Default.Lightbulb,
                    accent = SettingsAccent.Primary,
                    trailing = SettingsTrailing.Action("Додати"),
                    onClick = { pinWidget(context, LightWidgetReceiver::class.java) }
                )
            )
        )

        settingsSection(
            "Інтеграції",
            listOf(
                SettingsRow(
                    title = "Android Auto",
                    subtitle = "Графік відключень на екрані автомобіля. Застосунок сам з'явиться в меню Android Auto",
                    icon = Icons.Default.DirectionsCar,
                    accent = SettingsAccent.Tertiary,
                    trailing = SettingsTrailing.None
                ),
                SettingsRow(
                    title = "Плитка швидких налаштувань",
                    subtitle = "Статус світла в шторці одним свайпом",
                    icon = Icons.Default.GridView,
                    accent = SettingsAccent.Tertiary,
                    trailing = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        SettingsTrailing.Action("Додати")
                    } else SettingsTrailing.None,
                    onClick = { requestAddTile(context) }
                ),
                SettingsRow(
                    title = "Посилання на адресу",
                    subtitle = "Поділіться адресою з головного екрана — у друзів одразу відкриється її графік",
                    icon = Icons.Default.Link,
                    accent = SettingsAccent.Tertiary,
                    trailing = SettingsTrailing.None
                )
            )
        )

        item(key = "footer") {
            Text(
                "Ми постійно додаємо нові можливості — слідкуйте за оновленнями.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp)
            )
        }
    }
}

/** Asks the launcher to pin a widget (the launcher shows its own confirmation). */
private fun pinWidget(context: Context, receiver: Class<*>) {
    val manager = AppWidgetManager.getInstance(context)
    if (manager.isRequestPinAppWidgetSupported) {
        manager.requestPinAppWidget(ComponentName(context, receiver), null, null)
    } else {
        Toast.makeText(context, "Лаунчер не підтримує додавання віджетів із застосунку", Toast.LENGTH_SHORT).show()
    }
}

/** Android 13+: system dialog offering to add our Quick Settings tile. */
private fun requestAddTile(context: Context) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
    val statusBarManager = context.getSystemService(android.app.StatusBarManager::class.java) ?: return
    statusBarManager.requestAddTileService(
        ComponentName(context, com.occaecat.ztoeschedule.domain.notification.PowerStatusTileService::class.java),
        "СвітлоЄ?",
        android.graphics.drawable.Icon.createWithResource(context, com.occaecat.ztoeschedule.R.drawable.ic_bolt),
        context.mainExecutor
    ) { }
}
