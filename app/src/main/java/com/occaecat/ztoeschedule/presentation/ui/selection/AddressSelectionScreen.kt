package com.occaecat.ztoeschedule.presentation.ui.selection

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber
import com.occaecat.ztoeschedule.presentation.ui.*

/**
 * Экран выбора адреса/будинку (Шаг 4)
 */
@Composable
fun AddressSelectionScreen(
    houseNumbers: List<ParsedHouseNumber>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onHouseNumberSelected: (ParsedHouseNumber) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Stepper
        SelectionStepper(
            currentStep = 4,
            steps = listOf("РЕМ", "Населений пункт", "Вулиця", "Будинок")
        )

        HorizontalDivider()

        // Заголовок
        StepHeader(
            title = "Оберіть будинок",
            subtitle = "Крок 4 з 4",
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
        )

        // Поиск
        SearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = "Пошук будинку..."
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Grid адресов
        if (houseNumbers.isEmpty()) {
            EmptyState(message = "Нічого не знайдено")
        } else {
            AddressGridScreen(
                houseNumbers = houseNumbers,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onClearSearch = onClearSearch,
                onHouseNumberSelected = onHouseNumberSelected,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

