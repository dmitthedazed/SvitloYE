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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope

import com.occaecat.ztoeschedule.data.model.FontScale
import com.occaecat.ztoeschedule.data.model.SavedAddress

import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runBlocking

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

        // If onboarding is not completed or no saved selection, redirect to dedicated OnboardingActivity
        if (shouldStartOnboarding()) {
            startActivity(Intent(this, OnboardingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
            return
        }

        // Schedule notification monitoring
        NotificationScheduler.schedulePowerMonitoring(this)
        
        // Trigger immediate check to update widgets
        NotificationScheduler.runImmediateCheck(this)

        // Ensure persistent notification services are running if enabled
        CoroutineScope(Dispatchers.Main).launch {
            val statusEnabled = preferencesManager.statusNotificationEnabledFlow.first()
            if (statusEnabled) {
                // Unified service handles both standard and live activity styles internally
                com.occaecat.ztoeschedule.domain.notification.PowerStatusService.start(this@MainActivity)
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

            val colorTheme by preferencesManager.colorThemeFlow.collectAsState(initial = ColorTheme.System)
            val cornerRadius by preferencesManager.cornerRadiusFlow.collectAsState(initial = 24)

            SvitloYeZhytomyrTheme(
                themePreference = colorTheme,
                cornerRadius = cornerRadius
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
            startActivity(Intent(this, AddressPickerActivity::class.java))
        } else if (intent.hasExtra("address_id")) {
            val addressId = intent.getStringExtra("address_id")
            viewModel.setRequestedAddressId(addressId)
        }

        // Handle Deep Link
        intent.data?.let { uri ->
            val params = com.occaecat.ztoeschedule.presentation.util.DeepLinkHelper.parseUri(uri)
            params?.let { (streetId, houseId) ->
                if (streetId != null && houseId != null) {
                    // 1. Try to find in already saved addresses
                    val existing = viewModel.uiState.value.savedAddresses.find { 
                        it.streetId == streetId && it.addressId == houseId 
                    }
                    
                    if (existing != null) {
                        viewModel.setRequestedAddressId(existing.id)
                    } else {
                        // 2. If not saved, use the new inspect logic
                        viewModel.inspectAddressByIds(streetId, houseId)
                    }
                }
            }
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

    override fun onProvideKeyboardShortcuts(
        data: MutableList<android.view.KeyboardShortcutGroup>?,
        menu: android.view.Menu?,
        deviceId: Int
    ) {
        val navGroup = android.view.KeyboardShortcutGroup(
            "Навігація",
            listOf(
                android.view.KeyboardShortcutGroup("Дії", listOf(
                    android.view.KeyboardShortcutInfo("Додати адресу", android.view.KeyEvent.KEYCODE_N, android.view.KeyEvent.META_CTRL_ON),
                    android.view.KeyboardShortcutInfo("Оновити графіки", android.view.KeyEvent.KEYCODE_R, android.view.KeyEvent.META_CTRL_ON),
                    android.view.KeyboardShortcutInfo("Відкрити меню", android.view.KeyEvent.KEYCODE_M, android.view.KeyEvent.META_CTRL_ON)
                ))
            ).flatMap { it.items }
        )
        data?.add(navGroup)
    }

    private fun shouldStartOnboarding(): Boolean = runBlocking {
        val completed = preferencesManager.onboardingCompletedFlow.first()
        val selection = preferencesManager.savedSelectionFlow.first()
        return@runBlocking !completed || selection == null
    }
}

        