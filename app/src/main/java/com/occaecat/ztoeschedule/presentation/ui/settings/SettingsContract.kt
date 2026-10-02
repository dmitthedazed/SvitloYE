package com.occaecat.ztoeschedule.presentation.ui.settings

import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.data.model.NotificationSettings

data class SettingsState(
    val colorTheme: ColorTheme = ColorTheme.System,
    val displayMode: DisplayMode = DisplayMode.Comfortable,
    val dynamicColors: Boolean = true,
    val isAmoled: Boolean = false,
    val liquidGlass: Boolean = false,
    val notifications: NotificationSettings = NotificationSettings(),
    val analyticsEnabled: Boolean = true
)

sealed interface SettingsAction {
    // Navigation
    data class Navigate(val route: SettingsRoute) : SettingsAction
    data object GoBack : SettingsAction

    // Theme
    data class SetTheme(val theme: ColorTheme) : SettingsAction
    data class SetDisplayMode(val mode: DisplayMode) : SettingsAction
    data class SetDynamicColors(val enabled: Boolean) : SettingsAction
    data class SetAmoled(val enabled: Boolean) : SettingsAction
    data class SetLiquidGlass(val enabled: Boolean) : SettingsAction

    // Notifications
    data class UpdateNotifications(val transform: (NotificationSettings) -> NotificationSettings) : SettingsAction
    
    // Data
    data object ResetSettings : SettingsAction
    data object ClearData : SettingsAction
    data class SetAnalytics(val enabled: Boolean) : SettingsAction
    data object AddDemoLocation : SettingsAction
    data object AddPreviewLocation : SettingsAction
}

sealed class SettingsRoute {
    data object Main : SettingsRoute()
    data object Style : SettingsRoute()
    data object Notifications : SettingsRoute()
    data object Language : SettingsRoute()
    data object Developers : SettingsRoute()
    data object Integrations : SettingsRoute()
}
