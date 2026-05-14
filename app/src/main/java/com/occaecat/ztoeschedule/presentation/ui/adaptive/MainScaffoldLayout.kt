package com.occaecat.ztoeschedule.presentation.ui.adaptive

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

enum class MainNavigationChrome {
    BottomBar,
    NavigationRail,
}

data class MainScaffoldLayout(
    val navigationChrome: MainNavigationChrome,
    val useWideLayout: Boolean,
)

fun mainScaffoldLayoutFor(widthSizeClass: WindowWidthSizeClass): MainScaffoldLayout {
    return when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> MainScaffoldLayout(
            navigationChrome = MainNavigationChrome.BottomBar,
            useWideLayout = false,
        )
        else -> MainScaffoldLayout(
            navigationChrome = MainNavigationChrome.NavigationRail,
            useWideLayout = true,
        )
    }
}
