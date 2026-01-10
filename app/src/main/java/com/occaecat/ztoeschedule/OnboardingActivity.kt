package com.occaecat.ztoeschedule

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.presentation.ui.onboarding.OnboardingScreen
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel
import com.occaecat.ztoeschedule.ui.theme.SvitloYeZhytomyrTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Activity that hosts the onboarding flow for new users
 */
@AndroidEntryPoint
class OnboardingActivity : ComponentActivity() {

    @Inject lateinit var preferencesManager: EnergyPreferencesManager
    private val viewModel: EnergyScheduleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val colorTheme by preferencesManager.colorThemeFlow.collectAsState(initial = ColorTheme.System)
            val cornerRadius by preferencesManager.cornerRadiusFlow.collectAsState(initial = 24)
            val dynamicColors by preferencesManager.dynamicColorsFlow.collectAsState(initial = true)
            val isAmoled by preferencesManager.isAmoledFlow.collectAsState(initial = false)
            val scope = rememberCoroutineScope()

            SvitloYeZhytomyrTheme(
                themePreference = colorTheme,
                cornerRadius = cornerRadius,
                dynamicColor = dynamicColors,
                isAmoled = isAmoled
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onComplete = { addressResult ->
                            scope.launch {
                                // Save the address if one was added
                                addressResult?.let { result ->
                                    viewModel.addSavedAddress(
                                        name = result.displayName,
                                        icon = result.iconName,
                                        rI = result.remId,
                                        rN = result.remName,
                                        cI = result.cityId,
                                        cN = result.cityName,
                                        sI = result.streetId,
                                        sN = result.streetName,
                                        aI = result.addressId,
                                        aN = result.addressName,
                                        c = result.cherga,
                                        p = result.pidcherga
                                    )
                                }
                                
                                // Mark onboarding as completed
                                preferencesManager.setOnboardingCompleted()
                                
                                // Navigate to MainActivity
                                startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                                finish()
                            }
                        },
                        onSkip = {
                            scope.launch {
                                preferencesManager.setOnboardingCompleted()
                                startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }
}
