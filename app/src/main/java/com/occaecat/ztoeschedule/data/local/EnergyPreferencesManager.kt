package com.occaecat.ztoeschedule.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.data.model.DisplayMode
import com.occaecat.ztoeschedule.data.model.FontScale
import com.occaecat.ztoeschedule.data.model.NotificationSettings
import com.occaecat.ztoeschedule.data.model.QuietHours

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
        private val KeyCherga = intPreferencesKey("cherga")
        private val KeyPidcherga = intPreferencesKey("pidcherga")

        // Selection chain keys
        private val KeyRemId = stringPreferencesKey("rem_id")
        private val KeyRemName = stringPreferencesKey("rem_name")
        private val KeyCityId = stringPreferencesKey("city_id")
        private val KeyCityName = stringPreferencesKey("city_name")
        private val KeyStreetId = stringPreferencesKey("street_id")
        private val KeyStreetName = stringPreferencesKey("street_name")
        private val KeyAddressId = stringPreferencesKey("address_id")
        private val KeyAddressName = stringPreferencesKey("address_name")


        // Anonymous usage statistics and crash reports (opt-out)
        private val KeyAnalyticsEnabled = booleanPreferencesKey("analytics_enabled")

        // Notification settings
        private val KeyNotificationsEnabled = booleanPreferencesKey("notifications_enabled")
        private val KeyNotificationAdvanceMinutes = intPreferencesKey("notification_advance_minutes")
        private val KeyStatusNotificationEnabled = booleanPreferencesKey("status_notification_enabled")
        private val KeyOutageAlerts = booleanPreferencesKey("notif_outage_alerts")
        private val KeyRestoreAlerts = booleanPreferencesKey("notif_restore_alerts")
        private val KeyScheduleChangeAlerts = booleanPreferencesKey("notif_schedule_change_alerts")
        private val KeyQuietEnabled = booleanPreferencesKey("notif_quiet_enabled")
        private val KeyQuietStartMinute = intPreferencesKey("notif_quiet_start_minute")
        private val KeyQuietEndMinute = intPreferencesKey("notif_quiet_end_minute")

        // Legacy keys, read once to migrate users to the new settings
        private val LegacyQuietStartHour = intPreferencesKey("notif_quiet_start")
        private val LegacyQuietEndHour = intPreferencesKey("notif_quiet_end")
        private val LegacyPriority = intPreferencesKey("notif_priority") // 0 = All (quiet hours ignored)

        // Theme settings
        private val KeyDisplayMode = intPreferencesKey("display_mode")
        private val KeyColorTheme = intPreferencesKey("color_theme")
        private val KeyDynamicColors = booleanPreferencesKey("dynamic_colors")
        private val KeyIsAmoled = booleanPreferencesKey("is_amoled")
        private val KeyLiquidGlass = booleanPreferencesKey("liquid_glass")

        // Cache
        private val KeyLastScheduleHash = stringPreferencesKey("last_schedule_hash")
        private val KeyLastScheduleServerUpdatedMs = longPreferencesKey("last_schedule_server_updated_ms")

        // Onboarding
        private val KeyOnboardingCompleted = booleanPreferencesKey("onboarding_completed")

        // Default values
        private const val DefaultCherga = 0
        private const val DefaultPidcherga = 0
    }

    /**
     * Flow that emits dynamic colors preference
     */
    val dynamicColorsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KeyDynamicColors] ?: true
    }.distinctUntilChanged()

    /**
     * Flow that emits AMOLED mode preference
     */
    val isAmoledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KeyIsAmoled] ?: false
    }.distinctUntilChanged()

    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KeyDynamicColors] = enabled
        }
    }

    suspend fun setIsAmoled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KeyIsAmoled] = enabled
        }
    }

    val analyticsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KeyAnalyticsEnabled] ?: true
    }.distinctUntilChanged()

    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KeyAnalyticsEnabled] = enabled
        }
    }

    val liquidGlassFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KeyLiquidGlass] ?: false
    }.distinctUntilChanged()

    suspend fun setLiquidGlass(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KeyLiquidGlass] = enabled
        }
    }

    /**
     * Flow that emits display mode preference
     */
    val displayModeFlow: Flow<DisplayMode> = context.dataStore.data.map { preferences ->
        val ordinal = preferences[KeyDisplayMode] ?: DisplayMode.Comfortable.ordinal
        DisplayMode.entries.getOrElse(ordinal) { DisplayMode.Comfortable }
    }.distinctUntilChanged()

    /**
     * Flow that emits color theme preference
     */
    val colorThemeFlow: Flow<ColorTheme> = context.dataStore.data.map { preferences ->
        val ordinal = preferences[KeyColorTheme] ?: ColorTheme.System.ordinal
        ColorTheme.entries.getOrElse(ordinal) { ColorTheme.System }
    }.distinctUntilChanged()



    /**
     * Last seen schedule snapshot, used to detect schedule updates (see ScheduleChangeDetector)
     */
    val scheduleSnapshotFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KeyLastScheduleHash]
    }.distinctUntilChanged()

    /**
     * Flow that emits the last server-provided schedule update time.
     */
    val lastScheduleServerUpdatedFlow: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[KeyLastScheduleServerUpdatedMs]
    }.distinctUntilChanged()

    /**
     * Save schedule snapshot
     */
    suspend fun saveScheduleSnapshot(snapshot: String) {
        context.dataStore.edit { preferences ->
            preferences[KeyLastScheduleHash] = snapshot
        }
    }

    /**
     * Save last server schedule update time (milliseconds).
     */
    suspend fun saveLastScheduleServerUpdatedMs(value: Long) {
        context.dataStore.edit { preferences ->
            preferences[KeyLastScheduleServerUpdatedMs] = value
        }
    }

    /**
     * Save display mode
     */
    suspend fun setDisplayMode(mode: DisplayMode) {
        context.dataStore.edit { preferences ->
            preferences[KeyDisplayMode] = mode.ordinal
        }
    }

    /**
     * Save color theme
     */
    suspend fun setColorTheme(theme: ColorTheme) {
        context.dataStore.edit { preferences ->
            preferences[KeyColorTheme] = theme.ordinal
        }
    }

    /**
     * Flow that emits the saved cherga value
     */
    val chergaFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KeyCherga] ?: DefaultCherga
    }.distinctUntilChanged()

    /**
     * Flow that emits the saved pidcherga value
     */
    val pidchergaFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KeyPidcherga] ?: DefaultPidcherga
    }.distinctUntilChanged()

    /**
     * Flow that emits both cherga and pidcherga as a Pair
     */
    val queueIdentifiersFlow: Flow<Pair<Int, Int>> = context.dataStore.data.map { preferences ->
        val cherga = preferences[KeyCherga] ?: DefaultCherga
        val pidcherga = preferences[KeyPidcherga] ?: DefaultPidcherga
        Pair(cherga, pidcherga)
    }.distinctUntilChanged()

    /**
     * Flow that emits saved selection chain
     */
    val savedSelectionFlow: Flow<SavedSelection?> = context.dataStore.data.map { preferences ->
        val remId = preferences[KeyRemId]
        val remName = preferences[KeyRemName]
        val cityId = preferences[KeyCityId]
        val cityName = preferences[KeyCityName]
        val streetId = preferences[KeyStreetId]
        val streetName = preferences[KeyStreetName]
        val addressId = preferences[KeyAddressId]
        val addressName = preferences[KeyAddressName]
        val cherga = preferences[KeyCherga] ?: DefaultCherga
        val pidcherga = preferences[KeyPidcherga] ?: DefaultPidcherga

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



    /** Master switch for pop-up alerts. */
    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KeyNotificationsEnabled] ?: true
    }.distinctUntilChanged()

    val statusNotificationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KeyStatusNotificationEnabled] ?: false
    }.distinctUntilChanged()

    val notificationSettingsFlow: Flow<NotificationSettings> = context.dataStore.data.map { p ->
        val defaults = NotificationSettings()
        val legacyQuiet = p[LegacyPriority]?.let { it != 0 } ?: false
        NotificationSettings(
            alertsEnabled = p[KeyNotificationsEnabled] ?: defaults.alertsEnabled,
            outageAlerts = p[KeyOutageAlerts] ?: defaults.outageAlerts,
            restoreAlerts = p[KeyRestoreAlerts] ?: defaults.restoreAlerts,
            leadMinutes = p[KeyNotificationAdvanceMinutes] ?: defaults.leadMinutes,
            scheduleChangeAlerts = p[KeyScheduleChangeAlerts] ?: defaults.scheduleChangeAlerts,
            statusNotification = p[KeyStatusNotificationEnabled] ?: defaults.statusNotification,
            quietHours = QuietHours(
                enabled = p[KeyQuietEnabled] ?: legacyQuiet,
                startMinute = p[KeyQuietStartMinute] ?: p[LegacyQuietStartHour]?.times(60) ?: defaults.quietHours.startMinute,
                endMinute = p[KeyQuietEndMinute] ?: p[LegacyQuietEndHour]?.times(60) ?: defaults.quietHours.endMinute
            )
        )
    }.distinctUntilChanged()

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KeyNotificationsEnabled] = enabled }
    }

    suspend fun setStatusNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KeyStatusNotificationEnabled] = enabled }
    }

    suspend fun saveNotificationSettings(settings: NotificationSettings) {
        context.dataStore.edit { p ->
            p[KeyNotificationsEnabled] = settings.alertsEnabled
            p[KeyOutageAlerts] = settings.outageAlerts
            p[KeyRestoreAlerts] = settings.restoreAlerts
            p[KeyNotificationAdvanceMinutes] = settings.leadMinutes
            p[KeyScheduleChangeAlerts] = settings.scheduleChangeAlerts
            p[KeyStatusNotificationEnabled] = settings.statusNotification
            p[KeyQuietEnabled] = settings.quietHours.enabled
            p[KeyQuietStartMinute] = settings.quietHours.startMinute
            p[KeyQuietEndMinute] = settings.quietHours.endMinute
            p.remove(LegacyQuietStartHour)
            p.remove(LegacyQuietEndHour)
            p.remove(LegacyPriority)
        }
    }


    /**
     * Save cherga value to DataStore
     * @param cherga The cherga value to save
     */
    suspend fun saveCherga(cherga: Int) {
        context.dataStore.edit { preferences ->
            preferences[KeyCherga] = cherga
        }
    }

    /**
     * Save pidcherga value to DataStore
     * @param pidcherga The pidcherga value to save
     */
    suspend fun savePidcherga(pidcherga: Int) {
        context.dataStore.edit { preferences ->
            preferences[KeyPidcherga] = pidcherga
        }
    }

    /**
     * Save both cherga and pidcherga values to DataStore
     * @param cherga The cherga value to save
     * @param pidcherga The pidcherga value to save
     */
    suspend fun saveQueueIdentifiers(cherga: Int, pidcherga: Int) {
        context.dataStore.edit { preferences ->
            preferences[KeyCherga] = cherga
            preferences[KeyPidcherga] = pidcherga
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
            remId?.let { preferences[KeyRemId] = it }
            remName?.let { preferences[KeyRemName] = it }
            cityId?.let { preferences[KeyCityId] = it }
            cityName?.let { preferences[KeyCityName] = it }
            streetId?.let { preferences[KeyStreetId] = it }
            streetName?.let { preferences[KeyStreetName] = it }
            preferences[KeyAddressId] = addressId
            preferences[KeyAddressName] = addressName

            // Save queue identifiers
            preferences[KeyCherga] = cherga
            preferences[KeyPidcherga] = pidcherga
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

    /**
     * Flow that emits whether onboarding is completed
     */
    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KeyOnboardingCompleted] ?: false
    }.distinctUntilChanged()

    /**
     * Mark onboarding as completed
     */
    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[KeyOnboardingCompleted] = true
        }
    }

    /**
     * Reset onboarding to show it again on next app start
     */
    suspend fun resetOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[KeyOnboardingCompleted] = false
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

