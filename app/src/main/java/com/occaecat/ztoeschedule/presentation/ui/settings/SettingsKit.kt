@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)

package com.occaecat.ztoeschedule.presentation.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.presentation.ui.components.SettingsGroupItem
import com.occaecat.ztoeschedule.presentation.ui.components.StepGutter
import com.occaecat.ztoeschedule.presentation.ui.components.StepLeadingIcon

/*
 * Shared building blocks for every settings page: one scaffold, section headers and
 * grouped rows, so all pages share gutters, type scale, icon badges and controls.
 */

/** Colour role for a row's icon badge. */
enum class SettingsAccent { Primary, Secondary, Tertiary, Error, Neutral }

/** What sits at the end of a row. */
sealed interface SettingsTrailing {
    data object Chevron : SettingsTrailing
    data object External : SettingsTrailing
    data object None : SettingsTrailing
    data class Toggle(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : SettingsTrailing
    data class Value(val text: String) : SettingsTrailing
    /** A small tonal button that runs the row's action. */
    data class Action(val text: String) : SettingsTrailing
    data object Progress : SettingsTrailing
    data class Custom(val content: @Composable () -> Unit) : SettingsTrailing
}

data class SettingsRow(
    val title: String,
    val icon: ImageVector,
    val subtitle: String? = null,
    val accent: SettingsAccent = SettingsAccent.Secondary,
    val trailing: SettingsTrailing = SettingsTrailing.Chevron,
    val enabled: Boolean = true,
    /** Full-colour brand logo (drawable) shown instead of [icon], on a neutral badge. */
    @param:androidx.annotation.DrawableRes val brandIcon: Int? = null,
    val onClick: () -> Unit = {}
)

/** Page scaffold: large flexible title that collapses on scroll, tonal back button, lazy content. */
@Composable
fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    content: LazyListScope.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                subtitle = subtitle?.let { { Text(it) } },
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
                    scrolledContainerColor = colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(padding),
            contentPadding = PaddingValues(
                start = StepGutter,
                end = StepGutter,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            content = content
        )
    }
}

fun LazyListScope.settingsSection(title: String?, rows: List<SettingsRow>) {
    if (rows.isEmpty()) return
    if (title != null) {
        item(key = "header_$title") { SettingsSectionHeader(title) }
    } else {
        item(key = "spacer_${rows.first().title}") { Spacer(Modifier.height(12.dp)) }
    }
    item(key = "rows_${title ?: rows.first().title}") {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            rows.forEachIndexed { index, row -> SettingsRowItem(row, index, rows.size) }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
fun accentColors(accent: SettingsAccent): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (accent) {
        SettingsAccent.Primary -> c.primaryContainer to c.onPrimaryContainer
        SettingsAccent.Secondary -> c.secondaryContainer to c.onSecondaryContainer
        SettingsAccent.Tertiary -> c.tertiaryContainer to c.onTertiaryContainer
        SettingsAccent.Error -> c.errorContainer to c.onErrorContainer
        SettingsAccent.Neutral -> c.surfaceContainerHighest to c.onSurfaceVariant
    }
}

@Composable
fun SettingsRowItem(row: SettingsRow, index: Int, count: Int) {
    val (badge, onBadge) = accentColors(row.accent)
    val onClick: () -> Unit = when (val t = row.trailing) {
        is SettingsTrailing.Toggle -> ({ t.onCheckedChange(!t.checked) })
        else -> row.onClick
    }
    SettingsGroupItem(
        index = index,
        totalCount = count,
        modifier = Modifier.alpha(if (row.enabled) 1f else 0.5f),
        headlineContent = { Text(row.title, fontWeight = FontWeight.SemiBold) },
        supportingContent = row.subtitle?.let { { Text(it) } },
        leadingContent = {
            val brand = row.brandIcon
            if (brand != null) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        androidx.compose.ui.res.painterResource(brand),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                StepLeadingIcon(row.icon, containerColor = badge, contentColor = onBadge)
            }
        },
        trailingContent = {
            when (val t = row.trailing) {
                SettingsTrailing.Chevron -> Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SettingsTrailing.External -> Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                SettingsTrailing.None -> Unit
                is SettingsTrailing.Toggle -> Switch(
                    checked = t.checked,
                    onCheckedChange = if (row.enabled) t.onCheckedChange else null,
                    enabled = row.enabled,
                    thumbContent = if (t.checked) {
                        {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    } else null
                )
                is SettingsTrailing.Value -> Text(
                    t.text,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SettingsTrailing.Progress -> LoadingIndicator(modifier = Modifier.size(32.dp))
                is SettingsTrailing.Custom -> t.content()
                is SettingsTrailing.Action -> FilledTonalButton(
                    onClick = { if (row.enabled) onClick() },
                    enabled = row.enabled,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    shapes = ButtonDefaults.shapes()
                ) { Text(t.text) }
            }
        },
        onClick = { if (row.enabled) onClick() }
    )
}

/** App identity at the top of settings: icon on an expressive shape, name and version. */
@Composable
fun AppIdentityCard(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.primaryContainer,
        contentColor = colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            com.occaecat.ztoeschedule.presentation.ui.components.ExpressiveHeroBadge(
                shape = MaterialShapes.Cookie9Sided.toShape(),
                containerColor = colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                size = 72.dp
            ) {
                com.occaecat.ztoeschedule.presentation.ui.components.AppLogo(Modifier.size(50.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("СвітлоЄ?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Версія ${com.occaecat.ztoeschedule.BuildConfig.VERSION_NAME} · Житомирщина",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

