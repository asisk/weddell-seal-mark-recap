package weddellseal.markrecap.ui

/*
 * Main screen for entering seal data
 */

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.Screens
import weddellseal.markrecap.model.AddLogViewModelFactory
import weddellseal.markrecap.model.AddObservationLogViewModel
import weddellseal.markrecap.ui.theme.WeddellSealMarkRecapTheme


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AddObservationLogScreen(
    navController: NavHostController,
    viewModel: AddObservationLogViewModel = viewModel(factory = AddLogViewModelFactory())
) {
    // region State initialization
    val state = viewModel.uiState
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val adultSeal = viewModel.adultSeal
    val pupOne = viewModel.pupOne
    val pupTwo = viewModel.pupTwo
    val internalPhotoPickerState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
//    var gpsData by remember { mutableStateOf("No data") }
    // endregion

    // Register ActivityResult to request Location permissions
    val requestLocationPermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                //viewModel.onPermissionChange(ACCESS_COARSE_LOCATION, isGranted)
                viewModel.onPermissionChange(ACCESS_FINE_LOCATION, isGranted)
                viewModel.fetchCurrentLocation()
            } else {
                //coroutineScope.launch {
                //    snackbarHostState.showSnackbar("Location currently disabled due to denied permission.")
                //}
            }
        }

    // Add explanation dialog for Location permissions
    var showExplanationDialogForLocationPermission by remember { mutableStateOf(false) }
    if (showExplanationDialogForLocationPermission) {
        LocationExplanationDialog(
            onConfirm = {
                requestLocationPermissions.launch(ACCESS_FINE_LOCATION)
                showExplanationDialogForLocationPermission = false
                viewModel.fetchCurrentLocation()
//                viewModel.fetchGeoCoderLocation()
            },
            onDismiss = { showExplanationDialogForLocationPermission = false },
        )
    }

    // method called on the initial load of the ObservationLog Screen
    // if permissions are in place it gathers information about the
    // current and last known locations to populate location fields
    fun canAddLocation() {
        if (viewModel.hasPermission(ACCESS_FINE_LOCATION)) {
            viewModel.fetchCurrentLocation()
//            viewModel.fetchGeoCoderLocation()
        } else {
            //requestLocationPermissions.launch(ACCESS_FINE_LOCATION)
            showExplanationDialogForLocationPermission = true
        }
    }

    // region helper functions

    LaunchedEffect(Unit) {
        // preload the model with location data
        canAddLocation()
//        gpsData = viewModel.uiState.currentLocation
    }

    //send the user back to the home screen when a log is saved
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            navController.navigate(Screens.HomeScreen.route) {
                popUpTo(Screens.HomeScreen.route) {
                    inclusive = false
                }
            }
        }
    }

    fun canSaveLog(callback: () -> Unit) {
        if (viewModel.isValid()) {
            callback()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Successfully saved!")
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("You haven't completed all details")
            }
        }
    }
    // endregion

    var showAdult by remember { mutableStateOf(true) }
    var showPup by remember { mutableStateOf(false) }
    var showPupTwo by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // region UI - Top Bar & Action Button
        topBar = {
            TopAppBar(title = { Text("Back", fontFamily = FontFamily.Serif) },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        if (showSummary) {
                            IconButton(onClick = { showSummary = false }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        } else {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                if (!showSummary) {
                    BottomNavigation(
                        modifier = Modifier.fillMaxSize(),
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        val contentColor =
                            if (adultSeal.isStarted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = ContentAlpha.disabled
                            )
                        BottomNavigationItem(
                            enabled = adultSeal.isStarted,
                            modifier = Modifier.clickable {
                                if (adultSeal.isStarted) {
//                                canSaveLog {
//                                    viewModel.createLog()
//                                }
                                }
                            },
                            label = { Text(text = "Save", color = contentColor) },
                            selected = false,
                            onClick = {
                                if (adultSeal.isStarted) {
                                    viewModel.updateNotebookEntry(adultSeal)
                                    viewModel.updateNotebookEntry(pupOne)
                                    showSummary = true
                                }
                            },
                            icon = {
                                if (state.isSaving) {
                                    CircularProgressIndicator(Modifier.size(24.0.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = contentColor
                                    )
                                }
                            }
                        )

                        if (showAdult) {
                            // PUP BUTTON - LIVE
                            if (adultSeal.numRelatives == 1 && !pupOne.isStarted) {
                                BottomNavigationItem(
                                    label = { Text(text = "Add Pup") },
                                    selected = false,
                                    onClick = {
                                        showAdult = false
                                        showPup = true
                                    },
                                    icon = { Icon(Icons.Filled.Add, null) }
                                )
                            }
                            if (pupOne.isStarted) {
                                BottomNavigationItem(
                                    label = { Text(text = "View Pup") },
                                    selected = false,
                                    onClick = {
                                        showAdult = false
                                        showPup = true
                                    },
                                    icon = { Icon(Icons.Filled.ArrowUpward, null) }
                                )
                            }
                        } else {
                            BottomNavigationItem(
                                label = { Text(text = "View Adult") },
                                selected = false,
                                onClick = {
                                    showAdult = true
                                    showPup = false
                                },
                                icon = { Icon(Icons.Filled.ArrowUpward, null) })
                        }
                    }
                }
            }
        }
        // endregion
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (!showSummary) {
                if (showAdult) {
                    SealCard(viewModel, adultSeal)
                }
                if (showPup) {
                    // show new card and show summary fields from parent
                    SealCard(viewModel, pupOne)
                }
                if (showPupTwo) {
                    // show new card and show summary fields from parent
                    SealCard(viewModel, pupTwo)
                }
            } else {
                SummaryCard(viewModel, adultSeal, pupOne, pupTwo)
                ExtendedFloatingActionButton(
                    modifier = Modifier.padding(16.dp),
                    containerColor = Color.LightGray,
                    onClick = {
                        canSaveLog {
                            viewModel.createLog()
                        }
                    },
                    icon = { Icon(Icons.Filled.Save, "Continue Saving") },
                    text = { Text(text = "Continue Saving") }
                )
            }

//                // NOTEBOOK DATA DISPLAY
//                val adultNotebookDataString = getSealNotebookEntry(adultSeal)
//                val pupNotebookDataString = getSealNotebookEntry(pupOne)

//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth(.7f)
//                        .padding(10.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    // ADULT NOTEBOOK DISPLAY
//                    ListItem(
//                        headlineContent = {
//                            Text(
//                                "Adult Seal Details: ",
//                                style = MaterialTheme.typography.titleLarge
//                            )
//                        },
//                        trailingContent = {
//                            Text(text = adultNotebookDataString, style = MaterialTheme.typography.titleLarge)
//                        }
//                    )
//                    // PUP NOTEBOOK DISPLAY
//                    if (pupOne.isStarted) {
//                        ListItem(
//                            headlineContent = {
//                                Text(
//                                    "Pup One Details: ",
//                                    style = MaterialTheme.typography.titleLarge
//                                )
//                            },
//                            trailingContent = {
//                                Text(text = pupNotebookDataString, style = MaterialTheme.typography.titleLarge)
//                            }
//                        )
//                    }
//                }
            // GPS LOCATION - SYSTEM
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth(.7f)
//                        .padding(10.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth(.8f)
//                        .padding(15.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    if (viewModel.uiState.latLong.isNotEmpty()) {
//                        ListItem(
//                            headlineContent = {
//                                Text(
//                                    "Device GPS",
//                                    style = MaterialTheme.typography.titleMedium
//                                )
//                            },
//                            trailingContent = { Text(text = viewModel.uiState.latLong, style = MaterialTheme.typography.titleMedium) }
//                        )
//                    }
//                }
        }
    }
}

@Composable
fun LocationExplanationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location access") },
        text = { Text("Weddell Seal Mark Recap app would like access to your location to save it when creating a log") },
        icon = {
            Icon(
                Icons.Filled.Explore,
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

@Preview
@Composable
fun ObservationScreen() {
    WeddellSealMarkRecapTheme {
    }
}





