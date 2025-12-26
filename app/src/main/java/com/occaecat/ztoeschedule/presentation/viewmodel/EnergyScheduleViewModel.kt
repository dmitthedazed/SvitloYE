package com.occaecat.ztoeschedule.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.occaecat.ztoeschedule.data.model.*
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.model.getUserMessage
import com.occaecat.ztoeschedule.domain.model.toAppError
import com.occaecat.ztoeschedule.domain.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class AddressDataState(
    val address: SavedAddress,
    val scheduleList: List<Schedule> = emptyList(),
    val groupedSchedule: List<GroupedSchedule> = emptyList(),
    val currentStatus: Schedule? = null,
    val isOffline: Boolean = false,
    val isLoading: Boolean = false,
    val lastUpdateTime: String = ""
)

@HiltViewModel
class EnergyScheduleViewModel @Inject constructor(
    private val repository: EnergyRepository,
    private val networkObserver: com.occaecat.ztoeschedule.domain.NetworkObserver,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                launch {
                    networkObserver.isConnected.collect { connected ->
                        val wasOffline = !_uiState.value.isConnected
                        _uiState.update { it.copy(isConnected = connected) }
                        if (connected && wasOffline) refreshAllSchedules()
                    }
                }
                launch { loadNotificationSettings() }
                launch { loadThemeSettings() }
                launch { checkTimeSync() }
                launch {
                    combine(repository.getOnboardingCompletedFlow(), repository.getSavedSelectionFlow()) { completed, selection ->
                        Pair(completed, selection)
                    }.collect { (completed, selection) ->
                        _uiState.update { it.copy(onboardingCompleted = completed, hasSavedSelection = selection != null, isInitialLoadComplete = true) }
                        loadSavedAddresses()
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isInitialLoadComplete = true, error = e.toAppError().getUserMessage()) }
            }
        }
    }

    fun refreshAllSchedules() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, lastLoadFailed = false) }
            launch {
                repository.getMessages().onSuccess { messages ->
                    _uiState.update { it.copy(infoMessages = messages, formattedMessage = formatMessages(messages)) }
                }
            }
            val addresses = repository.getSavedAddresses().sortedBy { it.priority }
            val deferreds = addresses.map { address -> async { loadSingleAddressData(address) } }
            val results = deferreds.awaitAll()
            _uiState.update { it.copy(addressDataList = results, isLoading = false) }
            refreshAllStatuses(addresses)
        }
    }

    private suspend fun loadSingleAddressData(address: SavedAddress): AddressDataState {
        val result = repository.getScheduleWithMessages(address.cherga, address.pidcherga)
        return if (result.isSuccess) {
            val data = result.getOrThrow()
            AddressDataState(
                address = address,
                scheduleList = data.schedules,
                groupedSchedule = ScheduleMapper.getGroupedSchedule(data.schedules),
                currentStatus = repository.getCurrentStatus(data.schedules),
                isOffline = false,
                lastUpdateTime = LocalTime.now().withNano(0).toString()
            )
        } else {
            val existing = _uiState.value.addressDataList.find { it.address.id == address.id }
            existing?.copy(isOffline = true) ?: AddressDataState(address = address, isOffline = true)
        }
    }

    fun loadSavedAddresses() {
        viewModelScope.launch {
            val addresses = repository.getSavedAddresses().sortedBy { it.priority }
            _uiState.update { it.copy(savedAddresses = addresses) }
            refreshAllSchedules()
        }
    }

    private fun refreshAllStatuses(addresses: List<SavedAddress>) {
        if (addresses.isEmpty()) {
            _uiState.update { it.copy(addressStatuses = emptyMap()) }; return
        }
        viewModelScope.launch {
            val statusMap = mutableMapOf<String, GroupedSchedule?>()
            addresses.forEach { address ->
                val cachedData = _uiState.value.addressDataList.find { it.address.id == address.id }
                if (cachedData != null && cachedData.groupedSchedule.isNotEmpty()) {
                    statusMap[address.id] = ScheduleMapper.getCurrentGroupedStatus(cachedData.groupedSchedule)
                } else {
                    repository.getSchedule(address.cherga, address.pidcherga).onSuccess { schedules ->
                        statusMap[address.id] = ScheduleMapper.getCurrentGroupedStatus(ScheduleMapper.getGroupedSchedule(schedules))
                    }
                }
            }
            _uiState.update { it.copy(addressStatuses = statusMap) }
        }
    }

    fun addSavedAddress(name: String, icon: String, remId: String, remName: String, cityId: String, cityName: String, streetId: String, streetName: String, addrId: String, addrName: String, cherga: Int, pid: Int) {
        viewModelScope.launch {
            val address = SavedAddress(
                name = name, iconName = icon, priority = repository.getSavedAddresses().size + 1,
                remId = remId, remName = remName, cityId = cityId, cityName = cityName,
                streetId = streetId, streetName = streetName, addressId = addrId,
                addressName = addrName, cherga = cherga, pidcherga = pid
            )
            repository.saveNewAddress(address)
            loadSavedAddresses()
            _uiState.update { it.copy(isAddingNewAddress = false) }
            NotificationScheduler.runImmediateCheck(context)
        }
    }

    fun deleteSavedAddress(id: String) {
        viewModelScope.launch { repository.deleteAddress(id); loadSavedAddresses(); NotificationScheduler.runImmediateCheck(context) }
    }

    fun updateAddressesOrder(list: List<SavedAddress>) {
        viewModelScope.launch { repository.reorderAddresses(list); loadSavedAddresses(); NotificationScheduler.runImmediateCheck(context) }
    }

    private fun loadNotificationSettings() {
        viewModelScope.launch { repository.getNotificationsEnabledFlow().collect { v -> _uiState.update { it.copy(notificationsEnabled = v) } } }
        viewModelScope.launch { repository.getNotificationAdvanceMinutesFlow().collect { v -> _uiState.update { it.copy(notificationAdvanceMinutes = v) } } }
        viewModelScope.launch { repository.getStatusNotificationEnabledFlow().collect { v -> _uiState.update { it.copy(statusNotificationEnabled = v) } } }
        viewModelScope.launch { repository.getLiveActivityEnabledFlow().collect { v -> _uiState.update { it.copy(liveActivityEnabled = v) } } }
        viewModelScope.launch { repository.getSmartNotificationSettingsFlow().collect { v -> _uiState.update { it.copy(smartNotificationSettings = v) } } }
        viewModelScope.launch { repository.getNotificationModeFlow().collect { v -> _uiState.update { it.copy(notificationMode = v) } } }
    }

    private fun loadThemeSettings() {
        viewModelScope.launch { repository.getDisplayModeFlow().collect { v -> _uiState.update { it.copy(displayMode = v) } } }
        viewModelScope.launch { repository.getColorThemeFlow().collect { v -> _uiState.update { it.copy(colorTheme = v) } } }
        viewModelScope.launch { repository.getFontScaleFlow().collect { v -> _uiState.update { it.copy(fontScale = v) } } }
    }

    fun setDisplayMode(m: DisplayMode) = viewModelScope.launch { repository.setDisplayMode(m) }
    fun setColorTheme(t: ColorTheme) = viewModelScope.launch { repository.setColorTheme(t) }
    fun setFontScale(s: FontScale) = viewModelScope.launch { repository.setFontScale(s) }
    fun setNotificationsEnabled(e: Boolean) = viewModelScope.launch { repository.setNotificationsEnabled(e) }
    fun setNotificationAdvanceMinutes(m: Int) = viewModelScope.launch { repository.setNotificationAdvanceMinutes(m) }
    fun setStatusNotificationEnabled(e: Boolean) = viewModelScope.launch { repository.setStatusNotificationEnabled(e) }
    fun setLiveActivityEnabled(e: Boolean) = viewModelScope.launch { repository.setLiveActivityEnabled(e) }
    fun setSmartNotificationSettings(s: SmartNotificationSettings) = viewModelScope.launch { repository.saveSmartNotificationSettings(s) }
    fun setNotificationMode(m: Int) = viewModelScope.launch { repository.setNotificationMode(m) }
    fun completeOnboarding() = viewModelScope.launch { repository.setOnboardingCompleted() }
    fun resetOnboarding() = viewModelScope.launch { repository.resetOnboarding() }
    fun startAddingAddress() { _uiState.update { it.copy(isAddingNewAddress = true) } }
    fun cancelAddingAddress() { _uiState.update { it.copy(isAddingNewAddress = false) } }
    fun dismissTimeSyncWarning() { _uiState.update { it.copy(isTimeOutOfSync = false) } }
    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun clearData() = viewModelScope.launch { repository.clearPreferences(); _uiState.value = UiState() }
    fun setShowWidgetConfig(show: Boolean) { _uiState.update { it.copy(showWidgetConfig = show) } }
    
    fun selectWidgetAddress(address: SavedAddress) {
        viewModelScope.launch {
            repository.setPrimaryAddress(address.id)
            loadSavedAddresses()
            setShowWidgetConfig(false)
            NotificationScheduler.runImmediateCheck(context)
        }
    }

    private fun checkTimeSync() {
        viewModelScope.launch {
            repository.getServerTime().onSuccess { serverMs ->
                if (Math.abs(serverMs - System.currentTimeMillis()) / 60000 > 5) {
                    _uiState.update { it.copy(isTimeOutOfSync = true) }
                }
            }
        }
    }

    fun startInspectingAddress(address: SavedAddress) {
        _uiState.update { it.copy(inspectedAddress = address, isInspectingLoading = true) }
        viewModelScope.launch {
            val data = loadSingleAddressData(address)
            _uiState.update { it.copy(inspectedScheduleList = data.scheduleList, inspectedGroupedSchedule = data.groupedSchedule, isInspectingLoading = false) }
        }
    }
    fun stopInspectingAddress() { _uiState.update { it.copy(inspectedAddress = null) } }

    fun retryLoading() { refreshAllSchedules() }
    fun loadScheduleWithMessages(cherga: Int, pid: Int) { refreshAllSchedules() }

    private fun formatMessages(m: List<ScheduleMessagePart>): String = m.sortedBy { it.id }.joinToString("\n") { it.text }
        .replace("&quot;", "\"").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")

    fun saveSelection(remId: String?, remName: String?, cityId: String?, cityName: String?, streetId: String?, streetName: String?, addrId: String, addrName: String, cherga: Int, pid: Int, customName: String = "Дім", icon: String = "home") {
        viewModelScope.launch {
            repository.saveCompleteSelection(remId, remName, cityId, cityName, streetId, streetName, addrId, addrName, cherga, pid)
            val addr = SavedAddress(name = customName, iconName = icon, priority = 1, remId = remId ?: "", remName = remName ?: "", cityId = cityId ?: "", cityName = cityName ?: "", streetId = streetId ?: "", streetName = streetName ?: "", addressId = addrId, addressName = addrName, cherga = cherga, pidcherga = pid)
            repository.saveNewAddress(addr)
            loadSavedAddresses()
        }
    }

    fun loadRemList() { viewModelScope.launch { repository.getRemList().onSuccess { r -> _uiState.update { it.copy(remList = r) } } } }
    fun loadCityList(id: String) { viewModelScope.launch { repository.getCityList(id).onSuccess { r -> _uiState.update { it.copy(cityList = r) } } } }
    fun loadStreetList(id: String) { viewModelScope.launch { repository.getStreetList(id).onSuccess { r -> _uiState.update { it.copy(streetList = r) } } } }
    fun loadAddressList(id: String) { viewModelScope.launch { repository.getAddressList(id).onSuccess { r -> _uiState.update { it.copy(addressList = r, filteredHouseNumbers = repository.getAllParsedHouseNumbers(r)) } } } }
    fun filterHouseNumbers(q: String) { _uiState.update { it.copy(houseNumberSearchQuery = q) } }
    fun clearHouseNumberSearch() { _uiState.update { it.copy(houseNumberSearchQuery = "") } }
}

data class UiState(
    val remList: List<Rem> = emptyList(),
    val cityList: List<City> = emptyList(),
    val streetList: List<Street> = emptyList(),
    val addressList: List<Address> = emptyList(),
    val savedAddresses: List<SavedAddress> = emptyList(),
    val addressDataList: List<AddressDataState> = emptyList(),
    val isAddingNewAddress: Boolean = false,
    val addressStatuses: Map<String, GroupedSchedule?> = emptyMap(),
    val filteredHouseNumbers: List<ParsedHouseNumber> = emptyList(),
    val houseNumberSearchQuery: String = "",
    val scheduleList: List<Schedule> = emptyList(),
    val groupedSchedule: List<GroupedSchedule> = emptyList(),
    val infoMessages: List<ScheduleMessagePart> = emptyList(),
    val formattedMessage: String = "",
    val currentStatus: Schedule? = null,
    val lastUpdateTime: String = "",
    val isLoading: Boolean = false,
    val isInitialLoadComplete: Boolean = false,
    val isTimeOutOfSync: Boolean = false,
    val error: String? = null,
    val hasSavedSelection: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val notificationAdvanceMinutes: Int = 15,
    val statusNotificationEnabled: Boolean = false,
    val liveActivityEnabled: Boolean = false,
    val isConnected: Boolean = true,
    val retryCountdown: Int = 0,
    val lastLoadFailed: Boolean = false,
    val isOffline: Boolean = false,
    val showWidgetConfig: Boolean = false,
    val notificationMode: Int = 0,
    val displayMode: DisplayMode = DisplayMode.COMFORTABLE,
    val colorTheme: ColorTheme = ColorTheme.SYSTEM,
    val fontScale: FontScale = FontScale.NORMAL,
    val smartNotificationSettings: SmartNotificationSettings = SmartNotificationSettings(),
    val inspectedAddress: SavedAddress? = null,
    val inspectedScheduleList: List<Schedule> = emptyList(),
    val inspectedGroupedSchedule: List<GroupedSchedule> = emptyList(),
    val isInspectingLoading: Boolean = false
)