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
import androidx.hilt.navigation.compose.hiltViewModel
import com.occaecat.ztoeschedule.presentation.ui.settings.SettingsScreenRoot
import com.occaecat.ztoeschedule.presentation.viewmodel.SettingsViewModel
import com.occaecat.ztoeschedule.ui.theme.SvitloYeZhytomyrTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SettingsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            SvitloYeZhytomyrTheme(
                themePreference = state.colorTheme,
                cornerRadius = state.cornerRadius,
                dynamicColor = state.dynamicColors,
                isAmoled = state.isAmoled
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SettingsScreenRoot(
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}
