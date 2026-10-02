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
import com.occaecat.ztoeschedule.presentation.ui.components.StepGutter
import com.occaecat.ztoeschedule.presentation.ui.components.StepLeadingIcon
import com.occaecat.ztoeschedule.presentation.ui.components.StepListHeader
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width

/**
 * Shared layout for the REM / city / street steps: header, search, grouped list.
 */
@Composable
private fun <T> SearchableSelectionPage(
    title: String,
    subtitle: String?,
    searchHint: String,
    items: List<T>,
    isLoading: Boolean,
    key: (T) -> Any,
    label: (T) -> String,
    icon: ImageVector,
    onSelected: (T) -> Unit
) {
    var query by remember { mutableStateOf("") }
    // The API can return the same entry twice; duplicate keys crash LazyColumn
    val uniqueItems = remember(items) { items.distinctBy(key) }
    val filteredList = remember(query, uniqueItems) {
        if (query.isBlank()) uniqueItems else uniqueItems.filter { label(it).contains(query, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StepListHeader(title = title, subtitle = subtitle)
        SearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = searchHint
        )

        when {
            isLoading && items.isEmpty() -> ListSkeleton()
            filteredList.isEmpty() && query.isNotBlank() -> EmptyListMessage(stringResource(R.string.house_not_found))
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = StepGutter),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(filteredList, key = { _, item -> key(item) }) { index, item ->
                    SettingsGroupItem(
                        index = index,
                        totalCount = filteredList.size,
                        headlineContent = { Text(label(item)) },
                        leadingContent = { StepLeadingIcon(icon) },
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onSelected(item) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
fun RemSelectionPage(
    rems: List<Rem>,
    isLoading: Boolean,
    onRemSelected: (Rem) -> Unit,
    subtitle: String? = null
) {
    SearchableSelectionPage(
        title = "Оберіть район",
        subtitle = subtitle,
        searchHint = stringResource(R.string.search_rem_hint),
        items = rems,
        isLoading = isLoading,
        key = { it.id },
        label = { it.name },
        icon = Icons.Default.Business,
        onSelected = onRemSelected
    )
}

@Composable
fun CitySelectionPage(
    cities: List<City>,
    isLoading: Boolean,
    onCitySelected: (City) -> Unit,
    subtitle: String? = null
) {
    SearchableSelectionPage(
        title = "Оберіть населений пункт",
        subtitle = subtitle,
        searchHint = stringResource(R.string.search_city_hint),
        items = cities,
        isLoading = isLoading,
        key = { it.id },
        label = { it.name },
        icon = Icons.Default.LocationCity,
        onSelected = onCitySelected
    )
}

@Composable
fun StreetSelectionPage(
    streets: List<Street>,
    isLoading: Boolean,
    onStreetSelected: (Street) -> Unit,
    subtitle: String? = null
) {
    SearchableSelectionPage(
        title = "Оберіть вулицю",
        subtitle = subtitle,
        searchHint = stringResource(R.string.search_street_hint),
        items = streets,
        isLoading = isLoading,
        key = { it.id },
        label = { it.name },
        icon = Icons.Default.Signpost,
        onSelected = onStreetSelected
    )
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
    onHouseSelected: (ParsedHouseNumber) -> Unit,
    subtitle: String? = null
) {
    // Several house numbers can share an address id (and repeat), so de-duplicate by the
    // full key: a duplicate key in LazyColumn throws and crashed the add-address flow
    val uniqueHouses = remember(houseNumbers) { houseNumbers.distinctBy { houseKey(it) } }
    Column(modifier = Modifier.fillMaxSize()) {
        StepListHeader(title = "Оберіть будинок", subtitle = subtitle)
        SearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = stringResource(R.string.search_house_hint)
        )
        
        CategoryFilterRow(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (isLoading && uniqueHouses.isEmpty()) {
            ListSkeleton()
        } else if (uniqueHouses.isEmpty()) {
            EmptyListMessage(
                if (searchQuery.isEmpty() && selectedCategory == null) stringResource(R.string.house_empty) else stringResource(R.string.house_not_found)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = StepGutter),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(uniqueHouses, key = { _, item -> houseKey(item) }) { index, house ->
                    SettingsGroupItem(
                        index = index,
                        totalCount = uniqueHouses.size,
                        headlineContent = { Text(house.houseNumber, fontWeight = FontWeight.SemiBold) },
                        supportingContent = if (selectedCategory == null) {
                            { Text(house.category.label) }
                        } else null,
                        leadingContent = { StepLeadingIcon(Icons.Default.Home) },
                        trailingContent = if (house.cherga > 0) {
                            { QueueBadge(cherga = house.cherga, pidcherga = house.pidcherga) }
                        } else null,
                        onClick = { onHouseSelected(house) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

private fun houseKey(house: ParsedHouseNumber): String =
    "${house.originalAddressId}_${house.houseNumber}_${house.cherga}_${house.pidcherga}_${house.category}"

/** Queue number shown at the end of a house row. */
@Composable
private fun QueueBadge(cherga: Int, pidcherga: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = CircleShape
    ) {
        Text(
            text = "$cherga.$pidcherga",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun EmptyListMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(StepGutter * 2),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun ListSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = StepGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(8) { index ->
            SettingsGroupItem(
                index = index,
                totalCount = 8,
                headlineContent = { ShimmerItem(height = 16.dp, modifier = Modifier.width(120.dp)) },
                leadingContent = { ShimmerItem(height = 40.dp, modifier = Modifier.width(40.dp), shape = CircleShape) },
                onClick = {} // No-op
            )
        }
    }
}
