package weddellseal.markrecap.ui.tagretag

/*
 * Main screen for entering seal data
 */

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BabyChangingStation
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import weddellseal.markrecap.Screens
import weddellseal.markrecap.frameworks.room.observations.ObservationLogEntry
import weddellseal.markrecap.models.HomeViewModel
import weddellseal.markrecap.models.RecentObservationsViewModel
import weddellseal.markrecap.models.SealLookupViewModel
import weddellseal.markrecap.models.TagRetagModel
import weddellseal.markrecap.ui.ConfirmEditDialog
import weddellseal.markrecap.ui.ObservationItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagRetagScreen(
    navController: NavHostController,
    viewModel: TagRetagModel,
    homeViewModel: HomeViewModel,
    sealLookupViewModel: SealLookupViewModel,
    recentObsViewModel: RecentObservationsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()

    val currentObservations by recentObsViewModel.currentObservations.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    context.contentResolver
    var observationToEdit by remember { mutableStateOf<ObservationLogEntry?>(null) }

    val primarySeal by viewModel.primarySeal.collectAsState()
    val primaryWedCheckSeal by viewModel.primaryWedCheckSeal.collectAsState()
    val pupOneSeal by viewModel.pupOne.collectAsState()
    val pupOneWedCheckSeal by viewModel.pupOneWedCheckSeal.collectAsState()
    val pupTwoSeal by viewModel.pupTwo.collectAsState()
    val pupTwoWedCheckSeal by viewModel.pupTwoWedCheckSeal.collectAsState()

//    var showConfirmEntryDialog by remember { mutableStateOf(false) }
//    var showIneligibleDialog by remember { mutableStateOf(false) }
//    var ineligibleReason by remember { mutableStateOf("") }

    // TODO, remove once location testing is complete
//    LaunchedEffect(location) {
//        Log.d("UI", "Observed location: $location")
//    }

    // this should be activated after the saveAction() is triggered
//    LaunchedEffect(viewModel.uiState.isValidated) {
//        var showDialog = false
//        if (viewModel.uiState.isValidated && !viewModel.uiState.validEntry) {
//            showDialog = true
//        }
//        showConfirmEntryDialog = showDialog
//    }
//
//    LaunchedEffect(viewModel.uiState.isSaved) {
//        if (viewModel.uiState.isSaved) {
//            coroutineScope.launch {
//                snackbarHostState.showSnackbar("Entry successfully saved!")
//            }
//            viewModel.resetSaved()
//        }
//    }

    // validates the seal against the wedcheck seal if present
//    fun saveAction() {
//        if (primarySeal.isStarted) {
//            viewModel.validate(primarySeal, primaryWedCheckSeal)
//        }
//
//        if (pupOneSeal.isStarted) {
//            viewModel.validate(pupOneSeal, pupOneWedCheckSeal)
//        }
//
//        if (pupTwoSeal.isStarted) {
//            viewModel.validate(pupTwoSeal, pupTwoWedCheckSeal)
//        }
//
//        if (viewModel.uiState.isValidated && viewModel.uiState.validEntry) {
//            viewModel.createLog(location)
//        }
//    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(.9f),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (homeUiState.isCensusMode) "Census #${homeUiState.selectedCensusNumber}" else "Tag/Retag",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontSize = 36.sp // Adjust this value as needed
                                )
                            }
                        }
                        // TOGGLE CENSUS MODE
                        Box(
                            modifier = Modifier
                                .weight(.4f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Census",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(end = 10.dp)
                                )

                                Switch(
                                    checked = homeUiState.isCensusMode,
                                    onCheckedChange = {
                                        homeViewModel.updateIsCensusMode(it)
                                    }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screens.HomeScreen.route) }) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Home",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            TagRetagHeader(viewModel, homeViewModel)

            // TODO, move this to the a Header component specific to Census
            // CENSUS METADATA
            if (homeUiState.selectedCensusNumber.isNotEmpty() && homeUiState.isCensusMode) {
                // Prepopulate Options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box() {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ExtendedFloatingActionButton(
                                icon = {
                                    Icon(
                                        Icons.Filled.BabyChangingStation,
                                        "Mom & Pup",
                                        Modifier.size(36.dp)
                                    )
                                },
                                text = { Text("Mom & Pup") },
                                onClick = {
                                    // update viewModel with prefilled fields
                                    if (primarySeal.age == "") {
                                        viewModel.prefillMomAndPup()
                                    }
                                },
                                modifier = Modifier
                                    .alpha(if (uiState.isPrefilled) 0.5f else 1f) // Change opacity when inactive
                                    .clickable(enabled = !uiState.isPrefilled) { } // Disable clicks if already selected
                            )
                            ExtendedFloatingActionButton(
                                icon = {
                                    Icon(
                                        Icons.Filled.Female,
                                        "Single Female",
                                        Modifier.size(36.dp)
                                    )
                                },
                                text = { Text("Single Female") },
                                onClick = {
                                    // update viewModel with prefilled fields
                                    if (primarySeal.age == "") {
                                        viewModel.prefillSingleFemale()
                                    }
                                },
                                modifier = Modifier
                                    .alpha(if (uiState.isPrefilled) 0.5f else 1f) // Change opacity when inactive
                                    .clickable(enabled = !uiState.isPrefilled) { } // Disable clicks if already selected
                            )
                            ExtendedFloatingActionButton(
                                icon = {
                                    Icon(
                                        Icons.Filled.Male,
                                        "Single Male",
                                        Modifier.size(36.dp)
                                    )
                                },
                                text = { Text("Single Male") },
                                onClick = {
                                    // update viewModel with prefilled fields
                                    if (primarySeal.age == "") {
                                        viewModel.prefillSingleMale()
                                    }
                                },
                                modifier = Modifier
                                    .alpha(if (uiState.isPrefilled) 0.5f else 1f) // Change opacity when inactive
                                    .clickable(enabled = !uiState.isPrefilled) { } // Disable clicks if already selected
                            )
                        }
                    }
                }
            }

            // SEAL CARDS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {

                TabbedCards(viewModel, sealLookupViewModel, primarySeal, pupOneSeal, pupTwoSeal)
            }

            // RECENT OBSERVATIONS VIEW
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recently \nEntered",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp) // Limit the height
                            .padding(10.dp)
                            .border(1.dp, Color.LightGray) // Add border for visual purposes
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            userScrollEnabled = true
                        ) {
                            items(currentObservations) { observation ->
                                ObservationItem(
                                    onEditDo = {
                                        if (!primarySeal.isStarted) {
                                            showEditDialog = true
                                            observationToEdit = observation
                                        } else {
                                            // Show a Toast message if the seal is already started
                                            Toast.makeText(
                                                context,
                                                "Looks like you're already editing another seal! Save or clear, then edit this record.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    onViewDo = {
                                        viewModel.updateObservationEntry(observation)
                                        navController.navigate(Screens.ObservationViewer.route)
                                    },
                                    observation = observation
                                )

                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }

//        // because this action results in removing any entered data
//        // Show the dialog if showDialog is true
//        if (showConfirmEntryDialog) {
//            SealInvalidDialog(
//                viewModel,
//                onDismissRequest = {
//                    showConfirmEntryDialog = false
//                    viewModel.clearValidationState()
//                },
//                onConfirmation = {
////                    canAddLocation() // refresh the gps coordinates
//
//                    // flag seals for review
//                    if (!primarySeal.isValid) {
//                        viewModel.flagSealForReview(primarySeal.name)
//                    }
//                    if (!pupOneSeal.isValid) {
//                        viewModel.flagSealForReview(pupOneSeal.name)
//                    }
//                    if (!pupTwoSeal.isValid) {
//                        viewModel.flagSealForReview(pupTwoSeal.name)
//                    }
//
//                    showConfirmEntryDialog = false
//
//                    viewModel.createLog(location)
//                },
//            )
//        }
//        }

        // Show the dialog if showDialog is true
        if (showEditDialog) {
            ConfirmEditDialog(
                onDismissRequest = {
                    showEditDialog = false
                },
                onConfirmation = {
                    showEditDialog = false
                    Toast.makeText(
                        context,
                        "You are about to edit this seal. To edit relatives, select records for editing separately.",
                        Toast.LENGTH_LONG
                    ).show()

                    // set the seal in the observation view model & navigate to edit
                    if (observationToEdit != null) {
                        viewModel.resetSaved()
                        viewModel.populateSealFromObservation(observationToEdit)
                        navController.navigate(Screens.AddObservationLog.route)
                    }
                },
            )
        }
    }
}

