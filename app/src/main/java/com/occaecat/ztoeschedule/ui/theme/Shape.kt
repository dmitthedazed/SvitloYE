package com.occaecat.ztoeschedule.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class MaterialShapeScale(
    val extraSmall: Int,
    val small: Int,
    val medium: Int,
    val large: Int,
    val extraLarge: Int,
)

internal fun materialShapeScaleForCornerRadius(baseCornerRadius: Int): MaterialShapeScale {
    val safeBase = baseCornerRadius.coerceAtLeast(16)
    return MaterialShapeScale(
        extraSmall = (baseCornerRadius / 3f).roundToInt().coerceAtLeast(4),
        small = (baseCornerRadius / 2f).roundToInt().coerceAtLeast(8),
        medium = (baseCornerRadius * 2f / 3f).roundToInt().coerceAtLeast(12),
        large = safeBase,
        extraLarge = (baseCornerRadius * 4f / 3f).roundToInt().coerceAtLeast(24),
    )
}

fun materialShapesForCornerRadius(baseCornerRadius: Int): Shapes {
    val scale = materialShapeScaleForCornerRadius(baseCornerRadius)
    return Shapes(
        extraSmall = RoundedCornerShape(scale.extraSmall.dp),
        small = RoundedCornerShape(scale.small.dp),
        medium = RoundedCornerShape(scale.medium.dp),
        large = RoundedCornerShape(scale.large.dp),
        extraLarge = RoundedCornerShape(scale.extraLarge.dp),
    )
}

/**
 * Material 3 Expressive shapes.
 * 
 * Expressive design uses larger corner radii for a more friendly, 
 * approachable look while maintaining hierarchy.
 */
val ExpressiveShapes = Shapes(
    // Extra small - chips, small badges
    extraSmall = RoundedCornerShape(8.dp),
    
    // Small - text fields, small cards
    small = RoundedCornerShape(12.dp),
    
    // Medium - cards, dialogs (default)
    medium = RoundedCornerShape(16.dp),
    
    // Large - FABs, large cards
    large = RoundedCornerShape(24.dp),
    
    // Extra large - bottom sheets, full-screen dialogs
    extraLarge = RoundedCornerShape(32.dp)
)

// Grouped List Shapes (Tomato Style)
val TopItemShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
val MiddleItemShape = RoundedCornerShape(4.dp)
val BottomItemShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
val SingleItemShape = RoundedCornerShape(24.dp)
