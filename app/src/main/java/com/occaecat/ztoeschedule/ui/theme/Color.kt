package com.occaecat.ztoeschedule.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable

// ═══════════════════════════════════════════════════════════════════════════
// Tomato Expressive Palette — for Accent UI
// ═══════════════════════════════════════════════════════════════════════════
val TomatoRed = Color(0xFFE53935)       // Vibrant red - primary
val TomatoOrange = Color(0xFFFF7043)    // Warm orange - secondary
val TomatoYellow = Color(0xFFFFCA28)    // Sunny yellow - tertiary
val TomatoGreen = Color(0xFF66BB6A)     // Fresh green - success
val TomatoPink = Color(0xFFEC407A)      // Accent pink - notifications

// Gradient pairs for accent screens
val GradientWelcomeStart = Color(0xFFFF9800)   // Orange
val GradientWelcomeEnd = Color(0xFFFFEB3B)     // Yellow
val GradientAddressStart = Color(0xFFE53935)   // Red
val GradientAddressEnd = Color(0xFFFF7043)     // Orange
val GradientScheduleStart = Color(0xFFFFCA28)  // Yellow
val GradientScheduleEnd = Color(0xFF66BB6A)    // Green
val GradientNotificationsStart = Color(0xFFEC407A) // Pink
val GradientNotificationsEnd = Color(0xFFE53935)   // Red

// ═══════════════════════════════════════════════════════════════════════════
// Energy Theme - Light
// ═══════════════════════════════════════════════════════════════════════════
val PrimaryLight = Color(0xFF6750A4)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFEADDFF)
val OnPrimaryContainerLight = Color(0xFF21005D)

val SecondaryLight = Color(0xFF625B71)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE8DEF8)
val OnSecondaryContainerLight = Color(0xFF1D192B)

val TertiaryLight = Color(0xFF7D5260)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFD8E4)
val OnTertiaryContainerLight = Color(0xFF31111D)

// Energy Theme - Dark
val PrimaryDark = Color(0xFFD0BCFF)
val OnPrimaryDark = Color(0xFF381E72)
val PrimaryContainerDark = Color(0xFF4F378B)
val OnPrimaryContainerDark = Color(0xFFEADDFF)

val SecondaryDark = Color(0xFFCCC2DC)
val OnSecondaryDark = Color(0xFF332D41)
val SecondaryContainerDark = Color(0xFF4A4458)
val OnSecondaryContainerDark = Color(0xFFE8DEF8)

val TertiaryDark = Color(0xFFEFB8C8)
val OnTertiaryDark = Color(0xFF492532)
val TertiaryContainerDark = Color(0xFF633B48)
val OnTertiaryContainerDark = Color(0xFFFFD8E4)

object CustomColors {
    var black = false

    val listItemColors: ListItemColors
        @Composable get() =
            ListItemDefaults.colors(
                containerColor = if (!black) {
                    colorScheme.surfaceBright
                } else {
                    colorScheme.surfaceContainerHigh
                }
            )
}
