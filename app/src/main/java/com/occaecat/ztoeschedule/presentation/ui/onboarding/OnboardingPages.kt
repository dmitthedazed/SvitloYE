package com.occaecat.ztoeschedule.presentation.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.data.model.City
import com.occaecat.ztoeschedule.data.model.Rem
import com.occaecat.ztoeschedule.data.model.Street
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber
import com.occaecat.ztoeschedule.presentation.ui.SearchField
import com.occaecat.ztoeschedule.presentation.ui.SelectionListItem

import androidx.compose.ui.res.stringResource
import com.occaecat.ztoeschedule.R

import com.occaecat.ztoeschedule.presentation.ui.components.ShimmerItem

@Composable
fun RemSelectionPage(
    rems: List<Rem>,
    isLoading: Boolean,
    onRemSelected: (Rem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredList = remember(query, rems) {
        if (query.isBlank()) rems else rems.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        SearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.search_rem_hint)
        )

        if (isLoading) {
            ListSkeleton()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(filteredList) { rem ->
                    SelectionListItem(
                        title = rem.name,
                        onClick = { onRemSelected(rem) }
                    )
                }
            }
        }
    }
}

@Composable
fun CitySelectionPage(
    cities: List<City>,
    isLoading: Boolean,
    onCitySelected: (City) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredList = remember(query, cities) {
        if (query.isBlank()) cities else cities.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        SearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.search_city_hint)
        )

        if (isLoading) {
            ListSkeleton()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(filteredList) { city ->
                    SelectionListItem(
                        title = city.name,
                        onClick = { onCitySelected(city) }
                    )
                }
            }
        }
    }
}

@Composable
fun StreetSelectionPage(
    streets: List<Street>,
    isLoading: Boolean,
    onStreetSelected: (Street) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredList = remember(query, streets) {
        if (query.isBlank()) streets else streets.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        SearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.search_street_hint)
        )

        if (isLoading) {
            ListSkeleton()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(filteredList) { street ->
                    SelectionListItem(
                        title = street.name,
                        onClick = { onStreetSelected(street) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseNumberSelectionPage(
    houseNumbers: List<ParsedHouseNumber>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onHouseSelected: (ParsedHouseNumber) -> Unit
) {
    val filteredList = remember(searchQuery, houseNumbers) {
        if (searchQuery.isBlank()) houseNumbers else houseNumbers.filter {
            it.houseNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        SearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = stringResource(R.string.search_house_hint)
        )

        if (isLoading) {
            GridSkeleton()
        } else if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isEmpty()) stringResource(R.string.house_empty) else stringResource(R.string.house_not_found),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList) { house ->
                    OutlinedCard(
                        onClick = { onHouseSelected(house) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = house.houseNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(8) {
            ShimmerItem(height = 56.dp, modifier = Modifier.padding(horizontal = 16.dp), shape = MaterialTheme.shapes.medium)
        }
    }
}

@Composable
private fun GridSkeleton() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    ShimmerItem(height = 60.dp, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large)
                }
            }
        }
    }
}