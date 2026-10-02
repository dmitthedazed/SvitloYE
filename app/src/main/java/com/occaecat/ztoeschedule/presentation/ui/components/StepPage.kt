package com.occaecat.ztoeschedule.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/*
 * Building blocks shared by onboarding and the address picker so every step
 * uses the same gutters, type scale, hero treatment and action buttons.
 */

/** Horizontal gutter for step pages. */
val StepGutter = 16.dp

/**
 * Centered page: expressive hero, headline, supporting text, free content
 * and an optional action area pinned to the bottom.
 */
@Composable
fun StepHeroPage(
    hero: @Composable () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = StepGutter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            hero()
            Spacer(Modifier.height(32.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            if (subtitle != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Spacer(Modifier.height(32.dp))
            content()
            Spacer(Modifier.height(24.dp))
        }

        if (actions != null) {
            StepActions(content = actions)
        }
    }
}

/** Bottom action area with consistent padding. */
@Composable
fun StepActions(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = StepGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

/** Left-aligned header for list-style steps (search + list below). */
@Composable
fun StepListHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = StepGutter + 8.dp, end = StepGutter + 8.dp, top = 8.dp, bottom = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Standard hero: icon on an expressive shape. */
@Composable
fun StepHeroIcon(
    icon: ImageVector,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    rotate: Boolean = false
) {
    ExpressiveHeroBadge(
        shape = shape,
        containerColor = containerColor,
        size = 144.dp,
        rotate = rotate
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(56.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StepPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val height = ButtonDefaults.MediumContainerHeight
    Button(
        onClick = onClick,
        enabled = enabled,
        shapes = ButtonDefaults.shapesFor(height),
        contentPadding = ButtonDefaults.contentPaddingFor(height),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = height)
    ) {
        Text(text, style = ButtonDefaults.textStyleFor(height))
        if (icon != null) {
            Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
            Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StepSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val height = ButtonDefaults.MediumContainerHeight
    OutlinedButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapesFor(height),
        contentPadding = ButtonDefaults.contentPaddingFor(height),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = height)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)))
            Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
        }
        Text(text, style = ButtonDefaults.textStyleFor(height))
    }
}

/** Round tonal container for list leading icons. */
@Composable
fun StepLeadingIcon(
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
    }
}

/** Non-clickable row styled to match [SettingsGroupItem] groups. */
@Composable
fun GroupedInfoRow(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    overline: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val top = if (index == 0) 24.dp else 4.dp
    val bottom = if (index == count - 1) 24.dp else 4.dp

    ListItem(
        overlineContent = overline?.let { { Text(it) } },
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = supporting?.let { { Text(it) } },
        leadingContent = { StepLeadingIcon(icon) },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
    )
}

/** Dots where the current one stretches into a pill; used for onboarding steps and home pages. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PageIndicatorDots(
    current: Int,
    total: Int,
    description: String,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    // Onboarding also tints completed steps; pagers only highlight the current page
    fillCompleted: Boolean = true
) {
    val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.Dp>()
    val colorSpec = MaterialTheme.motionScheme.fastEffectsSpec<Color>()
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val width by animateDpAsState(if (index == current) 28.dp else 8.dp, spatialSpec, label = "dot_width")
            val color by animateColorAsState(
                if (index == current || (fillCompleted && index < current)) activeColor else inactiveColor,
                colorSpec,
                label = "dot_color"
            )
            Box(
                Modifier
                    .size(width = width, height = 8.dp)
                    .background(color, CircleShape)
            )
        }
    }
}
