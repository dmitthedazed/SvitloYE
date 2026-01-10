package com.occaecat.ztoeschedule.presentation.ui.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.core.layout.WindowHeightSizeClass

/**
 * Adaptive layout utilities for responsive UI across different screen sizes.
 * 
 * Uses Material 3 WindowSizeClass API to determine optimal layout configuration.
 * 
 * Screen size breakpoints:
 * - Compact: < 600dp (phones in portrait)
 * - Medium: 600dp - 840dp (phones landscape, small tablets)
 * - Expanded: >= 840dp (tablets, foldables, desktop)
 */

/**
 * Device category based on screen size and form factor.
 */
enum class DeviceCategory {
    /** Compact phone in portrait mode */
    COMPACT_PHONE,
    /** Phone in landscape or small tablet */
    MEDIUM_TABLET,
    /** Large tablet, foldable unfolded, desktop */
    EXPANDED_TABLET
}

/**
 * Layout configuration for responsive UI components.
 */
data class AdaptiveLayoutConfig(
    val deviceCategory: DeviceCategory,
    val widthClass: WindowWidthSizeClass,
    val heightClass: WindowHeightSizeClass,
    
    /** Number of columns for grid layouts */
    val gridColumns: Int,
    
    /** Use navigation rail instead of bottom nav */
    val useNavigationRail: Boolean,
    
    /** Use list-detail pane for schedule display */
    val useListDetailPane: Boolean,
    
    /** Content padding in dp */
    val contentPadding: Int,
    
    /** Card elevation in dp */
    val cardElevation: Int,
    
    /** Use large titles/headers */
    val useLargeTitles: Boolean
)

/**
 * Composable that provides adaptive layout configuration based on current window size.
 */
@Composable
fun rememberAdaptiveLayoutConfig(): AdaptiveLayoutConfig {
    val windowInfo = currentWindowAdaptiveInfo()
    val windowSizeClass = windowInfo.windowSizeClass
    
    return remember(windowSizeClass) {
        createAdaptiveConfig(
            widthClass = windowSizeClass.windowWidthSizeClass,
            heightClass = windowSizeClass.windowHeightSizeClass
        )
    }
}

/**
 * Create adaptive config based on window size classes.
 */
private fun createAdaptiveConfig(
    widthClass: WindowWidthSizeClass,
    heightClass: WindowHeightSizeClass
): AdaptiveLayoutConfig {
    val deviceCategory = when (widthClass) {
        WindowWidthSizeClass.COMPACT -> DeviceCategory.COMPACT_PHONE
        WindowWidthSizeClass.MEDIUM -> DeviceCategory.MEDIUM_TABLET
        else -> DeviceCategory.EXPANDED_TABLET
    }
    
    return when (deviceCategory) {
        DeviceCategory.COMPACT_PHONE -> AdaptiveLayoutConfig(
            deviceCategory = deviceCategory,
            widthClass = widthClass,
            heightClass = heightClass,
            gridColumns = 1,
            useNavigationRail = false,
            useListDetailPane = false,
            contentPadding = 16,
            cardElevation = 1,
            useLargeTitles = false
        )
        
        DeviceCategory.MEDIUM_TABLET -> AdaptiveLayoutConfig(
            deviceCategory = deviceCategory,
            widthClass = widthClass,
            heightClass = heightClass,
            gridColumns = 2,
            useNavigationRail = true,
            useListDetailPane = false,
            contentPadding = 24,
            cardElevation = 2,
            useLargeTitles = true
        )
        
        DeviceCategory.EXPANDED_TABLET -> AdaptiveLayoutConfig(
            deviceCategory = deviceCategory,
            widthClass = widthClass,
            heightClass = heightClass,
            gridColumns = 3,
            useNavigationRail = true,
            useListDetailPane = true,
            contentPadding = 32,
            cardElevation = 2,
            useLargeTitles = true
        )
    }
}

/**
 * Check if device is in compact mode (single-column layout recommended).
 */
@Composable
fun isCompactDevice(): Boolean {
    val config = rememberAdaptiveLayoutConfig()
    return config.deviceCategory == DeviceCategory.COMPACT_PHONE
}

/**
 * Check if list-detail pane layout should be used.
 */
@Composable
fun shouldUseListDetailPane(): Boolean {
    val config = rememberAdaptiveLayoutConfig()
    return config.useListDetailPane
}

/**
 * Check if navigation rail should be used instead of bottom navigation.
 */
@Composable
fun shouldUseNavigationRail(): Boolean {
    val config = rememberAdaptiveLayoutConfig()
    return config.useNavigationRail
}
