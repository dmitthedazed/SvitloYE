package com.occaecat.ztoeschedule.presentation.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val iconColor: String
)

/**
 * Improved Welcome/Onboarding screen with progress indicator
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImprovedWelcomeScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = remember {
        listOf(
            OnboardingPage(
                icon = Icons.Default.Home,
                title = "Ласкаво просимо!",
                description = "СвітлоЄ? - ваш помічник для моніторингу графіків відключень електроенергії",
                iconColor = "primary"
            ),
            OnboardingPage(
                icon = Icons.Default.LocationOn,
                title = "Легко дібрати адресу",
                description = "Виберіть вашу локацію в кілька кліків та отримайте персональний графік",
                iconColor = "secondary"
            ),
            OnboardingPage(
                icon = Icons.Default.DateRange,
                title = "Актуальний графік",
                description = "Детальні розклади з точним часом та тривалістю кожного відключення",
                iconColor = "tertiary"
            ),
            OnboardingPage(
                icon = Icons.Default.Notifications,
                title = "Сповіщення в реальному часі",
                description = "Будьте завжди в курсі змін графіків та отримуйте вчасні оновлення",
                iconColor = "primary"
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with progress
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Крок ${pagerState.currentPage + 1} з ${pages.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onComplete) {
                        Text("Пропустити")
                    }
                }

                // Linear progress indicator
                LinearProgressIndicator(
                    progress = { (pagerState.currentPage + 1).toFloat() / pages.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                )
            }

            // Content pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = pages[page],
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                )
            }

            // Footer with navigation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Page indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { iteration ->
                        val isSelected = pagerState.currentPage == iteration
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                                .size(if (isSelected) 12.dp else 8.dp)
                        )
                    }
                }

                // Navigation buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Back button
                    AnimatedVisibility(
                        visible = pagerState.currentPage > 0,
                        enter = fadeIn() + slideInHorizontally { -it },
                        exit = fadeOut() + slideOutHorizontally { -it }
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Назад")
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Next/Complete button
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                if (pagerState.currentPage < pages.size - 1) {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                } else {
                                    onComplete()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (pagerState.currentPage == pages.size - 1)
                                "Почати"
                            else
                                "Далі"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (pagerState.currentPage == pages.size - 1)
                                Icons.Default.Check
                            else
                                Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with colored background
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = when (page.iconColor) {
                "primary" -> MaterialTheme.colorScheme.primaryContainer
                "secondary" -> MaterialTheme.colorScheme.secondaryContainer
                "tertiary" -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = when (page.iconColor) {
                        "primary" -> MaterialTheme.colorScheme.onPrimaryContainer
                        "secondary" -> MaterialTheme.colorScheme.onSecondaryContainer
                        "tertiary" -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Title
        Text(
            page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            page.description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
