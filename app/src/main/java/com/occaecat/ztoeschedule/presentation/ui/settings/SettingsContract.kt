package com.occaecat.ztoeschedule.presentation.ui.settings

import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.data.model.DisplayMode

data class SettingsState(
    val colorTheme: ColorTheme = ColorTheme.System,
    val displayMode: DisplayMode = DisplayMode.Comfortable,
    val cornerRadius: Int = -1,
    val dynamicColors: Boolean = true,
    val isAmoled: Boolean = false,
    val liquidGlass: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val statusNotificationEnabled: Boolean = false
)

sealed interface SettingsAction {
    // Navigation
    data class Navigate(val route: SettingsRoute) : SettingsAction
    data object GoBack : SettingsAction

    // Theme
    data class SetTheme(val theme: ColorTheme) : SettingsAction
    data class SetDisplayMode(val mode: DisplayMode) : SettingsAction
    data class SetCornerRadius(val radius: Int) : SettingsAction
    data class SetDynamicColors(val enabled: Boolean) : SettingsAction
    data class SetAmoled(val enabled: Boolean) : SettingsAction
    data class SetLiquidGlass(val enabled: Boolean) : SettingsAction

    // Notifications
    data class SetNotificationsEnabled(val enabled: Boolean) : SettingsAction
    data class SetStatusNotificationEnabled(val enabled: Boolean) : SettingsAction
    
    // Data
    data object ResetSettings : SettingsAction
    data object ClearData : SettingsAction
    data object AddDemoLocation : SettingsAction
}

sealed class SettingsRoute {
    data object Main : SettingsRoute()
    data object Style : SettingsRoute()
    data object Notifications : SettingsRoute()
    data object Language : SettingsRoute()
    data object Developers : SettingsRoute()
    data object Integrations : SettingsRoute()
}
