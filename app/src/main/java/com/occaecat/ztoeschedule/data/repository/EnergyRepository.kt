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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.supervisorScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.occaecat.ztoeschedule.data.local.dao.ScheduleDao
import com.occaecat.ztoeschedule.data.local.entity.ScheduleCacheEntity
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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

    /**
     * Check if address already exists based on addressId (unique API identifier)
     */
    suspend fun isAddressAlreadyAdded(addressId: String): Boolean {
        return addressStorage.getAddresses().any { 
            it.addressId == addressId 
        }
    }

    suspend fun saveNewAddress(address: com.occaecat.ztoeschedule.data.model.SavedAddress) {
        addressStorage.addAddress(address)
        if (address.priority == 1) {
            syncAddressToPreferences(address)
        }
    }

    suspend fun updateAddress(address: com.occaecat.ztoeschedule.data.model.SavedAddress) {
        addressStorage.updateAddress(address)
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
        }
        // Don't clear preferences - keep showing the last selected address even if deleted
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
            parseRawAddressString(address.name).map { (houseNumber, category) ->
                ParsedHouseNumber(
                    houseNumber = houseNumber,
                    cherga = address.cherga,
                    pidcherga = address.pidcherga,
                    originalAddressId = address.id,
                    category = category
                )
            }
        }.sortedWith(Comparator { o1, o2 ->
            val r1 = Regex("(\\d+)(.*)").find(o1.houseNumber)
            val r2 = Regex("(\\d+)(.*)").find(o2.houseNumber)

            if (r1 != null && r2 != null) {
                val (n1, s1) = r1.destructured
                val (n2, s2) = r2.destructured
                val numComp = n1.toInt().compareTo(n2.toInt())
                if (numComp != 0) numComp else s1.compareTo(s2)
            } else {
                o1.houseNumber.compareTo(o2.houseNumber)
            }
        })
    }

    private fun parseRawAddressString(raw: String): List<Pair<String, ConsumerCategory>> {
        return raw.split(",")
            .map { it.trim() }
            .mapNotNull { entry ->
                // Detect category based on suffix
                val category = when {
                    entry.contains("побутові", ignoreCase = true) && !entry.contains("непобутові", ignoreCase = true) -> ConsumerCategory.HOUSEHOLD
                    entry.contains("юридичні", ignoreCase = true) || 
                    entry.contains("непобутові", ignoreCase = true) ||
                    entry.contains("фізичні особи", ignoreCase = true) ||
                    entry.contains("ФОП", ignoreCase = true) ||
                    entry.contains("підприємство", ignoreCase = true) ||
                    entry.contains("установа", ignoreCase = true) ||
                    entry.contains("промислові", ignoreCase = true) -> ConsumerCategory.LEGAL
                    else -> ConsumerCategory.OTHER
                }

                // Clean the string from all possible technical suffixes
                val cleaned = entry
                    .replace(Regex("""\s*-\s*побутові споживачі\s*""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\s*-\s*непобутові\s*\(юридичні\)\s*споживачі\s*""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\s*-\s*юридичні споживачі\s*""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\s*-\s*фізичні особи\s*-\s*підприємці\s*""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\s*-\s*\w+\s+споживачі\s*""", RegexOption.IGNORE_CASE), "")
                    .trim()

                if (cleaned.isNotEmpty()) Pair(cleaned, category) else null
            }
            .distinctBy { it.first + it.second.name } 
    }

    // ========== Schedule and Messages Methods ==========

    suspend fun getScheduleWithMessages(
        cherga: Int,
        pidcherga: Int
    ): Result<ScheduleWithMessages> = try {
        // Check if this is a demo/test location
        if (com.occaecat.ztoeschedule.domain.debug.MockScheduleProvider.isDemoLocation(cherga, pidcherga)) {
            val mockSchedules = com.occaecat.ztoeschedule.domain.debug.MockScheduleProvider.generateMockSchedule()
            val mockMessages = listOf(
                ScheduleMessagePart(
                    id = 1,
                    text = "🔧 ДЕМО-РЕЖИМ: Статус змінюється кожну хвилину для тестування алертів"
                )
            )
            return Result.success(ScheduleWithMessages(mockSchedules, mockMessages))
        }
        
        supervisorScope {
            val scheduleDeferred = async { apiService.getSchedule(cherga, pidcherga) }
            val messagesDeferred = async { apiService.getMessages() }

            val scheduleResponse = try { scheduleDeferred.await() } catch (e: Exception) { null }
            val messagesResponse = try { messagesDeferred.await() } catch (e: Exception) { null }

            val schedules = scheduleResponse?.takeIf { it.isSuccessful }?.body()
            val messages = messagesResponse?.takeIf { it.isSuccessful }?.body()

            val scheduleHeader = scheduleResponse?.headers()?.get("Last-Modified-Shedules-Date")
            val messagesHeader = messagesResponse?.headers()?.get("Last-Modified-Shedules-Date")
            val serverUpdatedMs = parseServerLastModified(scheduleHeader)
                ?: parseServerLastModified(messagesHeader)

            if (schedules == null && messages == null) {
                // Both failed - load from cache
                loadFromCache(cherga, pidcherga)
            } else {
                // At least one succeeded - merge with cached data for partial fails
                val cached = scheduleDao.getScheduleOnce(cherga, pidcherga)
                val cachedData = if (cached != null) {
                    val typeS = object : TypeToken<List<Schedule>>() {}.type
                    val typeM = object : TypeToken<List<ScheduleMessagePart>>() {}.type
                    ScheduleWithMessages(
                        gson.fromJson(cached.scheduleJson, typeS) ?: emptyList(),
                        gson.fromJson(cached.messagesJson, typeM) ?: emptyList()
                    )
                } else null
                
                // Use fresh data where available, fall back to cached
                val finalSchedules = schedules ?: cachedData?.schedules ?: emptyList()
                val finalMessages = messages ?: cachedData?.messages ?: emptyList()
                
                val result = ScheduleWithMessages(finalSchedules, finalMessages)
                
                // Only update cache with non-empty data
                val entity = ScheduleCacheEntity(
                    cherga = cherga,
                    pidcherga = pidcherga,
                    scheduleJson = if (schedules != null) gson.toJson(schedules) else cached?.scheduleJson ?: "[]",
                    messagesJson = if (messages != null) gson.toJson(messages) else cached?.messagesJson ?: "[]",
                    lastUpdated = serverUpdatedMs ?: cached?.lastUpdated ?: System.currentTimeMillis()
                )
                scheduleDao.insertSchedule(entity)
                serverUpdatedMs?.let { preferencesManager.saveLastScheduleServerUpdatedMs(it) }
                Result.success(result)
            }
        }
    } catch (e: Exception) {
        loadFromCache(cherga, pidcherga)
    }

    /**
     * Retrieves schedule only from local cache.
     * Use this for frequent updates (like persistent notifications) to save battery.
     */
    suspend fun getCachedScheduleWithMessages(cherga: Int, pidcherga: Int): Result<ScheduleWithMessages> {
        // Check if this is a demo/test location
        if (com.occaecat.ztoeschedule.domain.debug.MockScheduleProvider.isDemoLocation(cherga, pidcherga)) {
            val mockSchedules = com.occaecat.ztoeschedule.domain.debug.MockScheduleProvider.generateMockSchedule()
            val mockMessages = listOf(
                ScheduleMessagePart(
                    id = 1,
                    text = "🔧 ДЕМО-РЕЖИМ: Статус змінюється кожну хвилину"
                )
            )
            return Result.success(ScheduleWithMessages(mockSchedules, mockMessages))
        }
        
        return loadFromCache(cherga, pidcherga)
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

    suspend fun getCacheLastUpdated(cherga: Int, pidcherga: Int): Long? {
        return scheduleDao.getScheduleOnce(cherga, pidcherga)?.lastUpdated
    }

    suspend fun getMessages(): Result<List<ScheduleMessagePart>> = try {
        val response = apiService.getMessages()
        if (response.isSuccessful) {
            val header = response.headers().get("Last-Modified-Shedules-Date")
            parseServerLastModified(header)?.let { preferencesManager.saveLastScheduleServerUpdatedMs(it) }
            Result.success(response.body() ?: emptyList())
        } else {
            Result.failure(Exception("HTTP error: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getSchedule(cherga: Int, pidcherga: Int): Result<List<Schedule>> = try {
        val response = apiService.getSchedule(cherga, pidcherga)
        if (response.isSuccessful) {
            val header = response.headers().get("Last-Modified-Shedules-Date")
            parseServerLastModified(header)?.let { preferencesManager.saveLastScheduleServerUpdatedMs(it) }
            Result.success(response.body() ?: emptyList())
        } else {
            Result.failure(Exception("HTTP error: ${response.code()}"))
        }
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

    fun getLastScheduleServerUpdatedFlow(): Flow<Long?> = preferencesManager.lastScheduleServerUpdatedFlow

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
    
    suspend fun clearAllData() {
        scheduleDao.deleteAll()
        addressStorage.clearAll()
        preferencesManager.clearPreferences()
    }

    // ========== Theme Settings ==========
    fun getDisplayModeFlow(): Flow<DisplayMode> = preferencesManager.displayModeFlow
    fun getColorThemeFlow(): Flow<ColorTheme> = preferencesManager.colorThemeFlow
    fun getCornerRadiusFlow(): Flow<Int> = preferencesManager.cornerRadiusFlow
    fun getDynamicColorsFlow(): Flow<Boolean> = preferencesManager.dynamicColorsFlow
    fun getIsAmoledFlow(): Flow<Boolean> = preferencesManager.isAmoledFlow
    fun getSmartNotificationSettingsFlow(): Flow<SmartNotificationSettings> = preferencesManager.smartNotificationSettingsFlow
    suspend fun setDisplayMode(mode: DisplayMode) = preferencesManager.setDisplayMode(mode)
    suspend fun setColorTheme(theme: ColorTheme) = preferencesManager.setColorTheme(theme)
    suspend fun setCornerRadius(radius: Int) = preferencesManager.setCornerRadius(radius)
    suspend fun setDynamicColors(enabled: Boolean) = preferencesManager.setDynamicColors(enabled)
    suspend fun setIsAmoled(enabled: Boolean) = preferencesManager.setIsAmoled(enabled)
    suspend fun saveSmartNotificationSettings(settings: SmartNotificationSettings) = preferencesManager.saveSmartNotificationSettings(settings)

    // ========== Notification Settings ==========
    fun getNotificationsEnabledFlow(): Flow<Boolean> = preferencesManager.notificationsEnabledFlow
    fun getNotificationAdvanceMinutesFlow(): Flow<Int> = preferencesManager.notificationAdvanceMinutesFlow
    fun getStatusNotificationEnabledFlow(): Flow<Boolean> = preferencesManager.statusNotificationEnabledFlow
    fun getNotificationModeFlow(): Flow<Int> = preferencesManager.notificationModeFlow

    suspend fun setNotificationsEnabled(enabled: Boolean) = preferencesManager.setNotificationsEnabled(enabled)
    suspend fun setNotificationAdvanceMinutes(minutes: Int) = preferencesManager.setNotificationAdvanceMinutes(minutes)
    suspend fun setStatusNotificationEnabled(enabled: Boolean) = preferencesManager.setStatusNotificationEnabled(enabled)
    suspend fun setNotificationMode(mode: Int) = preferencesManager.setNotificationMode(mode)
}

data class ScheduleWithMessages(val schedules: List<Schedule>, val messages: List<ScheduleMessagePart>)

private fun parseServerLastModified(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return try {
        val formatter = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        formatter.parse(value)?.time
    } catch (_: ParseException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

enum class ConsumerCategory(val label: String) {
    HOUSEHOLD("Побутові"),
    LEGAL("Юридичні"),
    OTHER("Інше")
}

data class ParsedHouseNumber(
    val houseNumber: String, 
    val cherga: Int, 
    val pidcherga: Int, 
    val originalAddressId: String,
    val category: ConsumerCategory
)
