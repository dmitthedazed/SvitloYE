package com.occaecat.ztoeschedule

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
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
import com.occaecat.ztoeschedule.data.model.SavedAddress

import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Main Activity for ZTOE Schedule Application
 *
 * Main Activity that hosts the single-activity Compose UI.
 * with bottom navigation: Home, My Addresses, Settings
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesManager: EnergyPreferencesManager
    private val activityViewModel: EnergyScheduleViewModel by viewModels()
    private val addAddressLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val name = data.getStringExtra("name") ?: return@registerForActivityResult
        val icon = data.getStringExtra("icon") ?: "home"
        val remId = data.getStringExtra("remId") ?: return@registerForActivityResult
        val remName = data.getStringExtra("remName") ?: ""
        val cityId = data.getStringExtra("cityId") ?: return@registerForActivityResult
        val cityName = data.getStringExtra("cityName") ?: ""
        val streetId = data.getStringExtra("streetId") ?: return@registerForActivityResult
        val streetName = data.getStringExtra("streetName") ?: ""
        val addressId = data.getStringExtra("addressId") ?: return@registerForActivityResult
        val addressName = data.getStringExtra("addressName") ?: ""
        val cherga = data.getIntExtra("cherga", 0)
        val pidcherga = data.getIntExtra("pidcherga", 0)
        activityViewModel.addSavedAddress(name, icon, remId, remName, cityId, cityName, streetId, streetName, addressId, addressName, cherga, pidcherga)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        lifecycleScope.launch {
            // Check if onboarding is completed
            val onboardingCompleted = preferencesManager.onboardingCompletedFlow.first()
            if (!onboardingCompleted) {
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                finish()
                return@launch
            }

            // Schedule notification monitoring
            NotificationScheduler.schedulePowerMonitoring(this@MainActivity)

            // Trigger immediate check to update widgets
            NotificationScheduler.runImmediateCheck(this@MainActivity)

            // Ensure persistent notification services are running if enabled
            val statusEnabled = preferencesManager.statusNotificationEnabledFlow.first()
            if (statusEnabled) {
                // Unified service handles both standard and live activity styles internally
                com.occaecat.ztoeschedule.domain.notification.PowerStatusService.start(this@MainActivity)
            }

            keepSplash = false

            setContent {
                val viewModel = activityViewModel
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

            val colorTheme by preferencesManager.colorThemeFlow.collectAsStateWithLifecycle(initialValue = ColorTheme.System)
            val cornerRadius by preferencesManager.cornerRadiusFlow.collectAsStateWithLifecycle(initialValue = 24)
            val dynamicColors by preferencesManager.dynamicColorsFlow.collectAsStateWithLifecycle(initialValue = true)
            val isAmoled by preferencesManager.isAmoledFlow.collectAsStateWithLifecycle(initialValue = false)
            val displayMode by preferencesManager.displayModeFlow.collectAsStateWithLifecycle(initialValue = com.occaecat.ztoeschedule.data.model.DisplayMode.Comfortable)
            val liquidGlass by preferencesManager.liquidGlassFlow.collectAsStateWithLifecycle(initialValue = false)

            SvitloYeZhytomyrTheme(
                themePreference = colorTheme,
                cornerRadius = cornerRadius,
                displayMode = displayMode,
                liquidGlass = liquidGlass,
                dynamicColor = dynamicColors,
                isAmoled = isAmoled
            ) {
                val windowSizeClass = calculateWindowSizeClass(this@MainActivity)
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
            if (!viewModel.uiState.value.isConnected) {
                viewModel.showInfoMessage(getString(R.string.error_no_connection))
                return
            }
            addAddressLauncher.launch(Intent(this, AddressPickerActivity::class.java))
        } else if (intent.hasExtra("address_id")) {
            val addressId = intent.getStringExtra("address_id")
            viewModel.setRequestedAddressId(addressId)
        }

        // Handle Deep Link
        intent.data?.let { uri ->
            val params = com.occaecat.ztoeschedule.presentation.util.DeepLinkHelper.parseUri(uri)
            params?.let { (streetId, addressId, houseName) ->
                if (streetId != null && addressId != null) {
                    // 1. Try to find in already saved addresses
                    val existing = viewModel.uiState.value.savedAddresses.find { 
                        it.streetId == streetId && it.addressId == addressId 
                    }
                    
                    if (existing != null) {
                        viewModel.setRequestedAddressId(existing.id)
                    } else {
                        // 2. If not saved, use the new inspect logic
                        viewModel.inspectAddressByIds(streetId, addressId, houseName)
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
            getString(R.string.shortcut_group_navigation),
            listOf(
                android.view.KeyboardShortcutGroup(getString(R.string.shortcut_group_actions), listOf(
                    android.view.KeyboardShortcutInfo(getString(R.string.shortcut_add_address), android.view.KeyEvent.KEYCODE_N, android.view.KeyEvent.META_CTRL_ON),
                    android.view.KeyboardShortcutInfo(getString(R.string.shortcut_refresh_schedules), android.view.KeyEvent.KEYCODE_R, android.view.KeyEvent.META_CTRL_ON),
                    android.view.KeyboardShortcutInfo(getString(R.string.shortcut_open_menu), android.view.KeyEvent.KEYCODE_M, android.view.KeyEvent.META_CTRL_ON)
                ))
            ).flatMap { it.items }
        )
        data?.add(navGroup)
    }
}
