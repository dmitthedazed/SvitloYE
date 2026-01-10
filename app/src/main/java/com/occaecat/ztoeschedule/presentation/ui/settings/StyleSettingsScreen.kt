@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.occaecat.ztoeschedule.presentation.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.presentation.ui.components.SettingsGroupItem
import com.occaecat.ztoeschedule.ui.theme.SingleItemShape

import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StyleSettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val isDynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Стиль та вигляд") },
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsAction.GoBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode
            Text(
                text = "Тема оформлення",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            
            val themes = listOf(ColorTheme.System, ColorTheme.Light, ColorTheme.Dark)
            FlowRow(
                modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                themes.forEachIndexed { index, theme ->
                    val isSelected = state.colorTheme == theme
                    val label = when(theme) {
                        ColorTheme.System -> "Системна"
                        ColorTheme.Light -> "Світла"
                        ColorTheme.Dark -> "Темна"
                        else -> ""
                    }
                    val icon = when(theme) {
                        ColorTheme.System -> if (isSelected) Icons.Filled.SettingsSuggest else Icons.Outlined.SettingsSuggest
                        ColorTheme.Light -> if (isSelected) Icons.Filled.LightMode else Icons.Outlined.LightMode
                        ColorTheme.Dark -> if (isSelected) Icons.Filled.DarkMode else Icons.Outlined.DarkMode
                        else -> Icons.Default.Check
                    }
                    
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { onAction(SettingsAction.SetTheme(theme)) },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            themes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier.semantics { role = Role.RadioButton },
                    ) {
                        Icon(icon, contentDescription = null)
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(label)
                    }
                }
            }

            // Advanced Theme Options
            Text(
                text = "Додатково",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val showAmoled = state.colorTheme == ColorTheme.Dark || state.colorTheme == ColorTheme.System
                val itemsCount = (if (isDynamicSupported) 1 else 0) + (if (showAmoled) 1 else 0)
                var currentIndex = 0

                if (isDynamicSupported) {
                    SettingsGroupItem(
                        index = currentIndex++,
                        totalCount = itemsCount,
                        headlineContent = { Text("Динамічні кольори") },
                        supportingContent = { Text("Кольори з шпалер (Material You)") },
                        trailingContent = {
                            Switch(
                                checked = state.dynamicColors,
                                onCheckedChange = { onAction(SettingsAction.SetDynamicColors(it)) }
                            )
                        },
                        onClick = { onAction(SettingsAction.SetDynamicColors(!state.dynamicColors)) }
                    )
                }
                
                if (showAmoled) {
                    SettingsGroupItem(
                        index = currentIndex,
                        totalCount = itemsCount,
                        headlineContent = { Text("Чистий чорний") },
                        supportingContent = { Text("Економія для AMOLED екранів") },
                        trailingContent = {
                            Switch(
                                checked = state.isAmoled,
                                onCheckedChange = { onAction(SettingsAction.SetAmoled(it)) }
                            )
                        },
                        onClick = { onAction(SettingsAction.SetAmoled(!state.isAmoled)) }
                    )
                }
            }

            // Corner Radius
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = SingleItemShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val currentRadius = com.occaecat.ztoeschedule.ui.theme.LocalCornerRadius.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Скруглення кутів",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (state.cornerRadius == -1) "Системне" else "${state.cornerRadius} dp",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = if (state.cornerRadius == -1) currentRadius.toFloat() else state.cornerRadius.toFloat(),
                        onValueChange = { onAction(SettingsAction.SetCornerRadius(it.toInt())) },
                        valueRange = 0f..48f,
                        steps = 47
                    )
                    if (state.cornerRadius != -1) {
                        TextButton(
                            onClick = { onAction(SettingsAction.SetCornerRadius(-1)) },
                            modifier = Modifier.align(androidx.compose.ui.Alignment.End)
                        ) {
                            Text("Скинути")
                        }
                    }
                }
            }

            // Display Mode
            Text(
                text = "Розмір інтерфейсу",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )

            val displayModes = DisplayMode.entries
            FlowRow(
                modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                displayModes.forEachIndexed { index, mode ->
                    val isSelected = state.displayMode == mode
                    val label = when(mode) {
                        DisplayMode.Compact -> "Щільний"
                        DisplayMode.Comfortable -> "Зручний"
                        DisplayMode.Spacious -> "Великий"
                    }
                    val icon = when(mode) {
                        DisplayMode.Compact -> if (isSelected) Icons.Filled.DensitySmall else Icons.Outlined.DensitySmall
                        DisplayMode.Comfortable -> if (isSelected) Icons.Filled.DensityMedium else Icons.Outlined.DensityMedium
                        DisplayMode.Spacious -> if (isSelected) Icons.Filled.DensityLarge else Icons.Outlined.DensityLarge
                    }
                    
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { onAction(SettingsAction.SetDisplayMode(mode)) },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            displayModes.size - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier.semantics { role = Role.RadioButton },
                    ) {
                        Icon(icon, contentDescription = null)
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(label)
                    }
                }
            }
        }
    }
}
