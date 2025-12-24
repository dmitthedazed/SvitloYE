package com.occaecat.ztoeschedule.presentation.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.occaecat.ztoeschedule.data.model.*
import com.occaecat.ztoeschedule.data.repository.ParsedHouseNumber
import kotlinx.coroutines.launch

/**
 * Onboarding flow with address selection
 * 5 steps: Welcome → REM → City → Street → House Number
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFlow(
    // Data lists
    remList: List<Rem>,
    cityList: List<City>,
    streetList: List<Street>,
    houseNumbers: List<ParsedHouseNumber>,
    searchQuery: String,
    isLoading: Boolean,
    
    // Callbacks
    onLoadRem: () -> Unit,
    onLoadCity: (String) -> Unit,
    onLoadStreet: (String) -> Unit,
    onLoadAddress: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onComplete: (
        remId: String?,
        remName: String?,
        cityId: String?,
        cityName: String?,
        streetId: String?,
        streetName: String?,
        addressId: String,
        addressName: String,
        cherga: Int,
        pidcherga: Int
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()
    
    var selectedRem by remember { mutableStateOf<Rem?>(null) }
    var selectedCity by remember { mutableStateOf<City?>(null) }
    var selectedStreet by remember { mutableStateOf<Street?>(null) }
    
    // Load initial data
    LaunchedEffect(Unit) {
        if (remList.isEmpty()) {
            onLoadRem()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (pagerState.currentPage) {
                            0 -> "Ласкаво просимо"
                            1 -> "Крок 1: Оберіть РЕМ"
                            2 -> "Крок 2: Оберіть місто"
                            3 -> "Крок 3: Оберіть вулицю"
                            4 -> "Крок 4: Оберіть будинок"
                            else -> ""
                        }
                    )
                },
                navigationIcon = {
                    if (pagerState.currentPage > 0) {
                        IconButton(onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (pagerState.currentPage + 1) / 5f },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false // Disable swipe, use buttons only
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> RemSelectionPage(
                        rems = remList,
                        isLoading = isLoading,
                        onRemSelected = { rem ->
                            selectedRem = rem
                            onLoadCity(rem.id)
                            scope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        }
                    )
                    2 -> CitySelectionPage(
                        cities = cityList,
                        isLoading = isLoading,
                        onCitySelected = { city ->
                            selectedCity = city
                            onLoadStreet(city.id)
                            scope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        }
                    )
                    3 -> StreetSelectionPage(
                        streets = streetList,
                        isLoading = isLoading,
                        onStreetSelected = { street ->
                            selectedStreet = street
                            onLoadAddress(street.id)
                            scope.launch {
                                pagerState.animateScrollToPage(4)
                            }
                        }
                    )
                    4 -> HouseNumberSelectionPage(
                        houseNumbers = houseNumbers,
                        searchQuery = searchQuery,
                        isLoading = isLoading,
                        onSearchQueryChange = onSearchQueryChange,
                        onClearSearch = onClearSearch,
                        onHouseSelected = { house ->
                            onComplete(
                                selectedRem?.id,
                                selectedRem?.name,
                                selectedCity?.id,
                                selectedCity?.name,
                                selectedStreet?.id,
                                selectedStreet?.name,
                                house.originalAddressId,
                                house.houseNumber,
                                house.cherga,
                                house.pidcherga
                            )
                        }
                    )
                }
            }

            // Navigation button (only on welcome page)
            if (pagerState.currentPage == 0) {
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Почати налаштування")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Ласкаво просимо до ZTOE Schedule!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Давайте налаштуємо вашу адресу для перегляду графіків відключень",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Це займе лише 4 кроки:",
                    fontWeight = FontWeight.SemiBold
                )
                Text("✓ Вибір РЕМ")
                Text("✓ Вибір міста")
                Text("✓ Вибір вулиці")
                Text("✓ Вибір будинку")
            }
        }
    }
}

