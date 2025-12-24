package com.occaecat.ztoeschedule.data.repository

import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.model.Address
import com.occaecat.ztoeschedule.data.model.City
import com.occaecat.ztoeschedule.data.model.Rem
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.data.model.ScheduleMessagePart
import com.occaecat.ztoeschedule.data.model.Street
import com.occaecat.ztoeschedule.data.network.GpvApiService
import com.occaecat.ztoeschedule.domain.ScheduleDomainLogic
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing energy outage data
 *
 * This repository acts as a single source of truth for the app, coordinating
 * between network API calls and local data persistence.
 */
class EnergyRepository(
    private val apiService: GpvApiService,
    private val preferencesManager: EnergyPreferencesManager
) {

    // ========== Selection Chain Methods ==========

    /**
     * Fetch list of regional energy managements (REMs)
     * First step in the selection chain
     */
    suspend fun getRemList(): Result<List<Rem>> {
        return try {
            val rems = apiService.getRemList()
            Result.success(rems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch list of cities for a specific REM
     * Second step in the selection chain
     *
     * @param remId The REM identifier
     */
    suspend fun getCityList(remId: String): Result<List<City>> {
        return try {
            val cities = apiService.getCityList(remId)
            Result.success(cities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch list of streets for a specific city
     * Third step in the selection chain
     *
     * @param cityId The city identifier
     */
    suspend fun getStreetList(cityId: String): Result<List<Street>> {
        return try {
            val streets = apiService.getStreetList(cityId)
            Result.success(streets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch list of addresses for a specific street
     * Fourth and final step in the selection chain
     *
     * @param streetId The street identifier
     */
    suspend fun getAddressList(streetId: String): Result<List<Address>> {
        return try {
            val addresses = apiService.getAddressList(streetId)
            Result.success(addresses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse raw address string containing multiple house numbers
     * Example: "25 - побутові споживачі, 27А - побутові споживачі, 31 - побутові споживачі"
     * Result: ["25", "27А", "31"]
     *
     * @param raw The raw address string from the API
     * @return List of clean house numbers
     */
    private fun parseRawAddressString(raw: String): List<String> {
        return raw
            .split(",")
            .map { it.trim() }
            .mapNotNull { entry ->
                // Remove suffix patterns like " - побутові споживачі"
                val cleaned = entry
                    .replace(Regex("""\s*-\s*побутові споживачі\s*"""), "")
                    .replace(Regex("""\s*-\s*\w+\s+споживачі\s*"""), "")
                    .trim()

                if (cleaned.isNotEmpty()) cleaned else null
            }
            .distinct()
    }

    /**
     * Get parsed house numbers for a given address
     * Transforms the raw API data into a clean list suitable for UI display
     *
     * @param address The address object from the API
     * @return List of individual house numbers
     */
    fun getParsedHouseNumbers(address: Address): List<String> {
        return parseRawAddressString(address.name)
    }

    /**
     * Get all parsed house numbers from a list of addresses
     * Useful for displaying all available houses in a grid layout
     *
     * @param addresses List of address objects
     * @return Flat list of all house numbers with their associated queue identifiers
     */
    fun getAllParsedHouseNumbers(addresses: List<Address>): List<ParsedHouseNumber> {
        return addresses.flatMap { address ->
            parseRawAddressString(address.name).map { houseNumber ->
                ParsedHouseNumber(
                    houseNumber = houseNumber,
                    cherga = address.cherga,
                    pidcherga = address.pidcherga,
                    originalAddressId = address.id
                )
            }
        }
    }

    // ========== Schedule and Messages Methods ==========

    /**
     * Fetch schedule and messages simultaneously using coroutines
     *
     * Uses async/await pattern to fetch both data sources in parallel,
     * improving performance by reducing total wait time.
     *
     * @param cherga Queue identifier
     * @param pidcherga Sub-queue identifier
     * @return Result containing ScheduleWithMessages or error
     */
    suspend fun getScheduleWithMessages(
        cherga: Int,
        pidcherga: Int
    ): Result<ScheduleWithMessages> = coroutineScope {
        try {
            // Launch both API calls simultaneously
            val scheduleDeferred = async { apiService.getSchedule(cherga, pidcherga) }
            val messagesDeferred = async { apiService.getMessages() }

            // Await both results
            val schedules = scheduleDeferred.await()
            val messages = messagesDeferred.await()

            Result.success(ScheduleWithMessages(schedules, messages))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch only schedule data
     *
     * @param cherga Queue identifier
     * @param pidcherga Sub-queue identifier
     */
    suspend fun getSchedule(cherga: Int, pidcherga: Int): Result<List<Schedule>> {
        return try {
            val schedules = apiService.getSchedule(cherga, pidcherga)
            Result.success(schedules)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch only messages data
     */
    suspend fun getMessages(): Result<List<ScheduleMessagePart>> {
        return try {
            val messages = apiService.getMessages()
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Domain Logic ==========

    /**
     * Get the currently active schedule based on current time
     *
     * Uses domain logic to parse time spans and determine which schedule
     * entry is active at the current moment.
     *
     * @param schedules List of Schedule objects
     * @return The active Schedule or null if none is active
     */
    fun getCurrentStatus(schedules: List<Schedule>): Schedule? {
        return ScheduleDomainLogic.getCurrentStatus(schedules)
    }

    // ========== Persistence Methods ==========

    /**
     * Save queue identifiers to DataStore
     *
     * @param cherga Queue identifier
     * @param pidcherga Sub-queue identifier
     */
    suspend fun saveQueueIdentifiers(cherga: Int, pidcherga: Int) {
        preferencesManager.saveQueueIdentifiers(cherga, pidcherga)
    }

    /**
     * Save complete selection chain to DataStore
     * This allows the app to restore the user's selection on next launch
     */
    suspend fun saveCompleteSelection(
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
        preferencesManager.saveCompleteSelection(
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
    }

    /**
     * Get saved queue identifiers as a Flow
     *
     * @return Flow emitting Pair<cherga, pidcherga>
     */
    fun getQueueIdentifiersFlow(): Flow<Pair<Int, Int>> {
        return preferencesManager.queueIdentifiersFlow
    }

    /**
     * Get saved selection chain as a Flow
     *
     * @return Flow emitting SavedSelection or null if no selection saved
     */
    fun getSavedSelectionFlow(): Flow<com.occaecat.ztoeschedule.data.local.SavedSelection?> {
        return preferencesManager.savedSelectionFlow
    }

    /**
     * Get saved cherga as a Flow
     */
    fun getChergaFlow(): Flow<Int> {
        return preferencesManager.chergaFlow
    }

    /**
     * Get saved pidcherga as a Flow
     */
    fun getPidchergaFlow(): Flow<Int> {
        return preferencesManager.pidchergaFlow
    }

    /**
     * Get onboarding completion status as Flow
     */
    fun getOnboardingCompletedFlow(): Flow<Boolean> {
        return preferencesManager.onboardingCompletedFlow
    }

    /**
     * Mark onboarding as completed
     */
    suspend fun setOnboardingCompleted() {
        preferencesManager.setOnboardingCompleted()
    }

    /**
     * Reset onboarding status (for settings)
     */
    suspend fun resetOnboarding() {
        preferencesManager.resetOnboarding()
    }

    /**
     * Clear all saved preferences
     */
    suspend fun clearPreferences() {
        preferencesManager.clearPreferences()
    }

    // ========== Notification Settings ==========

    /**
     * Get notifications enabled flow
     */
    fun getNotificationsEnabledFlow(): Flow<Boolean> {
        return preferencesManager.notificationsEnabledFlow
    }

    /**
     * Get notification advance minutes flow
     */
    fun getNotificationAdvanceMinutesFlow(): Flow<Int> {
        return preferencesManager.notificationAdvanceMinutesFlow
    }

    /**
     * Set notifications enabled status
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        preferencesManager.setNotificationsEnabled(enabled)
    }

    /**
     * Set notification advance time in minutes
     */
    suspend fun setNotificationAdvanceMinutes(minutes: Int) {
        preferencesManager.setNotificationAdvanceMinutes(minutes)
    }
}

/**
 * Data class combining schedule and messages
 */
data class ScheduleWithMessages(
    val schedules: List<Schedule>,
    val messages: List<ScheduleMessagePart>
)

/**
 * Data class representing a parsed house number with queue identifiers
 * Used for displaying individual house numbers in the UI
 */
data class ParsedHouseNumber(
    val houseNumber: String,
    val cherga: Int,
    val pidcherga: Int,
    val originalAddressId: String
)

