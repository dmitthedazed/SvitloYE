package com.occaecat.ztoeschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsTab
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel
import com.occaecat.ztoeschedule.ui.theme.SvitloYeZhytomyrTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: EnergyScheduleViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            SvitloYeZhytomyrTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Налаштування") },
                                navigationIcon = {
                                    IconButton(onClick = { finish() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        SettingsTab(
                            notificationsEnabled = uiState.notificationsEnabled,
                            statusNotificationEnabled = uiState.statusNotificationEnabled,
                            lastUpdateTime = uiState.lastUpdateTime,
                            displayMode = uiState.displayMode,
                            colorTheme = uiState.colorTheme,
                            cornerRadius = uiState.cornerRadius,
                            onNotificationsEnabledChange = { viewModel.setNotificationsEnabled(it) },
                            onStatusNotificationEnabledChange = { viewModel.setStatusNotificationEnabled(it) },
                            onDisplayModeChange = { viewModel.setDisplayMode(it) },
                            onColorThemeChange = { viewModel.setColorTheme(it) },
                            onCornerRadiusChange = { viewModel.setCornerRadius(it) },
                            onResetOnboarding = { viewModel.resetOnboarding() },
                            onClearData = { viewModel.clearData() },
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = innerPadding
                        )
                    }
                }
            }
        }
    }
}
