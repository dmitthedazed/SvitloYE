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
                        .windowInsetsPadding(WindowInsets.safeDrawing), // Handle gesture nav
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

            // Grid needs fixed height inside scrollable column? No, FlowRow or similar is better for responsiveness.
            // But LazyVerticalGrid doesn't nest well in Column.
            // Let's use a FlowRow equivalent or a custom grid layout loop since items are few (6).
            // Or just calculate rows manually.
            
            // Simple grid layout using Rows
            val rows = availableIcons.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowItems.forEach { item ->
                            val isSelected = selectedIconName == item.name
                            AddressIconItem(
                                item = item,
                                isSelected = isSelected,
                                onClick = { selectedIconName = item.name },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill empty space if row is incomplete
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressIconItem(
    item: AddressIcon,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(64.dp) // Slightly larger touch target
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
