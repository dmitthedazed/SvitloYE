package com.occaecat.ztoeschedule.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Minimal analytics wrapper for Firebase Analytics.
 * 
 * Privacy-first approach:
 * - The user can turn it off in settings (together with crash reports)
 * - No PII (personally identifiable information) collected
 * - No location data logged
 * - No address details logged
 * - Only anonymous usage patterns tracked
 * 
 * Tracked events:
 * - Screen views (anonymous)
 * - Feature usage counts
 * - Error categories (no stack traces)
 * - App lifecycle events
 */
@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    // Collection on/off is decided by the user (Settings → Дані) via [setCollectionEnabled];
    // Firebase persists that choice itself, so it must not be forced here
    private val firebaseAnalytics: FirebaseAnalytics by lazy { FirebaseAnalytics.getInstance(context) }
    
    // ===== Screen Tracking =====
    
    /**
     * Log screen view event.
     * Only logs screen name, no user data.
     */
    fun logScreenView(screenName: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        })
    }
    
    // ===== Feature Usage =====
    
    /**
     * Log when user adds an address (without address details).
     */
    fun logAddressAdded(method: AddressAddMethod) {
        firebaseAnalytics.logEvent("address_added", Bundle().apply {
            putString("method", method.name)
        })
    }
    
    /**
     * Log when user removes an address.
     */
    fun logAddressRemoved() {
        firebaseAnalytics.logEvent("address_removed", null)
    }
    
    /**
     * Log when user enables/disables status notification.
     */
    fun logNotificationToggle(enabled: Boolean) {
        firebaseAnalytics.logEvent("notification_toggle", Bundle().apply {
            putBoolean("enabled", enabled)
        })
    }
    
    /**
     * Log when widget is added to home screen.
     */
    fun logWidgetAdded(widgetType: String) {
        firebaseAnalytics.logEvent("widget_added", Bundle().apply {
            putString("widget_type", widgetType)
        })
    }
    
    /**
     * Log when user views schedule.
     */
    fun logScheduleViewed() {
        firebaseAnalytics.logEvent("schedule_viewed", null)
    }
    
    /**
     * Log when user uses QR scanner.
     */
    fun logQRScanAttempt(success: Boolean) {
        firebaseAnalytics.logEvent("qr_scan", Bundle().apply {
            putBoolean("success", success)
        })
    }
    
    /**
     * Log when user uses auto-location feature.
     */
    fun logAutoLocationAttempt(success: Boolean) {
        firebaseAnalytics.logEvent("auto_location", Bundle().apply {
            putBoolean("success", success)
        })
    }
    
    // ===== Error Tracking (Categories Only) =====
    
    /**
     * Log error category without sensitive details.
     */
    fun logError(category: ErrorCategory) {
        firebaseAnalytics.logEvent("app_error", Bundle().apply {
            putString("category", category.name)
        })
    }
    
    // ===== App Lifecycle =====
    
    /**
     * Log app opened event.
     */
    fun logAppOpened() {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
    }
    
    /**
     * Set user property for region (only region code, not exact location).
     */
    fun setRegion(regionCode: String) {
        firebaseAnalytics.setUserProperty("region", regionCode)
    }
    
    /**
     * Set user property for preferred theme.
     */
    fun setThemePreference(isDarkMode: Boolean) {
        firebaseAnalytics.setUserProperty("theme", if (isDarkMode) "dark" else "light")
    }
    
    // ===== Privacy Controls =====
    
    /** Turns usage statistics and crash reports on or off together (user choice in settings). */
    fun setCollectionEnabled(enabled: Boolean) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = enabled
    }
}

/**
 * Method used to add address.
 */
enum class AddressAddMethod {
    MANUAL,
    QR_CODE,
    AUTO_LOCATION,
    /** Opened from a shared link or QR (both carry the same URL), then saved. */
    LINK
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface AnalyticsEntryPoint {
    fun analytics(): AnalyticsManager
}

/** For composables that log directly (QR scanner, auto-location). */
@androidx.compose.runtime.Composable
fun rememberAnalytics(): AnalyticsManager {
    val context = androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.runtime.remember {
        dagger.hilt.android.EntryPointAccessors.fromApplication(context.applicationContext, AnalyticsEntryPoint::class.java).analytics()
    }
}

/**
 * Error categories for analytics (no sensitive data).
 */
enum class ErrorCategory {
    NETWORK_ERROR,
    API_ERROR,
    PARSE_ERROR,
    PERMISSION_DENIED,
    LOCATION_UNAVAILABLE,
    CAMERA_ERROR,
    STORAGE_ERROR,
    UNKNOWN
}
