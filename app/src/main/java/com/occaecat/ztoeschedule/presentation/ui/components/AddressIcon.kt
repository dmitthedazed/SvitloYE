package com.occaecat.ztoeschedule.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Icon a user picked for a saved address (see the address picker's icon grid). */
fun addressIconFor(name: String): ImageVector = when (name) {
    "home" -> Icons.Default.Home
    "apartment" -> Icons.Default.Apartment
    "work" -> Icons.Default.Work
    "school" -> Icons.Default.School
    "star" -> Icons.Default.Star
    "person" -> Icons.Default.Person
    "favorite" -> Icons.Default.Favorite
    "place" -> Icons.Default.Place
    "store" -> Icons.Default.Store
    else -> Icons.Default.LocationOn
}

/**
 * Address icon badge shared by "Мої адреси" and the home screen: the main address
 * gets the filled expressive shape, others a tonal circle.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddressIconBadge(
    iconName: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .background(
                if (isPrimary) colorScheme.primary else colorScheme.secondaryContainer,
                if (isPrimary) MaterialShapes.Cookie9Sided.toShape() else CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = addressIconFor(iconName),
            contentDescription = null,
            modifier = Modifier.size(size / 2),
            tint = if (isPrimary) colorScheme.onPrimary else colorScheme.onSecondaryContainer
        )
    }
}
