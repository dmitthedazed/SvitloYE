package com.occaecat.ztoeschedule.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.data.model.NotificationSettings
import com.occaecat.ztoeschedule.domain.notification.AlarmScheduler
import com.occaecat.ztoeschedule.domain.notification.NotificationSync
import com.occaecat.ztoeschedule.domain.notification.Notifier
import com.occaecat.ztoeschedule.domain.notification.requestTileUpdate
import com.occaecat.ztoeschedule.widget.WidgetUpdater
import com.occaecat.ztoeschedule.analytics.AnalyticsManager
import com.occaecat.ztoeschedule.domain.notification.StatusNotificationService
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsAction
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsRoute
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsState
import com.occaecat.ztoeschedule.data.model.SavedAddress
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesManager: EnergyPreferencesManager,
    private val repository: EnergyRepository,
    private val alarmScheduler: AlarmScheduler,
    private val notifier: Notifier,
    private val widgetUpdater: WidgetUpdater,
    private val analytics: AnalyticsManager
) : ViewModel() {

    private val _backStack = mutableStateListOf<SettingsRoute>(SettingsRoute.Main)
    val backStack: List<SettingsRoute> = _backStack

    private val flows: List<kotlinx.coroutines.flow.Flow<Any>> = listOf(
        preferencesManager.colorThemeFlow,
        preferencesManager.displayModeFlow,
        preferencesManager.dynamicColorsFlow,
        preferencesManager.isAmoledFlow,
        preferencesManager.liquidGlassFlow,
        preferencesManager.notificationSettingsFlow,
        preferencesManager.analyticsEnabledFlow
    )

    val state: StateFlow<SettingsState> = combine(flows) { args ->
        SettingsState(
            colorTheme = args[0] as ColorTheme,
            displayMode = args[1] as DisplayMode,
            dynamicColors = args[2] as Boolean,
            isAmoled = args[3] as Boolean,
            liquidGlass = args[4] as Boolean,
            notifications = args[5] as NotificationSettings,
            analyticsEnabled = args[6] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, SettingsState())

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.Navigate -> {
                _backStack.add(action.route)
            }
            is SettingsAction.GoBack -> {
                if (_backStack.size > 1) {
                    _backStack.removeAt(_backStack.lastIndex)
                }
            }
            is SettingsAction.SetTheme -> {
                viewModelScope.launch { preferencesManager.setColorTheme(action.theme) }
            }
            is SettingsAction.SetDisplayMode -> {
                viewModelScope.launch { preferencesManager.setDisplayMode(action.mode) }
            }
            is SettingsAction.SetDynamicColors -> {
                viewModelScope.launch {
                    preferencesManager.setDynamicColors(action.enabled)
                    // Widgets read this only when drawn
                    widgetUpdater.updateAll()
                }
            }
            is SettingsAction.SetAmoled -> {
                viewModelScope.launch { preferencesManager.setIsAmoled(action.enabled) }
            }
            is SettingsAction.SetLiquidGlass -> {
                viewModelScope.launch { preferencesManager.setLiquidGlass(action.enabled) }
            }
            is SettingsAction.UpdateNotifications -> {
                viewModelScope.launch {
                    val old = preferencesManager.notificationSettingsFlow.first()
                    val new = action.transform(old)
                    if (new == old) return@launch
                    preferencesManager.saveNotificationSettings(new)
                    if (new.alertsEnabled != old.alertsEnabled) analytics.logNotificationToggle(new.alertsEnabled)
                    if (new.statusNotification != old.statusNotification) {
                        if (new.statusNotification) StatusNotificationService.start(context)
                        else StatusNotificationService.stop(context)
                    }
                    NotificationSync.syncNow(context, fetch = false)
                }
            }
            is SettingsAction.ResetSettings -> {
                viewModelScope.launch { 
                    preferencesManager.resetOnboarding()
                }
            }
            is SettingsAction.SetAnalytics -> {
                // The application applies it to Firebase as soon as it's stored
                viewModelScope.launch { preferencesManager.setAnalyticsEnabled(action.enabled) }
            }
            is SettingsAction.ClearData -> {
                viewModelScope.launch {
                    // Everything the dialog promises: addresses, cached schedules and settings,
                    // plus whatever still runs on their behalf
                    repository.clearAllData()
                    alarmScheduler.cancelAll()
                    StatusNotificationService.stop(context)
                    notifier.cancelAll()
                    NotificationSync.cancelAll(context)
                    widgetUpdater.updateAll()
                    requestTileUpdate(context)
                }
            }
            is SettingsAction.AddDemoLocation -> {
                viewModelScope.launch {
                    val demoAddress = SavedAddress(
                        name = com.occaecat.ztoeschedule.domain.debug.MockScheduleProvider.getDemoAddressName(),
                        iconName = "star",
                        priority = repository.getSavedAddresses().size + 1,
                        remId = "0",
                        remName = "DEMO",
                        cityId = "0",
                        cityName = "Тест",
                        streetId = "0",
                        streetName = "Демо вулиця",
                        addressId = "0",
                        addressName = "Тест 1",
                        cherga = 9999,
                        pidcherga = 9999
                    )
                    repository.saveNewAddress(demoAddress)
                }
            }
            is SettingsAction.AddPreviewLocation -> {
                viewModelScope.launch {
                    repository.saveNewAddress(
                        SavedAddress(
                            name = com.occaecat.ztoeschedule.domain.debug.MockScheduleProvider.getPreviewAddressName(),
                            iconName = "favorite",
                            priority = repository.getSavedAddresses().size + 1,
                            remId = "0",
                            remName = "Житомирський",
                            cityId = "0",
                            cityName = "місто Житомир",
                            streetId = "0",
                            streetName = "вул. Київська",
                            addressId = "0",
                            addressName = "12",
                            cherga = 9998,
                            pidcherga = 9998
                        )
                    )
                }
            }
        }
    }
}
