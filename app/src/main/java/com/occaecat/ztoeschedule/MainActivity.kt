package com.occaecat.ztoeschedule

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.domain.notification.NotificationScheduler
import com.occaecat.ztoeschedule.presentation.ui.MainScreen
import com.occaecat.ztoeschedule.ui.theme.SvitloYeZhytomyrTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope

import com.occaecat.ztoeschedule.data.model.FontScale
import com.occaecat.ztoeschedule.data.model.SavedAddress

import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

import com.occaecat.ztoeschedule.domain.notification.StatusNotificationService
import com.occaecat.ztoeschedule.domain.notification.LiveActivityNotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Main Activity for ZTOE Schedule Application
 *
 * Displays onboarding for new users, then shows the main app
 * with bottom navigation: Home, My Addresses, Settings
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesManager: EnergyPreferencesManager

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule notification monitoring
        NotificationScheduler.schedulePowerMonitoring(this)
        
        // Trigger immediate check to update widgets
        NotificationScheduler.runImmediateCheck(this)

        // Ensure persistent notification services are running if enabled
        CoroutineScope(Dispatchers.Main).launch {
            val statusEnabled = preferencesManager.statusNotificationEnabledFlow.first()
            if (statusEnabled) {
                val liveEnabled = preferencesManager.liveActivityEnabledFlow.first()
                if (liveEnabled) {
                    LiveActivityNotificationService.start(this@MainActivity)
                } else {
                    StatusNotificationService.start(this@MainActivity)
                }
            }
        }

        setContent {
            val viewModel: EnergyScheduleViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            // Update Dynamic Shortcuts whenever addresses change
            LaunchedEffect(uiState.savedAddresses) {
                updateDynamicShortcuts(uiState.savedAddresses)
            }

            // Keep splash screen on until initial load is complete
            LaunchedEffect(uiState.isInitialLoadComplete) {
                if (uiState.isInitialLoadComplete) {
                    val content: View = findViewById(android.R.id.content)
                    content.viewTreeObserver.addOnPreDrawListener(
                        object : ViewTreeObserver.OnPreDrawListener {
                            override fun onPreDraw(): Boolean {
                                return if (uiState.isInitialLoadComplete) {
                                    content.viewTreeObserver.removeOnPreDrawListener(this)
                                    true
                                } else {
                                    false
                                }
                            }
                        }
                    )
                }
            }
            
            // Handle Intent for shortcuts and widget configuration
            LaunchedEffect(intent) {
                handleIntent(intent, viewModel)
            }

            val colorTheme by preferencesManager.colorThemeFlow.collectAsState(initial = ColorTheme.SYSTEM)
            val fontScale by preferencesManager.fontScaleFlow.collectAsState(initial = FontScale.NORMAL)

            SvitloYeZhytomyrTheme(
                themePreference = colorTheme,
                fontScalePreference = fontScale
            ) {
                val windowSizeClass = calculateWindowSizeClass(this)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        windowSizeClass = windowSizeClass
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIntent(intent: Intent, viewModel: EnergyScheduleViewModel) {
        if (intent.getBooleanExtra("configure_widget", false)) {
            viewModel.setShowWidgetConfig(true)
        }
        
        val shortcutId = intent.getStringExtra("shortcut_id")
        if (shortcutId == "add_address") {
            viewModel.startAddingAddress()
        } else if (intent.hasExtra("address_id")) {
            val addressId = intent.getStringExtra("address_id")
            viewModel.setRequestedAddressId(addressId)
        }
    }

    private fun updateDynamicShortcuts(addresses: List<SavedAddress>) {
        val shortcuts = mutableListOf<ShortcutInfoCompat>()

        // 1. Static-like dynamic shortcut for Check Status (always first)
        shortcuts.add(
            ShortcutInfoCompat.Builder(this, "check_status")
                .setShortLabel(getString(R.string.shortcut_check_status))
                .setIcon(IconCompat.createWithResource(this, R.drawable.ic_bolt))
                .setIntent(Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("shortcut_id", "check_status")
                })
                .setRank(0)
                .build()
        )

        // 2. Add shortcuts for top 3 saved addresses
        addresses.take(3).forEachIndexed { index, address ->
            val iconRes = when (address.iconName) {
                "home" -> R.drawable.ic_home_filled
                else -> R.drawable.ic_bolt
            }
            
            shortcuts.add(
                ShortcutInfoCompat.Builder(this, "address_${address.id}")
                    .setShortLabel(address.name)
                    .setLongLabel("${address.cityName}, ${address.streetName}")
                    .setIcon(IconCompat.createWithResource(this, iconRes))
                    .setIntent(Intent(this, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("shortcut_id", "address_${address.id}")
                        putExtra("address_id", address.id)
                    })
                    .setRank(index + 1)
                    .build()
            )
        }

        ShortcutManagerCompat.setDynamicShortcuts(this, shortcuts)
    }
}