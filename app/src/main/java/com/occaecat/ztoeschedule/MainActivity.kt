package com.occaecat.ztoeschedule

import android.content.Intent
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

import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

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

        // Setup dynamic app shortcuts
        setupAppShortcuts()

        // Schedule notification monitoring
        NotificationScheduler.schedulePowerMonitoring(this)
        
        // Trigger immediate check to update widgets
        NotificationScheduler.runImmediateCheck(this)

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

    private fun setupAppShortcuts() {
        val shortcut = ShortcutInfoCompat.Builder(this, "check_status")
            .setShortLabel("Перевірити статус")
            .setLongLabel("Перевірити статус електропостачання")
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_bolt))
            .setIntent(
                Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("shortcut_id", "check_status")
                }
            )
            .build()

        ShortcutManagerCompat.addDynamicShortcuts(this, listOf(shortcut))
    }
}
