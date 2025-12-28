package com.occaecat.ztoeschedule.presentation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Reusable M3 Indication that scales the component down when pressed
 * and adds a highlight overlay when hovered (stylus/mouse).
 */
object ScaleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return ScaleNode(interactionSource)
    }

    override fun equals(other: Any?): Boolean = other === ScaleIndication
    override fun hashCode(): Int = 0
}

private class ScaleNode(private val interactionSource: InteractionSource) :
    Modifier.Node(), DrawModifierNode {

    private val animatedScalePercent = Animatable(1f)
    private val animatedHoverAlpha = Animatable(0f)

    private suspend fun animateToPressed() {
        animatedScalePercent.animateTo(0.97f, spring())
    }

    private suspend fun animateToResting() {
        animatedScalePercent.animateTo(1f, spring())
    }

    private suspend fun animateHover(visible: Boolean) {
        animatedHoverAlpha.animateTo(if (visible) 0.1f else 0f, spring())
    }

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> animateToPressed()
                    is PressInteraction.Release -> animateToResting()
                    is PressInteraction.Cancel -> animateToResting()
                    is HoverInteraction.Enter -> animateHover(true)
                    is HoverInteraction.Exit -> animateHover(false)
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val hoverAlpha = animatedHoverAlpha.value
        
        scale(scale = animatedScalePercent.value) {
            this@draw.drawContent()
            if (hoverAlpha > 0f) {
                drawRect(
                    color = Color.White,
                    alpha = hoverAlpha,
                    size = size
                )
            }
        }
    }
}
