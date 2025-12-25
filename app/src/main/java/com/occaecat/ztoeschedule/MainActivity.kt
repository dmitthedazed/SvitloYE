package com.occaecat.ztoeschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.domain.notification.NotificationScheduler
import com.occaecat.ztoeschedule.presentation.ui.MainScreen
import com.occaecat.ztoeschedule.ui.theme.SvitloYeZhytomyrTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import com.occaecat.ztoeschedule.data.model.FontScale

/**
 * Main Activity for ZTOE Schedule Application
 *
 * Displays onboarding for new users, then shows the main app
 * with bottom navigation: Home, My Addresses, Settings
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesManager: EnergyPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule notification monitoring
        NotificationScheduler.schedulePowerMonitoring(this)

        setContent {
            val colorTheme by preferencesManager.colorThemeFlow.collectAsState(initial = ColorTheme.SYSTEM)
            val fontScale by preferencesManager.fontScaleFlow.collectAsState(initial = FontScale.NORMAL)

            SvitloYeZhytomyrTheme(
                themePreference = colorTheme,
                fontScalePreference = fontScale
            ) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}
