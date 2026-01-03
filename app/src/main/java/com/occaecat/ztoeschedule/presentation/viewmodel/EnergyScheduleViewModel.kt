package com.occaecat.ztoeschedule.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.occaecat.ztoeschedule.data.model.*
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber
import com.occaecat.ztoeschedule.data.repository.ConsumerCategory
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.model.getUserMessage
import com.occaecat.ztoeschedule.domain.model.toAppError
import com.occaecat.ztoeschedule.domain.notification.NotificationScheduler
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import androidx.lifecycle.SavedStateHandle
import androidx.compose.runtime.Stable

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
    private val timeProvider: TimeProvider,
    private val savedStateHandle: SavedStateHandle,
    private val scheduledAlarmManager: com.occaecat.ztoeschedule.domain.notification.ScheduledAlarmManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val restoredInspectId = savedStateHandle.get<String>("inspected_id")
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
                launch {
                    combine(repository.getOnboardingCompletedFlow(), repository.getSavedSelectionFlow()) { completed, selection ->
                        Pair(completed, selection)
                    }.collect { (completed, selection) ->
                        val wasInitiallyLoading = !_uiState.value.isInitialLoadComplete
                        _uiState.update { it.copy(onboardingCompleted = completed, hasSavedSelection = selection != null, isInitialLoadComplete = true) }
                        
                        // Only load addresses if:
                        // 1. This is initial load (wasInitiallyLoading = true), OR
                        // 2. User has completed onboarding and has selection
                        if (wasInitiallyLoading || (completed && selection != null)) {
                            loadSavedAddresses()
                        }
                        
                        if (restoredInspectId != null) {
                            val addresses = repository.getSavedAddresses()
                            val found = addresses.find { it.id == restoredInspectId }
                            if (found != null) { startInspectingAddress(found) }
                        }
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
            launch { repository.getMessages().onSuccess { messages -> _uiState.update { it.copy(infoMessages = messages, formattedMessage = formatMessages(messages)) } } }
            val addresses = repository.getSavedAddresses().sortedBy { it.priority }
            val results = addresses.map { async { loadSingleAddressData(it) } }.awaitAll()
            _uiState.update { it.copy(addressDataList = results, isLoading = false) }
            refreshAllStatuses(addresses)
        }
    }

    private suspend fun loadSingleAddressData(address: SavedAddress): AddressDataState {
        val result = repository.getScheduleWithMessages(address.cherga, address.pidcherga)
        return if (result.isSuccess) {
            val data = result.getOrThrow()
            val groupedSchedule = ScheduleMapper.getGroupedSchedule(data.schedules)
            
            // Schedule alarms for this address whenever schedule is loaded
            scheduledAlarmManager.scheduleAlarmsForAddress(
                address = Address(
                    id = address.addressId,
                    name = address.addressName,
                    cherga = address.cherga,
                    pidcherga = address.pidcherga
                ),
                schedules = groupedSchedule
            )
            
            AddressDataState(address, data.schedules, groupedSchedule, repository.getCurrentStatus(data.schedules), false, false, LocalTime.now().withNano(0).toString())
        } else {
            _uiState.value.addressDataList.find { it.address.id == address.id }?.copy(isOffline = true) ?: AddressDataState(address, isOffline = true)
        }
    }

    fun loadSavedAddresses() { viewModelScope.launch { val addrs = repository.getSavedAddresses().sortedBy { it.priority }; _uiState.update { it.copy(savedAddresses = addrs) }; refreshAllSchedules() } }

    private fun refreshAllStatuses(addresses: List<SavedAddress>) {
        if (addresses.isEmpty()) { _uiState.update { it.copy(addressStatuses = emptyMap()) }; return }
        viewModelScope.launch {
            val statusMap = mutableMapOf<String, GroupedSchedule?>()
            val nowMs = timeProvider.now()
            addresses.forEach { address ->
                val cached = _uiState.value.addressDataList.find { it.address.id == address.id }
                if (cached != null && cached.groupedSchedule.isNotEmpty()) { statusMap[address.id] = ScheduleMapper.getCurrentGroupedStatus(cached.groupedSchedule, nowMs) }
                else { repository.getSchedule(address.cherga, address.pidcherga).onSuccess { s -> statusMap[address.id] = ScheduleMapper.getCurrentGroupedStatus(ScheduleMapper.getGroupedSchedule(s), nowMs) } }
            }
            _uiState.update { it.copy(addressStatuses = statusMap) }
        }
    }

    fun addSavedAddress(name: String, icon: String, rI: String, rN: String, cI: String, cN: String, sI: String, sN: String, aI: String, aN: String, c: Int, p: Int) {
        viewModelScope.launch {
            // Check for duplicates by addressId (unique identifier)
            if (repository.isAddressAlreadyAdded(aI)) {
                // Address already exists, show error
                _uiState.update { it.copy(
                    isAddingNewAddress = false,
                    error = "Ця адреса вже додана до списку"
                ) }
                return@launch
            }
            
            val address = SavedAddress(
                name = name, iconName = icon, priority = repository.getSavedAddresses().size + 1,
                remId = rI, remName = rN, cityId = cI, cityName = cN,
                streetId = sI, streetName = sN, addressId = aI, addressName = aN,
                cherga = c, pidcherga = p
            )
            repository.saveNewAddress(address)
            loadSavedAddresses()
            _uiState.update { it.copy(isAddingNewAddress = false) }
            NotificationScheduler.runImmediateCheck(context)
        }
    }

    fun deleteSavedAddress(id: String) { viewModelScope.launch { repository.deleteAddress(id); loadSavedAddresses(); NotificationScheduler.runImmediateCheck(context) } }    
    fun updateAddressesOrder(list: List<SavedAddress>) { viewModelScope.launch { repository.reorderAddresses(list); loadSavedAddresses(); NotificationScheduler.runImmediateCheck(context) } }

    /**
     * Add demo/test location that changes status every minute
     * Useful for debugging notification alerts
     */
    fun addDemoLocation() {
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
            loadSavedAddresses()
            _uiState.update { it.copy(infoMessage = "Додано демо-локацію для тестування") }
            NotificationScheduler.runImmediateCheck(context)
        }
    }


    private fun loadNotificationSettings() {
        viewModelScope.launch { repository.getNotificationsEnabledFlow().collect { v -> _uiState.update { it.copy(notificationsEnabled = v) } } }
        viewModelScope.launch { repository.getStatusNotificationEnabledFlow().collect { v -> _uiState.update { it.copy(statusNotificationEnabled = v) } } }
    }

    private fun loadThemeSettings() {
        viewModelScope.launch { repository.getDisplayModeFlow().collect { v -> _uiState.update { it.copy(displayMode = v) } } }
        viewModelScope.launch { repository.getColorThemeFlow().collect { v -> _uiState.update { it.copy(colorTheme = v) } } }
        viewModelScope.launch { repository.getCornerRadiusFlow().collect { v -> _uiState.update { it.copy(cornerRadius = v) } } }
    }

    fun setDisplayMode(m: DisplayMode) = viewModelScope.launch { repository.setDisplayMode(m) }
    fun setColorTheme(t: ColorTheme) = viewModelScope.launch { repository.setColorTheme(t) }
    fun setCornerRadius(r: Int) = viewModelScope.launch { repository.setCornerRadius(r) }
    fun setNotificationsEnabled(e: Boolean) = viewModelScope.launch { repository.setNotificationsEnabled(e) }
    fun setStatusNotificationEnabled(e: Boolean) = viewModelScope.launch { 
        repository.setStatusNotificationEnabled(e)
        // Immediately start or stop the status notification service
        if (e) {
            com.occaecat.ztoeschedule.domain.notification.PowerStatusService.start(context)
        } else {
            com.occaecat.ztoeschedule.domain.notification.PowerStatusService.stop(context)
        }
    }
    fun completeOnboarding() = viewModelScope.launch { repository.setOnboardingCompleted() }
    
    fun resetOnboarding() = viewModelScope.launch { 
        repository.resetOnboarding()
        // Reset state but keep isInitialLoadComplete true so flows continue working
        _uiState.update { 
            UiState(
                onboardingCompleted = false,
                hasSavedSelection = false,
                isInitialLoadComplete = true
            ) 
        }
    }
    
    fun startAddingAddress() { _uiState.update { it.copy(isAddingNewAddress = true) } }
    fun cancelAddingAddress() { _uiState.update { it.copy(isAddingNewAddress = false) } }
    fun dismissTimeSyncWarning() { _uiState.update { it.copy(isTimeOutOfSync = false) } }
    
    fun clearData() = viewModelScope.launch { 
        repository.clearAllData()
        // Reset state completely - flows in init will refresh automatically
        _uiState.update { 
            UiState(
                onboardingCompleted = false,
                hasSavedSelection = false,
                isInitialLoadComplete = true  // Keep true so UI doesn't freeze
            ) 
        }
    }
    fun setShowWidgetConfig(show: Boolean) { _uiState.update { it.copy(showWidgetConfig = show) } }
    fun setRequestedAddressId(id: String?) { _uiState.update { it.copy(requestedAddressId = id) } }
    fun selectWidgetAddress(address: SavedAddress) { viewModelScope.launch { repository.setPrimaryAddress(address.id); loadSavedAddresses(); setShowWidgetConfig(false); NotificationScheduler.runImmediateCheck(context) } }
    fun startInspectingAddress(address: SavedAddress) { savedStateHandle["inspected_id"] = address.id; _uiState.update { it.copy(inspectedAddress = address, isInspectingLoading = true) }; viewModelScope.launch { val data = loadSingleAddressData(address); _uiState.update { it.copy(inspectedScheduleList = data.scheduleList, inspectedGroupedSchedule = data.groupedSchedule, isInspectingLoading = false) } } }
    fun stopInspectingAddress() { savedStateHandle["inspected_id"] = null; _uiState.update { it.copy(inspectedAddress = null) } }
    
    fun isInspectedAddressSaved(): Boolean {
        val inspected = _uiState.value.inspectedAddress ?: return false
        return _uiState.value.savedAddresses.any { 
            it.streetId == inspected.streetId && it.addressId == inspected.addressId 
        }
    }

    fun saveInspectedAddress() {
        val inspected = _uiState.value.inspectedAddress ?: return
        viewModelScope.launch {
            if (!isInspectedAddressSaved()) {
                val newAddress = inspected.copy(
                    id = UUID.randomUUID().toString(), // Generate real ID
                    priority = repository.getSavedAddresses().size + 1
                )
                repository.saveNewAddress(newAddress)
                loadSavedAddresses()
                _uiState.update { it.copy(infoMessage = "Адресу збережено") }
            }
        }
    }

    fun inspectAddressByIds(streetId: String, houseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAddressList(streetId).onSuccess { addresses ->
                val targetHouse = addresses.find { it.id == houseId }
                if (targetHouse != null) {
                    val savedAddr = SavedAddress(
                        id = "temp_${System.currentTimeMillis()}",
                        name = targetHouse.name,
                        iconName = "location_on",
                        priority = 0,
                        remId = "", // We could fetch more, but cherga/pidcherga are enough for schedule
                        remName = "",
                        cityId = "",
                        cityName = "",
                        streetId = streetId,
                        streetName = "",
                        addressId = houseId,
                        addressName = targetHouse.name,
                        cherga = targetHouse.cherga,
                        pidcherga = targetHouse.pidcherga
                    )
                    startInspectingAddress(savedAddr)
                } else {
                    _uiState.update { it.copy(error = "Адресу не знайдено") }
                }
            }.onFailure {
                _uiState.update { it.copy(error = "Помилка завантаження даних адреси") }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun retryLoading() { refreshAllSchedules() }
    fun loadScheduleWithMessages(cherga: Int, pid: Int) { refreshAllSchedules() }
    private fun formatMessages(m: List<ScheduleMessagePart>): String = m.sortedBy { it.id }.joinToString("\n") { it.text }
    fun saveSelection(rI: String?, rN: String?, cI: String?, cN: String?, sI: String?, sN: String?, aI: String, aN: String, c: Int, p: Int, customName: String = "Дім", icon: String = "home") {
        viewModelScope.launch {
            repository.saveCompleteSelection(rI, rN, cI, cN, sI, sN, aI, aN, c, p)
            val address = SavedAddress(
                name = customName, iconName = icon, priority = 1,
                remId = rI ?: "", remName = rN ?: "", cityId = cI ?: "", cityName = cN ?: "",
                streetId = sI ?: "", streetName = sN ?: "", addressId = aI, addressName = aN,
                cherga = c, pidcherga = p
            )
            repository.saveNewAddress(address)
            loadSavedAddresses()
        }
    }
    fun loadRemList() { viewModelScope.launch { repository.getRemList().onSuccess { r -> _uiState.update { it.copy(remList = r) } } } }
    fun loadCityList(id: String) { viewModelScope.launch { repository.getCityList(id).onSuccess { r -> _uiState.update { it.copy(cityList = r) } } } }
    fun loadStreetList(id: String) { viewModelScope.launch { repository.getStreetList(id).onSuccess { r -> _uiState.update { it.copy(streetList = r) } } } }
    fun loadAddressList(id: String) { viewModelScope.launch { repository.getAddressList(id).onSuccess { r -> _uiState.update { it.copy(addressList = r, filteredHouseNumbers = repository.getAllParsedHouseNumbers(r)) } } } }
    fun filterHouseNumbers(q: String) { 
        val currentCategory = _uiState.value.selectedCategory
        _uiState.update { 
            it.copy(
                houseNumberSearchQuery = q,
                filteredHouseNumbers = filterAddresses(it.addressList, q, currentCategory)
            ) 
        } 
    }
    
    fun selectCategory(category: ConsumerCategory?) {
        val currentQuery = _uiState.value.houseNumberSearchQuery
        _uiState.update {
            it.copy(
                selectedCategory = category,
                filteredHouseNumbers = filterAddresses(it.addressList, currentQuery, category)
            )
        }
    }

    private fun filterAddresses(rawList: List<Address>, query: String, category: ConsumerCategory?): List<ParsedHouseNumber> {
        val allParsed = repository.getAllParsedHouseNumbers(rawList)
        return allParsed.filter { item ->
            val matchesQuery = item.houseNumber.contains(query, ignoreCase = true)
            val matchesCategory = category == null || item.category == category
            matchesQuery && matchesCategory
        }
    }

    fun clearHouseNumberSearch() { 
        _uiState.update { 
            it.copy(
                houseNumberSearchQuery = "",
                selectedCategory = null, // Reset category too? Or keep it? Let's reset for full clear.
                filteredHouseNumbers = repository.getAllParsedHouseNumbers(it.addressList)
            ) 
        } 
    }
    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun showInfoMessage(msg: String) { _uiState.update { it.copy(infoMessage = msg) } }
    fun clearInfoMessage() { _uiState.update { it.copy(infoMessage = null) } }
}

@Stable
data class UiState(
    val remList: List<Rem> = emptyList(), val cityList: List<City> = emptyList(), val streetList: List<Street> = emptyList(),
    val addressList: List<Address> = emptyList(), val savedAddresses: List<SavedAddress> = emptyList(),
    val addressDataList: List<AddressDataState> = emptyList(), val isAddingNewAddress: Boolean = false,
    val addressStatuses: Map<String, GroupedSchedule?> = emptyMap(), val filteredHouseNumbers: List<ParsedHouseNumber> = emptyList(),
    val houseNumberSearchQuery: String = "", val scheduleList: List<Schedule> = emptyList(),
    val selectedCategory: ConsumerCategory? = null,
    val groupedSchedule: List<GroupedSchedule> = emptyList(), val infoMessages: List<ScheduleMessagePart> = emptyList(),
    val formattedMessage: String = "", val currentStatus: Schedule? = null, val lastUpdateTime: String = "",
    val isLoading: Boolean = false, val isInitialLoadComplete: Boolean = false, val isTimeOutOfSync: Boolean = false,
    val error: String? = null, val infoMessage: String? = null, val hasSavedSelection: Boolean = false,
    val onboardingCompleted: Boolean = false, val notificationsEnabled: Boolean = true,
    val statusNotificationEnabled: Boolean = false,
    val isConnected: Boolean = true, val retryCountdown: Int = 0,
    val lastLoadFailed: Boolean = false, val isOffline: Boolean = false, val showWidgetConfig: Boolean = false,
    val displayMode: DisplayMode = DisplayMode.Comfortable,
    val colorTheme: ColorTheme = ColorTheme.System,
    val cornerRadius: Int = 24,
    val inspectedAddress: SavedAddress? = null, val inspectedScheduleList: List<Schedule> = emptyList(),
    val inspectedGroupedSchedule: List<GroupedSchedule> = emptyList(), val isInspectingLoading: Boolean = false,
    val requestedAddressId: String? = null
)