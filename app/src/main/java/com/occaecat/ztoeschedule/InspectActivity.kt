package com.occaecat.ztoeschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.presentation.ui.home.HomeTab
import com.occaecat.ztoeschedule.presentation.viewmodel.EnergyScheduleViewModel
import com.occaecat.ztoeschedule.ui.theme.SvitloYeZhytomyrTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.occaecat.ztoeschedule.ui.theme.robotoFlexTopBar
import androidx.compose.ui.input.nestedscroll.nestedScroll

@AndroidEntryPoint
class InspectActivity : ComponentActivity() {
    
    @Inject lateinit var preferencesManager: EnergyPreferencesManager
    
    @OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: EnergyScheduleViewModel = hiltViewModel()
            val uiStateValue = viewModel.uiState.collectAsState()
            val uiState = uiStateValue.value
            
            var showSaveDialog by remember { mutableStateOf(false) }
            
            val colorThemeState = preferencesManager.colorThemeFlow.collectAsState(initial = ColorTheme.System)
            val colorTheme = colorThemeState.value
            val cornerRadiusState = preferencesManager.cornerRadiusFlow.collectAsState(initial = 24)
            val cornerRadius = cornerRadiusState.value
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
            val motionScheme = MaterialTheme.motionScheme

            SvitloYeZhytomyrTheme(
                themePreference = colorTheme,
                cornerRadius = cornerRadius
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val addr = uiState.inspectedAddress
                    
                    // Load address if not yet loaded
                    LaunchedEffect(Unit) {
                        if (addr == null) {
                            // Try to get address from intent extras first
                            val remId = intent.getStringExtra("remId")
                            val streetId = intent.getStringExtra("streetId")
                            val addressId = intent.getStringExtra("addressId")
                            val name = intent.getStringExtra("name")
                            val houseName = intent.getStringExtra("houseName")
                            if (!remId.isNullOrBlank() && !streetId.isNullOrBlank() && !addressId.isNullOrBlank() && !name.isNullOrBlank()) {
                                viewModel.startInspectingAddress(
                                    com.occaecat.ztoeschedule.data.model.SavedAddress(
                                        id = addressId,
                                        name = name,
                                        iconName = intent.getStringExtra("iconName") ?: "other",
                                        priority = intent.getIntExtra("priority", 1),
                                        remId = remId,
                                        remName = intent.getStringExtra("remName") ?: "",
                                        cityId = intent.getStringExtra("cityId") ?: "",
                                        cityName = intent.getStringExtra("cityName") ?: "",
                                        streetId = streetId,
                                        streetName = intent.getStringExtra("streetName") ?: "",
                                        addressId = addressId,
                                        addressName = intent.getStringExtra("addressName") ?: "",
                                        cherga = intent.getIntExtra("cherga", 0),
                                        pidcherga = intent.getIntExtra("pidcherga", 0)
                                    )
                                )
                            } else if (!streetId.isNullOrBlank() && !addressId.isNullOrBlank()) {
                                viewModel.inspectAddressByIds(
                                    streetId = streetId,
                                    houseId = addressId,
                                    houseName = houseName?.takeIf { it.isNotBlank() }
                                )
                            }
                        }
                    }

                    if (addr == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val isSaved = viewModel.isInspectedAddressSaved()
                        val schedulesRetrieved = uiState.inspectedScheduleList
                        val groupedSchedulesRetrieved = uiState.inspectedGroupedSchedule
                        val currentAddr = addr  // Capture non-null addr in this scope
                        
                        if (showSaveDialog) {
                            SaveAddressDialog(
                                initialName = currentAddr.name,
                                initialIcon = currentAddr.iconName,
                                onDismiss = { showSaveDialog = false },
                                onSave = { name, icon ->
                                    viewModel.saveInspectedAddress(name, icon)
                                    showSaveDialog = false
                                }
                            )
                        }

                        Scaffold(
                            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                            topBar = {
                                CenterAlignedTopAppBar(
                                    title = { 
                                        AnimatedContent(
                                            targetState = currentAddr.name,
                                            transitionSpec = {
                                                slideInVertically(
                                                    animationSpec = motionScheme.defaultSpatialSpec(),
                                                    initialOffsetY = { (-it * 1.25).toInt() }
                                                ).togetherWith(
                                                    slideOutVertically(
                                                        animationSpec = motionScheme.defaultSpatialSpec(),
                                                        targetOffsetY = { (it * 1.25).toInt() }
                                                    )
                                                )
                                            },
                                            label = "title_animation",
                                            modifier = Modifier.fillMaxWidth(0.9f),
                                            contentAlignment = Alignment.CenterStart
                                        ) { title ->
                                            Text(
                                                text = title, 
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontSize = 32.sp,
                                                    lineHeight = 32.sp,
                                                    fontFamily = robotoFlexTopBar
                                                ),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Start
                                            )
                                        }
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = { finish() }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                        }
                                    },
                                    actions = {
                                        if (!isSaved) {
                                            IconButton(onClick = { showSaveDialog = true }) {
                                                Icon(Icons.Default.BookmarkBorder, stringResource(R.string.action_save_address))
                                            }
                                        } else {
                                            Icon(Icons.Default.Bookmark, stringResource(R.string.action_saved), modifier = Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = Color.Transparent,
                                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                    scrollBehavior = scrollBehavior
                                )
                            }
                        ) { padding ->
                            HomeTab(
                                remId = currentAddr.remId,
                                cityId = currentAddr.cityId,
                                remName = currentAddr.remName,
                                cityName = currentAddr.cityName,
                                streetName = currentAddr.streetName,
                                addressName = currentAddr.addressName.ifBlank { currentAddr.name },
                                cherga = currentAddr.cherga,
                                pidcherga = currentAddr.pidcherga,
                                currentStatus = null,
                                schedules = schedulesRetrieved,
                                groupedSchedule = groupedSchedulesRetrieved,
                                onRefresh = { viewModel.startInspectingAddress(currentAddr) },
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = padding,
                                lastUpdateTime = "",
                                isOffline = false,
                                isLoading = uiState.isInspectingLoading,
                                streetId = currentAddr.streetId,
                                addressId = currentAddr.addressId
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaveAddressDialog(
    initialName: String,
    initialIcon: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var icon by remember { mutableStateOf(initialIcon) }
    var userEditedName by remember { mutableStateOf(false) }

    val icons = listOf(
        Triple("home", "Дім", Icons.Default.Home),
        Triple("work", "Робота", Icons.Default.Work),
        Triple("person", "Рідні", Icons.Default.Person),
        Triple("favorite", "Улюблене", Icons.Default.Favorite),
        Triple("star", "Важливе", Icons.Default.Star),
        Triple("place", "Місце", Icons.Default.Place),
        Triple("store", "Магазин", Icons.Default.Store),
        Triple("school", "Школа", Icons.Default.School)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Зберегти адресу") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        userEditedName = true
                    },
                    label = { Text("Назва") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Оберіть іконку:", style = MaterialTheme.typography.labelLarge)
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    icons.forEach { (id, label, img) ->
                        val selected = icon == id
                        AssistChip(
                            onClick = { 
                                icon = id 
                                if (!userEditedName) name = label
                            },
                            label = { Text(label) },
                            leadingIcon = { Icon(img, null, Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, icon) },
                enabled = name.isNotBlank()
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}
