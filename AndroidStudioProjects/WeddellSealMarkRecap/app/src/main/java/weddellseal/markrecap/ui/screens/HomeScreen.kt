package weddellseal.markrecap.ui.screens

import android.Manifest
import android.content.Context
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.navigation.NavHostController
import weddellseal.markrecap.R
import weddellseal.markrecap.Screens
import weddellseal.markrecap.models.AddObservationLogViewModel
import weddellseal.markrecap.models.HomeViewModel
import weddellseal.markrecap.ui.components.CensusDialog
import weddellseal.markrecap.ui.components.DropdownField

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
    val state = viewModel.uiState
    var showCensusDialog by remember { mutableStateOf(false) }
    val locationList by viewModel.locations.collectAsState()
    val observerList by viewModel.observers.collectAsState()

    LaunchedEffect(Unit) {
       if (locationList.isEmpty()) {
           viewModel.fetchLocations()
       }

        //TODO, add in line for observers once data is available
    }

    // Register ActivityResult to request Location permissions
    val requestFilePermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.onPermissionChange(Manifest.permission.READ_EXTERNAL_STORAGE, isGranted)
//                viewModel.fetchCurrentLocation()
            } else {
                //coroutineScope.launch {
                //    snackbarHostState.showSnackbar("Location currently disabled due to denied permission.")
                //}
            }
        }

    // Add explanation dialog for File permissions
    var showExplanationDialogForReadAccessPermission by remember { mutableStateOf(false) }
    if (showExplanationDialogForReadAccessPermission) {
        FileAccessExplanationDialog(
            onConfirm = {
                requestFilePermissions.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                showExplanationDialogForReadAccessPermission = false
//                isPermissionGranted = true
            },
            onDismiss = {
                showExplanationDialogForReadAccessPermission = false
//                isPermissionGranted = true
            },
            title = "File access",
            text = "Weddell Seal Mark Recap app would like access to your stored files",
        )
    }

    // Add explanation dialog for file name validation error
    var showExplanationDialogForFileMatchError by remember { mutableStateOf(false) }
    if (showExplanationDialogForFileMatchError) {
        FileAccessExplanationDialog(
            onConfirm = {
                showExplanationDialogForFileMatchError = false
            },
            onDismiss = { showExplanationDialogForFileMatchError = false },
            title = "Error",
            text = "File name does not match! Please rename your file to Colony_Locations.csv and try again!"
        )
    }

    Scaffold(
        // region UI - Top Bar & Action Button
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Row(
                        Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(20.dp),
                            text = "Weddell Seal Mark Recap",
                            style = MaterialTheme.typography.titleLarge
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
                    val contentColor = MaterialTheme.colorScheme.primary
                    IconButton(
                        onClick = {
                            navController.navigate(Screens.AdminScreen.route)
                        },
                        enabled = true
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
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
                .scrollable(rememberScrollState(), Orientation.Vertical)
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
                            var observerSelected by remember { mutableStateOf(obsViewModel.uiState.observerInitials) }
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
                                DropdownField(
                                    observerList,
                                    observerSelected
                                ) { valueSelected ->
                                    observerSelected = valueSelected
                                    obsViewModel.updateObserverInitials(valueSelected)
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
                            var observationSiteSelected by remember { mutableStateOf(obsViewModel.uiState.colonyLocation) }
                            Column(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth(.5f)
                            ) {
                                Text(text = "Location", style = MaterialTheme.typography.titleLarge)
                            }

                            Column(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth(.8f)
                            ) {
                                DropdownField(
                                    locationList,
                                    observationSiteSelected
                                ) { valueSelected ->
//                                    observationSiteSelected = valueSelected
                                    obsViewModel.updateColonySelection(valueSelected)
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
                                Text(text = deviceName, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    // Show the dialog if showDialog is true
                    if (showCensusDialog) {
                        CensusDialog(
                            obsViewModel,
                            onDismissRequest = { showCensusDialog = false },
                            onConfirmation = { showCensusDialog = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileAccessExplanationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String,
    text: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = text) },
        icon = {
            Icon(
                Icons.Filled.FileUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceTint
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
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