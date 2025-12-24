package com.occaecat.ztoeschedule.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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

/**
 * Welcome/Onboarding screen for first-time users
 * Beautiful Material 3 design with horizontal pager
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = remember {
        listOf(
            OnboardingPage(
                icon = Icons.Default.Home,
                title = "Ласкаво просимо!",
                description = "ZTOE Schedule - ваш помічник для перегляду графіків відключень електроенергії",
                iconColor = "primary"
            ),
            OnboardingPage(
                icon = Icons.Default.LocationOn,
                title = "Оберіть вашу адресу",
                description = "Простий процес вибору: РЕМ → Місто → Вулиця → Будинок. Всього кілька кроків!",
                iconColor = "secondary"
            ),
            OnboardingPage(
                icon = Icons.Default.DateRange,
                title = "Актуальний графік",
                description = "Перегляд детального графіку відключень з поточним статусом та повідомленнями",
                iconColor = "tertiary"
            ),
            OnboardingPage(
                icon = Icons.Default.Notifications,
                title = "Завжди в курсі",
                description = "Додаток автоматично оновлює статус кожну хвилину. Ваш графік завжди під рукою!",
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
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onComplete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Пропустити")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Horizontal pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = pages[page],
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Page indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                AnimatedVisibility(
                    visible = pagerState.currentPage > 0,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Назад")
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Next/Start button
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
                        text = if (pagerState.currentPage == pages.size - 1)
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
        // Icon with background
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
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

/**
 * Data class for onboarding page content
 */
private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val iconColor: String = "primary"
)

