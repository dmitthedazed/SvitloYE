package com.occaecat.ztoeschedule.presentation.ui.notifications

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.ScheduleMessagePart
import com.occaecat.ztoeschedule.presentation.ui.components.ShimmerItem

@OptIn(ExperimentalTextApi::class)
@Composable
fun NotificationsTab(
    messages: List<ScheduleMessagePart>,
    formattedMessage: String,
    lastUpdateTime: String = "",
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    isLoading: Boolean = false
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = when {
                isLoading -> "loading"
                messages.isEmpty() || formattedMessage.isBlank() -> "empty"
                else -> "content"
            },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "notifications_state_transition"
        ) { state ->
            when (state) {
                "loading" -> { NotificationsSkeleton(contentPadding) }
                "empty" -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                            Icon(imageVector = Icons.Default.NotificationsNone, contentDescription = "Немає сповіщень", modifier = Modifier.padding(20.dp).fillMaxSize(), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = "Поки що немає важливих повідомлень", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Тут з'являться сповіщення от Укренерго та Житомиробленерго", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
                else -> {
                    val uriHandler = LocalUriHandler.current
                    val annotatedMessage = AnnotatedString.fromHtml(formattedMessage)
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = false,
                        contentPadding = PaddingValues(
                            start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + 16.dp,
                            top = contentPadding.calculateTopPadding() + 16.dp,
                            end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + 16.dp,
                            bottom = contentPadding.calculateBottomPadding() + 80.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth().animateContentSize(),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Campaign, contentDescription = "Важливе повідомлення", tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp)); Text(text = "Актуальна інформація", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                    ClickableText(
                                        text = annotatedMessage,
                                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimaryContainer),
                                        onClick = { offset ->
                                            annotatedMessage.getUrlAnnotations(offset, offset)
                                                .firstOrNull()?.let { annotation ->
                                                    try {
                                                        uriHandler.openUri(annotation.item.url)
                                                    } catch (_: Exception) { }
                                                }
                                        }
                                    )
                                }
                            }
                        }
                        item { Text(text = "Повідомлення завантажено з офіційного сайту ztoe.com.ua", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp)) }
                        if (lastUpdateTime.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.home_last_updated, lastUpdateTime),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsSkeleton(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + 16.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 80.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(2) { ShimmerItem(height = 180.dp, shape = MaterialTheme.shapes.extraLarge) }
    }
}
