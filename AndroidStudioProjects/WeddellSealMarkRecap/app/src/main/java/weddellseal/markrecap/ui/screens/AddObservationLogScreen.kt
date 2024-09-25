package weddellseal.markrecap.ui.screens

/*
 * Main screen for entering seal data
 */

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BabyChangingStation
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.Screens
import weddellseal.markrecap.data.ObservationLogEntry
import weddellseal.markrecap.models.AddObservationLogViewModel
import weddellseal.markrecap.models.RecentObservationsViewModel
import weddellseal.markrecap.models.WedCheckViewModel
import weddellseal.markrecap.ui.components.ConfirmEditDialog
import weddellseal.markrecap.ui.components.IneligibleForSaveDialog
import weddellseal.markrecap.ui.components.ObservationItem
import weddellseal.markrecap.ui.components.RemoveDialog
import weddellseal.markrecap.ui.components.SealCard
import weddellseal.markrecap.ui.components.SealInvalidDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddObservationLogScreen(
    navController: NavHostController,
    viewModel: AddObservationLogViewModel,
    wedCheckViewModel: WedCheckViewModel,
    recentObsViewModel: RecentObservationsViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val recentObsState = recentObsViewModel.uiState
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    context.contentResolver
    var observationToEdit by remember { mutableStateOf<ObservationLogEntry?>(null) }

    // seals not "by remember" because the screen needs to respond to input that changes the seal's model values
    val primarySeal = viewModel.primarySeal
    val pupOne = viewModel.pupOne
    val pupTwo = viewModel.pupTwo

    var showConfirmEntryDialog by remember { mutableStateOf(false) }
    var showIneligibleDialog by remember { mutableStateOf(false) }
    var ineligibleReason by remember { mutableStateOf("") }
    var coordinates by remember { mutableStateOf(viewModel.uiState.currentLocation) }
    var colony by remember { mutableStateOf(viewModel.uiState.colonyLocation) }
    var census by remember { mutableStateOf(viewModel.uiState.censusNumber) }
    var isCensusMode by remember { mutableStateOf(false) }
    var isPrefilled by remember { mutableStateOf(false) }

    // Register ActivityResult to request Location permissions
    val requestLocationPermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.onPermissionChange(ACCESS_FINE_LOCATION, isGranted)
                viewModel.fetchCurrentLocation()
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Location currently disabled due to denied permission.")
                }
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
        recentObsViewModel.observationsFlow.collect {
            recentObsState.observations = it
        }
    }

    // this should be current for every observation
    LaunchedEffect(Unit) {
        // preload the model with location data
        canAddLocation()
    }

    // this should be current for every observation
    LaunchedEffect(viewModel.uiState.isPrefilled) {
        isPrefilled = viewModel.uiState.isPrefilled
    }

    LaunchedEffect(viewModel.uiState.currentLocation) {
        coordinates = viewModel.uiState.currentLocation
    }

    LaunchedEffect(viewModel.uiState.colonyLocation) {
        colony = viewModel.uiState.colonyLocation
    }

    LaunchedEffect(viewModel.uiState.censusNumber) {
        census = viewModel.uiState.censusNumber
    }

    LaunchedEffect(viewModel.uiState.isCensusMode) {
        isCensusMode = viewModel.uiState.isCensusMode
    }

    // this should be activated after the saveAction() is triggered
    LaunchedEffect(viewModel.uiState.isValidated) {
        var showDialog = false
        if (viewModel.uiState.isValidated && !viewModel.uiState.validEntry) {
            showDialog = true
        }
        showConfirmEntryDialog = showDialog
    }

    LaunchedEffect(viewModel.uiState.isSaved) {
        if (viewModel.uiState.isSaved) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Entry successfully saved!")
            }
            viewModel.resetSaved()
        }
    }

    fun checkSaveEnabled(): Boolean {

        // High-level checks
        val metadataSelectionsNeeded = StringBuilder()
        var eligible = true
        if (viewModel.uiState.colonyLocation == "Select an option") {
            metadataSelectionsNeeded.append("Select a colony on the Home Screen.")
            eligible = false
        }
        if (isCensusMode && census == "Select an option") {
            metadataSelectionsNeeded.append("\nSelect a census number on the Home Screen.")
            eligible = false
        }
        if (viewModel.uiState.observerInitials == "Select an option") {
            metadataSelectionsNeeded.append("\nSelect observer(s) on the Home Screen.")
            eligible = false
        }
        if (!eligible) {
            ineligibleReason = metadataSelectionsNeeded.toString()
            return false
        }

        // Seal checks
        val sealSelectionsNeeded = StringBuilder()
        if (primarySeal.age.isEmpty()) {
            sealSelectionsNeeded.append("\nSelect an age for Seal.")
            eligible = false
        } else if (primarySeal.age == "Pup" && primarySeal.condition.isEmpty()) {
            sealSelectionsNeeded.append("\nSelect condition for Pup.")
            eligible = false
        }
        if (primarySeal.sex.isEmpty()) {
            sealSelectionsNeeded.append("\nSelect a sex for Seal.")
            eligible = false
        }
        if (primarySeal.numRelatives.isEmpty()) {
            sealSelectionsNeeded.append("\nSelect relatives for Seal.")
            eligible = false
        }
        if (primarySeal.tagEventType.isEmpty()) {
            sealSelectionsNeeded.append("\nSelect an event type for Seal.")
            eligible = false
        }
        val tagNumberLength = primarySeal.tagNumber.length
        if (!primarySeal.isNoTag) { // when No Tag isn't selected
            if (tagNumberLength != 3 && tagNumberLength != 4) { // check that the Tag ID field is the right length
                sealSelectionsNeeded.append("\nSeal tag number needs to be 3 or 4 digits.")
                eligible = false
            }
            if (primarySeal.numTags == "") { // check that the technician has indicated how many tags are present
                sealSelectionsNeeded.append("\nSelect the number of tags for Seal.")
                eligible = false
            }
        }

        if (!eligible) {
            ineligibleReason = sealSelectionsNeeded.toString()
            return false
        }

        val pupOneSelectionsNeeded = StringBuilder()
        if (pupOne.isStarted) {
            if (pupOne.age.isEmpty()) {
                pupOneSelectionsNeeded.append("\nSelect an age for Pup One.")
                eligible = false
            }
            if (pupOne.age == "Pup" && pupOne.condition.isEmpty()) {
                pupOneSelectionsNeeded.append("\nSelect condition for Pup One.")
                eligible = false
            }
            if (pupOne.sex.isEmpty()) {
                pupOneSelectionsNeeded.append("\nSelect a sex for Pup One.")
                eligible = false
            }
            if (pupOne.numRelatives.isEmpty()) {
                pupOneSelectionsNeeded.append("\nNumber of relatives is empty for Pup One.")
                eligible = false
            }
            if (pupOne.tagEventType.isEmpty()) {
                pupOneSelectionsNeeded.append("\nSelect a tag event for Pup One.")
                eligible = false
            }
            val tagNumberLenPupOne = pupOne.tagNumber.length
            if (!pupOne.isNoTag) { // when No Tag isn't selected
                if (tagNumberLenPupOne != 3 && tagNumberLenPupOne != 4) { // check that the Tag ID field is the right length
                    pupOneSelectionsNeeded.append("\nPup One tag number needs to be 3 or 4 digits.")
                    eligible = false
                }
                if (pupOne.numTags == "") { // check that the technician has indicated how many tags are present
                    pupOneSelectionsNeeded.append("\nSelect the number of tags for Pup One.")
                    eligible = false
                }
            }
        }
        if (!eligible) {
            ineligibleReason = pupOneSelectionsNeeded.toString()
            return false
        }

        val pupTwoSelectionsNeeded = StringBuilder()
        if (pupTwo.isStarted) {
            if (pupTwo.age.isEmpty()) {
                pupTwoSelectionsNeeded.append("\nSelect an age for Pup Two.")
                eligible = false
            }
            if (pupTwo.age == "Pup" && pupTwo.condition.isEmpty()) {
                pupTwoSelectionsNeeded.append("\nSelect condition for Pup Two.")
                eligible = false
            }
            if (pupTwo.sex.isEmpty()) {
                pupTwoSelectionsNeeded.append("\nSelect a sex for Pup Two.")
                eligible = false
            }
            if (pupTwo.numRelatives.isEmpty()) {
                pupTwoSelectionsNeeded.append("\nNumber of relatives is empty for Pup Two.")
                eligible = false
            }
            if (pupTwo.tagEventType.isEmpty()) {
                pupTwoSelectionsNeeded.append("\nSelect a tag event for Pup Two.")
                eligible = false
            }
            val tagNumberLenPupTwo = pupTwo.tagNumber.length
            if (!pupTwo.isNoTag) { // when No Tag isn't selected
                if (tagNumberLenPupTwo != 3 && tagNumberLenPupTwo != 4) { // check that the Tag ID field is the right length
                    pupTwoSelectionsNeeded.append("\nPup Two tag number needs to be 3 or 4 digits.")
                    eligible = false
                }
                if (pupTwo.numTags == "") { // check that the technician has indicated how many tags are present
                    pupTwoSelectionsNeeded.append("\nSelect the number of tags for Pup Two.")
                    eligible = false
                }
            }
        }
        if (!eligible) {
            ineligibleReason = pupOneSelectionsNeeded.toString()
            return false
        }

        // All conditions passed
        return true
    }

    // validates the seal against the wedcheck seal if present
    fun saveAction() {
        if (primarySeal.isStarted) {
            viewModel.validate(primarySeal)
        }

        if (pupOne.isStarted) {
            viewModel.validate(pupOne)
        }

        if (pupTwo.isStarted) {
            viewModel.validate(pupTwo)
        }

        if (viewModel.uiState.isValidated && viewModel.uiState.validEntry) {
            viewModel.createLog()
        }
    }

    // COMPOSABLES

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
                                    if (isCensusMode) "Census #$census" else "Tag/Retag",
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
                                    checked = isCensusMode,
                                    onCheckedChange = {
                                        viewModel.updateIsObservationMode(it)
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

            // METADATA SECTION
            Box() {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp)
                ) {
                    Text(
                        text = "GPS Coordinates:",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            // GPS Location & Updated Timestamp
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box() {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (viewModel.uiState.hasGPS && coordinates != "") {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF1D9C06),
                                modifier = Modifier
                                    .padding(start = 10.dp, end = 10.dp)
                                    .size(48.dp), // Change the size here

                            )
                        } else {
                            Icon(
                                Icons.Filled.LocationOff,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier
                                    .padding(start = 10.dp, end = 10.dp)
                                    .size(48.dp), // Change the size here
                            )
                        }
                        Text(
                            text = coordinates,
                            style = MaterialTheme.typography.titleMedium,
                        )
//                        val contentColor =
//                            if (primarySeal.isStarted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
//                                alpha = ContentAlpha.disabled
//                            )

                        ExtendedFloatingActionButton(
                            modifier = Modifier
                                .padding(start = 30.dp, end = 20.dp)
                                .fillMaxWidth(),
                            containerColor = Color.LightGray,
                            onClick = {
                                if (checkSaveEnabled()) {
                                    canAddLocation() // refresh the gps coordinates on save
                                    saveAction()
                                } else {
                                    showIneligibleDialog = true
                                }
                            },
                            icon = { Icon(Icons.Filled.Save, "Save Seal") },
                            text = {
                                Text(
                                    text = "Save",
                                    fontSize = 18.sp, // Set your desired text size here
                                    fontWeight = FontWeight.Bold, // Optional: set the font weight
                                    color = Color.Black // Optional: set the text color
                                )
                            }
                        )

                    }
                }
            }
            // Colony Location
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box() {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, top = 20.dp)
                    ) {
                        Text(
                            text = "Colony:  $colony",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            // CENSUS METADATA
            if (viewModel.uiState.censusNumber != "Select an option" && isCensusMode) {
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
                                    .alpha(if (isPrefilled) 0.5f else 1f) // Change opacity when inactive
                                    .clickable(enabled = !isPrefilled) { } // Disable clicks if already selected
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
                                    .alpha(if (isPrefilled) 0.5f else 1f) // Change opacity when inactive
                                    .clickable(enabled = !isPrefilled) { } // Disable clicks if already selected
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
                                    .alpha(if (isPrefilled) 0.5f else 1f) // Change opacity when inactive
                                    .clickable(enabled = !isPrefilled) { } // Disable clicks if already selected
                            )
                        }
                    }
                }
            }

            // SEAL CARDS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
//                    .heightIn(max = 200.dp) // Limit the height of TabbedCards
            ) {
                TabbedCards(viewModel, wedCheckViewModel)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
//                    .heightIn(max = 200.dp) // Limit the height of TabbedCards
            ) {
                // RECENT OBSERVATIONS VIEW
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
                            items(recentObsState.observations) { observation ->
                                ObservationItem(
                                    onEditDo = {
                                        if (!viewModel.primarySeal.isStarted) {
                                            showEditDialog = true
                                            observationToEdit = observation
                                        } else {
                                            // Show a Toast message if the seal is already started
                                            Toast.makeText(context, "Looks like you're already editing another seal! Save or clear, then edit this record.", Toast.LENGTH_LONG).show()
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

        // because this action results in removing any entered data
        // Show the dialog if showDialog is true
        if (showConfirmEntryDialog) {
            SealInvalidDialog(
                viewModel,
                onDismissRequest = {
                    showConfirmEntryDialog = false
                    viewModel.clearValidationState()
                },
                onConfirmation = {
                    canAddLocation() // refresh the gps coordinates

                    // flag seals for review
                    if (!primarySeal.isValid) {
                        viewModel.flagSealForReview(primarySeal.name)
                    }
                    if (!pupOne.isValid) {
                        viewModel.flagSealForReview(pupOne.name)
                    }
                    if (!pupTwo.isValid) {
                        viewModel.flagSealForReview(pupTwo.name)
                    }

                    showConfirmEntryDialog = false

                    viewModel.createLog()
                },
            )
        }

        if (showIneligibleDialog) {
            IneligibleForSaveDialog(
                ineligibleReason,
                onDismissRequest = {
                    showIneligibleDialog = false
                }
            )
        }

        // Show the dialog if showDialog is true
        if (showEditDialog) {
            ConfirmEditDialog(
                onDismissRequest = {
                    showEditDialog = false
                },
                onConfirmation = {
                    showEditDialog = false
                    Toast.makeText(context, "You are about to edit this seal. To edit relatives, select records for editing separately.", Toast.LENGTH_LONG).show()

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

data class TabItem(
    val title: String,
    val sealName: String,
    val content: @Composable () -> Unit
)

@Composable
fun TabbedCards(
    viewModel: AddObservationLogViewModel,
    wedCheckViewModel: WedCheckViewModel
) {
    val showDeleteDialog = remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var tabItems by remember {
        mutableStateOf(
            createTabItems(viewModel, wedCheckViewModel)
        )
    }

    // Render the tabs list based on changes with number of relatives or pups started
    LaunchedEffect(
        viewModel.primarySeal.numRelatives,
        viewModel.pupOne.isStarted,
        viewModel.pupTwo.isStarted
    ) {
        tabItems = createTabItems(viewModel, wedCheckViewModel)

        // Ensure selectedTabIndex is within bounds after updating the list
        if (selectedTabIndex >= tabItems.size) {
            selectedTabIndex = tabItems.lastIndex.coerceAtLeast(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabItems.forEachIndexed { index, tabItem ->
                Tab(
                    text = {
                        Text(
                            tabItem.title, style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(horizontal = 20.dp) // Adjust padding here
                        )
                    },
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .border(
                    border = BorderStroke(
                        width = 2.dp,
                        color = Color.LightGray // Use a solid color for the border
                    ),
                    shape = RoundedCornerShape(8.dp) // Add rounded corners here
                )
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        showDeleteDialog.value = true
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove Tab",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // CONTENT
            Column(modifier = Modifier.fillMaxWidth()) {
                if (tabItems.isNotEmpty()) {
                    tabItems[selectedTabIndex].content()
                }
            }

            // Show the dialog if showDialog is true
            if (showDeleteDialog.value) {
                RemoveDialog(
                    onDismissRequest = { showDeleteDialog.value = false },
                    onConfirmation = {
                        if (tabItems.isNotEmpty()) {
                            // remove the current seal
                            viewModel.resetSeal(tabItems[selectedTabIndex].sealName)
                            showDeleteDialog.value = false
                        }
                    },
                )
            }
        }
    }
}

fun createTabItems(
    viewModel: AddObservationLogViewModel,
    wedCheckViewModel: WedCheckViewModel
): List<TabItem> {
    val items = mutableListOf<TabItem>()
    items.add(TabItem("Seal", viewModel.primarySeal.name) {
        SealCard(
            viewModel,
            viewModel.primarySeal,
            wedCheckViewModel
        )
    })

    if (viewModel.pupOne.isStarted) {
        items.add(TabItem("Pup One", viewModel.pupOne.name) {
            SealCard(
                viewModel,
                viewModel.pupOne,
                wedCheckViewModel
            )
        })
    }

    if (viewModel.pupTwo.isStarted) {
        items.add(TabItem("Pup Two", viewModel.pupTwo.name) {
            SealCard(
                viewModel,
                viewModel.pupTwo,
                wedCheckViewModel
            )
        })
    }

    return items
}
