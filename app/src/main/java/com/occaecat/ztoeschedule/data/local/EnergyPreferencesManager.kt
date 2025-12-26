package com.occaecat.ztoeschedule.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.data.model.FontScale
import com.occaecat.ztoeschedule.data.model.PriorityMode
import com.occaecat.ztoeschedule.data.model.SmartNotificationSettings

/**
 * Extension property to create DataStore instance
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "energy_preferences")

/**
 * DataStore manager for persisting user selections
 * Manages cherga and pidcherga preferences plus full selection chain
 */
class EnergyPreferencesManager(private val context: Context) {

    companion object {
        // Queue identifiers
        private val KEY_CHERGA = intPreferencesKey("cherga")
        private val KEY_PIDCHERGA = intPreferencesKey("pidcherga")

        // Selection chain keys
        private val KEY_REM_ID = stringPreferencesKey("rem_id")
        private val KEY_REM_NAME = stringPreferencesKey("rem_name")
        private val KEY_CITY_ID = stringPreferencesKey("city_id")
        private val KEY_CITY_NAME = stringPreferencesKey("city_name")
        private val KEY_STREET_ID = stringPreferencesKey("street_id")
        private val KEY_STREET_NAME = stringPreferencesKey("street_name")
        private val KEY_ADDRESS_ID = stringPreferencesKey("address_id")
        private val KEY_ADDRESS_NAME = stringPreferencesKey("address_name")

        // Onboarding flag
        private val KEY_ONBOARDING_COMPLETED = androidx.datastore.preferences.core.booleanPreferencesKey("onboarding_completed")

        // Notification settings
        private val KEY_NOTIFICATIONS_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("notifications_enabled")
        private val KEY_NOTIFICATION_MODE = intPreferencesKey("notification_mode") // 0: All, 1: Important, 2: Silent
        private val KEY_NOTIFICATION_ADVANCE_MINUTES = intPreferencesKey("notification_advance_minutes")
        private val KEY_STATUS_NOTIFICATION_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("status_notification_enabled")
        private val KEY_LIVE_ACTIVITY_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("live_activity_enabled")
        
        // Smart Notification Settings
        private val KEY_NOTIF_QUIET_START = intPreferencesKey("notif_quiet_start")
        private val KEY_NOTIF_QUIET_END = intPreferencesKey("notif_quiet_end")
        private val KEY_NOTIF_WORKDAY = androidx.datastore.preferences.core.booleanPreferencesKey("notif_workday")
        private val KEY_NOTIF_PRIORITY = intPreferencesKey("notif_priority")

        // Theme settings
        private val KEY_DISPLAY_MODE = intPreferencesKey("display_mode")
        private val KEY_COLOR_THEME = intPreferencesKey("color_theme")
        private val KEY_FONT_SCALE = intPreferencesKey("font_scale")
        
        // Cache
        private val KEY_LAST_SCHEDULE_HASH = stringPreferencesKey("last_schedule_hash")

        // Default values
        private const val DEFAULT_CHERGA = 0
        private const val DEFAULT_PIDCHERGA = 0
        private const val DEFAULT_NOTIFICATION_ADVANCE_MINUTES = 15 // 15 minutes before
    }

    /**
     * Flow that emits display mode preference
     */
    val displayModeFlow: Flow<DisplayMode> = context.dataStore.data.map { preferences ->
        val ordinal = preferences[KEY_DISPLAY_MODE] ?: DisplayMode.COMFORTABLE.ordinal
        DisplayMode.entries.getOrElse(ordinal) { DisplayMode.COMFORTABLE }
    }.distinctUntilChanged()

    /**
     * Flow that emits color theme preference
     */
    val colorThemeFlow: Flow<ColorTheme> = context.dataStore.data.map { preferences ->
        val ordinal = preferences[KEY_COLOR_THEME] ?: ColorTheme.SYSTEM.ordinal
        ColorTheme.entries.getOrElse(ordinal) { ColorTheme.SYSTEM }
    }.distinctUntilChanged()

    /**
     * Flow that emits font scale preference
     */
    val fontScaleFlow: Flow<FontScale> = context.dataStore.data.map { preferences ->
        val ordinal = preferences[KEY_FONT_SCALE] ?: FontScale.NORMAL.ordinal
        FontScale.entries.getOrElse(ordinal) { FontScale.NORMAL }
    }.distinctUntilChanged()

    /**
     * Flow that emits smart notification settings
     */
    val smartNotificationSettingsFlow: Flow<SmartNotificationSettings> = context.dataStore.data.map { preferences ->
        val start = preferences[KEY_NOTIF_QUIET_START] ?: 22
        val end = preferences[KEY_NOTIF_QUIET_END] ?: 7
        val workday = preferences[KEY_NOTIF_WORKDAY] ?: false
        val priorityOrdinal = preferences[KEY_NOTIF_PRIORITY] ?: PriorityMode.SMART.ordinal
        val priority = PriorityMode.entries.getOrElse(priorityOrdinal) { PriorityMode.SMART }

        SmartNotificationSettings(start, end, workday, priority)
    }.distinctUntilChanged()

    /**
     * Flow that emits the last known schedule hash
     */
    val lastScheduleHashFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_SCHEDULE_HASH]
    }.distinctUntilChanged()

    /**
     * Save last schedule hash
     */
    suspend fun saveLastScheduleHash(hash: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_SCHEDULE_HASH] = hash
        }
    }

    /**
     * Save smart notification settings
     */
    suspend fun saveSmartNotificationSettings(settings: SmartNotificationSettings) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIF_QUIET_START] = settings.quietHoursStart
            preferences[KEY_NOTIF_QUIET_END] = settings.quietHoursEnd
            preferences[KEY_NOTIF_WORKDAY] = settings.workdayMode
            preferences[KEY_NOTIF_PRIORITY] = settings.priorityMode.ordinal
        }
    }

    /**
     * Save display mode
     */
    suspend fun setDisplayMode(mode: DisplayMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DISPLAY_MODE] = mode.ordinal
        }
    }

    /**
     * Save color theme
     */
    suspend fun setColorTheme(theme: ColorTheme) {
        context.dataStore.edit { preferences ->
            preferences[KEY_COLOR_THEME] = theme.ordinal
        }
    }

    /**
     * Save font scale
     */
    suspend fun setFontScale(scale: FontScale) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FONT_SCALE] = scale.ordinal
        }
    }

    /**
     * Flow that emits the saved cherga value
     */
    val chergaFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_CHERGA] ?: DEFAULT_CHERGA
    }.distinctUntilChanged()

    /**
     * Flow that emits the saved pidcherga value
     */
    val pidchergaFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_PIDCHERGA] ?: DEFAULT_PIDCHERGA
    }.distinctUntilChanged()

    /**
     * Flow that emits both cherga and pidcherga as a Pair
     */
    val queueIdentifiersFlow: Flow<Pair<Int, Int>> = context.dataStore.data.map { preferences ->
        val cherga = preferences[KEY_CHERGA] ?: DEFAULT_CHERGA
        val pidcherga = preferences[KEY_PIDCHERGA] ?: DEFAULT_PIDCHERGA
        Pair(cherga, pidcherga)
    }.distinctUntilChanged()

    /**
     * Flow that emits saved selection chain
     */
    val savedSelectionFlow: Flow<SavedSelection?> = context.dataStore.data.map { preferences ->
        val remId = preferences[KEY_REM_ID]
        val remName = preferences[KEY_REM_NAME]
        val cityId = preferences[KEY_CITY_ID]
        val cityName = preferences[KEY_CITY_NAME]
        val streetId = preferences[KEY_STREET_ID]
        val streetName = preferences[KEY_STREET_NAME]
        val addressId = preferences[KEY_ADDRESS_ID]
        val addressName = preferences[KEY_ADDRESS_NAME]
        val cherga = preferences[KEY_CHERGA] ?: DEFAULT_CHERGA
        val pidcherga = preferences[KEY_PIDCHERGA] ?: DEFAULT_PIDCHERGA

        // Return SavedSelection only if we have required address data
        if (addressId != null && addressName != null && cherga > 0 && pidcherga > 0) {
            SavedSelection(
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
        } else {
            null
        }
    }.distinctUntilChanged()

    /**
     * Flow that emits onboarding completion status
     */
    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }.distinctUntilChanged()

    /**
     * Flow that emits notification enabled status
     */
    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_ENABLED] ?: true // Enabled by default
    }.distinctUntilChanged()

    /**
     * Flow that emits notification advance time in minutes
     */
    val notificationAdvanceMinutesFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATION_ADVANCE_MINUTES] ?: DEFAULT_NOTIFICATION_ADVANCE_MINUTES
    }.distinctUntilChanged()

    /**
     * Flow that emits status notification enabled status
     */
    val statusNotificationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_STATUS_NOTIFICATION_ENABLED] ?: false
    }.distinctUntilChanged()

    /**
     * Flow that emits live activity enabled status
     */
    val liveActivityEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_LIVE_ACTIVITY_ENABLED] ?: false
    }.distinctUntilChanged()

    val notificationModeFlow: Flow<Int> = context.dataStore.data.map { it[KEY_NOTIFICATION_MODE] ?: 0 }.distinctUntilChanged()

    /**
     * Save notification enabled status
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setNotificationMode(mode: Int) {
        context.dataStore.edit { it[KEY_NOTIFICATION_MODE] = mode }
    }

    /**
     * Save notification advance time in minutes
     */
    suspend fun setNotificationAdvanceMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_ADVANCE_MINUTES] = minutes
        }
    }

    /**
     * Save status notification enabled status
     */
    suspend fun setStatusNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_STATUS_NOTIFICATION_ENABLED] = enabled
        }
    }

    /**
     * Save live activity enabled status
     */
    suspend fun setLiveActivityEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LIVE_ACTIVITY_ENABLED] = enabled
        }
    }

    /**
     * Mark onboarding as completed
     */
    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    /**
     * Reset onboarding status (set to false)
     */
    suspend fun resetOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = false
        }
    }

    /**
     * Save cherga value to DataStore
     * @param cherga The cherga value to save
     */
    suspend fun saveCherga(cherga: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CHERGA] = cherga
        }
    }

    /**
     * Save pidcherga value to DataStore
     * @param pidcherga The pidcherga value to save
     */
    suspend fun savePidcherga(pidcherga: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PIDCHERGA] = pidcherga
        }
    }

    /**
     * Save both cherga and pidcherga values to DataStore
     * @param cherga The cherga value to save
     * @param pidcherga The pidcherga value to save
     */
    suspend fun saveQueueIdentifiers(cherga: Int, pidcherga: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CHERGA] = cherga
            preferences[KEY_PIDCHERGA] = pidcherga
        }
    }

    /**
     * Save complete selection chain
     * Saves all user selections from REM to Address
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
        context.dataStore.edit { preferences ->
            // Save selection chain
            remId?.let { preferences[KEY_REM_ID] = it }
            remName?.let { preferences[KEY_REM_NAME] = it }
            cityId?.let { preferences[KEY_CITY_ID] = it }
            cityName?.let { preferences[KEY_CITY_NAME] = it }
            streetId?.let { preferences[KEY_STREET_ID] = it }
            streetName?.let { preferences[KEY_STREET_NAME] = it }
            preferences[KEY_ADDRESS_ID] = addressId
            preferences[KEY_ADDRESS_NAME] = addressName

            // Save queue identifiers
            preferences[KEY_CHERGA] = cherga
            preferences[KEY_PIDCHERGA] = pidcherga
        }
    }

    /**
     * Clear all saved preferences
     */
    suspend fun clearPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

/**
 * Data class representing saved user selection
 * Contains the complete chain from REM to Address
 */
data class SavedSelection(
    val remId: String?,
    val remName: String?,
    val cityId: String?,
    val cityName: String?,
    val streetId: String?,
    val streetName: String?,
    val addressId: String,
    val addressName: String,
    val cherga: Int,
    val pidcherga: Int
)

