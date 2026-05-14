package com.occaecat.ztoeschedule.presentation.ui.info

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.presentation.ui.components.SettingsGroupItem
import com.occaecat.ztoeschedule.ui.theme.robotoFlexTopBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Про додаток", fontFamily = robotoFlexTopBar) },
                subtitle = { Text("СвітлоЄ? Житомир") },
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
            // Block 1: App Info & Author
            item {
                SettingsGroupItem(
                    index = 0,
                    totalCount = 2,
                    headlineContent = { Text("СвітлоЄ? Житомир", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Версія 1.2.0") },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    },
                    trailingContent = {
                        FilledTonalIconButton(
                            onClick = { uriHandler.openUri("https://github.com/occaecat/ZTOESchedule") }
                        ) {
                            Icon(Icons.Default.Code, contentDescription = "Репозиторій", modifier = Modifier.size(20.dp))
                        }
                    },
                    onClick = {}
                )
            }

            item {
                SettingsGroupItem(
                    index = 1,
                    totalCount = 2,
                    headlineContent = { Text("Дмитро Савін", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Розробник") },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    colorScheme.secondaryContainer,
                                    MaterialShapes.Square.toShape()
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = colorScheme.onSecondaryContainer
                            )
                        }
                    },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalIconButton(
                                onClick = { uriHandler.openUri("https://github.com/dmitthedazed") }
                            ) {
                                Icon(Icons.Default.Code, contentDescription = "GitHub", modifier = Modifier.size(18.dp))
                            }
                            FilledTonalIconButton(
                                onClick = { uriHandler.openUri("https://www.linkedin.com/in/dmitthedazed") }
                            ) {
                                Icon(Icons.Default.Work, contentDescription = "LinkedIn", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    onClick = {}
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Block 2: Support
            item {
                SettingsGroupItem(
                    index = 0,
                    totalCount = 2,
                    headlineContent = { Text("Monobank") },
                    supportingContent = { Text("Підтримати розробку") },
                    leadingContent = { Icon(Icons.Default.Coffee, null, tint = colorScheme.tertiary) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp)) },
                    onClick = { uriHandler.openUri("https://send.monobank.ua/2AMdpReyqQ") }
                )
            }
            item {
                SettingsGroupItem(
                    index = 1,
                    totalCount = 2,
                    headlineContent = { Text("Privat24") },
                    supportingContent = { Text("Підтримати розробку") },
                    leadingContent = { Icon(Icons.Default.AccountBalanceWallet, null, tint = colorScheme.tertiary) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp)) },
                    onClick = { uriHandler.openUri("https://www.privat24.ua/send/i3nk5") }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Block 3: Legal & Data
            item {
                SettingsGroupItem(
                    index = 0,
                    totalCount = 2,
                    headlineContent = { Text("MIT License") },
                    supportingContent = { Text("Ліцензія") },
                    leadingContent = { Icon(Icons.Default.Description, null, tint = colorScheme.secondary) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp)) },
                    onClick = { uriHandler.openUri("https://opensource.org/licenses/MIT") }
                )
            }
            item {
                SettingsGroupItem(
                    index = 1,
                    totalCount = 2,
                    headlineContent = { Text("ztoe.com.ua") },
                    supportingContent = { Text("Джерело даних") },
                    leadingContent = { Icon(Icons.Default.Language, null, tint = colorScheme.primary) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp)) },
                    onClick = { uriHandler.openUri("https://www.ztoe.com.ua") }
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Made with ❤️ in Zhytomyr",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
