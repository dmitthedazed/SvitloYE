package com.occaecat.ztoeschedule.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.domain.DailyStats

@Composable
fun DailyStatisticsCard(
    stats: DailyStats,
    modifier: Modifier = Modifier
) {
    val radius = com.occaecat.ztoeschedule.ui.theme.LocalCornerRadius.current
    val adaptivePadding = remember(radius) {
        val base = 12f
        val extra = if (radius > 24) (radius - 24).toFloat() / 3f else 0f
        (base + extra).dp
    }
    
    val density = LocalDensity.current
    val windowWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val isWide = windowWidthDp > 600.dp

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_statistics_card")
    ) {
        if (isWide) {
            // Wide layout: Horizontal split
            Row(
                modifier = Modifier.padding(adaptivePadding).fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    StatRow(
                        icon = Icons.Default.Bolt,
                        label = "Світло",
                        value = formatMinutes(stats.totalOnMinutes),
                        color = MaterialTheme.colorScheme.primary,
                        isFullWidth = false
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StatRow(
                        icon = Icons.Default.PowerOff,
                        label = "Офлайн",
                        value = formatMinutes(stats.totalOutageMinutes),
                        color = MaterialTheme.colorScheme.error,
                        isFullWidth = false
                    )
                }
                
                CircularIndicatorBox(stats)
            }
        } else {
            // Compact layout: Vertical (Circle on top)
            Column(
                modifier = Modifier.padding(adaptivePadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularIndicatorBox(stats, Modifier.padding(bottom = 12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatRow(
                    icon = Icons.Default.Bolt,
                    label = "Світло",
                    value = formatMinutes(stats.totalOnMinutes),
                    color = MaterialTheme.colorScheme.primary,
                    isFullWidth = false
                )
                
                StatRow(
                    icon = Icons.Default.PowerOff,
                    label = "Офлайн",
                    value = formatMinutes(stats.totalOutageMinutes),
                    color = MaterialTheme.colorScheme.error,
                    isFullWidth = false
                )
            }
            }
        }
    }
}

@Composable
private fun CircularIndicatorBox(stats: DailyStats, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 4.dp,
            strokeCap = StrokeCap.Round
        )
        CircularProgressIndicator(
            progress = { stats.percentageOutage },
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.error,
            strokeWidth = 4.dp,
            strokeCap = StrokeCap.Round
        )
        Text(
            text = "${(stats.percentageOutage * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    isFullWidth: Boolean = true
) {
    Row(
        modifier = if (isFullWidth) Modifier.fillMaxWidth() else Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isFullWidth) Arrangement.Center else Arrangement.Start
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = null, 
            modifier = Modifier.size(16.dp), 
            tint = color
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$label: ", 
            style = MaterialTheme.typography.bodySmall, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value, 
            style = MaterialTheme.typography.bodyMedium, 
            fontWeight = FontWeight.Bold, 
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}год ${minutes}хв" else "${minutes}хв"
}
