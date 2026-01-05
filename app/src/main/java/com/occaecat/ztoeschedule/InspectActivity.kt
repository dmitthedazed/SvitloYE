package com.occaecat.ztoeschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.presentation.ui.home.HomeTab
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel
import com.occaecat.ztoeschedule.ui.theme.SvitloYeZhytomyrTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InspectActivity : ComponentActivity() {
    
    @Inject lateinit var preferencesManager: EnergyPreferencesManager
    
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: EnergyScheduleViewModel = hiltViewModel()
            val uiStateValue = viewModel.uiState.collectAsState()
            val uiState = uiStateValue.value
            
            val colorThemeState = preferencesManager.colorThemeFlow.collectAsState(initial = ColorTheme.System)
            val colorTheme = colorThemeState.value
            val cornerRadiusState = preferencesManager.cornerRadiusFlow.collectAsState(initial = 24)
            val cornerRadius = cornerRadiusState.value

            SvitloYeZhytomyrTheme(
                themePreference = colorTheme,
                cornerRadius = cornerRadius
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val addr = uiState.inspectedAddress
                    
                    // Load address if not yet loaded
                    LaunchedEffect(Unit) {
                        if (addr == null) {
                            // Try to get address from intent extras first
                            val remId = intent.getStringExtra("remId")
                            val streetId = intent.getStringExtra("streetId")
                            val addressId = intent.getStringExtra("addressId")
                            val name = intent.getStringExtra("name")
                            if (remId != null && streetId != null && addressId != null && name != null) {
                                viewModel.startInspectingAddress(
                                    com.occaecat.ztoeschedule.data.model.SavedAddress(
                                        id = addressId,
                                        name = name,
                                        iconName = intent.getStringExtra("iconName") ?: "other",
                                        priority = intent.getIntExtra("priority", 1),
                                        remId = remId,
                                        remName = intent.getStringExtra("remName") ?: "",
                                        cityId = intent.getStringExtra("cityId") ?: "",
                                        cityName = intent.getStringExtra("cityName") ?: "",
                                        streetId = streetId,
                                        streetName = intent.getStringExtra("streetName") ?: "",
                                        addressId = addressId,
                                        addressName = intent.getStringExtra("addressName") ?: "",
                                        cherga = intent.getIntExtra("cherga", 0),
                                        pidcherga = intent.getIntExtra("pidcherga", 0)
                                    )
                                )
                            }
                        }
                    }

                    if (addr == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val isSaved = viewModel.isInspectedAddressSaved()
                        val schedulesRetrieved = uiState.inspectedScheduleList
                        val groupedSchedulesRetrieved = uiState.inspectedGroupedSchedule
                        val currentAddr = addr  // Capture non-null addr in this scope
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { 
                                        Text(
                                            text = currentAddr.name, 
                                            style = MaterialTheme.typography.titleLarge,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        ) 
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = { finish() }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                        }
                                    },
                                    actions = {
                                        if (!isSaved) {
                                            IconButton(onClick = { viewModel.saveInspectedAddress() }) {
                                                Icon(Icons.Default.BookmarkBorder, "Зберегти адресу")
                                            }
                                        } else {
                                            Icon(Icons.Default.Bookmark, "Збережено", modifier = Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        ) { padding ->
                            HomeTab(
                                remName = currentAddr.remName,
                                cityName = currentAddr.cityName,
                                streetName = currentAddr.streetName,
                                addressName = currentAddr.addressName,
                                cherga = currentAddr.cherga,
                                pidcherga = currentAddr.pidcherga,
                                currentStatus = null,
                                schedules = schedulesRetrieved,
                                groupedSchedule = groupedSchedulesRetrieved,
                                onRefresh = { viewModel.startInspectingAddress(currentAddr) },
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = padding,
                                lastUpdateTime = "",
                                isOffline = false,
                                isLoading = uiState.isInspectingLoading,
                                streetId = currentAddr.streetId,
                                addressId = currentAddr.addressId
                            )
                        }
                    }
                }
            }
        }
    }
}
