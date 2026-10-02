package com.occaecat.ztoeschedule.presentation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * Expressive shape badge used as the hero of onboarding-style pages.
 * Optionally pops in with a spring (only for a screen's first appearance: it fights page
 * transitions) and/or slowly spins the shape while [content] stays upright.
 */
@Composable
fun ExpressiveHeroBadge(
    shape: Shape,
    containerColor: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    rotate: Boolean = false,
    popIn: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val rotation = if (rotate) {
        val transition = rememberInfiniteTransition(label = "hero_rotation")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 30_000, easing = LinearEasing)),
            label = "hero_rotation_value"
        )
    } else null

    val scale = remember { Animatable(if (popIn) 0.6f else 1f) }
    LaunchedEffect(Unit) {
        if (popIn) scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = rotation?.value ?: 0f }
                .background(containerColor, shape)
        )
        content()
    }
}
