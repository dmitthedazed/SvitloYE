package com.occaecat.ztoeschedule.presentation.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow

/**
 * Tinted Liquid Glass capsule for the primary floating action, matching the glass dock.
 * The primary colour is applied as a hue tint so the refracted content stays visible.
 */
@Composable
fun GlassExtendedFab(
    backdrop: Backdrop,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "glass_fab_press"
    )

    Row(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(
                        refractionHeight = 12.dp.toPx(),
                        refractionAmount = 24.dp.toPx(),
                        chromaticAberration = true
                    )
                },
                highlight = { Highlight.Default.copy(alpha = lerp(0.6f, 1f, press)) },
                shadow = { Shadow(radius = 12.dp, alpha = if (isDark) 0.3f else 0.14f) },
                layerBlock = {
                    val s = lerp(1f, 1.06f, press)
                    scaleX = s
                    scaleY = s
                },
                onDrawSurface = {
                    drawRect(tint, blendMode = BlendMode.Hue)
                    drawRect(tint.copy(alpha = 0.72f))
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .height(56.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val content = if (tint.luminance() > 0.5f) Color.Black else Color.White
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
        Text(text, color = content, style = MaterialTheme.typography.labelLarge)
    }
}

/** Neutral Liquid Glass for small floating details (chips, banners, indicators). */
@Composable
fun Modifier.glassCapsule(backdrop: Backdrop, shape: Shape = CircleShape): Modifier {
    val surface = MaterialTheme.colorScheme.surface
    val isDark = surface.luminance() < 0.5f
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(6.dp.toPx())
            lens(refractionHeight = 8.dp.toPx(), refractionAmount = 16.dp.toPx())
        },
        highlight = { Highlight.Default.copy(alpha = 0.6f) },
        shadow = { Shadow(radius = 8.dp, alpha = if (isDark) 0.25f else 0.08f) },
        onDrawSurface = { drawRect(surface.copy(alpha = if (isDark) 0.3f else 0.5f)) }
    )
}

/** Snackbar drawn as a glass panel above the dock. */
@Composable
fun GlassSnackbar(backdrop: Backdrop, data: SnackbarData, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .glassCapsule(backdrop, shape)
            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
            .height(IntrinsicHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = data.visuals.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(vertical = 10.dp)
        )
        data.visuals.actionLabel?.let { label ->
            TextButton(onClick = { data.performAction() }) { Text(label) }
        }
    }
}

private val IntrinsicHeight = 52.dp
