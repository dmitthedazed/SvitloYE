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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for managing energy outage schedule state
 *
 * Follows MVVM architecture pattern, maintaining UI state and coordinating
 * with the repository for data operations.
 */
class EnergyScheduleViewModel(
    private val repository: EnergyRepository
) : ViewModel() {

    // Private mutable state
    private val _uiState = MutableStateFlow(UiState())

    // Public immutable state for UI observation
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Load saved selection on initialization
        loadSavedSelection()
        // Load onboarding status
        loadOnboardingStatus()
        // Load notification settings
        loadNotificationSettings()
    }

    // ========== Onboarding ==========

    /**
     * Load onboarding completion status
     */
    private fun loadOnboardingStatus() {
        viewModelScope.launch {
            repository.getOnboardingCompletedFlow().collect { completed ->
                _uiState.update { it.copy(onboardingCompleted = completed) }
            }
        }
    }

    /**
     * Load notification settings
     */
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
        }
    }

    /**
     * Mark onboarding as completed
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted()
            _uiState.update { it.copy(onboardingCompleted = true) }
        }
    }

    /**
     * Reset onboarding to allow user to go through setup again
     */
    fun resetOnboarding() {
        viewModelScope.launch {
            repository.resetOnboarding()
            _uiState.update { it.copy(onboardingCompleted = false) }
        }
    }

    // ========== Selection Chain Methods ==========

    /**
     * Load list of REMs (first step in selection chain)
     */
    fun loadRemList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getRemList()
                .onSuccess { rems ->
                    _uiState.update { it.copy(remList = rems, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }

    /**
     * Load list of cities for selected REM
     */
    fun loadCityList(remId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getCityList(remId)
                .onSuccess { cities ->
                    _uiState.update { it.copy(cityList = cities, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }

    /**
     * Load list of streets for selected city
     */
    fun loadStreetList(cityId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getStreetList(cityId)
                .onSuccess { streets ->
                    _uiState.update { it.copy(streetList = streets, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }

    /**
     * Load list of addresses for selected street
     */
    fun loadAddressList(streetId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getAddressList(streetId)
                .onSuccess { addresses ->
                    // Parse all house numbers for the UI
                    val parsedHouses = repository.getAllParsedHouseNumbers(addresses)
                    _uiState.update {
                        it.copy(
                            addressList = addresses,
                            parsedHouseNumbers = parsedHouses,
                            filteredHouseNumbers = parsedHouses,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }

    /**
     * Filter house numbers based on search query
     * @param query The search text to filter by
     */
    fun filterHouseNumbers(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                state.parsedHouseNumbers
            } else {
                state.parsedHouseNumbers.filter {
                    it.houseNumber.contains(query, ignoreCase = true)
                }
            }
            state.copy(
                houseNumberSearchQuery = query,
                filteredHouseNumbers = filtered
            )
        }
    }

    /**
     * Clear house number search
     */
    fun clearHouseNumberSearch() {
        _uiState.update { state ->
            state.copy(
                houseNumberSearchQuery = "",
                filteredHouseNumbers = state.parsedHouseNumbers
            )
        }
    }

    // ========== Message Processing Methods ==========

    /**
     * Process message parts into a formatted string
     * - Sorts by ID
     * - Joins with newlines
     * - Cleans HTML entities
     *
     * @param messages List of ScheduleMessagePart from API
     * @return Formatted string ready for display
     */
    fun formatMessages(messages: List<ScheduleMessagePart>): String {
        if (messages.isEmpty()) return ""

        return messages
            .sortedBy { it.id }
            .joinToString("\n") { it.text }
            .cleanHtmlEntities()
    }

    /**
     * Clean HTML entities from string
     * Replaces common HTML entities with their actual characters
     */
    private fun String.cleanHtmlEntities(): String {
        return this
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
    }

    // ========== Schedule Methods ==========

    /**
     * Load schedule and messages for the given queue identifiers
     * Fetches both data sources simultaneously using coroutines
     */
    fun loadScheduleWithMessages(cherga: Int, pidcherga: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getScheduleWithMessages(cherga, pidcherga)
                .onSuccess { data ->
                    val currentStatus = repository.getCurrentStatus(data.schedules)
                    val formattedMessage = formatMessages(data.messages)
                    val groupedSchedule = ScheduleMapper.getGroupedSchedule(data.schedules)

                    _uiState.update {
                        it.copy(
                            scheduleList = data.schedules,
                            groupedSchedule = groupedSchedule,
                            infoMessages = data.messages,
                            formattedMessage = formattedMessage,
                            currentStatus = currentStatus,
                            isLoading = false
                        )
                    }
                    // Save the queue identifiers
                    repository.saveQueueIdentifiers(cherga, pidcherga)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }

    /**
     * Refresh current status based on existing schedule data
     * Useful for periodic updates without re-fetching data
     */
    fun refreshCurrentStatus() {
        _uiState.update { state ->
            val currentStatus = repository.getCurrentStatus(state.scheduleList)
            state.copy(currentStatus = currentStatus)
        }
    }

    /**
     * Load saved selection and fetch schedule if available
     * Automatically loads schedule on app start if user has saved selection
     */
    private fun loadSavedSelection() {
        viewModelScope.launch {
            repository.getSavedSelectionFlow().collect { savedSelection ->
                savedSelection?.let { selection ->
                    // Update UI state with saved selection info
                    _uiState.update {
                        it.copy(
                            hasSavedSelection = true,
                            savedRemName = selection.remName ?: "",
                            savedCityName = selection.cityName ?: "",
                            savedStreetName = selection.streetName ?: "",
                            savedAddressName = selection.addressName,
                            savedCherga = selection.cherga,
                            savedPidcherga = selection.pidcherga
                        )
                    }

                    // Auto-load schedule with saved data
                    if (selection.cherga > 0 && selection.pidcherga > 0) {
                        loadScheduleWithMessages(selection.cherga, selection.pidcherga)
                    }
                } ?: run {
                    _uiState.update { it.copy(hasSavedSelection = false) }
                }
            }
        }
    }

    /**
     * Save complete selection chain
     * Call this when user selects an address
     */
    fun saveSelection(
        remId: String?,
        remName: String?,
        cityId: String?,
        cityName: String?,
        streetId: String?,
        streetName: String?,
        addressId: String,
        addressName: String,
        cherga: Int,
        pidcherga: Int
    ) {
        viewModelScope.launch {
            repository.saveCompleteSelection(
                remId = remId,
                remName = remName,
                cityId = cityId,
                cityName = cityName,
                streetId = streetId,
                streetName = streetName,
                addressId = addressId,
                addressName = addressName,
                cherga = cherga,
                pidcherga = pidcherga
            )
            // Update UiState with saved address info
            _uiState.update {
                it.copy(
                    hasSavedSelection = true,
                    savedRemName = remName ?: "",
                    savedCityName = cityName ?: "",
                    savedStreetName = streetName ?: "",
                    savedAddressName = addressName,
                    savedCherga = cherga,
                    savedPidcherga = pidcherga
                )
            }
        }
    }

    /**
     * Clear all data and reset to initial state
     */
    fun clearData() {
        viewModelScope.launch {
            repository.clearPreferences()
            _uiState.value = UiState()
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ========== Notification Settings ==========

    /**
     * Update notifications enabled status
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotificationsEnabled(enabled)
        }
    }

    /**
     * Update notification advance time in minutes
     */
    fun setNotificationAdvanceMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setNotificationAdvanceMinutes(minutes)
        }
    }
}

/**
 * UI State data class representing the complete state of the screen
 */
data class UiState(
    // Selection chain data
    val remList: List<Rem> = emptyList(),
    val cityList: List<City> = emptyList(),
    val streetList: List<Street> = emptyList(),
    val addressList: List<Address> = emptyList(),

    // Parsed house numbers for grid display
    val parsedHouseNumbers: List<ParsedHouseNumber> = emptyList(),
    val filteredHouseNumbers: List<ParsedHouseNumber> = emptyList(),
    val houseNumberSearchQuery: String = "",

    // Saved address information
    val savedRemName: String = "",
    val savedCityName: String = "",
    val savedStreetName: String = "",
    val savedAddressName: String = "",
    val savedCherga: Int = 0,
    val savedPidcherga: Int = 0,

    // Schedule data
    val scheduleList: List<Schedule> = emptyList(),
    val groupedSchedule: List<GroupedSchedule> = emptyList(),
    val infoMessages: List<ScheduleMessagePart> = emptyList(),
    val formattedMessage: String = "",
    val currentStatus: Schedule? = null,

    // UI state flags
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasSavedSelection: Boolean = false,
    val onboardingCompleted: Boolean = false,

    // Notification settings
    val notificationsEnabled: Boolean = true,
    val notificationAdvanceMinutes: Int = 15
)

