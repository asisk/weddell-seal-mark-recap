package weddellseal.markrecap.ui.screens

import android.Manifest
import android.content.Context
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import weddellseal.markrecap.models.AddObservationLogViewModel
import weddellseal.markrecap.models.HomeViewModel
import weddellseal.markrecap.ui.components.CensusDialog
import weddellseal.markrecap.ui.components.DropdownField
import weddellseal.markrecap.ui.components.MultiSelectDropdownObservers
import weddellseal.markrecap.ui.permissions.RequestPermissions
import weddellseal.markrecap.ui.permissions.missingPermissions
import weddellseal.markrecap.ui.utils.cancelAllAndClear

@Composable
fun HomeScreen(
    navController: NavHostController,
    obsViewModel: AddObservationLogViewModel,
    viewModel: HomeViewModel
) {
    HomeScaffold(navController, obsViewModel, viewModel)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScaffold(
    navController: NavHostController,
    obsViewModel: AddObservationLogViewModel,
    viewModel: HomeViewModel
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showCensusDialog by remember { mutableStateOf(false) }
    val coloniesList by viewModel.coloniesList.collectAsState()
    val currentColony by viewModel.autoDetectedColony.collectAsState()
    val gpsCoordinates by viewModel.coordinates.collectAsState()
    // Collect the overrideAutoColony state
    val overrideAutoColony by viewModel.overrideAutoColony.collectAsState()

    // Used to request permissions for Location
    RequestPermissionsEffect(viewModel)

    DisposableEffect(Unit) {
        onDispose {
            viewModel.jobs.cancelAllAndClear()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchColonyNamesList()
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
                            val observerSelected by remember { mutableStateOf(obsViewModel.uiState.observerInitials) }
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
                                MultiSelectDropdownObservers(
                                    viewModel,
                                    selectedOptions = observerSelected,
                                    onValueChange = { updatedItems ->
                                        // Convert selected items back to a concatenated string
                                        val concatenatedSelectedItems = updatedItems
                                            .joinToString(separator = ", ")

                                        obsViewModel.updateObserverInitials(
                                            concatenatedSelectedItems
                                        )
                                    }
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
                                Text(currentColony?.location.toString())
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val colonySelected by remember { mutableStateOf(obsViewModel.uiState.colonyLocation) }

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
                                        .fillMaxWidth(.8f)
                                ) {
                                    DropdownField(
                                        coloniesList,
                                        colonySelected
                                    ) { valueSelected ->
                                        obsViewModel.updateColonySelection(valueSelected)
                                    }
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
                            obsViewModel,
                            onClearRequest = {
                                showCensusDialog = false
                                obsViewModel.clearCensus()
                            },
                            onConfirmation = {
                                showCensusDialog = false
                                obsViewModel.updateIsObservationMode(true)
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

fun getDeviceName(context: Context): String {
    return Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        ?: "Unknown Device"
}

//
//@Composable
//fun CardWithClickableImages() {
//    var clickedImage by remember { mutableStateOf(0) }
//
//    Card(
//        modifier = Modifier
//            .padding(16.dp)
//    ) {
//        Column(
//            modifier = Modifier.fillMaxSize(),
//            verticalArrangement = Arrangement.Center
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                ClickableImage(imageResId = R.drawable.pup1_2, onClick = { clickedImage = 1 })
//                ClickableImage(imageResId = R.drawable.pup1_2, onClick = { clickedImage = 2 })
//                ClickableImage(imageResId = R.drawable.pup1_2, onClick = { clickedImage = 3 })
//            }
//
//            // Optionally, display some content based on the clickedImage value
//            when (clickedImage) {
//                1 -> Text("You clicked Image 1")
//                2 -> Text("You clicked Image 2")
//                3 -> Text("You clicked Image 3")
//            }
//        }
//    }
//}
//
//@Composable
//fun ClickableImage(imageResId: Int, onClick: () -> Unit) {
//    Image(
//        painter = painterResource(id = imageResId),
//        contentDescription = null, // Provide a proper content description
//        modifier = Modifier
//            .clickable { onClick() }
//            .padding(8.dp)
//    )
//}