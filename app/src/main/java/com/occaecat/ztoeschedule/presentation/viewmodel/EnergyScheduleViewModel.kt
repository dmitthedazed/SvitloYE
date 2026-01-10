package com.occaecat.ztoeschedule.presentation.viewmodel

import android.content.Context
import android.text.format.DateFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.model.*
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber
import com.occaecat.ztoeschedule.data.repository.ConsumerCategory
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import com.occaecat.ztoeschedule.domain.model.getUserMessage
import com.occaecat.ztoeschedule.domain.model.toAppError
import com.occaecat.ztoeschedule.domain.notification.NotificationScheduler
import com.occaecat.ztoeschedule.domain.notification.PowerStatusService
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
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
    companion object {
        private const val OFFLINE_REFRESH_DEBOUNCE_MS = 30_000L
    }

    private val _uiState = MutableStateFlow(UiState(isLoading = true))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private var lastOfflineRefreshMs: Long = 0L

    init {
        viewModelScope.launch {
            // 1. Initial load from storage (Fast)
            val addrs = withContext(Dispatchers.IO) {
                repository.getSavedAddresses().sortedBy { it.priority }
            }
            _uiState.update { it.copy(savedAddresses = addrs) }
            
            // 2. Load from cache IMMEDIATELY (Fast)
            if (addrs.isNotEmpty()) {
                val cachedResults = withContext(Dispatchers.IO) {
                    addrs.mapIndexed { index, addr ->
                        loadSingleAddressData(addr, useCacheOnly = true, isPrimary = index == 0)
                    }
                }
                _uiState.update { it.copy(addressDataList = cachedResults) }
            }
            
            // 3. Mark initialization as complete so UI shows cache content
            _uiState.update { it.copy(isInitialLoadComplete = true) }

            // 4. Trigger network refresh in background (don't await it here)
            launch {
                performRefreshAllSchedules(addrs, allowOffline = false)
            }

            // 5. Start background observers
            val restoredInspectId = savedStateHandle.get<String>("inspected_id")
            
            launch {
                networkObserver.isConnected.collect { connected ->
                    val wasOffline = !_uiState.value.isConnected
                    _uiState.update { it.copy(isConnected = connected) }
                    if (connected && wasOffline && _uiState.value.isInitialLoadComplete) {
                        refreshAllSchedules()
                    }
                }
            }
            
            launch { loadNotificationSettings() }
            launch { loadThemeSettings() }
            
            launch {
                repository.getLastScheduleServerUpdatedFlow().collect { updatedMs ->
                    _uiState.update {
                        it.copy(lastUpdateTime = updatedMs?.let { ms -> formatUpdateTime(ms) } ?: "")
                    }
                }
            }
            
            // Restore inspected address from saved state
            if (restoredInspectId != null) {
                val addresses = repository.getSavedAddresses()
                val found = addresses.find { it.id == restoredInspectId }
                if (found != null) { startInspectingAddress(found) }
            }
        }
    }

    fun refreshAllSchedules(
        addressesOverride: List<SavedAddress>? = null,
        allowOffline: Boolean = false
    ) {
        viewModelScope.launch {
            performRefreshAllSchedules(addressesOverride, allowOffline)
        }
    }

    private suspend fun performRefreshAllSchedules(
        addressesOverride: List<SavedAddress>? = null,
        allowOffline: Boolean = false
    ) {
        val startTime = timeProvider.now()
        val isConnected = _uiState.value.isConnected
        if (!isConnected && !allowOffline) {
            val now = timeProvider.now()
            if (now - lastOfflineRefreshMs < OFFLINE_REFRESH_DEBOUNCE_MS) {
                return
            }
            lastOfflineRefreshMs = now
        } else if (!isConnected) {
            lastOfflineRefreshMs = timeProvider.now()
        }
        
        // Always show loading state during refresh for better feedback
        _uiState.update { it.copy(isLoading = true, lastLoadFailed = false) }
        
        val addresses = addressesOverride ?: withContext(Dispatchers.IO) {
            repository.getSavedAddresses()
        }
        val sortedAddresses = addresses.sortedBy { it.priority }
        
        if (sortedAddresses.isEmpty()) {
            _uiState.update { it.copy(addressDataList = emptyList(), isLoading = false) }
            return
        }

        // If connected, perform network update
        if (_uiState.value.isConnected) {
            viewModelScope.launch {
                repository.getMessages().onSuccess { messages ->
                    _uiState.update { it.copy(infoMessages = messages, formattedMessage = formatMessages(messages)) }
                }
            }

            val networkResults = withContext(Dispatchers.IO) {
                sortedAddresses.mapIndexed { index, addr ->
                    async { loadSingleAddressData(addr, false, index == 0) }
                }.awaitAll()
            }
            
            // Ensure minimum loading duration of 2 seconds
            val elapsedTime = timeProvider.now() - startTime
            if (elapsedTime < 2000) {
                delay(2000 - elapsedTime)
            }
            
            _uiState.update { it.copy(addressDataList = networkResults, isLoading = false) }
            refreshAllStatuses(sortedAddresses)
        } else {
            // Offline or cache-only mode
            val cachedResults = withContext(Dispatchers.IO) {
                sortedAddresses.mapIndexed { index, addr ->
                    loadSingleAddressData(addr, useCacheOnly = true, isPrimary = index == 0)
                }
            }
            
            val elapsedTime = timeProvider.now() - startTime
            if (elapsedTime < 2000) {
                delay(2000 - elapsedTime)
            }
            
            _uiState.update { it.copy(addressDataList = cachedResults, isLoading = false) }
            refreshAllStatuses(sortedAddresses)
        }
    }

    private suspend fun loadSingleAddressData(
        address: SavedAddress,
        useCacheOnly: Boolean,
        isPrimary: Boolean
    ): AddressDataState =
        withContext(Dispatchers.IO) {
            val result = if (useCacheOnly) {
                repository.getCachedScheduleWithMessages(address.cherga, address.pidcherga)
            } else {
                repository.getScheduleWithMessages(address.cherga, address.pidcherga)
            }
            if (result.isSuccess) {
                val data = result.getOrThrow()
                val groupedSchedule = ScheduleMapper.getGroupedSchedule(data.schedules)
                val alarmAddress = Address(
                    id = address.addressId,
                    name = address.addressName,
                    cherga = address.cherga,
                    pidcherga = address.pidcherga
                )
                if (isPrimary) {
                    val maxHoursAhead = if (useCacheOnly) 2 else 24
                    scheduledAlarmManager.scheduleAlarmsForAddress(
                        address = alarmAddress,
                        schedules = groupedSchedule,
                        maxHoursAhead = maxHoursAhead
                    )
                } else {
                    scheduledAlarmManager.cancelAlarmsForAddress(alarmAddress)
                }

                val cachedUpdated = repository.getCacheLastUpdated(address.cherga, address.pidcherga)
                val lastUpdateTime = cachedUpdated?.let { formatUpdateTime(it) }
                    ?: if (!useCacheOnly) formatUpdateTime(timeProvider.now()) else ""

                AddressDataState(
                    address,
                    data.schedules,
                    groupedSchedule,
                    repository.getCurrentStatus(data.schedules),
                    useCacheOnly,
                    false,
                    lastUpdateTime
                )
            } else {
                _uiState.value.addressDataList.find { it.address.id == address.id }
                    ?.copy(isOffline = true)
                    ?: AddressDataState(address, isOffline = true)
            }
        }

    fun loadSavedAddresses() {
        viewModelScope.launch {
            val addrs = withContext(Dispatchers.IO) {
                repository.getSavedAddresses().sortedBy { it.priority }
            }
            _uiState.update { it.copy(savedAddresses = addrs) }
            refreshAllSchedules(addrs, allowOffline = true)
        }
    }

    private suspend fun resolveMissingAddressNames(address: SavedAddress): SavedAddress {
        val resolvedAddressName = address.addressName.ifBlank { address.name }
        val base = address.copy(addressName = resolvedAddressName)
        if (base.remName.isNotBlank() && base.cityName.isNotBlank() && base.streetName.isNotBlank() &&
            base.remId.isNotBlank() && base.cityId.isNotBlank()
        ) {
            return base
        }

        return withContext(Dispatchers.IO) {
            var updated = base

            if (updated.cityId.isNotBlank() && updated.streetName.isBlank()) {
                repository.getStreetList(updated.cityId).onSuccess { streets ->
                    val match = streets.firstOrNull { it.id == updated.streetId }
                    if (match != null) {
                        updated = updated.copy(streetName = match.name)
                    }
                }
            }

            if (updated.remId.isNotBlank() && (updated.cityId.isBlank() || updated.cityName.isBlank())) {
                repository.getCityList(updated.remId).onSuccess { cities ->
                    val match = cities.firstOrNull { it.id == updated.cityId }
                    if (match != null) {
                        updated = updated.copy(
                            cityId = if (updated.cityId.isBlank()) match.id else updated.cityId,
                            cityName = if (updated.cityName.isBlank()) match.name else updated.cityName
                        )
                    }
                }
            }

            if (updated.remId.isNotBlank() && updated.remName.isBlank()) {
                repository.getRemList().onSuccess { rems ->
                    val match = rems.firstOrNull { it.id == updated.remId }
                    if (match != null) {
                        updated = updated.copy(remName = match.name)
                    }
                }
            }

            if (updated.remName.isNotBlank() && updated.cityName.isNotBlank() && updated.streetName.isNotBlank()) {
                return@withContext updated
            }

            if (updated.streetId.isBlank()) return@withContext updated

            val rems = repository.getRemList().getOrNull() ?: return@withContext updated
            for (rem in rems) {
                val cities = repository.getCityList(rem.id).getOrNull() ?: continue
                for (city in cities) {
                    val streets = repository.getStreetList(city.id).getOrNull() ?: continue
                    val street = streets.firstOrNull { it.id == updated.streetId } ?: continue
                    updated = updated.copy(
                        remId = if (updated.remId.isBlank()) rem.id else updated.remId,
                        remName = if (updated.remName.isBlank()) rem.name else updated.remName,
                        cityId = if (updated.cityId.isBlank()) city.id else updated.cityId,
                        cityName = if (updated.cityName.isBlank()) city.name else updated.cityName,
                        streetName = if (updated.streetName.isBlank()) street.name else updated.streetName
                    )
                    return@withContext updated
                }
            }

            updated
        }
    }



    private fun refreshAllStatuses(addresses: List<SavedAddress>) {
        if (addresses.isEmpty()) {
            _uiState.update { it.copy(addressStatuses = emptyMap()) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val statusMap = mutableMapOf<String, GroupedSchedule?>()
            val nowMs = timeProvider.now()
            val isConnected = _uiState.value.isConnected
            addresses.forEach { address ->
                val cached = _uiState.value.addressDataList.find { it.address.id == address.id }
                if (cached != null && cached.groupedSchedule.isNotEmpty()) {
                    statusMap[address.id] = ScheduleMapper.getCurrentGroupedStatus(cached.groupedSchedule, nowMs)
                } else if (isConnected) {
                    repository.getSchedule(address.cherga, address.pidcherga)
                        .onSuccess { schedules ->
                            statusMap[address.id] = ScheduleMapper.getCurrentGroupedStatus(
                                ScheduleMapper.getGroupedSchedule(schedules),
                                nowMs
                            )
                        }
                }
            }
            _uiState.update { it.copy(addressStatuses = statusMap) }
        }
    }

    fun addSavedAddress(name: String, icon: String, rI: String, rN: String, cI: String, cN: String, sI: String, sN: String, aI: String, aN: String, c: Int, p: Int) {
        viewModelScope.launch {
            if (!_uiState.value.isConnected) {
                _uiState.update { it.copy(isAddingNewAddress = false, error = offlineErrorText()) }
                return@launch
            }
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
            val resolved = resolveMissingAddressNames(address)
            repository.saveNewAddress(resolved)
            loadSavedAddresses()
            _uiState.update { it.copy(isAddingNewAddress = false) }
            NotificationScheduler.runImmediateCheck(context)
        }
    }

    fun deleteSavedAddress(id: String) { viewModelScope.launch { repository.deleteAddress(id); loadSavedAddresses(); NotificationScheduler.runImmediateCheck(context) } }    
    fun updateAddressesOrder(list: List<SavedAddress>) {
        viewModelScope.launch {
            repository.reorderAddresses(list)
            val primary = list.firstOrNull()
            if (primary != null) {
                val cached = repository.getCachedScheduleWithMessages(primary.cherga, primary.pidcherga)
                cached.onSuccess { data ->
                    val grouped = ScheduleMapper.getGroupedSchedule(data.schedules)
                    if (grouped.isNotEmpty()) {
                        scheduledAlarmManager.scheduleAlarmsForAddress(
                            address = Address(
                                id = primary.addressId,
                                name = primary.addressName,
                                cherga = primary.cherga,
                                pidcherga = primary.pidcherga
                            ),
                            schedules = grouped,
                            maxHoursAhead = 2
                        )
                    }
                }
            }
            loadSavedAddresses()
            PowerStatusService.requestImmediateRefresh(context)
            NotificationScheduler.runImmediateCheck(context)
        }
    }

    /**
     * Add demo/test location that changes status every minute
     * Useful for debugging notification alerts
     */
    fun addDemoLocation() {
        viewModelScope.launch {
            if (!_uiState.value.isConnected) {
                _uiState.update { it.copy(infoMessage = offlineErrorText()) }
                return@launch
            }
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
        viewModelScope.launch { repository.getDynamicColorsFlow().collect { v -> _uiState.update { it.copy(dynamicColors = v) } } }
        viewModelScope.launch { repository.getIsAmoledFlow().collect { v -> _uiState.update { it.copy(isAmoled = v) } } }
    }

    fun setDisplayMode(m: DisplayMode) = viewModelScope.launch { repository.setDisplayMode(m) }
    fun setColorTheme(t: ColorTheme) = viewModelScope.launch { repository.setColorTheme(t) }
    fun setCornerRadius(r: Int) = viewModelScope.launch { repository.setCornerRadius(r) }
    fun setDynamicColors(e: Boolean) = viewModelScope.launch { repository.setDynamicColors(e) }
    fun setIsAmoled(e: Boolean) = viewModelScope.launch { repository.setIsAmoled(e) }
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

    
    fun startAddingAddress() { _uiState.update { it.copy(isAddingNewAddress = true) } }
    fun cancelAddingAddress() { _uiState.update { it.copy(isAddingNewAddress = false) } }
    fun dismissTimeSyncWarning() { _uiState.update { it.copy(isTimeOutOfSync = false) } }
    
    fun clearData() = viewModelScope.launch { 
        repository.clearAllData()
        // Reset state completely - flows in init will refresh automatically
        _uiState.update { 
            UiState(
                isInitialLoadComplete = true  // Keep true so UI doesn't freeze
            ) 
        }
    }
    fun setShowWidgetConfig(show: Boolean) { _uiState.update { it.copy(showWidgetConfig = show) } }
    fun setRequestedAddressId(id: String?) { _uiState.update { it.copy(requestedAddressId = id) } }
    fun selectWidgetAddress(address: SavedAddress) { viewModelScope.launch { repository.setPrimaryAddress(address.id); loadSavedAddresses(); setShowWidgetConfig(false); NotificationScheduler.runImmediateCheck(context) } }
    fun startInspectingAddress(address: SavedAddress) {
        savedStateHandle["inspected_id"] = address.id
        _uiState.update { it.copy(inspectedAddress = address, isInspectingLoading = true) }
        viewModelScope.launch {
            val useCacheOnly = !_uiState.value.isConnected
            val data = loadSingleAddressData(address, useCacheOnly, false)
            _uiState.update {
                it.copy(
                    inspectedScheduleList = data.scheduleList,
                    inspectedGroupedSchedule = data.groupedSchedule,
                    isInspectingLoading = false
                )
            }
        }
    }
    fun stopInspectingAddress() { savedStateHandle["inspected_id"] = null; _uiState.update { it.copy(inspectedAddress = null) } }
    
    fun isInspectedAddressSaved(): Boolean {
        val inspected = _uiState.value.inspectedAddress ?: return false
        return _uiState.value.savedAddresses.any { 
            it.streetId == inspected.streetId && it.addressId == inspected.addressId 
        }
    }

    fun saveInspectedAddress(customName: String? = null, customIcon: String? = null) {
        val inspected = _uiState.value.inspectedAddress ?: return
        viewModelScope.launch {
            if (!_uiState.value.isConnected) {
                _uiState.update { it.copy(error = offlineErrorText()) }
                return@launch
            }
            if (!isInspectedAddressSaved()) {
                val resolvedAddressName = inspected.addressName.ifBlank { inspected.name }
                val resolvedName = customName?.takeIf { it.isNotBlank() } ?: inspected.name.ifBlank { resolvedAddressName }
                val resolvedIcon = customIcon ?: inspected.iconName
                
                val newAddress = inspected.copy(
                    id = UUID.randomUUID().toString(), // Generate real ID
                    priority = repository.getSavedAddresses().size + 1,
                    name = resolvedName,
                    iconName = resolvedIcon,
                    addressName = resolvedAddressName
                )
                val resolved = resolveMissingAddressNames(newAddress)
                repository.saveNewAddress(resolved)
                loadSavedAddresses()
                _uiState.update { it.copy(inspectedAddress = resolved, infoMessage = "Адресу збережено") }
            }
        }
    }

    fun inspectAddressByIds(streetId: String, houseId: String, houseName: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAddressList(streetId).onSuccess { addresses ->
                val normalizedHouseName = houseName?.trim()?.takeIf { it.isNotEmpty() }
                val targetHouse = addresses.find { it.id == houseId }
                    ?: normalizedHouseName?.let { hn ->
                        addresses.find { address ->
                            address.getParsedHouseNumbers().any { it.equals(hn, ignoreCase = true) }
                        }
                    }
                if (targetHouse != null) {
                    val displayName = normalizedHouseName ?: targetHouse.name
                    val savedAddr = SavedAddress(
                        id = "temp_${System.currentTimeMillis()}",
                        name = displayName,
                        iconName = "location_on",
                        priority = 0,
                        remId = "", // We could fetch more, but cherga/pidcherga are enough for schedule
                        remName = "",
                        cityId = "",
                        cityName = "",
                        streetId = streetId,
                        streetName = "",
                        addressId = targetHouse.id,
                        addressName = displayName,
                        cherga = targetHouse.cherga,
                        pidcherga = targetHouse.pidcherga
                    )
                    val resolved = resolveMissingAddressNames(savedAddr)
                    startInspectingAddress(resolved)
                } else {
                    _uiState.update { it.copy(error = "Адресу не знайдено") }
                }
            }.onFailure {
                _uiState.update { it.copy(error = "Помилка завантаження даних адреси") }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun retryLoading() { refreshAllSchedules(allowOffline = true) }
    fun loadScheduleWithMessages(cherga: Int, pid: Int) { refreshAllSchedules(allowOffline = true) }
    private fun formatMessages(m: List<ScheduleMessagePart>): String = m.sortedBy { it.id }.joinToString("\n") { it.text }
    fun saveSelection(rI: String?, rN: String?, cI: String?, cN: String?, sI: String?, sN: String?, aI: String, aN: String, c: Int, p: Int, customName: String = "Дім", icon: String = "home") {
        viewModelScope.launch {
            if (!_uiState.value.isConnected) {
                _uiState.update { it.copy(error = offlineErrorText()) }
                return@launch
            }
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
    fun loadRemList() { 
        viewModelScope.launch { 
            _uiState.update { it.copy(isLoading = true) }
            repository.getRemList()
                .onSuccess { r -> _uiState.update { it.copy(remList = r, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        } 
    }
    fun loadCityList(id: String) { 
        viewModelScope.launch { 
            _uiState.update { it.copy(isLoading = true) }
            repository.getCityList(id)
                .onSuccess { r -> _uiState.update { it.copy(cityList = r, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        } 
    }
    fun loadStreetList(id: String) { 
        viewModelScope.launch { 
            _uiState.update { it.copy(isLoading = true) }
            repository.getStreetList(id)
                .onSuccess { r -> _uiState.update { it.copy(streetList = r, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        } 
    }
    fun loadAddressList(id: String) { 
        viewModelScope.launch { 
            _uiState.update { it.copy(isLoading = true) }
            repository.getAddressList(id)
                .onSuccess { r -> _uiState.update { it.copy(addressList = r, filteredHouseNumbers = repository.getAllParsedHouseNumbers(r), isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        } 
    }
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

    private fun formatUpdateTime(timestampMs: Long): String {
        return try {
            DateFormat.getTimeFormat(context).format(Date(timestampMs))
        } catch (e: Exception) {
            ""
        }
    }

    private fun offlineErrorText(): String = context.getString(R.string.error_no_connection)
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
    val notificationsEnabled: Boolean = true,
    val statusNotificationEnabled: Boolean = false,
    val isConnected: Boolean = false, val retryCountdown: Int = 0,
    val lastLoadFailed: Boolean = false, val isOffline: Boolean = false, val showWidgetConfig: Boolean = false,
    val displayMode: DisplayMode = DisplayMode.Comfortable,
    val colorTheme: ColorTheme = ColorTheme.System,
    val cornerRadius: Int = 24,
    val dynamicColors: Boolean = true,
    val isAmoled: Boolean = false,
    val inspectedAddress: SavedAddress? = null, val inspectedScheduleList: List<Schedule> = emptyList(),
    val inspectedGroupedSchedule: List<GroupedSchedule> = emptyList(), val isInspectingLoading: Boolean = false,
    val requestedAddressId: String? = null
)
