package com.occaecat.ztoeschedule.presentation.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsAction
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsRoute
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsState
import com.occaecat.ztoeschedule.data.model.SavedAddress
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: EnergyPreferencesManager,
    private val repository: EnergyRepository
) : ViewModel() {

    private val _backStack = mutableStateListOf<SettingsRoute>(SettingsRoute.Main)
    val backStack: List<SettingsRoute> = _backStack

    private val flows: List<kotlinx.coroutines.flow.Flow<Any>> = listOf(
        preferencesManager.colorThemeFlow,
        preferencesManager.displayModeFlow,
        preferencesManager.cornerRadiusFlow,
        preferencesManager.dynamicColorsFlow,
        preferencesManager.isAmoledFlow,
        preferencesManager.liquidGlassFlow,
        preferencesManager.notificationsEnabledFlow,
        preferencesManager.statusNotificationEnabledFlow
    )

    val state: StateFlow<SettingsState> = combine(flows) { args ->
        SettingsState(
            colorTheme = args[0] as ColorTheme,
            displayMode = args[1] as DisplayMode,
            cornerRadius = args[2] as Int,
            dynamicColors = args[3] as Boolean,
            isAmoled = args[4] as Boolean,
            liquidGlass = args[5] as Boolean,
            notificationsEnabled = args[6] as Boolean,
            statusNotificationEnabled = args[7] as Boolean
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
            is SettingsAction.SetCornerRadius -> {
                viewModelScope.launch { preferencesManager.setCornerRadius(action.radius) }
            }
            is SettingsAction.SetDynamicColors -> {
                viewModelScope.launch { preferencesManager.setDynamicColors(action.enabled) }
            }
            is SettingsAction.SetAmoled -> {
                viewModelScope.launch { preferencesManager.setIsAmoled(action.enabled) }
            }
            is SettingsAction.SetLiquidGlass -> {
                viewModelScope.launch { preferencesManager.setLiquidGlass(action.enabled) }
            }
            is SettingsAction.SetNotificationsEnabled -> {
                viewModelScope.launch { preferencesManager.setNotificationsEnabled(action.enabled) }
            }
            is SettingsAction.SetStatusNotificationEnabled -> {
                viewModelScope.launch { preferencesManager.setStatusNotificationEnabled(action.enabled) }
            }
            is SettingsAction.ResetSettings -> {
                viewModelScope.launch { 
                    preferencesManager.resetOnboarding()
                }
            }
            is SettingsAction.ClearData -> {
                viewModelScope.launch {
                    preferencesManager.clearPreferences()
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
        }
    }
}
