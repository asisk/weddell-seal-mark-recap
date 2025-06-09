package weddellseal.markrecap.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import weddellseal.markrecap.R
import weddellseal.markrecap.Screens
import weddellseal.markrecap.models.HomeViewModel
import weddellseal.markrecap.ui.permissions.RequestPermissions
import weddellseal.markrecap.ui.permissions.missingPermissions
import weddellseal.markrecap.ui.utils.cancelAllAndClear
import weddellseal.markrecap.ui.utils.getDeviceName

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel
) {
    HomeScaffold(navController, viewModel)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScaffold(
    navController: NavHostController,
    viewModel: HomeViewModel
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val uiState by viewModel.uiState.collectAsState()

    var showCensusDialog by remember { mutableStateOf(false) }
    val coloniesList by viewModel.coloniesList.collectAsState()
    val autoDetectedColony by viewModel.autoDetectedColony.collectAsState()
    val overrideAutoColony by viewModel.overrideAutoColony.collectAsState()
    val options by viewModel.observersList.collectAsState() // Collecting the list of observers

    // Used to request permissions for Location
    RequestPermissionsEffect(viewModel)

    DisposableEffect(Unit) {
        onDispose {
            viewModel.jobs.cancelAllAndClear()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Weddell Seal Mark Recap",
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 36.sp // Adjust this value as needed
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screens.RecentObservations.route) }) {
                        Icon(
                            imageVector = Icons.Filled.Dataset,
                            contentDescription = "Recent Observations",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            navController.navigate(Screens.AdminScreen.route)
                        },
                        enabled = true
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = "Admin",
                            modifier = Modifier.size(48.dp), // Change the size here
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(state = scrollState, enabled = true)
                .fillMaxSize(),
        ) {
            // Seal Pup Image
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Image(
                    painter = painterResource(R.drawable.pup1_2),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.5f // Adjust this value for desired transparency
                        }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(30.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ExtendedFloatingActionButton(
                            modifier = Modifier.padding(16.dp),
                            containerColor = Color.LightGray,
                            onClick = { navController.navigate(Screens.SealLookupScreen.route) },
                            icon = {
                                Icon(
                                    Icons.Filled.Search,
                                    "Search for SpeNo",
                                    Modifier.size(36.dp)
                                )
                            },
                            text = {
                                Text(
                                    "Seal Lookup",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        )
                        ExtendedFloatingActionButton(
                            modifier = Modifier.padding(16.dp),
                            containerColor = Color.LightGray,
                            onClick = { navController.navigate(Screens.AddObservationLog.route) },
                            icon = {
                                Icon(
                                    Icons.Filled.PostAdd,
                                    "Enter a new observation",
                                    Modifier.size(36.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Tag/Retag",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 30.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ExtendedFloatingActionButton(
                            modifier = Modifier.padding(16.dp),
                            containerColor = Color.LightGray,
                            onClick = {
                                showCensusDialog = true
                            },
                            icon = { Icon(Icons.Filled.Checklist, "Census", Modifier.size(36.dp)) },
                            text = {
                                Text(
                                    text = "Census",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        )
                    }
                    Card(
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 6.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(.8f)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth(.5f)
                            ) {
                                Text(
                                    text = "Observer Initials",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth(.8f)
                            ) {
                                ObserversDropDown(
                                    label = "Selected Observers",
                                    allOptions = options,
                                    selectedOptions = uiState.selectedObservers,
                                    onSelectionChanged = { updatedItems ->
                                        viewModel.updateObserversSelection(
                                            updatedItems
                                        )
                                    },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth(.5f)
                            ) {
                                Text(
                                    text = "Colony Detected",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth(.8f)
                            ) {
                                Text(
                                    text = autoDetectedColony?.location
                                        ?: "...detecting proximity to a known colony..."
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Colony",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .padding(4.dp)
                            )
                            Checkbox(
                                checked = overrideAutoColony,
                                onCheckedChange = {
                                    viewModel.updateOverrideAutoColony(it)
                                },
                                modifier = Modifier
                                    .padding(8.dp)
                            )
                            if (overrideAutoColony) {
                                Column(
                                    modifier = Modifier
                                        .padding(4.dp)
//                                        .fillMaxWidth(.8f)
                                        .wrapContentWidth()
                                ) {
                                    ColonyDropDown(
                                        label = "Selected Colony",
                                        options = coloniesList,
                                        selectedOption = uiState.selectedColony,
                                        onValueChange = { valueSelected ->
                                            viewModel.updateColonySelection(valueSelected)
                                        }
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var deviceName by remember { mutableStateOf("") }
                            deviceName = getDeviceName(context)
                            Column(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth(.5f)
                            ) {
                                Text(
                                    text = "Device Name",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth(.8f)
                            ) {
                                Text(
                                    text = deviceName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    // Show the Census dialog if showDialog is true
                    if (showCensusDialog) {
                        CensusDialog(
                            viewModel,
                            onClearRequest = {
                                showCensusDialog = false
                                viewModel.clearCensus()
                            },
                            onConfirmation = {
                                showCensusDialog = false
                                viewModel.updateIsCensusMode(true)
                                navController.navigate(Screens.AddObservationLog.route)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestPermissionsEffect(
    vm: HomeViewModel,
) {
    val missing = LocalContext.current.missingPermissions()
    if (missing.isEmpty()) {
        vm.onPermissionsResult(true)
        return
    }
    RequestPermissions(missing, vm::onPermissionsResult)
}