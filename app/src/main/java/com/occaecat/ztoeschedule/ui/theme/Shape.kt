package com.occaecat.ztoeschedule.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CutCornerShape

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    
    // Expressive shapes for 2025
    large = RoundedCornerShape(topStart = 28.dp, topEnd = 4.dp, bottomEnd = 28.dp, bottomStart = 4.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Custom Star Shape for high-action buttons (like Add Address)
val OctagonShape = CutCornerShape(percent = 30)
val StarShape = RoundedCornerShape(50) // Fallback or specialized