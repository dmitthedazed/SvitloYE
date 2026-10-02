@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.occaecat.ztoeschedule.presentation.ui.more

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.domain.DailyStats
import com.occaecat.ztoeschedule.domain.StatisticsCalculator
import com.occaecat.ztoeschedule.presentation.ui.components.SettingsGroupItem
import com.occaecat.ztoeschedule.presentation.ui.components.StepGutter
import com.occaecat.ztoeschedule.presentation.ui.components.StepLeadingIcon
import com.occaecat.ztoeschedule.domain.time.ScheduleZone
import java.util.*

@Composable
fun MoreTab(
    scheduleList: List<Schedule> = emptyList(),
    currentAddressRemName: String = "",
    currentAddressCityName: String = "",
    currentAddressStreetName: String = "",
    currentAddressHouseName: String = "",
    onNavigateToSettings: () -> Unit,
    onNavigateToIntegrations: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToFaq: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    modifier: Modifier = Modifier,
    onAddDemoLocation: () -> Unit = {},
    displayMode: DisplayMode = DisplayMode.Comfortable,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    // Schedule days are Kyiv days; roll over at midnight while the screen stays open
    var todayStr by remember { mutableStateOf(ScheduleZone.todayString(System.currentTimeMillis())) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            kotlinx.coroutines.delay(ScheduleZone.nextMidnight(now) - now + 1_000L)
            todayStr = ScheduleZone.todayString(System.currentTimeMillis())
        }
    }
    val stats = remember(scheduleList, todayStr) { StatisticsCalculator.calculateDailyStats(scheduleList, todayStr) }
    val hasStats = stats.totalOutageMinutes > 0 || stats.totalOnMinutes > 0 || stats.totalProbableMinutes > 0
    val addressLabel = listOf(currentAddressStreetName, currentAddressHouseName)
        .filter { it.isNotBlank() }
        .joinToString(", ")

    val shortcuts = listOf(
        Shortcut("Про проєкт", "Хто ми", Icons.Default.Info, MaterialShapes.Cookie4Sided, colorScheme.secondaryContainer, colorScheme.onSecondaryContainer, onNavigateToAbout),
        Shortcut("Зв'язок", "Напишіть нам", Icons.Default.Email, MaterialShapes.Clover4Leaf, colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer, onNavigateToFeedback),
        Shortcut("Питання", "FAQ", Icons.AutoMirrored.Filled.Help, MaterialShapes.Sunny, colorScheme.primaryContainer, colorScheme.onPrimaryContainer, onNavigateToFaq)
    )
    val services = listOf(
        ServiceItem("Сайт ZTOE", "ztoe.com.ua", Icons.Default.Language, "https://www.ztoe.com.ua"),
        ServiceItem("Графік онлайн", "Пошук відключень на сайті", Icons.Default.CalendarMonth, "https://www.ztoe.com.ua/unhooking-search.php")
    )

    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            // Only scrollable (and able to collapse the top bar) when the content doesn't fit
            .verticalScroll(scrollState, enabled = scrollState.maxValue > 0)
            .padding(contentPadding)
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = StepGutter)
        ) {
            // The top bar already separates the first section, so no extra gap here
            SectionHeader(title = "Сьогодні", topPadding = 8.dp)
            TodayCard(
                stats = stats,
                hasStats = hasStats,
                addressLabel = addressLabel.ifBlank { currentAddressCityName }
            )

            SectionHeader("Застосунок")
            // Equal-height tiles even when one title wraps
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shortcuts.forEach { item ->
                    ShortcutTile(item, Modifier.weight(1f).fillMaxHeight())
                }
            }
            Spacer(Modifier.height(8.dp))
            SettingsGroupItem(
                index = 0,
                totalCount = 1,
                headlineContent = { Text("Налаштування", fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text("Сповіщення, тема та інше") },
                leadingContent = {
                    StepLeadingIcon(
                        icon = Icons.Default.Settings,
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    )
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colorScheme.onSurfaceVariant)
                },
                onClick = onNavigateToSettings
            )

            SectionHeader("Сервіси ZTOE")
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                services.forEachIndexed { index, item ->
                    SettingsGroupItem(
                        index = index,
                        totalCount = services.size,
                        headlineContent = { Text(item.title, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { StepLeadingIcon(item.icon) },
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Відкрити в браузері",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, item.url.toUri())) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, topPadding: androidx.compose.ui.unit.Dp = 24.dp) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = topPadding, bottom = 12.dp)
    )
}

/** Share of the day with power for the current address, as a wavy ring plus totals. */
@Composable
private fun TodayCard(stats: DailyStats, hasStats: Boolean, addressLabel: String) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
      Column {
        if (addressLabel.isNotBlank()) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = addressLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!hasStats) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepLeadingIcon(Icons.Default.EventBusy)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Немає даних", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Графік на сьогодні ще не опубліковано",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        val knownMinutes = stats.totalOnMinutes + stats.totalOutageMinutes + stats.totalProbableMinutes
        val withPower = if (knownMinutes > 0) (stats.totalOnMinutes.toFloat() / knownMinutes).coerceIn(0f, 1f) else 0f
        val strokeWidth = with(LocalDensity.current) { 10.dp.toPx() }
        val stroke = remember(strokeWidth) { Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round) }

        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(112.dp)) {
                CircularWavyProgressIndicator(
                    progress = { withPower },
                    modifier = Modifier.fillMaxSize(),
                    color = colorScheme.primary,
                    trackColor = colorScheme.errorContainer,
                    stroke = stroke,
                    trackStroke = stroke,
                    waveSpeed = WavyProgressIndicatorDefaults.CircularWavelength / 4
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(withPower * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "зі світлом",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatLine(Icons.Default.Bolt, "Світло", formatMinutes(stats.totalOnMinutes), colorScheme.primary)
                StatLine(Icons.Default.PowerOff, "Без світла", formatMinutes(stats.totalOutageMinutes), colorScheme.error)
                if (stats.totalProbableMinutes > 0) {
                    StatLine(Icons.Default.Warning, "Можливо без світла", formatMinutes(stats.totalProbableMinutes), colorScheme.tertiary)
                }
            }
        }
      }
    }
}

@Composable
private fun StatLine(icon: ImageVector, label: String, value: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(accent.copy(alpha = 0.16f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun ShortcutTile(item: Shortcut, modifier: Modifier = Modifier) {
    Surface(
        onClick = item.onClick,
        color = item.containerColor,
        contentColor = item.contentColor,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(item.contentColor, item.shape.toShape()),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = item.containerColor, modifier = Modifier.size(22.dp))
            }
            // Pushes the labels to the bottom so tiles of equal height line up
            Spacer(Modifier.weight(1f).heightIn(min = 16.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours год $minutes хв"
        hours > 0 -> "$hours год"
        else -> "$minutes хв"
    }
}

private data class ServiceItem(val title: String, val subtitle: String, val icon: ImageVector, val url: String)

private data class Shortcut(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val shape: androidx.graphics.shapes.RoundedPolygon,
    val containerColor: Color,
    val contentColor: Color,
    val onClick: () -> Unit
)
