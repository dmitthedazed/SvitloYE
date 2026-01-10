package com.occaecat.ztoeschedule.presentation.ui.more

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.presentation.ui.components.SettingsGroupItem

import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IntegrationsScreen(onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Функції та інтеграції") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val items = listOf(
                IntegrationItem(
                    title = "Android Auto",
                    description = "Переглядайте графіки відключень прямо на медіасистемі вашого автомобіля. Додаток автоматично з'явиться в меню Android Auto.",
                    icon = Icons.Default.DirectionsCar,
                    color = MaterialTheme.colorScheme.primary
                ),
                IntegrationItem(
                    title = "Віджети",
                    description = "Додайте компактний або детальний віджет на робочий стіл, щоб завжди знати статус світла, не відкриваючи додаток.",
                    icon = Icons.Default.Widgets,
                    color = MaterialTheme.colorScheme.secondary
                ),
                IntegrationItem(
                    title = "Deep Links",
                    description = "Діліться посиланнями на конкретні адреси. При натисканні на посилання у друзів одразу відкриється потрібний графік.",
                    icon = Icons.Default.Link,
                    color = MaterialTheme.colorScheme.tertiary
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items.forEachIndexed { index, item ->
                    SettingsGroupItem(
                        index = index,
                        totalCount = items.size,
                        headlineContent = { Text(item.title, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(item.description) },
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = item.color.copy(alpha = 0.12f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(item.icon, null, tint = item.color, modifier = Modifier.size(24.dp))
                                }
                            }
                        },
                        onClick = { /* No action for now */ }
                    )
                }
            }
            
            Text(
                "Ми постійно працюємо над новими можливостями. Слідкуйте за оновленнями!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

private data class IntegrationItem(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: androidx.compose.ui.graphics.Color
)
