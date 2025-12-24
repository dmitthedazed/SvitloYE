package com.occaecat.ztoeschedule.presentation.ui.selection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.data.model.*
import com.occaecat.ztoeschedule.presentation.ui.*

/**
 * Экран выбора РЕМ (Шаг 1)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemSelectionScreen(
    rems: List<Rem>,
    onRemSelected: (Rem) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredRems = remember(rems, searchQuery) {
        if (searchQuery.isEmpty()) {
            rems
        } else {
            rems.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = { Text("Вибір РЕМ") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stepper
            SelectionStepper(
                currentStep = 1,
                steps = listOf("Район (РЕМ)", "Населений пункт", "Вулиця", "Будинок")
            )

            HorizontalDivider()

            // Заголовок с иконкой
            StepHeader(
                title = "Оберіть район (РЕМ)",
                subtitle = "Крок 1 з 4",
                icon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
            )

            // Поиск
            SearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Пошук..."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Список
            if (filteredRems.isEmpty()) {
                EmptyState(message = "Нічого не знайдено")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredRems) { rem ->
                        SelectionListItem(
                            title = rem.name,
                            onClick = { onRemSelected(rem) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Экран выбора города (Шаг 2)
 */
@Composable
fun CitySelectionScreen(
    cities: List<City>,
    onCitySelected: (City) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCities = remember(cities, searchQuery) {
        if (searchQuery.isEmpty()) {
            cities
        } else {
            cities.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Stepper
        SelectionStepper(
            currentStep = 2,
            steps = listOf("РЕМ", "Населений пункт", "Вулиця", "Будинок")
        )

        HorizontalDivider()

        // Заголовок
        StepHeader(
            title = "Оберіть населений пункт",
            subtitle = "Крок 2 з 4",
            icon = {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
        )

        // Поиск
        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Пошук..."
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Список
        if (filteredCities.isEmpty()) {
            EmptyState(message = "Нічого не знайдено")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCities) { city ->
                    SelectionListItem(
                        title = city.name,
                        onClick = { onCitySelected(city) }
                    )
                }
            }
        }
    }
}

/**
 * Экран выбора улицы (Шаг 3)
 */
@Composable
fun StreetSelectionScreen(
    streets: List<Street>,
    onStreetSelected: (Street) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredStreets = remember(streets, searchQuery) {
        if (searchQuery.isEmpty()) {
            streets
        } else {
            streets.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Stepper
        SelectionStepper(
            currentStep = 3,
            steps = listOf("РЕМ", "Населений пункт", "Вулиця", "Будинок")
        )

        HorizontalDivider()

        // Заголовок
        StepHeader(
            title = "Оберіть вулицю",
            subtitle = "Крок 3 з 4",
            icon = {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
        )

        // Поиск
        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Пошук вулиці..."
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Список
        if (filteredStreets.isEmpty()) {
            EmptyState(message = "Нічого не знайдено")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredStreets) { street ->
                    SelectionListItem(
                        title = street.name,
                        onClick = { onStreetSelected(street) }
                    )
                }
            }
        }
    }
}

