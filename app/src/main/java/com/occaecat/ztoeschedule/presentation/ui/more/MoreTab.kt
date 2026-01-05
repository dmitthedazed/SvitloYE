package com.occaecat.ztoeschedule.presentation.ui.more

import android.content.Intent
import android.net.Uri
import android.location.Geocoder
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.domain.StatisticsCalculator
import com.occaecat.ztoeschedule.presentation.ui.components.DailyStatisticsCard
import com.occaecat.ztoeschedule.presentation.ui.components.ScaleIndication
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreTab(
    scheduleList: List<Schedule> = emptyList(),
    currentAddressRemName: String = "",
    currentAddressCityName: String = "",
    currentAddressStreetName: String = "",
    currentAddressHouseName: String = "",
    onNavigateToSettings: () -> Unit,
    onNavigateToIntegrations: () -> Unit,
    onNavigateToDonate: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToFaq: () -> Unit,
    onAddDemoLocation: () -> Unit = {},
    displayMode: DisplayMode = DisplayMode.Comfortable,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var gpsAddress by remember { mutableStateOf<String?>(null) }
    var isLoadingGps by remember { mutableStateOf(false) }
    var gpsError by remember { mutableStateOf<String?>(null) }
    
    // Window width check for foldables and tablets
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp > 600
    
    var sDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val sDateStr = remember(sDateMillis) { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(sDateMillis)) }
    
    val promo = remember(colorScheme) { 
        listOf(
            PromoItem("Підтримати", "Допоможіть нам", Icons.Default.Favorite, colorScheme.primaryContainer, colorScheme.onPrimaryContainer, onNavigateToDonate), 
            PromoItem("Про проект", "Хто ми", Icons.Default.Info, colorScheme.secondaryContainer, colorScheme.onSecondaryContainer, onNavigateToAbout), 
            PromoItem("Зв'язок", "Напишіть нам", Icons.Default.Email, colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer, {
                val intent = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:olegkhasanovv@gmail.com") }
                context.startActivity(Intent.createChooser(intent, "Написати нам"))
            }),
            PromoItem("Питання", "FAQ", Icons.Default.Help, colorScheme.surfaceContainerHigh, colorScheme.onSurfaceVariant, onNavigateToFaq)
        ) 
    }

    val carouselState = rememberCarouselState { promo.size }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = if (isWideScreen) 240.dp else 160.dp,
            itemSpacing = 16.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth().height(if (isWideScreen) 240.dp else 210.dp)
        ) { index ->
            val item = promo[index]
            val radius = com.occaecat.ztoeschedule.ui.theme.LocalCornerRadius.current
            
            Card(
                onClick = item.onClick, 
                modifier = Modifier
                    .height(if (isWideScreen) 230.dp else 200.dp)
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
                            .offset(x = 16.dp, y = (-8).dp)
                            .size(if (isWideScreen) 144.dp else 112.dp)
                            .graphicsLayer { alpha = 0.12f },
                        tint = item.contentColor
                    )
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    ) { 
                        Surface(
                            color = item.contentColor.copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(if (isWideScreen) 48.dp else 32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(item.icon, null, Modifier.size(if (isWideScreen) 24.dp else 16.dp), tint = item.contentColor)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(item.title, style = if (isWideScreen) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge, color = item.contentColor)
                        Text(item.subtitle, style = MaterialTheme.typography.labelSmall, color = item.contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } 
                } 
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

                // Column 2: Services
                val services = listOf(
                    ServiceItem("Сайт ZTOE", Icons.Default.Language, "https://www.ztoe.com.ua"), 
                    ServiceItem("Графік онлайн", Icons.Default.OpenInBrowser, "https://www.ztoe.com.ua/unhooking-search.php")
                )
                
                Column(
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        services.forEach { item ->
                            Card(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) },
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Row(
                                    Modifier.fillMaxSize().padding(horizontal = 16.dp), 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(32.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = item.title, 
                                        style = MaterialTheme.typography.titleSmall, 
                                        maxLines = 2, 
                                        lineHeight = 16.sp,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- GET ADDRESS BUTTON ---
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                onClick = {
                    isLoadingGps = true
                    gpsError = null
                    scope.launch {
                        try {
                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                            @Suppress("MissingPermission")
                            val locationTask = fusedLocationClient.lastLocation
                            locationTask.addOnSuccessListener { location ->
                                if (location != null) {
                                    // Move geocoding to IO dispatcher to avoid blocking main thread
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val geocoder = Geocoder(context, Locale("uk", "UA"))
                                            // Check if Geocoder is available (requires Google Services on some devices)
                                            if (!Geocoder.isPresent()) {
                                                withContext(Dispatchers.Main) {
                                                    gpsError = "Геокодування недоступне на цьому пристрої"
                                                    isLoadingGps = false
                                                }
                                                return@launch
                                            }
                                            
                                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                            withContext(Dispatchers.Main) {
                                                if (!addresses.isNullOrEmpty()) {
                                                    val address = addresses[0]
                                                    val parts = mutableListOf<String>()
                                                    if (!address.adminArea.isNullOrEmpty()) parts.add(address.adminArea)
                                                    if (!address.thoroughfare.isNullOrEmpty()) parts.add(address.thoroughfare)
                                                    if (!address.featureName.isNullOrEmpty()) parts.add(address.featureName)
                                                    gpsAddress = if (parts.isNotEmpty()) parts.joinToString(", ") else "Невідома адреса"
                                                } else {
                                                    gpsError = "Адреса не знайдена"
                                                }
                                                isLoadingGps = false
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                gpsError = "Помилка геокодування: ${e.message}"
                                                isLoadingGps = false
                                            }
                                        }
                                    }
                                } else {
                                    gpsError = "Локація недоступна"
                                    isLoadingGps = false
                                }
                            }.addOnFailureListener { e ->
                                gpsError = "Помилка GPS: ${e.message}"
                                isLoadingGps = false
                            }
                        } catch (e: Exception) {
                            gpsError = "Помилка: ${e.message}"
                            isLoadingGps = false
                        }
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoadingGps) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Отримати адресу по GPS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (gpsError != null) {
                            Text(
                                text = gpsError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (gpsAddress != null) {
                            Text(
                                text = gpsAddress!!,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "Натисніть для отримання адреси",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    if (gpsAddress != null && gpsError == null) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(gpsAddress!!))
                                }
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // --- DEBUG: Demo Location ---
            if (com.occaecat.ztoeschedule.BuildConfig.DEBUG) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(), 
                    shape = MaterialTheme.shapes.large, 
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    ListItem(
                        headlineContent = { Text("🔧 Додати демо-локацію", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Для тестування алертів (статус міняється кожну хвилину)", fontSize = 12.sp) },
                        leadingContent = { 
                            Surface(
                                modifier = Modifier.size(40.dp), 
                                shape = CircleShape, 
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            ) { 
                                Box(contentAlignment = Alignment.Center) { 
                                    Icon(Icons.Default.BugReport, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error) 
                                } 
                            } 
                        },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                        },
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple()
                        ) { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onAddDemoLocation()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            // --- SECTION 3: SYSTEM ---
            Card(
                modifier = Modifier.fillMaxWidth(), 
                shape = MaterialTheme.shapes.extraLarge, 
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Налаштування", style = MaterialTheme.typography.titleMedium) },
                        supportingContent = { Text("Сповіщення та тема", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple()
                        ) { onNavigateToSettings() },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    
                    ListItem(
                        headlineContent = { Text("Функції та інтеграції", style = MaterialTheme.typography.titleMedium) },
                        supportingContent = { Text("Android Auto, віджети та інше", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingContent = { 
                            Surface(
                                modifier = Modifier.size(40.dp), 
                                shape = CircleShape, 
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                            ) { 
                                Box(contentAlignment = Alignment.Center) { 
                                    Icon(Icons.Default.Extension, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary) 
                                } 
                            } 
                        },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple()
                        ) { onNavigateToIntegrations() },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

private data class ServiceItem(val title: String, val icon: ImageVector, val url: String)
private data class PromoItem(val title: String, val subtitle: String, val icon: ImageVector, val containerColor: Color, val contentColor: Color, val onClick: () -> Unit)
