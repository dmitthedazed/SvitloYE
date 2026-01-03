package com.occaecat.ztoeschedule

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.occaecat.ztoeschedule.presentation.ui.onboarding.ImprovedOnboardingFlow
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel
import com.occaecat.ztoeschedule.ui.theme.SvitloYeZhytomyrTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: EnergyScheduleViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            // Если онбординг уже завершен, сразу идем в MainActivity
            LaunchedEffect(uiState.onboardingCompleted, uiState.hasSavedSelection) {
                if (uiState.onboardingCompleted && uiState.hasSavedSelection) {
                    startActivity(Intent(this@OnboardingActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    finish()
                }
            }

            SvitloYeZhytomyrTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Налаштування адреси") },
                                navigationIcon = {
                                    IconButton(onClick = { finish() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                    if (!uiState.isInitialLoadComplete) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        ImprovedOnboardingFlow(
                            uiState.remList,
                            uiState.cityList,
                            uiState.streetList,
                            uiState.filteredHouseNumbers,
                            uiState.houseNumberSearchQuery,
                            uiState.isLoading,
                            uiState.selectedCategory,
                            { viewModel.loadRemList() },
                            { viewModel.loadCityList(it) },
                            { viewModel.loadStreetList(it) },
                            { viewModel.loadAddressList(it) },
                            { viewModel.filterHouseNumbers(it) },
                            { viewModel.selectCategory(it) },
                            { viewModel.clearHouseNumberSearch() },
                            onComplete = { rI, rN, cI, cN, sI, sN, aI, aN, c, p, n, i ->
                                viewModel.saveSelection(rI, rN, cI, cN, sI, sN, aI, aN, c, p, n, i)
                                viewModel.completeOnboarding()
                                startActivity(Intent(this@OnboardingActivity, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                                finish()
                            }
                        )
                    }
                    }
                }
            }
        }
    }
}
