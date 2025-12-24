package com.occaecat.ztoeschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.occaecat.ztoeschedule.domain.notification.NotificationScheduler
import com.occaecat.ztoeschedule.presentation.ui.MainScreen
import com.occaecat.ztoeschedule.ui.theme.ZTOEScheduleTheme

/**
 * Main Activity for ZTOE Schedule Application
 *
 * Displays onboarding for new users, then shows the main app
 * with bottom navigation: Home, My Addresses, Settings
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule notification monitoring
        NotificationScheduler.schedulePowerMonitoring(this)

        setContent {
            ZTOEScheduleTheme {
                MainScreen()
            }
        }
    }
}
