package weddellseal.markrecap.ui

/*
 * Main screen for entering seal data
 */

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.Screens
import weddellseal.markrecap.models.AddLogViewModelFactory
import weddellseal.markrecap.models.AddObservationLogViewModel
import weddellseal.markrecap.models.WedCheckViewModel
import weddellseal.markrecap.ui.components.SealSearchField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddObservationLogScreen(
    navController: NavHostController,
    wedCheckViewModel: WedCheckViewModel,
    viewModel: AddObservationLogViewModel = viewModel(factory = AddLogViewModelFactory())
) {
    val state = viewModel.uiState
//    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val adultSeal = viewModel.adultSeal
    val pupOne = viewModel.pupOne
    val pupTwo = viewModel.pupTwo

    // Register ActivityResult to request Location permissions
    val requestLocationPermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
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
        } else {
            showExplanationDialogForLocationPermission = true
        }
    }

    LaunchedEffect(Unit) {
        // preload the model with location data
        canAddLocation()
    }

    //send the user back to the observation screen when a log is saved
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            navController.navigate(Screens.AddObservationLog.route) { }
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

    var showSealLookup by remember { mutableStateOf(true)}
    var showAdult by remember { mutableStateOf(true) }
    var showPup by remember { mutableStateOf(false) }
    var showPupTwo by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
        if (showSummary) {
                TopAppBar(title = { Text("Back", fontFamily = FontFamily.Serif) },
                    navigationIcon = {
                        if (navController.previousBackStackEntry != null) {
                            if (showSummary) {
                                IconButton(onClick = { showSummary = false }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to Observation"
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                BottomNavigation(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    BottomNavigationItem(
                        label = { Text(text = "Home") },
                        selected = false,
                        onClick = { navController.navigate(Screens.HomeScreen.route) },
                        icon = { Icon(Icons.Filled.Home, null) }
                    )
                    if (!showSummary) {
                        if (showAdult) {
                            // PUP BUTTON - LIVE
                            if (adultSeal.numRelatives >= 1 && !pupOne.isStarted) {
                                BottomNavigationItem(
                                    label = { Text(text = "Add Pup") },
                                    selected = false,
                                    onClick = {
                                        viewModel.startPup(pupOne)
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

                        val contentColor =
                            if (adultSeal.isStarted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = ContentAlpha.disabled
                            )
                        BottomNavigationItem(
                            enabled = adultSeal.isStarted,
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
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (!adultSeal.isStarted) {
                SealLookupRow(wedCheckViewModel = wedCheckViewModel)
            }
            if (!showSummary) {
                SealCard(viewModel, adultSeal, showAdult)
                if (pupOne.isStarted) {
                    SealCard(viewModel, pupOne, showPup)
                }
                if (pupTwo.isStarted) {
                    SealCard(viewModel, pupTwo, showPupTwo)
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
                    icon = { Icon(Icons.Filled.Save, "Save and Start New Observation") },
                    text = { Text(text = "Save and Start New Observation") }
                )
            }
        }
    }
}

@Composable
fun SealLookupRow (wedCheckViewModel: WedCheckViewModel) {
    // SEAL LOOKUP
    Row(
        modifier = Modifier
            .padding(6.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        var sealSpeno by remember { mutableStateOf("") }
        //TODO, add the ability to hide the field and display a spinner if searching
        // Call SealSearchField and pass the lambda to update sealSpeno
        SealSearchField { value ->
            sealSpeno = value
        }
        IconButton(
            onClick = {
                // TODO, launch search
                if (sealSpeno != null) {
                    try {
                        val spenoInt = sealSpeno.toInt()
                        wedCheckViewModel.findSeal(spenoInt)
                    } catch (e: NumberFormatException) {
                        //TODO, throw up an error window if the speno is not an int?
                        println("String cannot be parsed as an integer")
                    }
                }
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
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





