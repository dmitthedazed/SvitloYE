package com.occaecat.ztoeschedule.presentation.ui.addresses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class AddressIcon(
    val name: String,
    val icon: ImageVector,
    val label: String
)

val availableIcons = listOf(
    AddressIcon("home", Icons.Default.Home, "Дім"),
    AddressIcon("apartment", Icons.Default.Apartment, "Квартира"),
    AddressIcon("work", Icons.Default.Work, "Робота"),
    AddressIcon("school", Icons.Default.School, "Школа"),
    AddressIcon("location", Icons.Default.LocationOn, "Інше"),
    AddressIcon("star", Icons.Default.Star, "Улюблене")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressCustomizationScreen(
    onComplete: (name: String, iconName: String) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf("home") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Налаштування локації") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Скасувати")
                    }
                    Button(
                        onClick = {
                            val finalName = if (name.isBlank()) {
                                availableIcons.find { it.name == selectedIconName }?.label ?: "Локація"
                            } else name
                            onComplete(finalName, selectedIconName)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Зберегти")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Назва (напр. Додому, Офіс)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Edit, null) }
            )

            Text(
                text = "Виберіть іконку",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                availableIcons.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { item ->
                            val isSelected = selectedIconName == item.name
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedIconName = item.name },
                                label = { Text(item.label) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.large
                            )
                        }
                    }
                }
            }
        }
    }
}
