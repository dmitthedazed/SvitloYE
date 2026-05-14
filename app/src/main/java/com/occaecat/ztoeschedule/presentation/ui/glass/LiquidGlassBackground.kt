package com.occaecat.ztoeschedule.presentation.ui.glass

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiquidGlassBackground(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface = MaterialTheme.colorScheme.surface

    val infinite = rememberInfiniteTransition(label = "bg")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "phase"
    )

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(surface)

                val cx1 = size.width * (0.15f + 0.12f * sin(phase))
                val cy1 = size.height * (0.20f + 0.08f * cos(phase * 0.7f))
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(primary.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(cx1, cy1),
                        radius = size.minDimension * 0.7f
                    )
                )

                val cx2 = size.width * (0.78f + 0.10f * cos(phase * 1.3f))
                val cy2 = size.height * (0.55f + 0.08f * sin(phase * 0.9f))
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(secondary.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(cx2, cy2),
                        radius = size.minDimension * 0.55f
                    )
                )

                val cx3 = size.width * (0.45f + 0.08f * sin(phase * 0.5f))
                val cy3 = size.height * (0.82f + 0.05f * cos(phase * 1.1f))
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(tertiary.copy(alpha = 0.40f), Color.Transparent),
                        center = Offset(cx3, cy3),
                        radius = size.minDimension * 0.5f
                    )
                )
            }
    )
}
