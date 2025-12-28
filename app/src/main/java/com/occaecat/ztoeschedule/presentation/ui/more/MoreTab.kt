package com.occaecat.ztoeschedule.presentation.ui.more

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.domain.StatisticsCalculator
import com.occaecat.ztoeschedule.presentation.ui.components.DailyStatisticsCard
import com.occaecat.ztoeschedule.presentation.ui.components.ScaleIndication
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreTab(
    scheduleList: List<Schedule> = emptyList(),
    onNavigateToSettings: () -> Unit,
    onNavigateToDonate: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToFaq: () -> Unit,
    displayMode: DisplayMode = DisplayMode.Comfortable,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    
    var sDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDP by remember { mutableStateOf(false) }
    val sDateStr = remember(sDateMillis) { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(sDateMillis)) }
    
    val vSpace = when (displayMode) { 
        DisplayMode.Compact -> 12.dp
        DisplayMode.Comfortable -> 16.dp
        else -> 24.dp 
    }
    val iPad = when (displayMode) { 
        DisplayMode.Compact -> 12.dp
        DisplayMode.Comfortable -> 16.dp
        else -> 20.dp 
    }

    val promo = remember(colorScheme) { 
        listOf(
            PromoItem("Підтримати", "Допоможіть нам", Icons.Default.Favorite, colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer, onNavigateToDonate), 
            PromoItem("Про проект", "Хто ми", Icons.Default.Info, colorScheme.primaryContainer, colorScheme.onPrimaryContainer, onNavigateToAbout), 
            PromoItem("Питання", "FAQ", Icons.Default.Help, colorScheme.secondaryContainer, colorScheme.onSecondaryContainer, onNavigateToFaq)
        ) 
    }
    
    var sTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Аналітика", "Сервіси")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding)
            .padding(vertical = 16.dp), 
        verticalArrangement = Arrangement.spacedBy(vSpace)
    ) {
        val carouselState = rememberCarouselState { promo.size }
        
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 186.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth().height(221.dp)
        ) { index ->
            val item = promo[index]
            Card(
                onClick = item.onClick, 
                modifier = Modifier
                    .height(205.dp)
                    .fillMaxWidth()
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .semantics { isTraversalGroup = true }, 
                shape = MaterialTheme.shapes.extraLarge, 
                colors = CardDefaults.cardColors(
                    containerColor = item.containerColor,
                    contentColor = item.contentColor
                )
            ) { 
                Box(Modifier.fillMaxSize()) { 
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(80.dp)
                            .graphicsLayer { alpha = 0.15f },
                        tint = item.contentColor
                    )
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) { 
                        Surface(
                            color = item.contentColor.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(item.icon, null, Modifier.size(20.dp), tint = item.contentColor)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = item.contentColor)
                        Text(item.subtitle, style = MaterialTheme.typography.labelSmall, color = item.contentColor.copy(alpha = 0.8f))
                    } 
                } 
            }
        }
        
        TabRow(
            selectedTabIndex = sTab,
            modifier = Modifier.padding(horizontal = 16.dp),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = (sTab == index),
                    onClick = { sTab = index },
                    text = { Text(title) }
                )
            }
        }

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(vSpace)) {
            if (sTab == 0) {
                val stats = StatisticsCalculator.calculateDailyStats(scheduleList, sDateStr)
                val isTodayActual = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()) == sDateStr
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isTodayActual) "Статистика на сьогодні" else "Статистика за $sDateStr", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        FilledTonalIconButton(onClick = { showDP = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.DateRange, null, Modifier.size(18.dp)) }
                    }
                    if (stats.totalOutageMinutes > 0 || stats.totalOnMinutes > 0) DailyStatisticsCard(stats)
                    else Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("Немає даних", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                }
            } else {
                val services = listOf(ServiceItem("Офіційний сайт", Icons.Default.Language, "https://www.ztoe.com.ua"), ServiceItem("Facebook", Icons.Default.ThumbUp, "https://www.facebook.com/ztoe.com.ua"), ServiceItem("Показники", Icons.Default.Edit, "https://www.ztoe.com.ua/transmit-indices.php"), ServiceItem("Веб-графік", Icons.Default.DateRange, "https://www.ztoe.com.ua/unplugging.php"))
                Column(verticalArrangement = Arrangement.spacedBy(vSpace / 2)) {
                    Text("Сервіси Житомиробленерго", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
                    services.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { item ->
                                Card(
                                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) },
                                    modifier = Modifier.weight(1f).height(100.dp),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text(item.title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                            if (row.size < 2) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column {
                    ListItem(
                        headlineContent = { Text("Налаштування") },
                        supportingContent = { Text("Сповіщення та тема") },
                        leadingContent = { Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Settings, null, Modifier.size(20.dp)) } } },
                        modifier = Modifier.clickable { onNavigateToSettings() }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ListItem(
                        headlineContent = { Text("Зворотній зв'язок") },
                        leadingContent = { Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Email, null, Modifier.size(20.dp)) } } },
                        modifier = Modifier.clickable { 
                            val intent = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:support@occaecat.com") }
                            context.startActivity(Intent.createChooser(intent, "Написати нам"))
                        }
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/occaecat/ZTOESchedule"))) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.dev_signature), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        }
    }
    if (showDP) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = sDateMillis)
        DatePickerDialog(onDismissRequest = { showDP = false }, confirmButton = { TextButton(onClick = { sDateMillis = dpState.selectedDateMillis ?: sDateMillis; showDP = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showDP = false }) { Text("Скасувати") } }) { DatePicker(dpState) }
    }
}

private data class ServiceItem(val title: String, val icon: ImageVector, val url: String)
private data class PromoItem(val title: String, val subtitle: String, val icon: ImageVector, val containerColor: Color, val contentColor: Color, val onClick: () -> Unit)
