package com.occaecat.ztoeschedule.presentation.ui.more

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.R

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.domain.StatisticsCalculator
import com.occaecat.ztoeschedule.presentation.ui.components.DailyStatisticsCard

@Composable
fun MoreTab(
    scheduleList: List<Schedule> = emptyList(),
    onNavigateToSettings: () -> Unit,
    onNavigateToDonate: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToFaq: () -> Unit,
    displayMode: DisplayMode = DisplayMode.COMFORTABLE,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val verticalSpacing = when (displayMode) {
        DisplayMode.COMPACT -> 12.dp
        DisplayMode.COMFORTABLE -> 16.dp
        DisplayMode.SPACIOUS -> 24.dp
    }
    
    val itemPadding = when (displayMode) {
        DisplayMode.COMPACT -> 12.dp
        DisplayMode.COMFORTABLE -> 16.dp
        DisplayMode.SPACIOUS -> 20.dp
    }

    val services = remember {
        listOf(
            ServiceItem("Офіційний сайт", Icons.Default.Language, "https://www.ztoe.com.ua"),
            ServiceItem("Facebook", Icons.Default.ThumbUp, "https://www.facebook.com/ztoe.com.ua"),
            ServiceItem("Передача показників", Icons.Default.Edit, "https://www.ztoe.com.ua/transmit-indices.php"),
            ServiceItem("Веб-графік", Icons.Default.DateRange, "https://www.ztoe.com.ua/unplugging.php")
        )
    }
    
    val todayStats = remember(scheduleList) {
        StatisticsCalculator.calculateDailyStats(scheduleList)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        // Services Section
        Column(verticalArrangement = Arrangement.spacedBy(verticalSpacing / 2)) {
            Text(
                text = "Сервіси Житомиробленерго",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
            
            ServicesGrid(services, itemPadding) { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        // Statistics Card
        if (todayStats.totalOutageMinutes > 0 || todayStats.totalOnMinutes > 0) {
            DailyStatisticsCard(todayStats)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Main Menu Card (Single item "Google" style)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            MoreMenuItem(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.more_settings),
                supportingText = "Сповіщення, тема та налаштування",
                color = MaterialTheme.colorScheme.secondaryContainer,
                padding = itemPadding,
                onClick = onNavigateToSettings
            )
        }

        // Support & Info Card (Grouped style)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column {
                MoreMenuItem(
                    icon = Icons.Default.Favorite,
                    title = stringResource(R.string.more_donate),
                    supportingText = "Підтримайте розробку додатка",
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    padding = itemPadding,
                    onClick = onNavigateToDonate
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                MoreMenuItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.more_about),
                    supportingText = "Інформація про проект",
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    padding = itemPadding,
                    onClick = onNavigateToAbout
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                MoreMenuItem(
                    icon = Icons.Default.Help,
                    title = stringResource(R.string.more_faq),
                    supportingText = "Відповіді на запитання",
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    padding = itemPadding,
                    onClick = onNavigateToFaq
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                MoreMenuItem(
                    icon = Icons.Default.Email,
                    title = stringResource(R.string.more_contact),
                    supportingText = "Зворотній зв'язок",
                    color = MaterialTheme.colorScheme.primaryContainer,
                    padding = itemPadding,
                    onClick = { 
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@occaecat.com")
                            putExtra(Intent.EXTRA_SUBJECT, "СвітлоЄ? Житомир - Зворотній зв'язок")
                        }
                        context.startActivity(Intent.createChooser(intent, "Написати нам"))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Developer Signature
        val githubUrl = stringResource(R.string.github_url)
        TextButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.dev_signature),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ServicesGrid(
    items: List<ServiceItem>,
    itemPadding: androidx.compose.ui.unit.Dp,
    onItemClick: (String) -> Unit
) {
    val chunkedItems = items.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        chunkedItems.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    ServiceGridItem(
                        item = item,
                        padding = itemPadding,
                        onClick = { onItemClick(item.url) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ServiceGridItem(
    item: ServiceItem,
    padding: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    supportingText: String? = null,
    color: Color,
    padding: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = supportingText?.let { { Text(it) } },
        leadingContent = { 
            Surface(
                shape = CircleShape, 
                color = color,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onClick() }
    )
}

private data class ServiceItem(
    val title: String,
    val icon: ImageVector,
    val url: String
)