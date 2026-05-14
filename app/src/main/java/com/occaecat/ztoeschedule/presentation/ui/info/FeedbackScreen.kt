package com.occaecat.ztoeschedule.presentation.ui.info

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.occaecat.ztoeschedule.presentation.ui.components.SettingsGroupItem
import com.occaecat.ztoeschedule.ui.theme.robotoFlexTopBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    onNavigateToGemini: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Зв'язок", fontFamily = robotoFlexTopBar) },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    scrolledContainerColor = colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            )
        ) {
            item {
                Text(
                    text = "Оберіть спосіб зв'язку",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )
            }

            // Group: Contact Options
            item {
                SettingsGroupItem(
                    index = 0,
                    totalCount = 2,
                    headlineContent = { Text("Написати на пошту") },
                    supportingContent = { Text("Для пропозицій та помилок") },
                    leadingContent = {
                        Icon(Icons.Default.Email, null, tint = colorScheme.primary)
                    },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:olegkhasanovv@gmail.com".toUri()
                                putExtra(Intent.EXTRA_SUBJECT, "СвітлоЄ: Зворотній зв'язок")
                            }
                            context.startActivity(Intent.createChooser(intent, "Написати нам"))
                    }
                )
            }

            item {
                SettingsGroupItem(
                    index = 1,
                    totalCount = 2,
                    headlineContent = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ШІ-підтримка")
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    "BETA",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    },
                    supportingContent = { Text("Чат з Gemini Assistant") },
                    leadingContent = {
                        Icon(Icons.Default.AutoAwesome, null, tint = colorScheme.tertiary)
                    },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp)) },
                    onClick = onNavigateToGemini
                )
            }
            
            item {
                Spacer(Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Ми цінуємо вашу думку!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Якщо ви знайшли помилку в графіку або у вас є ідея, як покращити додаток, будь ласка, напишіть нам.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
