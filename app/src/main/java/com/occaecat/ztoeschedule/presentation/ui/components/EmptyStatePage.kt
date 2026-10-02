package com.occaecat.ztoeschedule.presentation.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.occaecat.ztoeschedule.presentation.ui.glass.glassCapsule
import com.occaecat.ztoeschedule.ui.theme.LocalLiquidGlass

data class EmptyStateChip(val icon: ImageVector, val label: String)

/**
 * Shared empty state for every tab, so the hero sits at the same height on all screens:
 * it is anchored at a fixed fraction of the available area instead of being centred
 * together with however much text and how many buttons a screen has.
 *
 * In Liquid Glass mode the action and chips become glass refracting a soft colour glow.
 */
@Composable
fun EmptyStatePage(
    icon: ImageVector,
    shape: Shape,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    rotateHero: Boolean = false,
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    onAction: () -> Unit = {},
    chips: List<EmptyStateChip> = emptyList()
) {
    val colorScheme = MaterialTheme.colorScheme
    val glass = LocalLiquidGlass.current && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        val topOffset = maxHeight * 0.14f
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = StepGutter)
                .padding(top = topOffset, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StepHeroIcon(
                icon = icon,
                shape = shape,
                containerColor = containerColor,
                contentColor = contentColor,
                rotate = rotateHero
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 360.dp)
            )

            if (actionText != null || chips.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                if (glass) {
                    GlassActions(actionText, actionIcon, onAction, chips)
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (actionText != null) {
                            StepPrimaryButton(
                                text = actionText,
                                icon = actionIcon,
                                onClick = onAction,
                                modifier = Modifier.widthIn(max = 360.dp)
                            )
                        }
                        if (chips.isNotEmpty()) ChipRow(chips) { content ->
                            Surface(color = colorScheme.surfaceContainerHigh, shape = CircleShape, content = content)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipRow(chips: List<EmptyStateChip>, container: @Composable (@Composable () -> Unit) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()
    ) {
        chips.forEach { chip ->
            container {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(chip.icon, null, tint = colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(chip.label, style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurface, maxLines = 1)
                }
            }
        }
    }
}

/** Glass button and chips over a recorded colour glow, which gives the lens something to bend. */
@Composable
private fun GlassActions(
    actionText: String?,
    actionIcon: ImageVector?,
    onAction: () -> Unit,
    chips: List<EmptyStateChip>
) {
    val colorScheme = MaterialTheme.colorScheme
    val primary = colorScheme.primary
    val tertiary = colorScheme.tertiary
    val glow = rememberLayerBackdrop()

    Box(contentAlignment = Alignment.Center) {
        // Recorded backdrop for the controls; on a plain page the glass reads through its rim and highlight
        Box(
            Modifier
                .matchParentSize()
                .layerBackdrop(glow)
        )
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (actionText != null) {
                GlassPrimaryButton(glow, actionText, actionIcon, onAction)
            }
            if (chips.isNotEmpty()) ChipRow(chips) { content ->
                Box(Modifier.glassCapsule(glow)) { content() }
            }
        }
    }
}

@Composable
private fun GlassPrimaryButton(
    backdrop: Backdrop,
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit
) {
    val tint = MaterialTheme.colorScheme.primary
    val onTint = if (tint.luminance() > 0.5f) Color.Black else Color.White
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val press by androidx.compose.animation.core.animateFloatAsState(
        if (pressed) 1f else 0f,
        androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "glass_button_press"
    )
    Row(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(12.dp.toPx(), 24.dp.toPx(), chromaticAberration = true)
                },
                highlight = { Highlight.Default.copy(alpha = lerp(0.6f, 1f, press)) },
                shadow = { Shadow(radius = 12.dp, alpha = 0.2f) },
                layerBlock = {
                    val s = lerp(1f, 1.04f, press)
                    scaleX = s
                    scaleY = s
                },
                onDrawSurface = {
                    drawRect(tint, blendMode = BlendMode.Hue)
                    drawRect(tint.copy(alpha = 0.7f))
                }
            )
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = onTint, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (icon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(icon, null, tint = onTint, modifier = Modifier.size(22.dp))
        }
    }
}
