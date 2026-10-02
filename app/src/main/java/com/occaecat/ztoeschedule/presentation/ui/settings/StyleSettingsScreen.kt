@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.occaecat.ztoeschedule.presentation.ui.settings

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.ui.theme.DarkColorScheme
import com.occaecat.ztoeschedule.ui.theme.LightColorScheme

@Composable
fun StyleSettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val isDynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isGlassSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val showAmoled = state.colorTheme == ColorTheme.Dark || state.colorTheme == ColorTheme.System ||
        state.colorTheme == ColorTheme.Amoled

    SettingsPage(
        title = "Стиль та тема",
        onBack = { onAction(SettingsAction.GoBack) }
    ) {
        item(key = "theme_header") { SettingsSectionHeader("Тема") }
        item(key = "theme_cards") {
            ThemePicker(
                selected = state.colorTheme,
                useDynamic = state.dynamicColors && isDynamicSupported,
                onSelect = { onAction(SettingsAction.SetTheme(it)) }
            )
        }

        item(key = "colors_header") { SettingsSectionHeader("Кольори") }
        item(key = "palette") { PaletteCard(dynamic = state.dynamicColors && isDynamicSupported) }
        item(key = "colors_rows") {
            val rows = buildList {
                if (isDynamicSupported) add(
                    SettingsRow(
                        title = "Material You",
                        subtitle = "Кольори з ваших шпалер",
                        icon = Icons.Default.Wallpaper,
                        accent = SettingsAccent.Primary,
                        trailing = SettingsTrailing.Toggle(state.dynamicColors) { onAction(SettingsAction.SetDynamicColors(it)) }
                    )
                )
                if (showAmoled) add(
                    SettingsRow(
                        title = "Чистий чорний",
                        subtitle = "Для OLED-екранів у темній темі",
                        icon = Icons.Default.Contrast,
                        accent = SettingsAccent.Neutral,
                        trailing = SettingsTrailing.Toggle(state.isAmoled) { onAction(SettingsAction.SetAmoled(it)) }
                    )
                )
            }
            // Continues the palette card's group
            Column(Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                rows.forEachIndexed { index, row -> SettingsRowItem(row, index + 1, rows.size + 1) }
            }
        }

        settingsSection(
            "Навігація",
            listOf(
                SettingsRow(
                    title = "Liquid Glass",
                    subtitle = if (isGlassSupported) "Скляний док із заломленням замість панелі" else "Потрібен Android 13 або новіший",
                    icon = Icons.Default.BlurOn,
                    accent = SettingsAccent.Tertiary,
                    enabled = isGlassSupported,
                    trailing = SettingsTrailing.Toggle(state.liquidGlass && isGlassSupported) { onAction(SettingsAction.SetLiquidGlass(it)) }
                )
            )
        )

        item(key = "density_header") { SettingsSectionHeader("Розмір інтерфейсу") }
        item(key = "density") {
            DensityPicker(selected = state.displayMode, onSelect = { onAction(SettingsAction.SetDisplayMode(it)) })
        }
    }
}

/** Three mini phone mockups rendered in the actual light / dark palettes. */
@Composable
private fun ThemePicker(
    selected: ColorTheme,
    useDynamic: Boolean,
    onSelect: (ColorTheme) -> Unit
) {
    val context = LocalContext.current
    val dynamicOk = useDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val light = if (dynamicOk) dynamicLightColorScheme(context) else LightColorScheme
    val dark = if (dynamicOk) dynamicDarkColorScheme(context) else DarkColorScheme
    val options = listOf(
        Triple(ColorTheme.System, "Системна", Icons.Outlined.SettingsSuggest),
        Triple(ColorTheme.Light, "Світла", Icons.Outlined.LightMode),
        Triple(ColorTheme.Dark, "Темна", Icons.Outlined.DarkMode)
    )
    // AMOLED is a dark variant toggled below, so it selects the "Dark" card
    val effective = if (selected == ColorTheme.Amoled) ColorTheme.Dark else selected

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (theme, label, icon) ->
            val isSelected = effective == theme
            val borderWidth by animateDpAsState(if (isSelected) 3.dp else 0.dp, label = "theme_border")
            Surface(
                onClick = { onSelect(theme) },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (isSelected) BorderStroke(borderWidth, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        role = Role.RadioButton
                        this.selected = isSelected
                    }
            ) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.72f)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        if (theme == ColorTheme.System) {
                            // Left half light, right half dark
                            Row(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(0.dp))) {
                                    MiniScreen(light, Modifier.wrapContentWidth(Alignment.Start, unbounded = true).requiredWidth(120.dp))
                                }
                                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(0.dp))) {
                                    MiniScreen(dark, Modifier.wrapContentWidth(Alignment.End, unbounded = true).requiredWidth(120.dp))
                                }
                            }
                        } else {
                            MiniScreen(if (theme == ColorTheme.Light) light else dark, Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isSelected) Icons.Filled.CheckCircle else icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

/** A tiny sketch of the home screen in the given colour scheme. */
@Composable
private fun MiniScreen(scheme: ColorScheme, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxHeight()
            .background(scheme.surface)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.fillMaxWidth(0.55f).height(7.dp).background(scheme.onSurface.copy(alpha = 0.8f), CircleShape))
        Box(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(scheme.primaryContainer, RoundedCornerShape(8.dp))
                .padding(6.dp)
        ) {
            Box(Modifier.size(12.dp).background(scheme.onPrimaryContainer, CircleShape))
        }
        repeat(3) { i ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(scheme.surfaceContainerHigh, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(3.dp).height(8.dp).background(if (i == 1) scheme.error else scheme.outlineVariant, CircleShape))
                Spacer(Modifier.width(4.dp))
                Box(Modifier.fillMaxWidth(0.6f).height(4.dp).background(scheme.onSurfaceVariant.copy(alpha = 0.6f), CircleShape))
            }
        }
    }
}

/** The palette currently in use: wallpaper-based or the app's own amber palette. */
@Composable
private fun PaletteCard(dynamic: Boolean) {
    val c = MaterialTheme.colorScheme
    Surface(
        color = c.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Палітра", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (dynamic) "Підібрана зі шпалер" else "Фірмова: бурштин і струм",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                listOf(c.primary, c.primaryContainer, c.secondaryContainer, c.tertiaryContainer).forEach { swatch ->
                    val animated by animateColorAsState(swatch, label = "swatch")
                    Box(
                        Modifier
                            .size(32.dp)
                            .background(c.surfaceContainerHigh, CircleShape)
                            .padding(2.dp)
                            .background(animated, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun DensityPicker(selected: DisplayMode, onSelect: (DisplayMode) -> Unit) {
    val modes = DisplayMode.entries
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        modes.forEachIndexed { index, mode ->
            val isSelected = selected == mode
            val (label, icon) = when (mode) {
                DisplayMode.Compact -> "Щільний" to if (isSelected) Icons.Filled.DensitySmall else Icons.Outlined.DensitySmall
                DisplayMode.Comfortable -> "Зручний" to if (isSelected) Icons.Filled.DensityMedium else Icons.Outlined.DensityMedium
                DisplayMode.Spacious -> "Великий" to if (isSelected) Icons.Filled.DensityLarge else Icons.Outlined.DensityLarge
            }
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onSelect(mode) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .semantics { role = Role.RadioButton }
            ) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(label, maxLines = 1)
            }
        }
    }
}
