package com.occaecat.ztoeschedule.presentation.ui.adaptive

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScaffoldLayoutTest {
    @Test
    fun `compact width uses bottom bar`() {
        val layout = mainScaffoldLayoutFor(WindowWidthSizeClass.Compact)

        assertEquals(MainNavigationChrome.BottomBar, layout.navigationChrome)
        assertFalse(layout.useWideLayout)
    }

    @Test
    fun `medium width uses navigation rail`() {
        val layout = mainScaffoldLayoutFor(WindowWidthSizeClass.Medium)

        assertEquals(MainNavigationChrome.NavigationRail, layout.navigationChrome)
        assertTrue(layout.useWideLayout)
    }

    @Test
    fun `expanded width keeps navigation rail and wide layout`() {
        val layout = mainScaffoldLayoutFor(WindowWidthSizeClass.Expanded)

        assertEquals(MainNavigationChrome.NavigationRail, layout.navigationChrome)
        assertTrue(layout.useWideLayout)
    }
}
