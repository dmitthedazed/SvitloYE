package com.occaecat.ztoeschedule.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.occaecat.ztoeschedule.data.model.Address
import com.occaecat.ztoeschedule.data.model.City
import com.occaecat.ztoeschedule.data.model.Rem
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.data.model.ScheduleMessagePart
import com.occaecat.ztoeschedule.data.model.Street
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.TimeZone

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for managing energy outage schedule state.
 * Optimized to prevent infinite network loops during priority changes.
 */
@HiltViewModel
class EnergyScheduleViewModel @Inject constructor(
    private val repository: EnergyRepository,
    private val networkObserver: com.occaecat.ztoeschedule.domain.NetworkObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Guard to prevent redundant network calls during preference sync
    private var lastLoadedAddressId: String? = null

    init {
        viewModelScope.launch {
            try {
                // Observe network status
                launch {
                    networkObserver.isConnected.collect { connected ->
                        val wasOffline = !_uiState.value.isConnected
                        _uiState.update { it.copy(isConnected = connected) }
                        
                        // Auto-retry if we were offline and now connected
                        if (connected && wasOffline) {
                            retryLoading()
                        }
                    }
                }

                // Start loading peripheral settings
                launch { loadNotificationSettings() }
                launch { loadSavedAddresses() }
                launch { checkTimeSync() }
                
                // Wait for critical data
                launch {
                    combine(
                        repository.getOnboardingCompletedFlow(),
                        repository.getSavedSelectionFlow()
                    ) { completed, selection ->
                        Pair(completed, selection)
                    }.collect { (completed, selection) ->
                        _uiState.update {
                            it.copy(
                                onboardingCompleted = completed,
                                hasSavedSelection = selection != null,
                                savedRemName = selection?.remName ?: "",
                                savedCityName = selection?.cityName ?: "",
                                savedStreetName = selection?.streetName ?: "",
                                savedAddressName = selection?.addressName ?: "",
                                savedCherga = selection?.cherga ?: 0,
                                savedPidcherga = selection?.pidcherga ?: 0,
                                isInitialLoadComplete = true
                            )
                        }
                        
                        // CRITICAL: Only load if Address ID changed or it's the first load
                        if (selection != null && selection.addressId != lastLoadedAddressId) {
                            lastLoadedAddressId = selection.addressId
                            loadScheduleWithMessages(selection.cherga, selection.pidcherga)
                        }
                    }
                }
            } catch (e: Exception) {
                // Global initialization error handler
                _uiState.update { 
                    it.copy(
                        isInitialLoadComplete = true,
                        lastLoadFailed = true,
                        error = "Помилка ініціалізації: ${e.message}"
                    ) 
                }
                startRetryTimer()
            }
        }
    }

    fun retryLoading() {
        _uiState.update { 
            it.copy(
                retryCountdown = 0, 
                lastLoadFailed = false, 
                isInitialLoadComplete = false,
                isLoading = true 
            ) 
        }
        val cherga = _uiState.value.savedCherga
        val pidcherga = _uiState.value.savedPidcherga
        
        if (cherga > 0) {
            loadScheduleWithMessages(cherga, pidcherga)
        } else {
            // If no address selected, try reloading basic lists
            loadRemList()
            loadSavedAddresses()
            checkTimeSync()
        }
    }

    private var retryJob: kotlinx.coroutines.Job? = null

    private fun startRetryTimer() {
        if (retryJob?.isActive == true) return
        
        retryJob = viewModelScope.launch {
            _uiState.update { it.copy(lastLoadFailed = true) }
            for (i in 30 downTo 1) {
                _uiState.update { it.copy(retryCountdown = i) }
                kotlinx.coroutines.delay(1000)
            }
            if (_uiState.value.lastLoadFailed) {
                retryLoading()
            }
        }
    }

    private fun checkTimeSync() {
        viewModelScope.launch {
            repository.getServerTime().onSuccess { serverMs ->
                val deviceMs = System.currentTimeMillis()
                val diffMinutes = Math.abs(serverMs - deviceMs) / 60000
                if (diffMinutes > 5) {
                    _uiState.update { it.copy(isTimeOutOfSync = true) }
                }
                _uiState.update { it.copy(lastLoadFailed = false) }
            }.onFailure {
                startRetryTimer()
            }
        }
    }

    // ========== Address Management ========== 

    fun loadSavedAddresses() {
        viewModelScope.launch {
            val addresses = repository.getSavedAddresses()
            val sorted = addresses.sortedBy { it.priority }
            
            val currentAddressIds = _uiState.value.savedAddresses.map { "${it.cherga}_${it.pidcherga}" }
            val newAddressIds = sorted.map { "${it.cherga}_${it.pidcherga}" }
            
            _uiState.update { it.copy(savedAddresses = sorted) }
            
            // Only refresh statuses if the set of queues/sub-queues has changed
            // or if the status map is empty.
            if (newAddressIds != currentAddressIds || _uiState.value.addressStatuses.isEmpty()) {
                refreshAllStatuses(sorted)
            }
        }
    }

    private fun refreshAllStatuses(addresses: List<com.occaecat.ztoeschedule.data.model.SavedAddress>) {
        if (addresses.isEmpty()) {
            _uiState.update { it.copy(addressStatuses = emptyMap()) }
            return
        }
        
        viewModelScope.launch {
            val statusMap = mutableMapOf<String, GroupedSchedule?>()
            // Launch status updates sequentially or with controlled parallelism to avoid quota issues
            addresses.forEach { address ->
                repository.getSchedule(address.cherga, address.pidcherga).onSuccess { schedules ->
                    val grouped = ScheduleMapper.getGroupedSchedule(schedules)
                    statusMap[address.id] = ScheduleMapper.getCurrentGroupedStatus(grouped)
                }
            }
            _uiState.update { it.copy(addressStatuses = statusMap) }
        }
    }

    fun addSavedAddress(
        name: String, iconName: String, remId: String, remName: String,
        cityId: String, cityName: String, streetId: String, streetName: String,
        addressId: String, addressName: String, cherga: Int, pidcherga: Int
    ) {
        viewModelScope.launch {
            val currentAddresses = repository.getSavedAddresses()
            if (currentAddresses.any { it.addressId == addressId }) {
                _uiState.update { it.copy(error = "Ця адреса вже додана", isAddingNewAddress = false) }
                return@launch
            }

            val address = com.occaecat.ztoeschedule.data.model.SavedAddress(
                name = name, iconName = iconName, priority = currentAddresses.size + 1,
                remId = remId, remName = remName, cityId = cityId, cityName = cityName,
                streetId = streetId, streetName = streetName, addressId = addressId,
                addressName = addressName, cherga = cherga, pidcherga = pidcherga
            )
            repository.saveNewAddress(address)
            loadSavedAddresses()
            _uiState.update { it.copy(isAddingNewAddress = false) }
        }
    }

    fun updateAddressesOrder(list: List<com.occaecat.ztoeschedule.data.model.SavedAddress>) {
        viewModelScope.launch {
            repository.reorderAddresses(list)
            loadSavedAddresses()
        }
    }

    fun deleteSavedAddress(id: String) {
        viewModelScope.launch {
            repository.deleteAddress(id)
            loadSavedAddresses()
        }
    }

    fun startAddingAddress() {
        _uiState.update { it.copy(isAddingNewAddress = true) }
    }

    fun cancelAddingAddress() {
        _uiState.update { it.copy(isAddingNewAddress = false) }
    }

    fun dismissTimeSyncWarning() {
        _uiState.update { it.copy(isTimeOutOfSync = false) }
    }

    // ========== Onboarding & Settings ========== 

    private fun loadNotificationSettings() {
        viewModelScope.launch {
            launch {
                repository.getNotificationsEnabledFlow().collect { enabled ->
                    _uiState.update { it.copy(notificationsEnabled = enabled) }
                }
            }
            launch {
                repository.getNotificationAdvanceMinutesFlow().collect { minutes ->
                    _uiState.update { it.copy(notificationAdvanceMinutes = minutes) }
                }
            }
            launch {
                repository.getStatusNotificationEnabledFlow().collect { enabled ->
                    _uiState.update { it.copy(statusNotificationEnabled = enabled) }
                }
            }
            launch {
                repository.getLiveActivityEnabledFlow().collect { enabled ->
                    _uiState.update { it.copy(liveActivityEnabled = enabled) }
                }
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setNotificationsEnabled(enabled) }
    }

    fun setNotificationAdvanceMinutes(minutes: Int) {
        viewModelScope.launch { repository.setNotificationAdvanceMinutes(minutes) }
    }

    fun setStatusNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setStatusNotificationEnabled(enabled) }
    }

    fun setLiveActivityEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setLiveActivityEnabled(enabled) }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted()
            _uiState.update { it.copy(onboardingCompleted = true) }
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            repository.resetOnboarding()
            _uiState.update { it.copy(onboardingCompleted = false) }
        }
    }

    // ========== Data Loading ========== 

    fun loadRemList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getRemList().onSuccess { rems ->
                _uiState.update { it.copy(remList = rems, isLoading = false, lastLoadFailed = false) }
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message, isLoading = false, lastLoadFailed = true) }
                startRetryTimer()
            }
        }
    }

    fun loadCityList(remId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getCityList(remId).onSuccess { cities ->
                _uiState.update { it.copy(cityList = cities, isLoading = false, lastLoadFailed = false) }
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message, isLoading = false, lastLoadFailed = true) }
                startRetryTimer()
            }
        }
    }

    fun loadStreetList(cityId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getStreetList(cityId).onSuccess { streets ->
                _uiState.update { it.copy(streetList = streets, isLoading = false, lastLoadFailed = false) }
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message, isLoading = false, lastLoadFailed = true) }
                startRetryTimer()
            }
        }
    }

    fun loadAddressList(streetId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getAddressList(streetId).onSuccess { addresses ->
                val parsedHouses = repository.getAllParsedHouseNumbers(addresses)
                _uiState.update {
                    it.copy(addressList = addresses, parsedHouseNumbers = parsedHouses, filteredHouseNumbers = parsedHouses, isLoading = false, lastLoadFailed = false)
                }
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message, isLoading = false, lastLoadFailed = true) }
                startRetryTimer()
            }
        }
    }

    fun filterHouseNumbers(query: String) {
        _uiState.update {
            val filtered = if (query.isBlank()) it.parsedHouseNumbers else it.parsedHouseNumbers.filter { it.houseNumber.contains(query, ignoreCase = true) }
            it.copy(houseNumberSearchQuery = query, filteredHouseNumbers = filtered)
        }
    }

    fun clearHouseNumberSearch() {
        _uiState.update { it.copy(houseNumberSearchQuery = "", filteredHouseNumbers = it.parsedHouseNumbers) }
    }

    fun loadScheduleWithMessages(cherga: Int, pidcherga: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getScheduleWithMessages(cherga, pidcherga).onSuccess { data ->
                val currentStatus = repository.getCurrentStatus(data.schedules)
                val formattedMessage = formatMessages(data.messages)
                val groupedSchedule = ScheduleMapper.getGroupedSchedule(data.schedules)
                val now = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                
                _uiState.update {
                    it.copy(
                        scheduleList = data.schedules,
                        groupedSchedule = groupedSchedule,
                        infoMessages = data.messages,
                        formattedMessage = formattedMessage,
                        currentStatus = currentStatus,
                        lastUpdateTime = now,
                        isLoading = false,
                        lastLoadFailed = false,
                        retryCountdown = 0,
                        isInitialLoadComplete = true
                    )
                }
                repository.saveQueueIdentifiers(cherga, pidcherga)
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message, isLoading = false) }
                if (!_uiState.value.isConnected) startRetryTimer()
            }
        }
    }

    fun formatMessages(messages: List<ScheduleMessagePart>): String {
        if (messages.isEmpty()) return ""
        return messages.sortedBy { it.id }.joinToString("\n") { it.text }.cleanHtmlEntities()
    }

    private fun String.cleanHtmlEntities(): String = this.replace("&quot;", "\"").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ").replace("&#39;", "'").replace("&apos;", "'")

    fun saveSelection(remId: String?, remName: String?, cityId: String?, cityName: String?, streetId: String?, streetName: String?, addressId: String, addressName: String, cherga: Int, pidcherga: Int, customName: String = "Дім", iconName: String = "home") {
        viewModelScope.launch {
            repository.saveCompleteSelection(remId, remName, cityId, cityName, streetId, streetName, addressId, addressName, cherga, pidcherga)
            val currentAddresses = repository.getSavedAddresses()
            if (!currentAddresses.any { it.addressId == addressId }) {
                val newAddr = com.occaecat.ztoeschedule.data.model.SavedAddress(name = customName, iconName = iconName, priority = currentAddresses.size + 1, remId = remId ?: "", remName = remName ?: "", cityId = cityId ?: "", cityName = cityName ?: "", streetId = streetId ?: "", streetName = streetName ?: "", addressId = addressId, addressName = addressName, cherga = cherga, pidcherga = pidcherga)
                repository.saveNewAddress(newAddr)
                loadSavedAddresses()
            }
            _uiState.update { it.copy(hasSavedSelection = true, savedRemName = remName ?: "", savedCityName = cityName ?: "", savedStreetName = streetName ?: "", savedAddressName = addressName, savedCherga = cherga, savedPidcherga = pidcherga) }
        }
    }

    fun clearData() {
        viewModelScope.launch {
            repository.clearPreferences()
            _uiState.value = UiState()
        }
    }

    fun refreshCurrentStatus() {
        _uiState.update { state ->
            val currentStatus = repository.getCurrentStatus(state.scheduleList)
            state.copy(currentStatus = currentStatus)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class UiState(
    val remList: List<Rem> = emptyList(),
    val cityList: List<City> = emptyList(),
    val streetList: List<Street> = emptyList(),
    val addressList: List<Address> = emptyList(),
    val savedAddresses: List<com.occaecat.ztoeschedule.data.model.SavedAddress> = emptyList(),
    val isAddingNewAddress: Boolean = false,
    val addressStatuses: Map<String, GroupedSchedule?> = emptyMap(),
    val parsedHouseNumbers: List<ParsedHouseNumber> = emptyList(),
    val filteredHouseNumbers: List<ParsedHouseNumber> = emptyList(),
    val houseNumberSearchQuery: String = "",
    val savedRemName: String = "",
    val savedCityName: String = "",
    val savedStreetName: String = "",
    val savedAddressName: String = "",
    val savedCherga: Int = 0,
    val savedPidcherga: Int = 0,
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
    val lastLoadFailed: Boolean = false
)