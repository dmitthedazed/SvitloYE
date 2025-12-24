package com.occaecat.ztoeschedule.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber

/**
 * Grid display for house numbers with search functionality
 * Uses Material 3 FilterChips for easy selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressGridScreen(
    houseNumbers: List<ParsedHouseNumber>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onHouseNumberSelected: (ParsedHouseNumber) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onClear = onClearSearch,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // Grid of house numbers
        if (houseNumbers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty())
                        "Не знайдено будинків"
                    else
                        "Немає доступних адрес",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(houseNumbers, key = { it.originalAddressId + it.houseNumber }) { house ->
                    HouseNumberChip(
                        houseNumber = house,
                        onClick = { onHouseNumberSelected(house) }
                    )
                }
            }
        }
    }
}

/**
 * Search bar component for filtering house numbers
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Пошук будинку...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Пошук")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Очистити")
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors()
    )
}

/**
 * Individual chip for a house number
 * Uses FilterChip for consistent Material 3 design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HouseNumberChip(
    houseNumber: ParsedHouseNumber,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = {
            Text(
                text = houseNumber.houseNumber,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        modifier = modifier.fillMaxWidth(),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

/**
 * Alternative: SuggestionChip implementation
 * Can be used instead of FilterChip for different visual style
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseNumberSuggestionChip(
    houseNumber: ParsedHouseNumber,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SuggestionChip(
        onClick = onClick,
        label = {
            Text(
                text = houseNumber.houseNumber,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        modifier = modifier,
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

