package com.occaecat.ztoeschedule.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive shapes.
 * 
 * Expressive design uses larger corner radii for a more friendly, 
 * approachable look while maintaining hierarchy.
 */
val Shapes = Shapes(
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
