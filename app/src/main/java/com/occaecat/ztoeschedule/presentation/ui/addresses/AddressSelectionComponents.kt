package com.occaecat.ztoeschedule.presentation.ui.addresses

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.occaecat.ztoeschedule.presentation.ui.CategoryFilterRow
import com.occaecat.ztoeschedule.data.repository.ConsumerCategory
import androidx.compose.ui.res.stringResource
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.presentation.ui.components.SettingsGroupItem
import com.occaecat.ztoeschedule.presentation.ui.components.ShimmerItem
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width

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

        if (isLoading && rems.isEmpty()) {
            ListSkeleton()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .animateContentSize(spring())
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(filteredList, key = { _, item -> item.id }) { index, rem ->
                    SettingsGroupItem(
                        index = index,
                        totalCount = filteredList.size,
                        headlineContent = { Text(rem.name) },
                        leadingContent = { Icon(Icons.Default.Business, null) },
                        onClick = { onRemSelected(rem) }
                    )
                }
            }
        }
        
        if (!isLoading && query.isEmpty()) {
            Text(
                "Оберіть ваш район зі списку або скористайтесь пошуком",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

        if (isLoading && cities.isEmpty()) {
            ListSkeleton()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .animateContentSize(spring())
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(filteredList, key = { _, item -> item.id }) { index, city ->
                    SettingsGroupItem(
                        index = index,
                        totalCount = filteredList.size,
                        headlineContent = { Text(city.name) },
                        leadingContent = { Icon(Icons.Default.LocationCity, null) },
                        onClick = { onCitySelected(city) }
                    )
                }
            }
        }

        if (!isLoading && query.isEmpty()) {
            Text(
                "Оберіть ваше місто або населений пункт",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

        if (isLoading && streets.isEmpty()) {
            ListSkeleton()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .animateContentSize(spring())
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(filteredList, key = { _, item -> item.id }) { index, street ->
                    SettingsGroupItem(
                        index = index,
                        totalCount = filteredList.size,
                        headlineContent = { Text(street.name) },
                        leadingContent = { Icon(Icons.Default.Signpost, null) },
                        onClick = { onStreetSelected(street) }
                    )
                }
            }
        }

        if (!isLoading && query.isEmpty()) {
            Text(
                "Почніть вводити назву вулиці для швидкого пошуку",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
fun HouseNumberSelectionPage(
    houseNumbers: List<ParsedHouseNumber>,
    searchQuery: String,
    isLoading: Boolean,
    selectedCategory: ConsumerCategory? = null,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (ConsumerCategory?) -> Unit = {},
    onClearSearch: () -> Unit,
    onHouseSelected: (ParsedHouseNumber) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        SearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = stringResource(R.string.search_house_hint)
        )
        
        CategoryFilterRow(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )

        if (isLoading && houseNumbers.isEmpty()) {
            ListSkeleton()
        } else if (houseNumbers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isEmpty() && selectedCategory == null) stringResource(R.string.house_empty) else stringResource(R.string.house_not_found),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .animateContentSize(spring())
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(houseNumbers, key = { _, item -> "${item.originalAddressId}_${item.houseNumber}" }) { index, house ->
                    SettingsGroupItem(
                        index = index,
                        totalCount = houseNumbers.size,
                        headlineContent = { Text(house.houseNumber) },
                        supportingContent = {
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                if (selectedCategory == null) {
                                    ReadOnlyChip(text = house.category.label)
                                }
                                ReadOnlyChip(text = "Черга: ${house.cherga}.${house.pidcherga}")
                            }
                        },
                        leadingContent = { Icon(Icons.Default.Home, null) },
                        onClick = { onHouseSelected(house) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ListSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(8) { index ->
            SettingsGroupItem(
                index = index,
                totalCount = 8,
                headlineContent = { ShimmerItem(height = 16.dp, modifier = Modifier.width(120.dp)) },
                leadingContent = { ShimmerItem(height = 24.dp, modifier = Modifier.width(24.dp), shape = CircleShape) },
                onClick = {} // No-op
            )
        }
    }
}
