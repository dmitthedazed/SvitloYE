package com.occaecat.ztoeschedule.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.data.model.DisplayMode

/**
 * The app theme driven by every appearance preference (theme, Material You, true black,
 * density, liquid glass). Every activity uses this so secondary screens look like the main one.
 */
@Composable
fun AppTheme(
    preferences: EnergyPreferencesManager,
    content: @Composable () -> Unit
) {
    val colorTheme by preferences.colorThemeFlow.collectAsStateWithLifecycle(initialValue = ColorTheme.System)
    val dynamicColors by preferences.dynamicColorsFlow.collectAsStateWithLifecycle(initialValue = true)
    val isAmoled by preferences.isAmoledFlow.collectAsStateWithLifecycle(initialValue = false)
    val displayMode by preferences.displayModeFlow.collectAsStateWithLifecycle(initialValue = DisplayMode.Comfortable)
    val liquidGlass by preferences.liquidGlassFlow.collectAsStateWithLifecycle(initialValue = false)

    SvitloYeZhytomyrTheme(
        themePreference = colorTheme,
        displayMode = displayMode,
        liquidGlass = liquidGlass,
        dynamicColor = dynamicColors,
        isAmoled = isAmoled,
        content = content
    )
}
