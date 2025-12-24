package com.occaecat.ztoeschedule.presentation.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModelFactory

/**
 * Example usage screen demonstrating how to use the EnergyScheduleViewModel
 *
 * This composable shows how to:
 * 1. Initialize the ViewModel with factory
 * 2. Observe UI state
 * 3. Handle loading and error states
 * 4. Display schedule data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExampleScheduleScreen() {
    val context = LocalContext.current

    // Initialize ViewModel with factory
    val viewModel: EnergyScheduleViewModel = viewModel(
        factory = EnergyScheduleViewModelFactory(context)
    )

    // Collect UI state
    val uiState by viewModel.uiState.collectAsState()

    // Example: Load initial data on first composition
    LaunchedEffect(Unit) {
        viewModel.loadRemList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ZTOE Schedule") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                uiState.isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    // Error state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                }

                else -> {
                    // Content state
                    ScheduleContent(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleContent(
    viewModel: EnergyScheduleViewModel,
    uiState: com.occaecat.ztoeschedule.presentation.viewmodel.UiState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Current Status Card
        uiState.currentStatus?.let { schedule ->
            item {
                CurrentStatusCard(schedule)
            }
        }

        // Info Messages
        if (uiState.infoMessages.isNotEmpty()) {
            item {
                Text(
                    text = "Information",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(uiState.infoMessages) { message ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Schedule List
        if (uiState.scheduleList.isNotEmpty()) {
            item {
                Text(
                    text = "Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(uiState.scheduleList) { schedule ->
                ScheduleCard(schedule)
            }
        }

        // Example: Selection chain
        if (uiState.remList.isNotEmpty()) {
            item {
                Text(
                    text = "Available REMs",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(uiState.remList) { rem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.loadCityList(rem.id) }
                ) {
                    Text(
                        text = rem.name,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentStatusCard(
    schedule: com.occaecat.ztoeschedule.data.model.Schedule
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Current Status",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = schedule.displayText ?: "Невідомо",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Time: ${schedule.span ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Date: ${schedule.date ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: com.occaecat.ztoeschedule.data.model.Schedule
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.displayText ?: "Невідомо",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = schedule.date ?: "N/A",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = schedule.span ?: "N/A",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Example: How to load schedule for specific address
 *
 * Usage in your app:
 * ```
 * // After user selects an address
 * val selectedAddress = addressList.first()
 * viewModel.loadScheduleWithMessages(
 *     cherga = selectedAddress.cherga,
 *     pidcherga = selectedAddress.pidcherga
 * )
 * ```
 */
@Composable
fun ExampleLoadScheduleButton(
    viewModel: EnergyScheduleViewModel,
    cherga: Int,
    pidcherga: Int
) {
    Button(
        onClick = {
            viewModel.loadScheduleWithMessages(cherga, pidcherga)
        }
    ) {
        Text("Load Schedule")
    }
}

