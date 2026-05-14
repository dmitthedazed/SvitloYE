@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.occaecat.ztoeschedule.presentation.ui.glass

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

data class GlassNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LiquidGlassNavBar(
    items: List<GlassNavItem>,
    currentRoute: String?,
    backdrop: Backdrop,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val blurPx = with(density) { 28.dp.toPx() }
    val lensHeightPx = with(density) { 14.dp.toPx() }
    val lensAmountPx = with(density) { 28.dp.toPx() }

    val selectedIndex = remember(currentRoute, items) {
        items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    }

    val indicatorProgress by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "indicator"
    )

    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(50) },
                    effects = {
                        vibrancy()
                        blur(blurPx)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            lens(
                                refractionHeight = lensHeightPx,
                                refractionAmount = lensAmountPx,
                                chromaticAberration = true
                            )
                        }
                    }
                )
                .height(64.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
        ) {
            val itemWidth = maxWidth / items.size

            // Sliding tinted indicator behind the tabs
            Box(
                modifier = Modifier
                    .offset { IntOffset((itemWidth * indicatorProgress).roundToPx(), 0) }
                    .padding(8.dp)
                    .width(itemWidth - 16.dp)
                    .height(48.dp)
                    .background(
                        color = primaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(50)
                    )
                    .align(Alignment.CenterStart)
            )

            // Tab items drawn on top of the indicator
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .minimumInteractiveComponentSize()
                            .semantics(mergeDescendants = true) {
                                contentDescription = item.label
                            }
                            .clip(RoundedCornerShape(50))
                            .selectable(
                                selected = isSelected,
                                role = Role.Tab,
                                interactionSource = interactionSource,
                                indication = ripple(),
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (!isSelected) onNavigate(item.route)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) onPrimaryContainer else onSurfaceVariant
                            )
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                Text(
                                    text = item.label,
                                    modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
