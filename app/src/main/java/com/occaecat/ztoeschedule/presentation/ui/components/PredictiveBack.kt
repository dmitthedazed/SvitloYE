package com.occaecat.ztoeschedule.presentation.ui.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Predictive back for in-screen navigation (steps, sub-pages): while the gesture is in progress
 * the content shrinks towards the swipe edge with rounded corners, as the Material spec describes.
 * [onBack] runs when the gesture is committed (or on a plain back press).
 */
@Composable
fun PredictiveBackContainer(
    enabled: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier.predictiveBack(enabled, onBack)) { content() }
}

@Composable
fun Modifier.predictiveBack(enabled: Boolean, onBack: () -> Unit): Modifier {
    val progress = remember { Animatable(0f) }
    val edge = remember { Animatable(0f) } // 1 = swipe from the left edge, -1 = from the right
    val scope = rememberCoroutineScope()
    val maxShift = with(LocalDensity.current) { 8.dp.toPx() }
    val currentOnBack by rememberUpdatedState(onBack)

    PredictiveBackHandler(enabled = enabled) { events ->
        try {
            events.collect { e ->
                progress.snapTo(e.progress)
                edge.snapTo(if (e.swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f)
            }
            currentOnBack()
            scope.launch { progress.animateTo(0f, tween(220)) }
        } catch (c: CancellationException) {
            scope.launch { progress.animateTo(0f, tween(220)) }
            throw c
        }
    }

    return this
        .graphicsLayer {
            val p = progress.value
            val s = 1f - 0.1f * p
            scaleX = s
            scaleY = s
            translationX = edge.value * maxShift * p
        }
        .clip(RoundedCornerShape((28 * progress.value).dp))
}
