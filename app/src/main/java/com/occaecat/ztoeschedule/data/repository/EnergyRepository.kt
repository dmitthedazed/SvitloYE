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
import com.occaecat.ztoeschedule.data.network.GpvApiService
import com.occaecat.ztoeschedule.domain.ScheduleDomainLogic
import kotlinx.coroutines.CancellationException
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
    /**
     * One API address record covers several houses ("1, 3, 5А"), so a saved address is
     * the record plus the chosen house name.
     */
    suspend fun isAddressAlreadyAdded(addressId: String, addressName: String): Boolean {
        return addressStorage.getAddresses().any { it.isSameHouse(addressId, addressName) }
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
        if (com.occaecat.ztoeschedule.domain.debug.MockScheduleProvider.isPreviewLocation(cherga, pidcherga)) {
            return Result.success(
                ScheduleWithMessages(
                    com.occaecat.ztoeschedule.domain.debug.MockScheduleProvider.generatePreviewSchedule(),
                    com.occaecat.ztoeschedule.domain.debug.MockScheduleProvider.generatePreviewMessages()
                )
            )
        }
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

            val freshSchedules = scheduleResponse?.takeIf { it.isSuccessful }?.body()
            val messages = messagesResponse?.takeIf { it.isSuccessful }?.body()

            // The header dates the schedule, so only trust it when the schedule itself came through
            val serverUpdatedMs = scheduleResponse?.takeIf { freshSchedules != null }
                ?.headers()?.get("Last-Modified-Shedules-Date")
                ?.let(::parseServerLastModified)

            val cached = scheduleDao.getScheduleOnce(cherga, pidcherga)
            val cachedSchedules = cached?.let { parseSchedules(it.scheduleJson) }.orEmpty()
            val cachedMessages = cached?.let { parseMessages(it.messagesJson) }.orEmpty()

            // An empty answer next to a non-empty cache is almost always an API hiccup:
            // dropping the schedule would also silently drop every planned alert
            val schedules = freshSchedules?.takeUnless { it.isEmpty() && cachedSchedules.isNotEmpty() }

            if (schedules == null && messages == null) {
                // Nothing usable from the network - load from cache
                loadFromCache(cherga, pidcherga)
            } else {
                val result = ScheduleWithMessages(
                    schedules = schedules ?: cachedSchedules,
                    messages = messages ?: cachedMessages,
                    fromCache = schedules == null
                )
                scheduleDao.insertSchedule(
                    ScheduleCacheEntity(
                        cherga = cherga,
                        pidcherga = pidcherga,
                        scheduleJson = if (schedules != null) gson.toJson(schedules) else cached?.scheduleJson ?: "[]",
                        messagesJson = if (messages != null) gson.toJson(messages) else cached?.messagesJson ?: "[]",
                        // A stale schedule keeps its old date even if the messages are fresh
                        lastUpdated = if (schedules != null) serverUpdatedMs ?: System.currentTimeMillis()
                        else cached?.lastUpdated ?: 0L
                    )
                )
                serverUpdatedMs?.let { preferencesManager.saveLastScheduleServerUpdatedMs(it) }
                Result.success(result)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        loadFromCache(cherga, pidcherga)
    }

    /**
     * Retrieves schedule only from local cache.
     * Use this for frequent updates (like persistent notifications) to save battery.
     */
    suspend fun getCachedScheduleWithMessages(cherga: Int, pidcherga: Int): Result<ScheduleWithMessages> {
        // Check if this is a demo/test location
        if (com.occaecat.ztoeschedule.domain.debug.MockScheduleProvider.isPreviewLocation(cherga, pidcherga)) {
            return Result.success(
                ScheduleWithMessages(
                    com.occaecat.ztoeschedule.domain.debug.MockScheduleProvider.generatePreviewSchedule(),
                    com.occaecat.ztoeschedule.domain.debug.MockScheduleProvider.generatePreviewMessages()
                )
            )
        }
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
            Result.success(
                ScheduleWithMessages(parseSchedules(cached.scheduleJson), parseMessages(cached.messagesJson), fromCache = true)
            )
        } else {
            Result.failure(Exception("Не вдалося завантажити дані (офлайн)"))
        }
    }

    private fun parseSchedules(json: String): List<Schedule> =
        runCatching { gson.fromJson<List<Schedule>>(json, object : TypeToken<List<Schedule>>() {}.type) }.getOrNull().orEmpty()

    private fun parseMessages(json: String): List<ScheduleMessagePart> =
        runCatching { gson.fromJson<List<ScheduleMessagePart>>(json, object : TypeToken<List<ScheduleMessagePart>>() {}.type) }.getOrNull().orEmpty()

    suspend fun getCacheLastUpdated(cherga: Int, pidcherga: Int): Long? {
        return scheduleDao.getScheduleOnce(cherga, pidcherga)?.lastUpdated
    }

    suspend fun getMessages(): Result<List<ScheduleMessagePart>> = try {
        val response = apiService.getMessages()
        if (response.isSuccessful) {
            Result.success(response.body() ?: emptyList())
        } else {
            Result.failure(Exception("HTTP error: ${response.code()}"))
        }
    } catch (e: CancellationException) {
        throw e
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

    fun getCurrentStatus(schedules: List<Schedule>, nowMs: Long): Schedule? {
        return ScheduleDomainLogic.getCurrentStatus(schedules, nowMs)
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
    fun getDynamicColorsFlow(): Flow<Boolean> = preferencesManager.dynamicColorsFlow
    fun getIsAmoledFlow(): Flow<Boolean> = preferencesManager.isAmoledFlow
    suspend fun setDisplayMode(mode: DisplayMode) = preferencesManager.setDisplayMode(mode)
    suspend fun setColorTheme(theme: ColorTheme) = preferencesManager.setColorTheme(theme)
    suspend fun setDynamicColors(enabled: Boolean) = preferencesManager.setDynamicColors(enabled)
    suspend fun setIsAmoled(enabled: Boolean) = preferencesManager.setIsAmoled(enabled)

    // ========== Notification Settings ==========
    fun getNotificationsEnabledFlow(): Flow<Boolean> = preferencesManager.notificationsEnabledFlow
    fun getStatusNotificationEnabledFlow(): Flow<Boolean> = preferencesManager.statusNotificationEnabledFlow

    suspend fun setNotificationsEnabled(enabled: Boolean) = preferencesManager.setNotificationsEnabled(enabled)
    suspend fun setStatusNotificationEnabled(enabled: Boolean) = preferencesManager.setStatusNotificationEnabled(enabled)
}

data class ScheduleWithMessages(
    val schedules: List<Schedule>,
    val messages: List<ScheduleMessagePart>,
    /** The schedule is not fresh from the server (offline, API error or a suspicious empty answer). */
    val fromCache: Boolean = false
)

fun com.occaecat.ztoeschedule.data.model.SavedAddress.isSameHouse(addressId: String, addressName: String): Boolean =
    this.addressId == addressId && this.addressName.trim().equals(addressName.trim(), ignoreCase = true)

private fun parseServerLastModified(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return try {
        val formatter = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US).apply {
            // The server writes this header in Kyiv time
            timeZone = com.occaecat.ztoeschedule.domain.time.ScheduleZone.timeZone
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
