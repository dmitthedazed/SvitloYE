package com.occaecat.ztoeschedule.presentation.ui.notifications

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.ScheduleMessagePart
import com.occaecat.ztoeschedule.presentation.ui.components.ShimmerItem
import com.occaecat.ztoeschedule.presentation.ui.components.StepGutter
import com.occaecat.ztoeschedule.presentation.ui.components.StepHeroIcon

private const val ZTOE_URL = "https://www.ztoe.com.ua"

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
                isLoading && messages.isEmpty() -> "loading"
                messages.isEmpty() || formattedMessage.isBlank() -> "empty"
                else -> "content"
            },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "notifications_state_transition"
        ) { state ->
            when (state) {
                "loading" -> NotificationsSkeleton(contentPadding)
                "empty" -> EmptyNotifications(contentPadding)
                else -> {
                    val uriHandler = LocalUriHandler.current
                    // An all-caps opening line ("УВАГА! ...") works as the announcement's title
                    val parts = remember(messages) { messages.sortedBy { it.id }.map { it.text.trim() }.filter { it.isNotEmpty() } }
                    val title = parts.firstOrNull()?.takeIf { it.length < 80 && it == it.uppercase() }
                    val paragraphs = if (title != null) parts.drop(1) else parts

                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        // Don't scroll (or collapse the top bar) when the announcement fits on screen
                        userScrollEnabled = listState.canScrollForward || listState.canScrollBackward,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + StepGutter,
                            top = contentPadding.calculateTopPadding() + 16.dp,
                            end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + StepGutter,
                            bottom = contentPadding.calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        item {
                            AnnouncementCard(
                                title = title?.let { sentenceCase(it) },
                                paragraphs = paragraphs
                            )
                        }
                        item {
                            SourceRow(
                                lastUpdateTime = lastUpdateTime,
                                onOpenSite = { runCatching { uriHandler.openUri(ZTOE_URL) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AnnouncementCard(
    title: String?,
    paragraphs: List<String>
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.primaryContainer,
        contentColor = colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(colorScheme.onPrimaryContainer, MaterialShapes.Cookie6Sided.toShape()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = colorScheme.primaryContainer)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Житомиробленерго", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text("Офіційне повідомлення", style = MaterialTheme.typography.bodySmall, color = colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                }
            }

            if (title != null) {
                Spacer(Modifier.height(20.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(if (title != null) 12.dp else 20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                paragraphs.forEach { paragraph ->
                    // fromHtml keeps <a> tags as link annotations, which Text opens on tap
                    val text = remember(paragraph) { emphasizeDatesAndTimes(AnnotatedString.fromHtml(paragraph)) }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceRow(lastUpdateTime: String, onOpenSite: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onOpenSite,
        color = colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Sync, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Джерело: ztoe.com.ua", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (lastUpdateTime.isNotEmpty()) {
                    Text(
                        stringResource(R.string.home_last_updated, lastUpdateTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Відкрити сайт",
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyNotifications(contentPadding: PaddingValues) {
    com.occaecat.ztoeschedule.presentation.ui.components.EmptyStatePage(
        icon = Icons.Default.NotificationsNone,
        shape = MaterialShapes.Cookie9Sided.toShape(),
        title = "Поки тихо",
        body = "Тут з'являться повідомлення від Укренерго та Житомиробленерго",
        contentPadding = contentPadding
    )
}

@Composable
private fun NotificationsSkeleton(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + StepGutter,
                top = contentPadding.calculateTopPadding() + 16.dp,
                end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + StepGutter
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ShimmerItem(height = 260.dp, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
        ShimmerItem(height = 64.dp, shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp))
    }
}

/** "УВАГА! ВАЖЛИВА ІНФОРМАЦІЯ!" -> "Увага! Важлива інформація!" */
private fun sentenceCase(text: String): String =
    Regex("""(^|[.!?]\s+)(\p{L})""").replace(text.lowercase()) { it.groupValues[1] + it.groupValues[2].uppercase() }

private val DATE_TIME_REGEX = Regex("""\b\d{1,2}\.\d{2}\.\d{4}\b|\b\d{1,2}:\d{2}\b|\b\d+\s+черг[аиу]?\b""")

/** Bold dates, clock times and queue counts so the key facts stand out when skimming. */
private fun emphasizeDatesAndTimes(source: AnnotatedString): AnnotatedString = buildAnnotatedString {
    append(source)
    DATE_TIME_REGEX.findAll(source.text).forEach { match ->
        addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
    }
}
