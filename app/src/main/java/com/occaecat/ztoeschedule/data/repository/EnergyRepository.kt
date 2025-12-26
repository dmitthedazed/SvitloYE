package com.occaecat.ztoeschedule.data.repository

import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.model.Address
import com.occaecat.ztoeschedule.data.model.City
import com.occaecat.ztoeschedule.data.model.Rem
import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.data.model.ScheduleMessagePart
import com.occaecat.ztoeschedule.data.model.Street
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.data.model.FontScale
import com.occaecat.ztoeschedule.data.model.SmartNotificationSettings
import com.occaecat.ztoeschedule.data.network.GpvApiService
import com.occaecat.ztoeschedule.domain.ScheduleDomainLogic
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.occaecat.ztoeschedule.data.local.dao.ScheduleDao
import com.occaecat.ztoeschedule.data.local.entity.ScheduleCacheEntity

/**
 * Repository for managing energy outage data
 */
class EnergyRepository(
    private val apiService: GpvApiService,
    private val preferencesManager: EnergyPreferencesManager,
    private val addressStorage: com.occaecat.ztoeschedule.data.local.AddressStorage,
    private val scheduleDao: ScheduleDao,
    private val gson: Gson
) {

    // ========== Address Management Methods ==========

    suspend fun getSavedAddresses(): List<com.occaecat.ztoeschedule.data.model.SavedAddress> {
        return addressStorage.getAddresses()
    }

    suspend fun saveNewAddress(address: com.occaecat.ztoeschedule.data.model.SavedAddress) {
        addressStorage.addAddress(address)
        if (address.priority == 1) {
            syncAddressToPreferences(address)
        }
    }

    suspend fun deleteAddress(id: String) {
        addressStorage.deleteAddress(id)
        val addresses = addressStorage.getAddresses()
        if (addresses.isNotEmpty()) {
            val primary = addresses.find { it.priority == 1 } ?: addresses.first()
            syncAddressToPreferences(primary)
        } else {
            preferencesManager.clearPreferences()
        }
    }

    suspend fun setPrimaryAddress(id: String) {
        val updatedList = addressStorage.setAsPrimary(id)
        val newPrimary = updatedList.find { it.priority == 1 }
        if (newPrimary != null) {
            syncAddressToPreferences(newPrimary)
        }
    }

    suspend fun reorderAddresses(list: List<com.occaecat.ztoeschedule.data.model.SavedAddress>) {
        val updatedList = list.mapIndexed { index, savedAddress ->
            savedAddress.copy(priority = index + 1)
        }
        addressStorage.updateAll(updatedList)
        updatedList.firstOrNull()?.let { syncAddressToPreferences(it) }
    }

    private suspend fun syncAddressToPreferences(address: com.occaecat.ztoeschedule.data.model.SavedAddress) {
        preferencesManager.saveCompleteSelection(
            remId = address.remId,
            remName = address.remName,
            cityId = address.cityId,
            cityName = address.cityName,
            streetId = address.streetId,
            streetName = address.streetName,
            addressId = address.addressId,
            addressName = address.addressName,
            cherga = address.cherga,
            pidcherga = address.pidcherga
        )
    }

    // ========== Selection Chain Methods ==========

    suspend fun getRemList(): Result<List<Rem>> = try {
        Result.success(apiService.getRemList())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getCityList(remId: String): Result<List<City>> = try {
        Result.success(apiService.getCityList(remId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getStreetList(cityId: String): Result<List<Street>> = try {
        Result.success(apiService.getStreetList(cityId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAddressList(streetId: String): Result<List<Address>> = try {
        Result.success(apiService.getAddressList(streetId))
    } catch (e: Exception) {
        Result.failure(e)
    }

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

    private fun parseRawAddressString(raw: String): List<String> {
        return raw.split(",")
            .map { it.trim() }
            .mapNotNull { entry ->
                val cleaned = entry
                    .replace(Regex("""\s*-\s*побутові споживачі\s*"""), "")
                    .replace(Regex("""\s*-\s*\w+\s+споживачі\s*"""), "")
                    .trim()
                if (cleaned.isNotEmpty()) cleaned else null
            }
            .distinct()
    }

    // ========== Schedule and Messages Methods ==========

    suspend fun getScheduleWithMessages(
        cherga: Int,
        pidcherga: Int
    ): Result<ScheduleWithMessages> = try {
        coroutineScope {
            val scheduleDeferred = async { apiService.getSchedule(cherga, pidcherga) }
            val messagesDeferred = async { apiService.getMessages() }

            val schedules = try { scheduleDeferred.await() } catch (e: Exception) { null }
            val messages = try { messagesDeferred.await() } catch (e: Exception) { null }

            if (schedules == null && messages == null) {
                loadFromCache(cherga, pidcherga)
            } else {
                val result = ScheduleWithMessages(schedules ?: emptyList(), messages ?: emptyList())
                val entity = ScheduleCacheEntity(
                    cherga = cherga,
                    pidcherga = pidcherga,
                    scheduleJson = gson.toJson(result.schedules),
                    messagesJson = gson.toJson(result.messages),
                    lastUpdated = System.currentTimeMillis()
                )
                scheduleDao.insertSchedule(entity)
                Result.success(result)
            }
        }
    } catch (e: Exception) {
        loadFromCache(cherga, pidcherga)
    }

    private suspend fun loadFromCache(cherga: Int, pidcherga: Int): Result<ScheduleWithMessages> {
        val cached = scheduleDao.getScheduleOnce(cherga, pidcherga)
        return if (cached != null) {
            val typeS = object : TypeToken<List<Schedule>>() {}.type
            val typeM = object : TypeToken<List<ScheduleMessagePart>>() {}.type
            val schedules: List<Schedule> = gson.fromJson(cached.scheduleJson, typeS)
            val messages: List<ScheduleMessagePart> = gson.fromJson(cached.messagesJson, typeM)
            Result.success(ScheduleWithMessages(schedules, messages))
        } else {
            Result.failure(Exception("Не вдалося завантажити дані (офлайн)"))
        }
    }

    suspend fun getMessages(): Result<List<ScheduleMessagePart>> = try {
        Result.success(apiService.getMessages())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getSchedule(cherga: Int, pidcherga: Int): Result<List<Schedule>> = try {
        Result.success(apiService.getSchedule(cherga, pidcherga))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getServerTime(): Result<Long> = try {
        val response = apiService.getHeaders()
        if (response.isSuccessful) {
            val dateHeader = response.headers().get("Date")
            val format = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.US)
            val serverDate = dateHeader?.let { format.parse(it) }
            Result.success(serverDate?.time ?: System.currentTimeMillis())
        } else {
            Result.failure(Exception("HTTP error: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getCurrentStatus(schedules: List<Schedule>): Schedule? {
        return ScheduleDomainLogic.getCurrentStatus(schedules)
    }

    // ========== Persistence & Flows ==========

    suspend fun saveQueueIdentifiers(cherga: Int, pidcherga: Int) {
        preferencesManager.saveQueueIdentifiers(cherga, pidcherga)
    }

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

    fun getSavedSelectionFlow(): Flow<com.occaecat.ztoeschedule.data.local.SavedSelection?> = preferencesManager.savedSelectionFlow
    fun getOnboardingCompletedFlow(): Flow<Boolean> = preferencesManager.onboardingCompletedFlow
    suspend fun setOnboardingCompleted() = preferencesManager.setOnboardingCompleted()
    suspend fun resetOnboarding() = preferencesManager.resetOnboarding()
    suspend fun clearPreferences() = preferencesManager.clearPreferences()

    // ========== Theme Settings ==========
    fun getDisplayModeFlow(): Flow<DisplayMode> = preferencesManager.displayModeFlow
    fun getColorThemeFlow(): Flow<ColorTheme> = preferencesManager.colorThemeFlow
    fun getFontScaleFlow(): Flow<FontScale> = preferencesManager.fontScaleFlow
    fun getSmartNotificationSettingsFlow(): Flow<SmartNotificationSettings> = preferencesManager.smartNotificationSettingsFlow
    suspend fun setDisplayMode(mode: DisplayMode) = preferencesManager.setDisplayMode(mode)
    suspend fun setColorTheme(theme: ColorTheme) = preferencesManager.setColorTheme(theme)
    suspend fun setFontScale(scale: FontScale) = preferencesManager.setFontScale(scale)
    suspend fun saveSmartNotificationSettings(settings: SmartNotificationSettings) = preferencesManager.saveSmartNotificationSettings(settings)

    // ========== Notification Settings ==========
    fun getNotificationsEnabledFlow(): Flow<Boolean> = preferencesManager.notificationsEnabledFlow
    fun getNotificationAdvanceMinutesFlow(): Flow<Int> = preferencesManager.notificationAdvanceMinutesFlow
    fun getStatusNotificationEnabledFlow(): Flow<Boolean> = preferencesManager.statusNotificationEnabledFlow
    fun getLiveActivityEnabledFlow(): Flow<Boolean> = preferencesManager.liveActivityEnabledFlow
    fun getNotificationModeFlow(): Flow<Int> = preferencesManager.notificationModeFlow

    suspend fun setNotificationsEnabled(enabled: Boolean) = preferencesManager.setNotificationsEnabled(enabled)
    suspend fun setNotificationAdvanceMinutes(minutes: Int) = preferencesManager.setNotificationAdvanceMinutes(minutes)
    suspend fun setStatusNotificationEnabled(enabled: Boolean) = preferencesManager.setStatusNotificationEnabled(enabled)
    suspend fun setLiveActivityEnabled(enabled: Boolean) = preferencesManager.setLiveActivityEnabled(enabled)
    suspend fun setNotificationMode(mode: Int) = preferencesManager.setNotificationMode(mode)

    // ========== Existing methods ... 
}

data class ScheduleWithMessages(val schedules: List<Schedule>, val messages: List<ScheduleMessagePart>)
data class ParsedHouseNumber(val houseNumber: String, val cherga: Int, val pidcherga: Int, val originalAddressId: String)