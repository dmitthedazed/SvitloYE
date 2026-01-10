@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.occaecat.ztoeschedule.presentation.ui.more

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.domain.StatisticsCalculator
import com.occaecat.ztoeschedule.presentation.ui.components.SettingsGroupItem
import com.occaecat.ztoeschedule.presentation.ui.components.DailyStatisticsCard
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

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
    onAddDemoLocation: () -> Unit = {},
    displayMode: DisplayMode = DisplayMode.Comfortable,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme
    
    // Window width check for foldables and tablets
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp > 600
    
    var sDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val sDateStr = remember(sDateMillis) { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(sDateMillis)) }
    
    val promo = remember(colorScheme) { 
        listOf(
            PromoItem("Про проект", "Хто ми", Icons.Default.Info, colorScheme.secondaryContainer, colorScheme.onSecondaryContainer, onNavigateToAbout), 
            PromoItem("Зв'язок", "Напишіть нам", Icons.Default.Email, colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer, onNavigateToFeedback),
            PromoItem("Питання", "FAQ", Icons.Default.Help, colorScheme.surfaceContainerHigh, colorScheme.onSurfaceVariant, onNavigateToFaq)
        ) 
    }

    val carouselState = rememberCarouselState { promo.size }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 186.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth().height(221.dp)
        ) { index ->
            val item = promo[index]
            
            // Mask using a generic path shape
            val pathShape = remember {
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density,
                    ): Outline {
                        val radiusPx = density.run { 24.dp.toPx() }
                        val roundRect =
                            RoundRect(0f, 0f, size.width, size.height, CornerRadius(radiusPx))
                        val shapePath = Path().apply { addRoundRect(roundRect) }
                        return Outline.Generic(shapePath)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .height(205.dp)
                    .maskClip(pathShape)
                    .maskBorder(BorderStroke(1.dp, item.contentColor.copy(alpha = 0.5f)), pathShape)
                    .background(item.containerColor)
                    .clickable(onClick = item.onClick)
            ) {
                // Background Icon
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .graphicsLayer { alpha = 0.1f },
                    tint = item.contentColor
                )
                
                // Fading Chip
                ElevatedAssistChip(
                    onClick = item.onClick,
                    label = { Text(item.title) },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 16.dp)
                        .graphicsLayer {
                            // Fade the chip in once the carousel item's size is large enough to
                            // display the entire chip
                            alpha = lerp(
                                0f,
                                1f,
                                max(
                                    size.width - (carouselItemDrawInfo.maxSize) +
                                        carouselItemDrawInfo.size,
                                    0f,
                                ) / size.width,
                            )
                            // Translate the chip to be pinned to the left side of the item's mask
                            translationX = carouselItemDrawInfo.maskRect.left + 16.dp.toPx()
                        },
                    leadingIcon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            Modifier.size(AssistChipDefaults.IconSize),
                            tint = item.contentColor
                        )
                    },
                    colors = AssistChipDefaults.elevatedAssistChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = item.contentColor,
                        leadingIconContentColor = item.contentColor
                    )
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Column(
            modifier = Modifier
                .widthIn(max = 1200.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp), 
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- ROW: ANALYTICS + SERVICES ---
            val stats = StatisticsCalculator.calculateDailyStats(scheduleList, sDateStr)
            val hasStats = stats.totalOutageMinutes > 0 || stats.totalOnMinutes > 0
            
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Column 1: Analytics
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text(
                        text = "Аналітика", 
                        style = MaterialTheme.typography.titleMedium, 
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                    )
                    
                    if (hasStats) {
                        DailyStatisticsCard(
                            stats = stats,
                            modifier = Modifier.fillMaxHeight()
                        )
                    } else {
                        Card(
                            Modifier.fillMaxWidth().weight(1f), 
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) { 
                            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) { 
                                Text("Немає даних", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                            } 
                        }
                    }
                }

                // ... (previous code)
                val services = listOf(
                    ServiceItem("Сайт ZTOE", Icons.Default.Language, "https://www.ztoe.com.ua"), 
                    ServiceItem("Графік онлайн", Icons.Default.OpenInBrowser, "https://www.ztoe.com.ua/unhooking-search.php")
                )
                
                Column(
// ... (rest of code)
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text(
                        text = "Сервіси", 
                        style = MaterialTheme.typography.titleMedium, 
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                    )
                    
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        services.forEachIndexed { index, item ->
                            SettingsGroupItem(
                                index = index,
                                totalCount = services.size,
                                modifier = Modifier.weight(1f),
                                headlineContent = { 
                                    Text(
                                        text = item.title, 
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    ) 
                                },
                                leadingContent = {
                                    Surface(
                                        modifier = Modifier.size(32.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                trailingContent = {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // --- SECTION 3: SYSTEM ---
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsGroupItem(
                    index = 0,
                    totalCount = 1,
                    leadingContent = {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Settings, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    headlineContent = { Text("Налаштування", style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("Сповіщення, тема та інше", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = { onNavigateToSettings() }
                )
            }
        }
    }
}

private data class ServiceItem(val title: String, val icon: ImageVector, val url: String)
private data class PromoItem(val title: String, val subtitle: String, val icon: ImageVector, val containerColor: Color, val contentColor: Color, val onClick: () -> Unit)