package com.occaecat.ztoeschedule.presentation.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.data.model.City
import com.occaecat.ztoeschedule.data.model.Rem
import com.occaecat.ztoeschedule.data.model.Street
import com.occaecat.ztoeschedule.data.repository.ConsumerCategory
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber

/**
 * Dialog-based address selection for improved onboarding flow
 * Replaces HorizontalPager approach with step-by-step selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSelectionDialog(
    remList: List<Rem>,
    cityList: List<City>,
    streetList: List<Street>,
    houseNumbers: List<ParsedHouseNumber>,
    searchQuery: String,
    isLoading: Boolean,
    selectedCategory: ConsumerCategory?,
    selectedRem: Rem?,
    selectedCity: City?,
    selectedStreet: Street?,
    selectedHouse: ParsedHouseNumber?,
    onLoadRem: () -> Unit,
    onLoadCity: (String) -> Unit,
    onLoadStreet: (String) -> Unit,
    onLoadAddress: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (ConsumerCategory?) -> Unit,
    onClearSearch: () -> Unit,
    onRemSelected: (Rem) -> Unit,
    onCitySelected: (City) -> Unit,
    onStreetSelected: (Street) -> Unit,
    onHouseSelected: (ParsedHouseNumber) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedRem, selectedCity, selectedStreet, selectedHouse) {
        step = when {
            selectedRem == null -> 0
            selectedCity == null -> 1
            selectedStreet == null -> 2
            else -> 3
        }
    }

    LaunchedEffect(selectedRem) {
        if (selectedRem != null && cityList.isEmpty()) onLoadCity(selectedRem.id)
    }

    LaunchedEffect(selectedCity) {
        if (selectedCity != null && streetList.isEmpty()) onLoadStreet(selectedCity.id)
    }

    LaunchedEffect(selectedStreet) {
        if (selectedStreet != null && houseNumbers.isEmpty()) onLoadAddress(selectedStreet.id)
    }

    val steps = listOf("Регіон", "Місто", "Вулиця", "Будинок")

    Box(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.90f)
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.extraLarge)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Вибір адреси",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                steps[step],
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        if (step > 0) {
                            IconButton(onClick = { step-- }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Закрити")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Progress
                LinearProgressIndicator(
                    progress = { (step + 1).toFloat() / steps.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )

                // Content
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                ) {
                    when (step) {
                        0 -> RemSelectionContent(
                            remList = remList,
                            isLoading = isLoading,
                            onLoadRem = onLoadRem,
                            onRemSelected = { rem ->
                                onRemSelected(rem)
                                onLoadCity(rem.id)
                                step = 1
                            },
                            selectedRem = selectedRem
                        )
                        1 -> CitySelectionContent(
                            cityList = cityList,
                            isLoading = isLoading,
                            remName = selectedRem?.name ?: "",
                            selectedCity = selectedCity,
                            onCitySelected = { city ->
                                onCitySelected(city)
                                onLoadStreet(city.id)
                                step = 2
                            }
                        )
                        2 -> StreetSelectionContent(
                            streetList = streetList,
                            isLoading = isLoading,
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            onClearSearch = onClearSearch,
                            onStreetSelected = { street ->
                                onStreetSelected(street)
                                onLoadAddress(street.id)
                                step = 3
                            },
                            selectedStreet = selectedStreet
                        )
                        3 -> HouseSelectionContent(
                            houseNumbers = houseNumbers,
                            isLoading = isLoading,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            onSearchQueryChange = onSearchQueryChange,
                            onCategorySelected = onCategorySelected,
                            onHouseSelected = { house ->
                                onHouseSelected(house)
                                onDismiss()
                            },
                            selectedHouse = selectedHouse
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RemSelectionContent(
    remList: List<Rem>,
    isLoading: Boolean,
    onLoadRem: () -> Unit,
    onRemSelected: (Rem) -> Unit,
    selectedRem: Rem?,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        if (remList.isEmpty()) {
            onLoadRem()
        }
    }

    var query by remember { mutableStateOf("") }
    val filteredList = remember(query, remList) {
        if (query.isBlank()) remList else remList.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Пошук РЕМ...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, "Очистити")
                    }
                }
            },
            singleLine = true
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { rem ->
                    RemSelectionItem(
                        rem = rem,
                        onClick = { onRemSelected(rem) },
                        selected = selectedRem?.id == rem.id
                    )
                }
            }
        }
    }
}

@Composable
fun CitySelectionContent(
    cityList: List<City>,
    isLoading: Boolean,
    remName: String,
    selectedCity: City?,
    onCitySelected: (City) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val filteredList = remember(query, cityList) {
        if (query.isBlank()) cityList else cityList.filter { it.name.contains(query, ignoreCase = true) }
    }

    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            Text(
                "РЕМ: $remName",
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Пошук міста...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, "Очистити")
                        }
                    }
                },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { city ->
                    CitySelectionItem(
                        city = city,
                        onClick = { onCitySelected(city) },
                        selected = selectedCity?.id == city.id
                    )
                }
            }
        }
    }
}

@Composable
fun StreetSelectionContent(
    streetList: List<Street>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onStreetSelected: (Street) -> Unit,
    selectedStreet: Street?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Пошук вулиці...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Default.Close, "Очистити")
                    }
                }
            },
            singleLine = true
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(streetList) { street ->
                    StreetSelectionItem(
                        street = street,
                        onClick = { onStreetSelected(street) },
                        selected = selectedStreet?.id == street.id
                    )
                }
            }
        }
    }
}

@Composable
fun HouseSelectionContent(
    houseNumbers: List<ParsedHouseNumber>,
    isLoading: Boolean,
    searchQuery: String,
    selectedCategory: ConsumerCategory?,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (ConsumerCategory?) -> Unit,
    onHouseSelected: (ParsedHouseNumber) -> Unit,
    selectedHouse: ParsedHouseNumber?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Category filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConsumerCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        onCategorySelected(if (selectedCategory == category) null else category)
                    },
                    label = { Text(category.label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Номер будинку...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, "Очистити пошук")
                    }
                }
            }
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (houseNumbers.isEmpty() && searchQuery.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Немає результатів для \"$searchQuery\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (houseNumbers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Немає доступних будинків", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(houseNumbers) { house ->
                    HouseSelectionItem(
                        house = house,
                        onClick = { onHouseSelected(house) },
                        selected = selectedHouse?.originalAddressId == house.originalAddressId
                    )
                }
            }
        }
    }
}

// Selection Items
@Composable
fun RemSelectionItem(
    rem: Rem,
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                rem.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CitySelectionItem(
    city: City,
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                city.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StreetSelectionItem(
    street: Street,
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                street.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun HouseSelectionItem(
    house: ParsedHouseNumber,
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: house number
            Text(
                house.houseNumber,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(0.4f)
            )
            
            // Right side: chips
            Row(
                modifier = Modifier
                    .weight(0.6f)
                    .padding(start = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category chip
                AssistChip(
                    onClick = onClick,
                    label = { 
                        Text(
                            if (house.category == ConsumerCategory.HOUSEHOLD) "Побутові" else "Юридичні",
                            style = MaterialTheme.typography.labelSmall
                        ) 
                    },
                    modifier = Modifier.height(28.dp)
                )
                
                // Cherga/Pidcherga chip
                AssistChip(
                    onClick = onClick,
                    label = { 
                        Text(
                            "${house.cherga}/${house.pidcherga}",
                            style = MaterialTheme.typography.labelSmall
                        ) 
                    },
                    modifier = Modifier.height(28.dp)
                )
            }
        }
    }
}
